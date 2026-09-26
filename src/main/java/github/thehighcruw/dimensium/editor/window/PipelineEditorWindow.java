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
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema.ParamDef;
import github.thehighcruw.dimensium.editor.pipeline.PipelineGraph;
import github.thehighcruw.dimensium.editor.pipeline.PipelineLibrary;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.tool.creating.tree.PipelinePresets;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
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
    private static final float NODE_W = 190f;
    private static final float HEADER_H = 22f;
    private static final float PARAM_H = 17f;
    private static final float PORT_R = 5f;
    private static final float WIRE_THICKNESS = 2f;
    private static final float ZOOM_MIN = 0.25f;
    private static final float ZOOM_MAX = 3.0f;
    private static final float ZOOM_STEP = 0.1f;
    private static final int BEZIER_SEGMENTS = 24;
    /** Below this canvasZoom, skip per-param text to stay within the 16-bit vertex budget. */
    private static final float LOD_PARAMS_ZOOM = 0.6f;
    /** Below this canvasZoom, skip all text (title too). */
    private static final float LOD_TITLE_ZOOM = 0.35f;

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
    private static final int COLOR_WIRE = 0xFF_CC_AA_55;
    private static final int COLOR_WIRE_HOVER = 0xFF_FF_CC_66;
    private static final int COLOR_TEXT = 0xFF_E0_E0_E0;
    private static final int COLOR_TEXT_DIM = 0xFF_88_88_88;

    private enum InteractionMode {
        SELECT,
        PAN,
        WIRE
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private PipelineGraph graph;
    private String selectedNodeId = null;
    private String draggingNodeId = null;
    private float dragOffsetX;
    private float dragOffsetY;
    private float panX = 0f;
    private float panY = 0f;
    private float canvasZoom = 1.0f;
    private InteractionMode interactionMode = InteractionMode.SELECT;

    // Wire dragging state
    private String wireFromId = null;
    private float wireFromScreenX;
    private float wireFromScreenY;

    // Hover detection for wires
    private String hoveredWireToId = null;
    private String hoveredWireToPort = null;

    private final ImString nameBuffer = new ImString(128);

    private PipelineEditorWindow() {}

    /** Toggles the window. Creates a blank pipeline if none is loaded. */
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

    /** Opens this window and sets the graph to edit. */
    public void open(PipelineGraph graphToEdit) {
        this.graph = graphToEdit;
        this.open = true;
        this.selectedNodeId = null;
        this.wireFromId = null;
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

        // Node details and preview in separate windows
        PipelineNodeDetailsWindow.INSTANCE.setContext(graph, selectedNodeId);
        PipelineNodeDetailsWindow.INSTANCE.renderImGui();
        PipelinePreviewWindow.INSTANCE.setGraph(graph);
        PipelinePreviewWindow.INSTANCE.renderImGui();
    }

    private void renderToolbar(float uiScale) {
        ImGui.setNextItemWidth(160 * uiScale);
        nameBuffer.set(graph.name);
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
        String currentName = graph != null ? graph.name : "";
        if (ImGui.beginCombo("##plib", currentName)) {
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

        if (ImGui.button("1:1##pzoomreset")) {
            canvasZoom = 1.0f;
        }
        ImGui.sameLine();
        ImGui.textDisabled(String.format("%.0f%%", canvasZoom * 100f));

        ImGui.sameLine();
        ImGui.separator();
        ImGui.sameLine();

        // Interaction mode buttons
        renderModeButton(
                I18n.format("dimensium.ui.pipeline.mode.select") + "##modeSelect", InteractionMode.SELECT, uiScale);
        ImGui.sameLine();
        renderModeButton(I18n.format("dimensium.ui.pipeline.mode.pan") + "##modePan", InteractionMode.PAN, uiScale);
        ImGui.sameLine();
        renderModeButton(I18n.format("dimensium.ui.pipeline.mode.wire") + "##modeWire", InteractionMode.WIRE, uiScale);

        // Second toolbar row: one button per node group, each opens a popup listing that group's nodes
        for (NodeGroup group : NodeGroup.values()) {
            String groupLabelKey = "dimensium.ui.pipeline.group." + group.name().toLowerCase();
            String popupId = "##addNodeGroup_" + group.name();
            if (ImGui.button(I18n.format(groupLabelKey) + popupId)) {
                ImGui.openPopup(popupId);
            }
            if (ImGui.beginPopup(popupId)) {
                for (String typeId : NodeRegistry.idsInGroup(group)) {
                    if (ImGui.menuItem(nodeDisplayName(typeId))) {
                        graph.addNode(typeId, 40f + panX / canvasZoom, 40f + panY / canvasZoom);
                        ImGui.closeCurrentPopup();
                    }
                }
                ImGui.endPopup();
            }
            ImGui.sameLine();
        }
    }

    private void renderModeButton(String label, InteractionMode mode, float uiScale) {
        boolean active = interactionMode == mode;
        if (active) {
            ImGui.pushStyleColor(ImGuiCol.Button, 0.55f, 0.40f, 0.10f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.65f, 0.50f, 0.15f, 1.0f);
        }
        if (ImGui.button(label)) {
            interactionMode = mode;
            if (mode != InteractionMode.WIRE) wireFromId = null;
        }
        if (active) ImGui.popStyleColor(2);
    }

    private void resetView() {
        panX = 0;
        panY = 0;
        canvasZoom = 1.0f;
        selectedNodeId = null;
        wireFromId = null;
    }

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

        // Dot grid — skip when too dense (high vertex count crashes 16-bit ImGui index buffer)
        float gridStep = 20f * viewScale;
        float gridOffX = panX % gridStep;
        float gridOffY = panY % gridStep;
        dl.pushClipRect(originX, originY, originX + canvasW, originY + canvasH, true);
        if (gridStep >= 16f) {
            for (float x = originX + gridOffX; x < originX + canvasW; x += gridStep) {
                for (float y = originY + gridOffY; y < originY + canvasH; y += gridStep) {
                    dl.addRectFilled(x, y, x + 1.5f, y + 1.5f, COLOR_DOT);
                }
            }
        }

        // Draw wires
        hoveredWireToId = null;
        hoveredWireToPort = null;
        float mouseX = ImGui.getMousePos().x;
        float mouseY = ImGui.getMousePos().y;
        for (PipelineGraph.Edge edge : graph.edges()) {
            PipelineGraph.NodeInstance fromNode = graph.findNode(edge.fromId);
            PipelineGraph.NodeInstance toNode = graph.findNode(edge.toId);
            if (fromNode == null || toNode == null) continue;
            float fromX = nodeOutputPortX(fromNode, originX, viewScale);
            float fromY = nodePortY(fromNode, originX, originY, viewScale);
            float toX = nodeInputPortX(toNode, originX, viewScale);
            float toY = nodePortY(toNode, originX, originY, viewScale);
            // Skip wires fully outside the canvas viewport
            float wireMinX = Math.min(fromX, toX);
            float wireMaxX = Math.max(fromX, toX);
            float wireMinY = Math.min(fromY, toY);
            float wireMaxY = Math.max(fromY, toY);
            if (wireMaxX < originX
                    || wireMinX > originX + canvasW
                    || wireMaxY < originY
                    || wireMinY > originY + canvasH) continue;
            float distToWire = distToWire(mouseX, mouseY, fromX, fromY, toX, toY);
            boolean hover = distToWire < 8f;
            if (hover) {
                hoveredWireToId = edge.toId;
                hoveredWireToPort = edge.toPort;
            }
            int wireColor = hover ? COLOR_WIRE_HOVER : COLOR_WIRE;
            float cpOffset = Math.abs(toX - fromX) * 0.5f;
            dl.addBezierCubic(
                    fromX,
                    fromY,
                    fromX + cpOffset,
                    fromY,
                    toX - cpOffset,
                    toY,
                    toX,
                    toY,
                    wireColor,
                    WIRE_THICKNESS * uiScale,
                    BEZIER_SEGMENTS);
        }

        // Draw in-progress wire
        if (wireFromId != null && ImGui.isMouseDown(0)) {
            float cpOffset = Math.abs(mouseX - wireFromScreenX) * 0.5f;
            dl.addBezierCubic(
                    wireFromScreenX,
                    wireFromScreenY,
                    wireFromScreenX + cpOffset,
                    wireFromScreenY,
                    mouseX - cpOffset,
                    mouseY,
                    mouseX,
                    mouseY,
                    COLOR_WIRE,
                    WIRE_THICKNESS * uiScale,
                    BEZIER_SEGMENTS);
        }

        // Draw nodes — skip those fully outside the canvas viewport
        List<PipelineGraph.NodeInstance> nodes = graph.nodes();
        for (PipelineGraph.NodeInstance node : nodes) {
            float nx = originX + panX + node.posX * viewScale;
            float ny = originY + panY + node.posY * viewScale;
            NodeSchema schema = NodeRegistry.create(node.typeId).schema();
            float nh = (HEADER_H + schema.params().size() * PARAM_H + 4f) * viewScale;
            float nw = NODE_W * viewScale;
            if (nx + nw < originX || nx > originX + canvasW || ny + nh < originY || ny > originY + canvasH) continue;
            drawNode(dl, node, originX, originY, uiScale, viewScale);
        }

        dl.popClipRect();

        ImGui.setCursorScreenPos(originX, originY);
        ImGui.invisibleButton("##pcanvas", canvasW, canvasH);
        boolean canvasHovered = ImGui.isItemHovered();
        boolean canvasActive = ImGui.isItemActive();

        handleMouse(nodes, originX, originY, canvasW, canvasH, uiScale, viewScale, canvasHovered, canvasActive);
        handleContextMenus(nodes, originX, originY, viewScale, canvasHovered);

        ImGui.endChild();
    }

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
        float localX = mouseX - originX - panX;
        float localY = mouseY - originY - panY;

        // Scroll to zoom around mouse cursor
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

        // RMB drag always pans regardless of mode
        if (canvasActive && ImGui.isMouseDragging(1, 3f * uiScale)) {
            ImVec2 delta = new ImVec2();
            ImGui.getMouseDragDelta(delta, 1);
            panX += delta.x;
            panY += delta.y;
            ImGui.resetMouseDragDelta(1);
        }

        // PAN mode: LMB drag also pans
        if (interactionMode == InteractionMode.PAN && canvasActive && ImGui.isMouseDragging(0, 3f * uiScale)) {
            ImVec2 delta = new ImVec2();
            ImGui.getMouseDragDelta(delta, 0);
            panX += delta.x;
            panY += delta.y;
            ImGui.resetMouseDragDelta(0);
        }

        // LMB release: finish wire drag or commit node drag
        if (ImGui.isMouseReleased(0)) {
            if (wireFromId != null) {
                String hitNode = hitTestNode(localX, localY, nodes, viewScale);
                if (hitNode != null && !hitNode.equals(wireFromId)) {
                    NodeSchema fromSchema = NodeRegistry.create(graph.findNode(wireFromId).typeId)
                            .schema();
                    NodeSchema toSchema =
                            NodeRegistry.create(graph.findNode(hitNode).typeId).schema();
                    NodeSchema.OutputPortDef fromOut = fromSchema.primaryOutput();
                    NodeSchema.InputPortDef toIn = toSchema.primaryInput();
                    if (toIn != null && fromOut != null && fromOut.type == toIn.type) {
                        graph.connect(wireFromId, fromOut.name, hitNode, toIn.name);
                    }
                }
                wireFromId = null;
            }
            draggingNodeId = null;

            if (hoveredWireToId != null && hoveredWireToPort != null && !ImGui.isMouseDragging(0, 4f)) {
                graph.disconnect(hoveredWireToId, hoveredWireToPort);
            }
        }

        // SELECT mode: LMB drag moves node
        if (interactionMode == InteractionMode.SELECT && ImGui.isMouseDragging(0, 2f) && draggingNodeId != null) {
            PipelineGraph.NodeInstance node = graph.findNode(draggingNodeId);
            if (node != null) {
                ImVec2 delta = new ImVec2();
                ImGui.getMouseDragDelta(delta, 0);
                node.posX = dragOffsetX + delta.x / viewScale;
                node.posY = dragOffsetY + delta.y / viewScale;
            }
        }

        // LMB click
        if (ImGui.isMouseClicked(0) && canvasHovered) {
            String hitNode = hitTestNode(localX, localY, nodes, viewScale);

            if (interactionMode == InteractionMode.WIRE) {
                if (wireFromId == null) {
                    // Start wire from any clicked node's output port
                    if (hitNode != null) {
                        NodeSchema schema = NodeRegistry.create(graph.findNode(hitNode).typeId)
                                .schema();
                        if (schema.primaryOutput() != null) {
                            wireFromId = hitNode;
                            selectedNodeId = hitNode;
                        }
                    }
                }
            } else if (interactionMode == InteractionMode.SELECT) {
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

        // Recalculate wire-from screen position each frame
        if (wireFromId != null) {
            PipelineGraph.NodeInstance fromNode = graph.findNode(wireFromId);
            if (fromNode != null) {
                wireFromScreenX = originX + panX + fromNode.posX * viewScale + NODE_W * viewScale;
                wireFromScreenY = originY + panY + fromNode.posY * viewScale + HEADER_H * viewScale / 2f;
            }
        }
    }

    private void handleContextMenus(
            List<PipelineGraph.NodeInstance> nodes,
            float originX,
            float originY,
            float viewScale,
            boolean canvasHovered) {

        float mouseX = ImGui.getMousePos().x;
        float mouseY = ImGui.getMousePos().y;
        float localX = mouseX - originX - panX;
        float localY = mouseY - originY - panY;

        float uiScale = ImGuiManager.INSTANCE.getUIScale();

        if (canvasHovered && ImGui.isMouseClicked(1) && !ImGui.isMouseDragging(1, 3f * uiScale)) {
            String hitNode = hitTestNode(localX, localY, nodes, viewScale);
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
        float paramCount = schema.params().size();
        float nh = (HEADER_H + paramCount * PARAM_H + 4f) * viewScale;

        dl.addRectFilled(nx + 3, ny + 3, nx + nw + 3, ny + nh + 3, COLOR_SHADOW, 4f * uiScale);
        dl.addRectFilled(nx, ny, nx + nw, ny + nh, COLOR_NODE_BODY, 4f * uiScale);

        int headerColor = node.instanceId.equals(selectedNodeId) ? COLOR_NODE_SELECTED : COLOR_NODE_HEADER;
        dl.addRectFilled(nx, ny, nx + nw, ny + HEADER_H * viewScale, headerColor, 4f * uiScale);
        dl.addRect(nx, ny, nx + nw, ny + nh, COLOR_NODE_BORDER, 4f * uiScale);

        if (canvasZoom >= LOD_TITLE_ZOOM) {
            dl.addText(nx + 6 * uiScale, ny + 5 * uiScale, COLOR_TEXT, nodeDisplayName(node.typeId));
        }

        if (canvasZoom >= LOD_PARAMS_ZOOM) {
            float py = ny + (HEADER_H + 2f) * viewScale;
            for (ParamDef def : schema.params()) {
                String valStr = formatParamValue(def, node.params);
                dl.addText(
                        nx + 6 * uiScale, py + 1 * uiScale, COLOR_TEXT_DIM, I18n.format(def.labelKey) + ": " + valStr);
                py += PARAM_H * viewScale;
            }
        }

        NodeSchema.InputPortDef inputPort = schema.primaryInput();
        if (inputPort != null) {
            int portColor = inputPort.type == PortType.SKELETON ? COLOR_PORT_SKELETON : COLOR_PORT_BLOCK_MAP;
            dl.addCircleFilled(nx, ny + HEADER_H * viewScale / 2f, PORT_R * viewScale, portColor);
            dl.addCircle(nx, ny + HEADER_H * viewScale / 2f, PORT_R * viewScale, COLOR_NODE_BORDER);
        }

        NodeSchema.OutputPortDef outputPort = schema.primaryOutput();
        if (outputPort != null) {
            int portColor = outputPort.type == PortType.SKELETON ? COLOR_PORT_SKELETON : COLOR_PORT_BLOCK_MAP;
            dl.addCircleFilled(nx + nw, ny + HEADER_H * viewScale / 2f, PORT_R * viewScale, portColor);
            dl.addCircle(nx + nw, ny + HEADER_H * viewScale / 2f, PORT_R * viewScale, COLOR_NODE_BORDER);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String hitTestNode(float localX, float localY, List<PipelineGraph.NodeInstance> nodes, float viewScale) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            PipelineGraph.NodeInstance node = nodes.get(i);
            NodeSchema schema = NodeRegistry.create(node.typeId).schema();
            float paramCount = schema.params().size();
            float nw = NODE_W * viewScale;
            float nh = (HEADER_H + paramCount * PARAM_H + 4f) * viewScale;
            if (localX >= node.posX * viewScale
                    && localX <= node.posX * viewScale + nw
                    && localY >= node.posY * viewScale
                    && localY <= node.posY * viewScale + nh) {
                return node.instanceId;
            }
        }
        return null;
    }

    private float nodeOutputPortX(PipelineGraph.NodeInstance node, float originX, float viewScale) {
        return originX + panX + node.posX * viewScale + NODE_W * viewScale;
    }

    private float nodeInputPortX(PipelineGraph.NodeInstance node, float originX, float viewScale) {
        return originX + panX + node.posX * viewScale;
    }

    private float nodePortY(PipelineGraph.NodeInstance node, float originX, float originY, float viewScale) {
        return originY + panY + node.posY * viewScale + HEADER_H * viewScale / 2f;
    }

    /** Rough approximate distance from point to a cubic Bezier (9 sample points). */
    private static float distToWire(float px, float py, float x0, float y0, float x3, float y3) {
        float minDist = Float.MAX_VALUE;
        float cpOffset = Math.abs(x3 - x0) * 0.5f;
        for (int step = 0; step <= 8; step++) {
            float t = step / 8f;
            float mt = 1f - t;
            float bx = mt * mt * mt * x0
                    + 3 * mt * mt * t * (x0 + cpOffset)
                    + 3 * mt * t * t * (x3 - cpOffset)
                    + t * t * t * x3;
            float by = mt * mt * mt * y0 + 3 * mt * mt * t * y0 + 3 * mt * t * t * y3 + t * t * t * y3;
            float dx = px - bx;
            float dy = py - by;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < minDist) minDist = dist;
        }
        return minDist;
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

    private static String formatParamValue(ParamDef def, NodeParams params) {
        switch (def.type) {
            case FLOAT:
                return String.format("%.2f", params.getFloat(def.key, (Float) def.defaultValue));
            case INT:
                return String.valueOf(params.getInt(def.key, (Integer) def.defaultValue));
            case BOOL:
                return String.valueOf(params.getBool(def.key, (Boolean) def.defaultValue));
            case LONG:
                return String.valueOf(params.getLong(def.key, (Long) def.defaultValue));
            case PALETTE:
                List<int[]> palette = params.getPalette(def.key, null);
                return palette != null ? palette.size() + " blocks" : "—";
            default:
                return "?";
        }
    }
}
