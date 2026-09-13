/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.distort;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.window.panel.ToolSection;
import imgui.ImGui;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class DistortSection implements ToolSection {

    private final DistortToolState state;
    private final BrushSection brushSection;
    private final float[] scale = new float[1];
    private final float[] distX = new float[1];
    private final float[] distY = new float[1];
    private final float[] distZ = new float[1];

    public DistortSection(DistortToolState state, BrushState bs) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.distort"));

        scale[0] = state.distortScale;
        if (ImGui.sliderFloat(I18n.format("dimensium.ui.distort.scale") + "##dist_scale", scale, 0.5f, 50f)) {
            state.distortScale = scale[0];
        }

        ImBoolean cbSep = new ImBoolean(state.distortSeparateAxis);
        if (ImGui.checkbox(I18n.format("dimensium.ui.distort.separate_axis") + "##dist_sep", cbSep)) {
            state.distortSeparateAxis = cbSep.get();
        }

        if (state.distortSeparateAxis) {
            distX[0] = state.distortDistanceX;
            if (ImGui.sliderFloat(I18n.format("dimensium.ui.distort.distance_x") + "##dist_x", distX, 0f, 20f)) {
                state.distortDistanceX = distX[0];
            }
            distY[0] = state.distortDistanceY;
            if (ImGui.sliderFloat(I18n.format("dimensium.ui.distort.distance_y") + "##dist_y", distY, 0f, 20f)) {
                state.distortDistanceY = distY[0];
            }
            distZ[0] = state.distortDistanceZ;
            if (ImGui.sliderFloat(I18n.format("dimensium.ui.distort.distance_z") + "##dist_z", distZ, 0f, 20f)) {
                state.distortDistanceZ = distZ[0];
            }
        } else {
            distX[0] = state.distortDistanceX;
            if (ImGui.sliderFloat(I18n.format("dimensium.ui.distort.distance") + "##dist_xyz", distX, 0f, 20f)) {
                state.distortDistanceX = distX[0];
                state.distortDistanceY = distX[0];
                state.distortDistanceZ = distX[0];
            }
        }

        ImBoolean cbSmooth = new ImBoolean(state.distortSmoothEdges);
        if (ImGui.checkbox(I18n.format("dimensium.ui.distort.smooth_edges") + "##dist_smooth", cbSmooth)) {
            state.distortSmoothEdges = cbSmooth.get();
        }

        if (ImGui.button(I18n.format("dimensium.ui.distort.randomize_seed") + "##dist_seed")) {
            state.distortSeed = ThreadLocalRandom.current()
                .nextLong();
        }
    }
}
