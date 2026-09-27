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
import imgui.flag.ImGuiCol;
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

    private void renderPortToggle(PipelineGraph.NodeInstance node, String paramKey, boolean currentlyExposed) {
        if (currentlyExposed) {
            ImGui.pushStyleColor(ImGuiCol.Button, 0xFF_44_99_44);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0xFF_55_BB_55);
        }
        if (ImGui.smallButton("~##pt_" + paramKey)) {
            boolean newState = !currentlyExposed;
            node.params.setPortExposed(paramKey, newState);
            if (!newState) graph.disconnect(node.instanceId, paramKey);
            graph.markDirty();
        }
        if (currentlyExposed) {
            ImGui.popStyleColor(2);
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(I18n.format("dimensium.ui.pipeline.param_port_toggle"));
        }
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

        float uiScale = ImGuiManager.INSTANCE.getUIScale();
        float portToggleWidth = 22f * uiScale;

        for (ParamDef def : schema.params()) {
            String label = I18n.format(def.labelKey);
            switch (def.type) {
                case FLOAT: {
                    boolean portExposed = node.params.isPortExposed(def.key);
                    ImGui.textDisabled(label);
                    if (def.portExposable) {
                        renderPortToggle(node, def.key, portExposed);
                        ImGui.sameLine();
                        float sliderWidth =
                                panelWidth - portToggleWidth - ImGui.getStyle().getItemSpacingX() * 2f;
                        ImGui.setNextItemWidth(sliderWidth);
                    } else {
                        ImGui.setNextItemWidth(panelWidth);
                    }
                    float current = node.params.getFloat(def.key, (Float) def.defaultValue);
                    float[] buf = {current};
                    if (ImGui.sliderFloat("##" + def.key, buf, def.min, def.max)) {
                        node.params.set(def.key, buf[0]);
                        graph.markDirty();
                    }
                    break;
                }
                case INT: {
                    boolean portExposed = node.params.isPortExposed(def.key);
                    ImGui.textDisabled(label);
                    if (def.portExposable) {
                        renderPortToggle(node, def.key, portExposed);
                        ImGui.sameLine();
                        float sliderWidth =
                                panelWidth - portToggleWidth - ImGui.getStyle().getItemSpacingX() * 2f;
                        ImGui.setNextItemWidth(sliderWidth);
                    } else {
                        ImGui.setNextItemWidth(panelWidth);
                    }
                    int current = node.params.getInt(def.key, (Integer) def.defaultValue);
                    int[] buf = {current};
                    if (ImGui.sliderInt("##" + def.key, buf, (int) def.min, (int) def.max)) {
                        node.params.set(def.key, buf[0]);
                        graph.markDirty();
                    }
                    break;
                }
                case ENUM: {
                    ImGui.textDisabled(label);
                    ImGui.setNextItemWidth(panelWidth);
                    int current = node.params.getInt(def.key, (Integer) def.defaultValue);
                    if (ImGui.beginCombo("##" + def.key, def.enumOptions[current])) {
                        for (int i = 0; i < def.enumOptions.length; i++) {
                            boolean selected = i == current;
                            if (ImGui.selectable(def.enumOptions[i] + "##" + def.key + i, selected)) {
                                node.params.set(def.key, i);
                                graph.markDirty();
                            }
                        }
                        ImGui.endCombo();
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
