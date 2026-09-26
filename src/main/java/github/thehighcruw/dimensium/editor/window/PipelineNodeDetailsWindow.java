/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.pipeline.NodeRegistry;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema.ParamDef;
import github.thehighcruw.dimensium.editor.pipeline.PipelineGraph;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.resources.I18n;

/** Floating window showing parameters and description for the selected pipeline node. */
@SideOnly(Side.CLIENT)
public class PipelineNodeDetailsWindow extends ToggleableWindow {

    public static final PipelineNodeDetailsWindow INSTANCE = new PipelineNodeDetailsWindow();

    private PipelineGraph graph;
    private String selectedNodeId;

    private PipelineNodeDetailsWindow() {}

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowPipelineNodeDetailsOpen(value);
    }

    public void setContext(PipelineGraph graph, String selectedNodeId) {
        this.graph = graph;
        this.selectedNodeId = selectedNodeId;
    }

    public void renderImGui() {
        if (!open) return;

        float uiScale = ImGuiManager.INSTANCE.getUIScale();
        ImGui.setNextWindowSize(260 * uiScale, 400 * uiScale, ImGuiCond.FirstUseEver);

        ImBoolean pOpen = new ImBoolean(true);
        if (ImGui.begin(
                I18n.format("dimensium.menu.window.pipeline_node_details") + "##pipelineNodeDetails",
                pOpen,
                ImGuiWindowFlags.NoCollapse)) {
            captureBounds();
            if (!pOpen.get()) {
                setOpen(false);
                ImGui.end();
                return;
            }
            if (graph != null && selectedNodeId != null) {
                PipelineGraph.NodeInstance node = graph.findNode(selectedNodeId);
                if (node != null) {
                    renderContent(node);
                }
            }
        }
        ImGui.end();
    }

    private void renderContent(PipelineGraph.NodeInstance node) {
        NodeSchema schema = NodeRegistry.create(node.typeId).schema();
        float panelWidth = ImGui.getContentRegionAvailX();

        String descKey = schema.descriptionKey();
        if (descKey != null) {
            String desc = I18n.format(descKey);
            ImGui.pushTextWrapPos(panelWidth);
            ImGui.textDisabled(desc);
            ImGui.popTextWrapPos();
            ImGui.separator();
        }

        for (ParamDef def : schema.params()) {
            String label = I18n.format(def.labelKey);
            switch (def.type) {
                case FLOAT: {
                    float current = node.params.getFloat(def.key, (Float) def.defaultValue);
                    float[] buf = {current};
                    if (ImGui.sliderFloat(label + "##" + def.key, buf, def.min, def.max)) {
                        node.params.set(def.key, buf[0]);
                        graph.markDirty();
                    }
                    break;
                }
                case INT: {
                    int current = node.params.getInt(def.key, (Integer) def.defaultValue);
                    int[] buf = {current};
                    if (ImGui.sliderInt(label + "##" + def.key, buf, (int) def.min, (int) def.max)) {
                        node.params.set(def.key, buf[0]);
                        graph.markDirty();
                    }
                    break;
                }
                case BOOL: {
                    boolean current = node.params.getBool(def.key, (Boolean) def.defaultValue);
                    ImBoolean buf = new ImBoolean(current);
                    if (ImGui.checkbox(label + "##" + def.key, buf)) {
                        node.params.set(def.key, buf.get());
                        graph.markDirty();
                    }
                    break;
                }
                case LONG: {
                    long current = node.params.getLong(def.key, (Long) def.defaultValue);
                    ImGui.text(label + ": " + current);
                    if (ImGui.button(I18n.format("dimensium.ui.pipeline.randomize") + "##" + def.key)) {
                        node.params.set(def.key, ThreadLocalRandom.current().nextLong());
                        graph.markDirty();
                    }
                    break;
                }
                case PALETTE: {
                    List<int[]> palette = node.params.getPalette(def.key, null);
                    if (palette == null) {
                        palette = new ArrayList<>();
                        node.params.set(def.key, palette);
                    } else if (!(palette instanceof ArrayList)) {
                        palette = new ArrayList<>(palette);
                        node.params.set(def.key, palette);
                    }
                    ImGui.text(label);
                    PipelineBlockPaletteWidget.render(def.key, palette, graph::markDirty);
                    ImGui.spacing();
                    break;
                }
                default:
                    break;
            }
        }
    }
}
