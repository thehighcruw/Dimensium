/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeRegistry;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeSchema.ParamDef;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.PipelinePreset;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class TreeSection implements ToolSection {

    private final TreeToolState state;
    private final ImInt presetIdx = new ImInt();
    private final float[] floatBuf = new float[1];
    private final int[] intBuf = new int[1];

    public TreeSection(TreeToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        ImGui.text(I18n.format("dimensium.ui.section.tree"));
        ImGui.separator();

        String[] presetNames = new String[state.presets.size()];
        for (int i = 0; i < state.presets.size(); i++) {
            presetNames[i] = state.presets.get(i).name;
        }
        presetIdx.set(state.selectedPresetIndex);
        if (ImGui.combo(I18n.format("dimensium.ui.tree.preset") + "##tree_preset", presetIdx, presetNames)) {
            state.selectedPresetIndex = presetIdx.get();
            state.rebuildPipeline();
        }

        if (ImGui.button(I18n.format("dimensium.ui.tree.randomize_seed") + "##tree_seed")) {
            state.seed = ThreadLocalRandom.current().nextLong();
        }

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.tree_params"));
        ImGui.separator();

        PipelinePreset preset = state.selectedPreset();
        NodeParams params = state.activeParams();

        for (String nodeId : preset.nodeIds()) {
            PipelineNode<?, ?> node = NodeRegistry.create(nodeId);
            NodeSchema schema = node.schema();
            for (ParamDef def : schema.params()) {
                renderParam(def, params, nodeId);
            }
        }
    }

    private void renderParam(ParamDef def, NodeParams params, String nodeId) {
        String label = I18n.format(def.labelKey) + "##tree_" + nodeId + "_" + def.key;

        switch (def.type) {
            case FLOAT: {
                floatBuf[0] = params.getFloat(def.key, (Float) def.defaultValue);
                if (ImGui.sliderFloat(label, floatBuf, def.min, def.max)) {
                    params.set(def.key, floatBuf[0]);
                }
                break;
            }
            case INT: {
                intBuf[0] = params.getInt(def.key, (Integer) def.defaultValue);
                if (ImGui.sliderInt(label, intBuf, (int) def.min, (int) def.max)) {
                    params.set(def.key, intBuf[0]);
                }
                break;
            }
            case BOOL: {
                ImBoolean boolBuf = new ImBoolean(params.getBool(def.key, (Boolean) def.defaultValue));
                if (ImGui.checkbox(label, boolBuf)) {
                    params.set(def.key, boolBuf.get());
                }
                break;
            }
            case PALETTE: {
                @SuppressWarnings("unchecked")
                List<int[]> palette = params.getPalette(def.key, (List<int[]>) def.defaultValue);
                ImGui.text(I18n.format(def.labelKey) + ": "
                        + palette.size()
                        + " "
                        + I18n.format("dimensium.ui.tree.palette_blocks"));
                break;
            }
            default:
                break;
        }
    }
}
