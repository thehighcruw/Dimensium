/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.smooth;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.manipulating.smooth.SmoothToolState.SmoothModifier;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class SmoothSection implements ToolSection {

    private final SmoothToolState state;
    private final BrushSection brushSection;
    private final int[] strength = new int[1];
    private final int[] blockRatio = new int[1];
    private final ImInt modifierIdx = new ImInt();
    private final ImBoolean fixEdges = new ImBoolean();

    public SmoothSection(SmoothToolState state, BrushState bs) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.smooth"));

        strength[0] = state.smoothStrength;
        if (ImGui.sliderInt(I18n.format("dimensium.ui.smooth.strength") + "##smooth_str", strength, 1, 10)) {
            state.smoothStrength = strength[0];
        }

        blockRatio[0] = state.smoothBlockRatio;
        if (ImGui.sliderInt(I18n.format("dimensium.ui.smooth.block_ratio") + "##smooth_ratio", blockRatio, 1, 100)) {
            state.smoothBlockRatio = blockRatio[0];
        }

        SmoothModifier[] mods = SmoothModifier.values();
        String[] modLabels = new String[mods.length];
        for (int i = 0; i < mods.length; i++) modLabels[i] = I18n.format(mods[i].label);
        modifierIdx.set(state.smoothModifier.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.smooth.modifier") + "##smooth_mod", modifierIdx, modLabels)) {
            state.smoothModifier = mods[modifierIdx.get()];
        }

        fixEdges.set(state.smoothFixEdges);
        if (ImGui.checkbox(I18n.format("dimensium.ui.smooth.fix_edges") + "##smooth_edges", fixEdges)) {
            state.smoothFixEdges = fixEdges.get();
        }
    }
}
