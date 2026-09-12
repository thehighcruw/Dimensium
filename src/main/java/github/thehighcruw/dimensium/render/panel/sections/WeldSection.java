/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.panel.sections;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.panel.ToolSection;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.WeldToolState;
import imgui.ImGui;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class WeldSection implements ToolSection {

    private final WeldToolState state;
    private final BrushSection brushSection;
    private final int[] smoothStrength = new int[1];
    private final float[] threshold = new float[1];
    private final ImBoolean replaceSolid = new ImBoolean();

    public WeldSection(WeldToolState state, BrushState bs) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.weld"));

        smoothStrength[0] = state.weldSmoothStrength;
        if (ImGui.sliderInt(I18n.format("dimensium.ui.weld.smooth_strength") + "##weld_str", smoothStrength, 1, 10)) {
            state.weldSmoothStrength = smoothStrength[0];
        }

        threshold[0] = state.weldThreshold;
        if (ImGui.sliderFloat(I18n.format("dimensium.ui.weld.threshold") + "##weld_thresh", threshold, 0.0f, 1.0f)) {
            state.weldThreshold = threshold[0];
        }

        replaceSolid.set(state.weldReplaceSolid);
        if (ImGui.checkbox(I18n.format("dimensium.ui.weld.replace_solid") + "##weld_replace", replaceSolid)) {
            state.weldReplaceSolid = replaceSolid.get();
        }
    }
}
