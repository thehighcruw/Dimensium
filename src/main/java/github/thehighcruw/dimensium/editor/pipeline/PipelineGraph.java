/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PipelineGraph {

    public String name;
    /** Folder path this pipeline belongs to, or {@code null} for the root level. */
    public String folder;

    private final List<NodeInstance> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private int nextNodeCounter = 0;
    private int revision = 0;

    public PipelineGraph(String name) {
        this.name = name;
    }

    public static class NodeInstance {

        public final String instanceId;
        public final String typeId;
        public final NodeParams params;
        public float posX;
        public float posY;

        public NodeInstance(String instanceId, String typeId) {
            this.instanceId = instanceId;
            this.typeId = typeId;
            this.params = new NodeParams();
            NodeRegistry.create(typeId).schema().applyDefaults(this.params);
        }

        NodeInstance(String instanceId, String typeId, NodeParams params, float posX, float posY) {
            this.instanceId = instanceId;
            this.typeId = typeId;
            this.params = params;
            this.posX = posX;
            this.posY = posY;
        }
    }

    public static class Edge {

        public final String fromId;
        public final String fromPort;
        public final String toId;
        public final String toPort;

        Edge(String fromId, String fromPort, String toId, String toPort) {
            this.fromId = fromId;
            this.fromPort = fromPort;
            this.toId = toId;
            this.toPort = toPort;
        }
    }

    public int revision() {
        return revision;
    }

    public void markDirty() {
        revision++;
    }

    public NodeInstance addNode(String typeId, float posX, float posY) {
        String id = "n" + (nextNodeCounter++);
        NodeInstance inst = new NodeInstance(id, typeId);
        inst.posX = posX;
        inst.posY = posY;
        nodes.add(inst);
        revision++;
        return inst;
    }

    /** Connect fromId's fromPort to toId's toPort. Replaces any existing connection on toPort. */
    public void connect(String fromId, String fromPort, String toId, String toPort) {
        edges.removeIf(e -> e.toId.equals(toId) && e.toPort.equals(toPort));
        edges.add(new Edge(fromId, fromPort, toId, toPort));
        revision++;
    }

    /** Returns the edge connected to the given (toId, toPort), or null. */
    public Edge getUpstreamEdge(String toId, String toPort) {
        for (Edge e : edges) {
            if (e.toId.equals(toId) && e.toPort.equals(toPort)) return e;
        }
        return null;
    }

    /** Returns all edges feeding into toId across all input ports. */
    public List<Edge> getUpstreamEdges(String toId) {
        List<Edge> result = new ArrayList<>();
        for (Edge e : edges) {
            if (e.toId.equals(toId)) result.add(e);
        }
        return result;
    }

    public void disconnect(String toId, String toPort) {
        edges.removeIf(e -> e.toId.equals(toId) && e.toPort.equals(toPort));
        revision++;
    }

    /** Disconnects all inputs to toId across all ports. */
    public void disconnectAll(String toId) {
        edges.removeIf(e -> e.toId.equals(toId));
        revision++;
    }

    public void removeNode(String instanceId) {
        nodes.removeIf(n -> n.instanceId.equals(instanceId));
        edges.removeIf(e -> e.fromId.equals(instanceId) || e.toId.equals(instanceId));
        revision++;
    }

    public List<NodeInstance> nodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    public NodeInstance findNode(String instanceId) {
        for (NodeInstance n : nodes) {
            if (n.instanceId.equals(instanceId)) return n;
        }
        return null;
    }

    public List<String> getDownstream(String instanceId) {
        List<String> result = new ArrayList<>();
        for (Edge e : edges) {
            if (e.fromId.equals(instanceId)) result.add(e.toId);
        }
        return result;
    }

    public BlockMap execute(PipelineContext context) {
        return PipelineExecutor.execute(this, context);
    }

    public PipelineGraph deepCopy() {
        return fromJson(toJson());
    }

    // ── JSON serialisation ────────────────────────────────────────────────────

    public String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("name", name);
        if (folder != null) root.addProperty("folder", folder);
        root.addProperty("nextNodeCounter", nextNodeCounter);

        JsonArray nodesArr = new JsonArray();
        for (NodeInstance n : nodes) {
            JsonObject no = new JsonObject();
            no.addProperty("instanceId", n.instanceId);
            no.addProperty("typeId", n.typeId);
            no.addProperty("posX", n.posX);
            no.addProperty("posY", n.posY);
            JsonObject paramsObj = new JsonObject();
            for (Map.Entry<String, Object> entry : n.params.rawValues().entrySet()) {
                paramsObj.add(entry.getKey(), serializeParamValue(entry.getValue()));
            }
            no.add("params", paramsObj);
            nodesArr.add(no);
        }
        root.add("nodes", nodesArr);

        JsonArray edgesArr = new JsonArray();
        for (Edge e : edges) {
            JsonObject eo = new JsonObject();
            eo.addProperty("from", e.fromId);
            eo.addProperty("fromPort", e.fromPort);
            eo.addProperty("to", e.toId);
            eo.addProperty("toPort", e.toPort);
            edgesArr.add(eo);
        }
        root.add("edges", edgesArr);

        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    @SuppressWarnings("unchecked")
    private static JsonElement serializeParamValue(Object value) {
        if (value instanceof Number) return new JsonPrimitive(((Number) value).doubleValue());
        if (value instanceof Boolean) return new JsonPrimitive((Boolean) value);
        if (value instanceof List) {
            JsonArray arr = new JsonArray();
            for (int[] entry : (List<int[]>) value) {
                JsonArray pair = new JsonArray();
                pair.add(new JsonPrimitive(entry[0]));
                pair.add(new JsonPrimitive(entry[1]));
                arr.add(pair);
            }
            return arr;
        }
        return JsonNull.INSTANCE;
    }

    public static PipelineGraph fromJson(String json) {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        PipelineGraph graph = new PipelineGraph(root.get("name").getAsString());
        if (root.has("folder")) graph.folder = root.get("folder").getAsString();
        if (root.has("nextNodeCounter")) {
            graph.nextNodeCounter = root.get("nextNodeCounter").getAsInt();
        }

        for (JsonElement ne : root.getAsJsonArray("nodes")) {
            JsonObject no = ne.getAsJsonObject();
            String instanceId = no.get("instanceId").getAsString();
            String typeId = no.get("typeId").getAsString();
            float posX = no.get("posX").getAsFloat();
            float posY = no.get("posY").getAsFloat();
            NodeParams params = new NodeParams();
            NodeRegistry.create(typeId).schema().applyDefaults(params);
            for (Map.Entry<String, JsonElement> entry :
                    no.getAsJsonObject("params").entrySet()) {
                Object val = deserializeParamValue(entry.getValue());
                if (val != null) params.set(entry.getKey(), val);
            }
            graph.nodes.add(new NodeInstance(instanceId, typeId, params, posX, posY));
        }

        for (JsonElement ee : root.getAsJsonArray("edges")) {
            JsonObject eo = ee.getAsJsonObject();
            String fromId = eo.get("from").getAsString();
            String toId = eo.get("to").getAsString();

            if (eo.has("fromPort") && eo.has("toPort")) {
                // New format with explicit port names
                String fromPort = eo.get("fromPort").getAsString();
                String toPort = eo.get("toPort").getAsString();
                graph.edges.add(new Edge(fromId, fromPort, toId, toPort));
            } else {
                // Legacy format: resolve primary ports from schema
                NodeInstance fromNode = graph.findNode(fromId);
                NodeInstance toNode = graph.findNode(toId);
                if (fromNode != null && toNode != null) {
                    NodeSchema.OutputPortDef outPort =
                            NodeRegistry.create(fromNode.typeId).schema().primaryOutput();
                    NodeSchema.InputPortDef inPort =
                            NodeRegistry.create(toNode.typeId).schema().primaryInput();
                    if (outPort != null && inPort != null) {
                        graph.edges.add(new Edge(fromId, outPort.name, toId, inPort.name));
                    }
                }
            }
        }

        return graph;
    }

    private static Object deserializeParamValue(JsonElement el) {
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            return p.getAsDouble();
        }
        if (el.isJsonArray()) {
            List<int[]> palette = new ArrayList<>();
            for (JsonElement item : el.getAsJsonArray()) {
                JsonArray pair = item.getAsJsonArray();
                palette.add(new int[] {pair.get(0).getAsInt(), pair.get(1).getAsInt()});
            }
            return palette;
        }
        return null;
    }
}
