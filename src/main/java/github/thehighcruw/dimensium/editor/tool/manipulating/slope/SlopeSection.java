/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.slope;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class SlopeSection implements ToolSection {

    private final SlopeToolState state;
    private final ImInt shapeIdx = new ImInt();
    private final ImInt applyModeIdx = new ImInt();
    private final int[] radius = new int[1];
    private final float[] smoothing = new float[1];
    private final ImBoolean clamp = new ImBoolean();

    public SlopeSection(SlopeToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        ImGui.text(I18n.format("dimensium.ui.section.mode"));
        ImGui.separator();

        SlopeToolState.SlopeShape[] shapes = SlopeToolState.SlopeShape.values();
        String[] shapeLabels = new String[shapes.length];
        for (int i = 0; i < shapes.length; i++) shapeLabels[i] = I18n.format(shapes[i].label);
        shapeIdx.set(state.slopeShape.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.slope.shape") + "##slope_shape", shapeIdx, shapeLabels)) {
            state.slopeShape = shapes[shapeIdx.get()];
        }

        SlopeToolState.SlopeApplyMode[] applyModes = SlopeToolState.SlopeApplyMode.values();
        String[] applyModeLabels = new String[applyModes.length];
        for (int i = 0; i < applyModes.length; i++) applyModeLabels[i] = I18n.format(applyModes[i].label);
        applyModeIdx.set(state.slopeApplyMode.ordinal());
        if (ImGui.combo(
                I18n.format("dimensium.ui.slope.apply_mode") + "##slope_apply", applyModeIdx, applyModeLabels)) {
            state.slopeApplyMode = applyModes[applyModeIdx.get()];
        }

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.brush"));
        ImGui.separator();

        radius[0] = state.slopeRadius;
        if (ImGui.sliderInt(
                I18n.format("dimensium.ui.slope.radius") + "##slope_rad",
                radius,
                SlopeToolState.RADIUS_MIN,
                SlopeToolState.RADIUS_MAX)) {
            state.slopeRadius = radius[0];
        }

        smoothing[0] = state.slopeSmoothing;
        if (ImGui.sliderFloat(
                I18n.format("dimensium.ui.slope.smoothing") + "##slope_smooth",
                smoothing,
                SlopeToolState.SMOOTHING_MIN,
                SlopeToolState.SMOOTHING_MAX)) {
            state.slopeSmoothing = smoothing[0];
        }

        clamp.set(state.slopeClamp);
        if (ImGui.checkbox(I18n.format("dimensium.ui.slope.clamp") + "##slope_clamp", clamp)) {
            state.slopeClamp = clamp.get();
        }

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.slope_info"));
        ImGui.separator();

        if (!state.hasPos1) {
            ImGui.textDisabled(I18n.format("dimensium.ui.slope.no_pos1"));
        } else {
            if (ImGui.button(I18n.format("dimensium.ui.slope.clear_pos1") + "##slope_clear")) {
                state.hasPos1 = false;
                state.hasPos2 = false;
            }
            if (state.hasPos2) {
                Vec3DInfoHelper.renderSlopeStats(state.pos1, state.pos2);
            } else {
                ImGui.textDisabled(I18n.format("dimensium.ui.slope.drag_hint"));
            }
        }
    }
}
