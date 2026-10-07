/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeRegistry;
import github.thehighcruw.dimensium.editor.pipeline.NodeRegistry.NodeGroup;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema.InputPortDef;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema.OutputPortDef;
import github.thehighcruw.dimensium.editor.pipeline.PipelineGraph;
import github.thehighcruw.dimensium.editor.pipeline.PipelineLibrary;
import github.thehighcruw.dimensium.editor.pipeline.PipelinePresets;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import java.util.List;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class PipelineEditorWindow extends ToggleableWindow {

    public static final PipelineEditorWindow INSTANCE = new PipelineEditorWindow();

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final float NODE_W = 200f;
    private static final float HEADER_H = 24f;
    private static final float PORT_ROW_H = 18f;
    private static final float BODY_PAD = 5f;
    private static final float PORT_R = 5f;
    /** Slack added to PORT_R for hit-testing. */
    private static final float PORT_HIT_SLACK = 3f;

    private static final float PORT_LABEL_PAD = 8f;
    private static final float WIRE_THICKNESS = 2f;
    private static final float ZOOM_MIN = 0.25f;
    private static final float ZOOM_MAX = 3.0f;
    private static final float ZOOM_STEP = 0.1f;
    private static final int BEZIER_SEGMENTS = 24;
    /** Below this zoom, skip port label text. */
    private static final float LOD_PORT_LABELS = 0.5f;
    /** Below this zoom, skip title text. */
    private static final float LOD_TITLE = 0.3f;

    // ── Colors (ABGR) ─────────────────────────────────────────────────────────
    private static final int COLOR_BG = 0xFF_1A_1A_1A;
    private static final int COLOR_DOT = 0xFF_2A_2A_2A;
    private static final int COLOR_NODE_HEADER = 0xFF_3A_3A_3A;
    private static final int COLOR_NODE_BODY = 0xFF_2A_2A_2A;
    private static final int COLOR_NODE_BORDER = 0xFF_55_55_55;
    private static final int COLOR_NODE_SELECTED = 0xFF_AA_88_00;
    private static final int COLOR_SHADOW = 0x44_00_00_00;
    private static final int COLOR_PORT_SKELETON = 0xFF_00_AA_FF;
    private static final int COLOR_PORT_BLOCK_MAP = 0xFF_00_FF_88;
    private static final int COLOR_PORT_FLOAT = 0xFF_AA_AA_AA;
    private static final int COLOR_PORT_VEC3 = 0xFF_00_CC_FF;
    private static final int COLOR_PORT_CURVE = 0xFF_FF_88_00;
    private static final int COLOR_PORT_HIGHLIGHT = 0xFF_FF_FF_FF;
    private static final int COLOR_WIRE = 0xFF_CC_AA_55;
    private static final int COLOR_WIRE_HOVER = 0xFF_FF_CC_66;
    private static final int COLOR_TEXT = 0xFF_E0_E0_E0;
    private static final int COLOR_TEXT_DIM = 0xFF_AA_AA_AA;
    private static final int COLOR_TEXT_OPTIONAL = 0xFF_77_77_77;

    // ── State ─────────────────────────────────────────────────────────────────
    private PipelineGraph graph;
    private String selectedNodeId = null;
    private String draggingNodeId = null;
    private float dragOffsetX;
    private float dragOffsetY;
    private float panX = 0f;
    private float panY = 0f;
    private float canvasZoom = 1.0f;

    // Wire drag state
    private String wireFromNodeId = null;
    private String wireFromPortName = null;
    private boolean wireFromIsOutput = false;
    private float wireFromScreenX;
    private float wireFromScreenY;

    // Hovered port — updated each frame before drawing
    private String hoveredPortNodeId = null;
    private String hoveredPortName = null;
    private boolean hoveredPortIsOutput = false;

    // Hovered wire
    private String hoveredWireToId = null;
    private String hoveredWireToPort = null;

    private final ImString nameBuffer = new ImString(128);
    private PipelineGraph nameBufferGraph = null;

    private PipelineEditorWindow() {}

    public void toggle() {
        if (open) {
            setOpen(false);
        } else {
            if (graph == null) {
                PipelinePresets.registerNodes();
                graph = PipelineLibrary.INSTANCE.createNew(I18n.format("dimensium.pipeline.library.new_name"));
            }
            setOpen(true);
        }
    }

    public void setOpen(boolean value) {
        if (value && graph == null) {
            PipelinePresets.registerNodes();
            graph = PipelineLibrary.INSTANCE.createNew(I18n.format("dimensium.pipeline.library.new_name"));
        }
        open = value;
        DimensiumConfig.setWindowPipelineEditorOpen(value);
    }

    public void open(PipelineGraph graphToEdit) {
        this.graph = graphToEdit;
        this.open = true;
        this.selectedNodeId = null;
        this.wireFromNodeId = null;
        PipelinePresets.registerNodes();
    }

    public String getSelectedNodeId() {
        return selectedNodeId;
    }

    public void renderImGui() {
        if (!open || graph == null) return;

        float uiScale = ImGuiManager.INSTANCE.getUIScale();
        ImGui.setNextWindowSize(900 * uiScale, 620 * uiScale, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowPos(80 * uiScale, 80 * uiScale, ImGuiCond.FirstUseEver);

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 4f * uiScale, 4f * uiScale);
        ImBoolean pOpen = new ImBoolean(true);
        if (ImGui.begin(
                I18n.format("dimensium.ui.pipeline.editor") + "##pipelineEditor",
                pOpen,
                ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse)) {
            captureBounds();
            if (!pOpen.get()) {
                setOpen(false);
                ImGui.end();
                ImGui.popStyleVar();
                return;
            }
            renderToolbar(uiScale);
            ImGui.separator();
            renderCanvas(uiScale);
        }
        ImGui.end();
        ImGui.popStyleVar();

        PipelineNodeDetailsWindow.INSTANCE.setContext(graph, selectedNodeId);
        PipelineNodeDetailsWindow.INSTANCE.renderImGui();
        PipelinePreviewWindow.INSTANCE.setGraph(graph);
        PipelinePreviewWindow.INSTANCE.renderImGui();
    }

    private void renderToolbar(float uiScale) {
        ImGui.setNextItemWidth(160 * uiScale);
        if (nameBufferGraph != graph) {
            nameBuffer.set(graph.name);
            nameBufferGraph = graph;
        }
        if (ImGui.inputText("##pname", nameBuffer)) {
            graph.name = nameBuffer.get();
        }
        ImGui.sameLine();

        if (ImGui.button(I18n.format("dimensium.ui.pipeline.new") + "##pnew")) {
            graph = PipelineLibrary.INSTANCE.createNew("New Pipeline");
            resetView();
        }
        ImGui.sameLine();

        boolean isBuiltin = PipelineLibrary.INSTANCE.isBuiltin(graph);
        if (isBuiltin) {
            if (ImGui.button(I18n.format("dimensium.ui.pipeline.save_as_copy") + "##psave")) {
                PipelineGraph copy = graph.deepCopy();
                copy.name = PipelineLibrary.INSTANCE.uniqueName(graph.name);
                PipelineLibrary.INSTANCE.save(copy);
                graph = copy;
                resetView();
            }
            ImGui.sameLine();
            ImGui.textDisabled("[" + I18n.format("dimensium.ui.pipeline.builtin") + "]");
        } else {
            if (ImGui.button(I18n.format("dimensium.ui.pipeline.save") + "##psave")) {
                PipelineLibrary.INSTANCE.save(graph);
            }
        }
        ImGui.sameLine();

        ImGui.setNextItemWidth(140 * uiScale);
        if (ImGui.beginCombo("##plib", graph.name)) {
            for (PipelineGraph g : PipelineLibrary.INSTANCE.all()) {
                boolean selected = g == graph;
                if (ImGui.selectable(g.name, selected)) {
                    graph = g;
                    resetView();
                }
            }
            ImGui.endCombo();
        }
        ImGui.sameLine();

        if (ImGui.button(I18n.format("dimensium.menu.window.pipeline_preview") + "##pprev")) {
            PipelinePreviewWindow.INSTANCE.setOpen(!PipelinePreviewWindow.INSTANCE.isOpen());
        }
        ImGui.sameLine();

        if (ImGui.button("1:1##pzoomreset")) canvasZoom = 1.0f;
        ImGui.sameLine();
        ImGui.textDisabled(String.format("%.0f%%", canvasZoom * 100f));

        // One button per node group — click opens popup listing that group's nodes (row 2)
        for (NodeGroup group : NodeGroup.values()) {
            String groupKey = "dimensium.ui.pipeline.group." + group.name().toLowerCase();
            String popupId = "##addNodeGroup_" + group.name();
            if (ImGui.button(I18n.format(groupKey) + popupId)) {
                ImGui.openPopup(popupId);
            }
            if (ImGui.beginPopup(popupId)) {
                String lastSubGroup = null;
                for (String typeId : NodeRegistry.idsInGroup(group)) {
                    String subGroupKey = NodeRegistry.subGroupKeyOf(typeId);
                    if (subGroupKey != null && !subGroupKey.equals(lastSubGroup)) {
                        if (lastSubGroup != null) ImGui.separator();
                        ImGui.textDisabled(I18n.format(subGroupKey));
                        lastSubGroup = subGroupKey;
                    }
                    if (ImGui.menuItem(nodeDisplayName(typeId))) {
                        float canvasCenter = 40f + panX / canvasZoom;
                        graph.addNode(typeId, canvasCenter, 40f + panY / canvasZoom);
                        ImGui.closeCurrentPopup();
                    }
                }
                ImGui.endPopup();
            }
            ImGui.sameLine();
        }
    }

    private void resetView() {
        panX = 0;
        panY = 0;
        canvasZoom = 1.0f;
        selectedNodeId = null;
        wireFromNodeId = null;
    }

    // ── Canvas ────────────────────────────────────────────────────────────────

    private void renderCanvas(float uiScale) {
        float viewScale = uiScale * canvasZoom;
        float canvasW = ImGui.getContentRegionAvailX();
        float canvasH = ImGui.getContentRegionAvailY();

        ImGui.beginChild(
                "##pcanvasChild",
                canvasW,
                canvasH,
                false,
                ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        ImVec2 canvasOrigin = new ImVec2();
        ImGui.getCursorScreenPos(canvasOrigin);
        float originX = canvasOrigin.x;
        float originY = canvasOrigin.y;

        ImDrawList dl = ImGui.getWindowDrawList();
        dl.addRectFilled(originX, originY, originX + canvasW, originY + canvasH, COLOR_BG);
        dl.pushClipRect(originX, originY, originX + canvasW, originY + canvasH, true);

        // Dot grid
        float gridStep = 20f * viewScale;
        if (gridStep >= 16f) {
            float gridOffX = panX % gridStep;
            float gridOffY = panY % gridStep;
            for (float x = originX + gridOffX; x < originX + canvasW; x += gridStep) {
                for (float y = originY + gridOffY; y < originY + canvasH; y += gridStep) {
                    dl.addRectFilled(x, y, x + 1.5f, y + 1.5f, COLOR_DOT);
                }
            }
        }

        float mouseX = ImGui.getMousePos().x;
        float mouseY = ImGui.getMousePos().y;

        // Update hovered port (before drawing so highlight renders correctly)
        updateHoveredPort(graph.nodes(), originX, originY, viewScale, mouseX, mouseY);

        // Draw wires
        hoveredWireToId = null;
        hoveredWireToPort = null;
        for (PipelineGraph.Edge edge : graph.edges()) {
            PipelineGraph.NodeInstance fromNode = graph.findNode(edge.fromId);
            PipelineGraph.NodeInstance toNode = graph.findNode(edge.toId);
            if (fromNode == null || toNode == null) continue;

            NodeSchema fromSchema = NodeRegistry.create(fromNode.typeId).schema();
            NodeSchema toSchema = NodeRegistry.create(toNode.typeId).schema();
            int fromIdx = outputPortIndex(fromSchema, edge.fromPort);
            int toIdx = inputPortIndex(toSchema, toNode.params, edge.toPort);

            float fromNodeY = originY + panY + fromNode.posY * viewScale;
            float toNodeY = originY + panY + toNode.posY * viewScale;
            float fromX = originX + panX + fromNode.posX * viewScale + NODE_W * viewScale;
            float fromY = portBodyY(
                    NodeSchema.effectiveInputPorts(fromSchema, fromNode.params).size() + fromIdx, fromNodeY, viewScale);
            float toX = originX + panX + toNode.posX * viewScale;
            float toY = portBodyY(toIdx, toNodeY, viewScale);

            // Cull wires fully outside the viewport
            if (Math.max(fromX, toX) < originX
                    || Math.min(fromX, toX) > originX + canvasW
                    || Math.max(fromY, toY) < originY
                    || Math.min(fromY, toY) > originY + canvasH) continue;

            boolean hover = distToWire(mouseX, mouseY, fromX, fromY, toX, toY) < 8f;
            if (hover) {
                hoveredWireToId = edge.toId;
                hoveredWireToPort = edge.toPort;
            }
            drawWire(dl, fromX, fromY, toX, toY, hover ? COLOR_WIRE_HOVER : COLOR_WIRE, uiScale);
        }

        // Draw in-progress wire
        if (wireFromNodeId != null && ImGui.isMouseDown(0)) {
            drawWire(dl, wireFromScreenX, wireFromScreenY, mouseX, mouseY, COLOR_WIRE, uiScale);
        }

        // Draw nodes
        List<PipelineGraph.NodeInstance> nodes = graph.nodes();
        for (PipelineGraph.NodeInstance node : nodes) {
            float nx = originX + panX + node.posX * viewScale;
            float ny = originY + panY + node.posY * viewScale;
            NodeSchema schema = NodeRegistry.create(node.typeId).schema();
            float nh = nodeHeight(schema, node.params, viewScale);
            if (nx + NODE_W * viewScale < originX
                    || nx > originX + canvasW
                    || ny + nh < originY
                    || ny > originY + canvasH) continue;
            drawNode(dl, node, originX, originY, uiScale, viewScale);
        }

        dl.popClipRect();

        ImGui.setCursorScreenPos(originX, originY);
        ImGui.invisibleButton("##pcanvas", canvasW, canvasH);
        boolean canvasHovered = ImGui.isItemHovered();
        boolean canvasActive = ImGui.isItemActive();

        handleMouse(nodes, originX, originY, canvasW, canvasH, uiScale, viewScale, canvasHovered, canvasActive);
        handleContextMenu(nodes, originX, originY, viewScale, canvasHovered, uiScale);

        ImGui.endChild();
    }

    // ── Hovered port detection ────────────────────────────────────────────────

    private void updateHoveredPort(
            List<PipelineGraph.NodeInstance> nodes,
            float originX,
            float originY,
            float viewScale,
            float mouseX,
            float mouseY) {
        hoveredPortNodeId = null;
        hoveredPortName = null;
        float hitR = (PORT_R + PORT_HIT_SLACK) * viewScale;

        for (PipelineGraph.NodeInstance node : nodes) {
            NodeSchema schema = NodeRegistry.create(node.typeId).schema();
            float nx = originX + panX + node.posX * viewScale;
            float ny = originY + panY + node.posY * viewScale;
            float nw = NODE_W * viewScale;

            List<InputPortDef> inputs = NodeSchema.effectiveInputPorts(schema, node.params);
            for (int i = 0; i < inputs.size(); i++) {
                float py = portBodyY(i, ny, viewScale);
                if (dist2(mouseX, mouseY, nx, py) <= hitR * hitR) {
                    hoveredPortNodeId = node.instanceId;
                    hoveredPortName = inputs.get(i).name;
                    hoveredPortIsOutput = false;
                    return;
                }
            }

            List<OutputPortDef> outputs = schema.outputPorts();
            int inputCount = inputs.size();
            for (int i = 0; i < outputs.size(); i++) {
                float py = portBodyY(inputCount + i, ny, viewScale);
                if (dist2(mouseX, mouseY, nx + nw, py) <= hitR * hitR) {
                    hoveredPortNodeId = node.instanceId;
                    hoveredPortName = outputs.get(i).name;
                    hoveredPortIsOutput = true;
                    return;
                }
            }
        }
    }

    // ── Node drawing ──────────────────────────────────────────────────────────

    private void drawNode(
            ImDrawList dl,
            PipelineGraph.NodeInstance node,
            float originX,
            float originY,
            float uiScale,
            float viewScale) {

        float nx = originX + panX + node.posX * viewScale;
        float ny = originY + panY + node.posY * viewScale;
        float nw = NODE_W * viewScale;
        NodeSchema schema = NodeRegistry.create(node.typeId).schema();
        float nh = nodeHeight(schema, node.params, viewScale);

        // Shadow
        dl.addRectFilled(nx + 3, ny + 3, nx + nw + 3, ny + nh + 3, COLOR_SHADOW, 4f * uiScale);
        // Body
        dl.addRectFilled(nx, ny, nx + nw, ny + nh, COLOR_NODE_BODY, 4f * uiScale);
        // Header
        int headerColor = node.instanceId.equals(selectedNodeId) ? COLOR_NODE_SELECTED : COLOR_NODE_HEADER;
        dl.addRectFilled(nx, ny, nx + nw, ny + HEADER_H * viewScale, headerColor, 4f * uiScale);
        // Border
        dl.addRect(nx, ny, nx + nw, ny + nh, COLOR_NODE_BORDER, 4f * uiScale);
        // Separator below header
        dl.addLine(nx + 1, ny + HEADER_H * viewScale, nx + nw - 1, ny + HEADER_H * viewScale, COLOR_NODE_BORDER);

        int scaledFontSize = Math.max(1, Math.round(ImGui.getFontSize() * canvasZoom));
        ImFont font = ImGui.getFont();

        // Title
        if (canvasZoom >= LOD_TITLE) {
            dl.addText(
                    font,
                    scaledFontSize,
                    nx + 6 * uiScale,
                    ny + (HEADER_H * viewScale - scaledFontSize) * 0.5f,
                    COLOR_TEXT,
                    nodeDisplayName(node.typeId));
        }

        boolean showLabels = canvasZoom >= LOD_PORT_LABELS;

        // Input ports (schema-defined + param-exposed)
        List<InputPortDef> inputs = NodeSchema.effectiveInputPorts(schema, node.params);
        for (int i = 0; i < inputs.size(); i++) {
            InputPortDef port = inputs.get(i);
            float px = nx;
            float py = portBodyY(i, ny, viewScale);
            boolean hovered = node.instanceId.equals(hoveredPortNodeId)
                    && port.name.equals(hoveredPortName)
                    && !hoveredPortIsOutput;
            int color = hovered ? COLOR_PORT_HIGHLIGHT : portColor(port.type);
            dl.addCircleFilled(px, py, PORT_R * viewScale, color);
            dl.addCircle(px, py, PORT_R * viewScale, COLOR_NODE_BORDER);
            if (showLabels) {
                int labelColor = port.required ? COLOR_TEXT_DIM : COLOR_TEXT_OPTIONAL;
                dl.addText(
                        font,
                        scaledFontSize,
                        px + (PORT_R + PORT_LABEL_PAD) * viewScale,
                        py - scaledFontSize * 0.5f,
                        labelColor,
                        portLabel(schema, port.name));
            }
        }

        // Output ports — rows start after all input rows
        int inputCount = inputs.size();
        List<OutputPortDef> outputs = schema.outputPorts();
        for (int i = 0; i < outputs.size(); i++) {
            OutputPortDef port = outputs.get(i);
            float px = nx + nw;
            float py = portBodyY(inputCount + i, ny, viewScale);
            boolean hovered = node.instanceId.equals(hoveredPortNodeId)
                    && port.name.equals(hoveredPortName)
                    && hoveredPortIsOutput;
            int color = hovered ? COLOR_PORT_HIGHLIGHT : portColor(port.type);
            dl.addCircleFilled(px, py, PORT_R * viewScale, color);
            dl.addCircle(px, py, PORT_R * viewScale, COLOR_NODE_BORDER);
            if (showLabels) {
                dl.addText(
                        font,
                        scaledFontSize,
                        nx + (PORT_R + PORT_LABEL_PAD) * viewScale,
                        py - scaledFontSize * 0.5f,
                        COLOR_TEXT_DIM,
                        port.name);
            }
        }
    }

    // ── Mouse handling ────────────────────────────────────────────────────────

    private void handleMouse(
            List<PipelineGraph.NodeInstance> nodes,
            float originX,
            float originY,
            float canvasW,
            float canvasH,
            float uiScale,
            float viewScale,
            boolean canvasHovered,
            boolean canvasActive) {

        float mouseX = ImGui.getMousePos().x;
        float mouseY = ImGui.getMousePos().y;

        // Zoom toward cursor
        float scroll = ImGui.getIO().getMouseWheel();
        if (scroll != 0 && canvasHovered) {
            float mouseLocalX = (mouseX - originX - panX) / viewScale;
            float mouseLocalY = (mouseY - originY - panY) / viewScale;
            float newZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, canvasZoom * (1f + scroll * ZOOM_STEP)));
            float newViewScale = uiScale * newZoom;
            panX = mouseX - originX - mouseLocalX * newViewScale;
            panY = mouseY - originY - mouseLocalY * newViewScale;
            canvasZoom = newZoom;
        }

        // RMB drag — always pans
        if (canvasHovered && ImGui.isMouseDragging(1, 3f * uiScale)) {
            ImVec2 delta = new ImVec2();
            ImGui.getMouseDragDelta(delta, 1);
            panX += delta.x;
            panY += delta.y;
            ImGui.resetMouseDragDelta(1);
        }

        // LMB drag — move node
        if (draggingNodeId != null && ImGui.isMouseDragging(0, 2f)) {
            PipelineGraph.NodeInstance node = graph.findNode(draggingNodeId);
            if (node != null) {
                ImVec2 delta = new ImVec2();
                ImGui.getMouseDragDelta(delta, 0);
                node.posX = dragOffsetX + delta.x / viewScale;
                node.posY = dragOffsetY + delta.y / viewScale;
            }
        }

        // Keep wire end updated this frame
        if (wireFromNodeId != null) {
            PipelineGraph.NodeInstance fromNode = graph.findNode(wireFromNodeId);
            if (fromNode != null) {
                if (wireFromIsOutput) {
                    wireFromScreenX = originX + panX + fromNode.posX * viewScale + NODE_W * viewScale;
                    NodeSchema fromSchema = NodeRegistry.create(fromNode.typeId).schema();
                    int idx = outputPortIndex(fromSchema, wireFromPortName);
                    wireFromScreenY = portBodyY(
                            NodeSchema.effectiveInputPorts(fromSchema, fromNode.params)
                                            .size()
                                    + idx,
                            originY + panY + fromNode.posY * viewScale,
                            viewScale);
                } else {
                    wireFromScreenX = originX + panX + fromNode.posX * viewScale;
                    NodeSchema fromSchema = NodeRegistry.create(fromNode.typeId).schema();
                    int idx = inputPortIndex(fromSchema, fromNode.params, wireFromPortName);
                    wireFromScreenY = portBodyY(idx, originY + panY + fromNode.posY * viewScale, viewScale);
                }
            }
        }

        // LMB released
        if (ImGui.isMouseReleased(0)) {
            if (wireFromNodeId != null) {
                // Try to complete wire
                if (hoveredPortNodeId != null && !hoveredPortNodeId.equals(wireFromNodeId)) {
                    if (wireFromIsOutput && !hoveredPortIsOutput) {
                        // Output → Input: normal direction
                        graph.connect(wireFromNodeId, wireFromPortName, hoveredPortNodeId, hoveredPortName);
                    } else if (!wireFromIsOutput && hoveredPortIsOutput) {
                        // Input → Output: reverse direction
                        graph.connect(hoveredPortNodeId, hoveredPortName, wireFromNodeId, wireFromPortName);
                    }
                }
                wireFromNodeId = null;
            }
            draggingNodeId = null;

            // Clicking a wire (not on a port) while not dragging disconnects it
            if (hoveredWireToId != null
                    && hoveredWireToPort != null
                    && hoveredPortNodeId == null
                    && !ImGui.isMouseDragging(0, 4f)) {
                graph.disconnect(hoveredWireToId, hoveredWireToPort);
            }
        }

        // LMB pressed
        if (ImGui.isMouseClicked(0) && canvasHovered) {
            if (hoveredPortNodeId != null) {
                // Start wire drag from a port
                wireFromNodeId = hoveredPortNodeId;
                wireFromPortName = hoveredPortName;
                wireFromIsOutput = hoveredPortIsOutput;
                selectedNodeId = hoveredPortNodeId;
            } else {
                // Hit-test nodes for selection / drag start
                String hitNode = hitTestNode(mouseX - originX - panX, mouseY - originY - panY, nodes, viewScale);
                if (hitNode != null) {
                    selectedNodeId = hitNode;
                    draggingNodeId = hitNode;
                    PipelineGraph.NodeInstance node = graph.findNode(hitNode);
                    dragOffsetX = node.posX;
                    dragOffsetY = node.posY;
                } else {
                    selectedNodeId = null;
                }
            }
        }
    }

    // ── Context menu ──────────────────────────────────────────────────────────

    private void handleContextMenu(
            List<PipelineGraph.NodeInstance> nodes,
            float originX,
            float originY,
            float viewScale,
            boolean canvasHovered,
            float uiScale) {

        float mouseX = ImGui.getMousePos().x;
        float mouseY = ImGui.getMousePos().y;

        if (canvasHovered && ImGui.isMouseClicked(1) && !ImGui.isMouseDragging(1, 3f * uiScale)) {
            String hitNode = hitTestNode(mouseX - originX - panX, mouseY - originY - panY, nodes, viewScale);
            if (hitNode != null) {
                selectedNodeId = hitNode;
                ImGui.openPopup("##nodeCtx");
            }
        }

        if (ImGui.beginPopup("##nodeCtx")) {
            boolean canDelete = graph.nodes().size() > 1;
            if (!canDelete) ImGui.beginDisabled();
            if (ImGui.menuItem(I18n.format("dimensium.ui.pipeline.delete_node"))) {
                if (selectedNodeId != null) {
                    graph.removeNode(selectedNodeId);
                    selectedNodeId = null;
                }
            }
            if (!canDelete) ImGui.endDisabled();
            if (ImGui.menuItem(I18n.format("dimensium.ui.pipeline.disconnect"))) {
                if (selectedNodeId != null) graph.disconnectAll(selectedNodeId);
            }
            ImGui.endPopup();
        }

        if (ImGui.isKeyPressed(ImGuiKey.Delete)
                && selectedNodeId != null
                && graph.nodes().size() > 1) {
            graph.removeNode(selectedNodeId);
            selectedNodeId = null;
        }
    }

    // ── Wire drawing ──────────────────────────────────────────────────────────

    private static void drawWire(ImDrawList dl, float x0, float y0, float x1, float y1, int color, float uiScale) {
        float cpOffset = Math.abs(x1 - x0) * 0.5f + Math.abs(y1 - y0) * 0.1f;
        dl.addBezierCubic(
                x0, y0, x0 + cpOffset, y0, x1 - cpOffset, y1, x1, y1, color, WIRE_THICKNESS * uiScale, BEZIER_SEGMENTS);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Y position (in screen space) of the port circle centre for a port at the given
     * row index within the node's body area.
     */
    private float portBodyY(int portIndex, float nodeScreenTop, float viewScale) {
        return nodeScreenTop + (HEADER_H + BODY_PAD + (portIndex + 0.5f) * PORT_ROW_H) * viewScale;
    }

    private static float nodeHeight(NodeSchema schema, NodeParams params, float viewScale) {
        int rows = Math.max(
                NodeSchema.effectiveInputPorts(schema, params).size()
                        + schema.outputPorts().size(),
                1);
        return (HEADER_H + BODY_PAD * 2f + rows * PORT_ROW_H) * viewScale;
    }

    private static int inputPortIndex(NodeSchema schema, NodeParams params, String portName) {
        List<InputPortDef> ports = NodeSchema.effectiveInputPorts(schema, params);
        for (int i = 0; i < ports.size(); i++) {
            if (ports.get(i).name.equals(portName)) return i;
        }
        return 0;
    }

    private static String portLabel(NodeSchema schema, String portName) {
        for (NodeSchema.ParamDef def : schema.params()) {
            if (def.key.equals(portName)) {
                return I18n.format(def.labelKey);
            }
        }
        return portName;
    }

    private static int outputPortIndex(NodeSchema schema, String portName) {
        List<OutputPortDef> ports = schema.outputPorts();
        for (int i = 0; i < ports.size(); i++) {
            if (ports.get(i).name.equals(portName)) return i;
        }
        return 0;
    }

    private String hitTestNode(float localX, float localY, List<PipelineGraph.NodeInstance> nodes, float viewScale) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            PipelineGraph.NodeInstance node = nodes.get(i);
            NodeSchema schema = NodeRegistry.create(node.typeId).schema();
            float nw = NODE_W * viewScale;
            float nh = nodeHeight(schema, node.params, viewScale);
            if (localX >= node.posX * viewScale
                    && localX <= node.posX * viewScale + nw
                    && localY >= node.posY * viewScale
                    && localY <= node.posY * viewScale + nh) {
                return node.instanceId;
            }
        }
        return null;
    }

    /** Approximate distance from (px,py) to a Bezier wire (9 sample points). */
    private static float distToWire(float px, float py, float x0, float y0, float x3, float y3) {
        float minDist = Float.MAX_VALUE;
        float cpOffset = Math.abs(x3 - x0) * 0.5f + Math.abs(y3 - y0) * 0.1f;
        for (int step = 0; step <= 8; step++) {
            float t = step / 8f;
            float mt = 1f - t;
            float x1 = x0 + cpOffset;
            float x2 = x3 - cpOffset;
            float bx = mt * mt * mt * x0 + 3 * mt * mt * t * x1 + 3 * mt * t * t * x2 + t * t * t * x3;
            float by = mt * mt * mt * y0 + 3 * mt * mt * t * y0 + 3 * mt * t * t * y3 + t * t * t * y3;
            float dx = px - bx;
            float dy = py - by;
            float dSq = dx * dx + dy * dy;
            if (dSq < minDist) minDist = dSq;
        }
        return (float) Math.sqrt(minDist);
    }

    /** Squared distance between two points (avoids sqrt in hot path). */
    private static float dist2(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }

    private static int portColor(PortType type) {
        switch (type) {
            case SKELETON:
                return COLOR_PORT_SKELETON;
            case BLOCK_MAP:
                return COLOR_PORT_BLOCK_MAP;
            case FLOAT:
                return COLOR_PORT_FLOAT;
            case VEC3:
                return COLOR_PORT_VEC3;
            case CURVE:
                return COLOR_PORT_CURVE;
            default:
                return COLOR_PORT_BLOCK_MAP;
        }
    }

    private static String nodeDisplayName(String typeId) {
        String[] parts = typeId.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1));
            sb.append(' ');
        }
        return sb.toString().trim();
    }
}
