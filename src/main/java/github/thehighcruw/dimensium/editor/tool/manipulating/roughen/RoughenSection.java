/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.roughen;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.window.panel.ToolSection;
import imgui.ImGui;

@SideOnly(Side.CLIENT)
public class RoughenSection implements ToolSection {

    private final RoughenToolState state;
    private final BrushSection brushSection;
    private final int[] faces = new int[1];
    private final float[] ratio = new float[1];

    public RoughenSection(RoughenToolState state, BrushState bs) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.roughen"));

        faces[0] = state.faces;
        if (ImGui.sliderInt(I18n.format("dimensium.ui.roughen.faces") + "##roughen_faces", faces, 1, 6)) {
            state.faces = faces[0];
        }

        ratio[0] = state.rougheningRatio;
        if (ImGui.sliderFloat(I18n.format("dimensium.ui.roughen.ratio") + "##roughen_ratio", ratio, 0.0f, 1.0f)) {
            state.rougheningRatio = ratio[0];
        }
    }
}
