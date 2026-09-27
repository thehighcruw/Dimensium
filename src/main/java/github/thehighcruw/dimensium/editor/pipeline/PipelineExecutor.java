/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public final class PipelineExecutor {

    public static BlockMap execute(PipelineGraph graph, PipelineContext context) {
        List<PipelineGraph.NodeInstance> order = topologicalSort(graph);
        Map<String, PortValues> nodeOutputs = executeNodes(graph, context, order, null);

        BlockMap result = new BlockMap();
        for (PipelineGraph.NodeInstance inst : order) {
            if (graph.getDownstream(inst.instanceId).isEmpty()) {
                PortValues outputs = nodeOutputs.get(inst.instanceId);
                if (outputs != null) {
                    BlockMap blockMap = outputs.get("blocks", BlockMap.class);
                    if (blockMap != null) result.merge(blockMap);
                }
            }
        }
        return result;
    }

    /**
     * Like {@link #execute} but returns the raw leaf output for preview purposes.
     * Returns a {@link Skeleton} if the last leaf produces one, otherwise a merged {@link BlockMap}.
     */
    public static Object executeForPreview(PipelineGraph graph, PipelineContext context) {
        List<PipelineGraph.NodeInstance> order = topologicalSort(graph);
        Map<String, PortValues> nodeOutputs = executeNodes(graph, context, order, null);

        Skeleton lastSkeleton = null;
        Curve lastCurve = null;
        BlockMap result = new BlockMap();
        for (PipelineGraph.NodeInstance inst : order) {
            if (graph.getDownstream(inst.instanceId).isEmpty()) {
                PortValues outputs = nodeOutputs.get(inst.instanceId);
                if (outputs == null) continue;
                BlockMap blockMap = outputs.get("blocks", BlockMap.class);
                if (blockMap != null) result.merge(blockMap);
                Skeleton skeleton = outputs.get("skeleton", Skeleton.class);
                if (skeleton != null) lastSkeleton = skeleton;
                Curve curve = outputs.get("curve", Curve.class);
                if (curve != null) lastCurve = curve;
            }
        }
        if (result.size() == 0 && lastSkeleton != null) return lastSkeleton;
        if (result.size() == 0 && lastCurve != null) return lastCurve;
        return result;
    }

    /**
     * Executes the pipeline up to and including {@code stopNodeId}, then returns the raw outputs of
     * that node. If {@code stopNodeId} is null or not found, falls back to {@link #execute}.
     */
    public static Object executeUpTo(PipelineGraph graph, PipelineContext context, String stopNodeId) {
        if (stopNodeId == null) return execute(graph, context);
        List<PipelineGraph.NodeInstance> order = topologicalSort(graph);
        Map<String, PortValues> nodeOutputs = executeNodes(graph, context, order, stopNodeId);
        PortValues outputs = nodeOutputs.get(stopNodeId);
        if (outputs == null) return new BlockMap();
        BlockMap blockMap = outputs.get("blocks", BlockMap.class);
        if (blockMap != null) return blockMap;
        Skeleton skeleton = outputs.get("skeleton", Skeleton.class);
        if (skeleton != null) return skeleton;
        Curve curve = outputs.get("curve", Curve.class);
        if (curve != null) return curve;
        return new BlockMap();
    }

    private static Map<String, PortValues> executeNodes(
            PipelineGraph graph, PipelineContext context, List<PipelineGraph.NodeInstance> order, String stopNodeId) {

        Map<String, PortValues> nodeOutputs = new LinkedHashMap<>();

        for (PipelineGraph.NodeInstance inst : order) {
            PipelineNode node = NodeRegistry.create(inst.typeId);
            NodeSchema schema = node.schema();

            PortValues inputs = new PortValues();
            for (NodeSchema.InputPortDef inputPort : schema.inputPorts()) {
                PipelineGraph.Edge edge = graph.getUpstreamEdge(inst.instanceId, inputPort.name);
                if (edge != null) {
                    PortValues upstreamOutputs = nodeOutputs.get(edge.fromId);
                    if (upstreamOutputs != null) {
                        Object value = upstreamOutputs.getRaw(edge.fromPort);
                        if (value != null) inputs.set(inputPort.name, value);
                    }
                }
            }
            if (schema.inputPorts().isEmpty()) {
                inputs.set("origin", context.origin);
            }

            NodeParams resolvedParams = resolveParamPortOverrides(graph, nodeOutputs, inst);
            PortValues outputs = new PortValues();
            node.apply(inputs, outputs, resolvedParams, context);
            nodeOutputs.put(inst.instanceId, outputs);

            if (inst.instanceId.equals(stopNodeId)) break;
        }

        return nodeOutputs;
    }

    private static NodeParams resolveParamPortOverrides(
            PipelineGraph graph, Map<String, PortValues> nodeOutputs, PipelineGraph.NodeInstance inst) {
        if (inst.params.exposedParamPorts().isEmpty()) return inst.params;
        NodeParams copy = inst.params.copy();
        for (String key : inst.params.exposedParamPorts()) {
            PipelineGraph.Edge edge = graph.getUpstreamEdge(inst.instanceId, key);
            if (edge == null) continue;
            PortValues upstreamOutputs = nodeOutputs.get(edge.fromId);
            if (upstreamOutputs == null) continue;
            Object value = upstreamOutputs.getRaw(edge.fromPort);
            if (value instanceof Number) {
                copy.set(key, ((Number) value).floatValue());
            }
        }
        return copy;
    }

    private static List<PipelineGraph.NodeInstance> topologicalSort(PipelineGraph graph) {
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (PipelineGraph.NodeInstance n : graph.nodes()) inDegree.put(n.instanceId, 0);
        for (PipelineGraph.Edge e : graph.edges()) inDegree.merge(e.toId, 1, Integer::sum);

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }

        List<PipelineGraph.NodeInstance> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            PipelineGraph.NodeInstance inst = graph.findNode(id);
            if (inst != null) sorted.add(inst);
            for (String downstream : graph.getDownstream(id)) {
                int deg = inDegree.merge(downstream, -1, Integer::sum);
                if (deg == 0) queue.add(downstream);
            }
        }
        return sorted;
    }

    private PipelineExecutor() {}
}
