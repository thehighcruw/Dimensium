/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.shatter;

import net.minecraft.client.resources.I18n;

import com.google.common.base.Objects;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.manipulating.shatter.ShatterToolState.AxisMode;
import github.thehighcruw.dimensium.editor.tool.noise.NoisePreviewRenderer;
import github.thehighcruw.dimensium.editor.tool.painting.NoiseParamSection;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseParams;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class ShatterSection implements ToolSection {

    private final ShatterToolState state;
    private final BrushSection brushSection;
    private final NoiseParamSection noiseParamSection;
    private final float[] crackWidth = new float[1];
    private final ImInt axisModeIdx = new ImInt();

    private int noisePreviewTex = -1;
    private NoiseParams cachedParams = null;
    private float cachedCrackWidth = Float.NaN;

    public ShatterSection(ShatterToolState state, BrushState bs) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
        this.noiseParamSection = new NoiseParamSection(() -> state.noiseParams, p -> state.noiseParams = p, "shat");
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.crack"));
        ImGui.separator();

        crackWidth[0] = state.crackWidth;
        if (ImGui
            .sliderFloat(I18n.format("dimensium.ui.shatter.crack_width") + "##shat_crack", crackWidth, 0.0f, 1.0f)) {
            state.crackWidth = crackWidth[0];
        }

        AxisMode[] modes = AxisMode.values();
        String[] modeLabels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) modeLabels[i] = I18n.format(modes[i].label);
        axisModeIdx.set(state.axisMode.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.shatter.axis_mode") + "##shat_axis", axisModeIdx, modeLabels)) {
            state.axisMode = modes[axisModeIdx.get()];
        }

        ImBoolean fillMode = new ImBoolean(state.fillMode);
        if (ImGui.checkbox(I18n.format("dimensium.ui.shatter.fill_mode") + "##shat_fill", fillMode)) {
            state.fillMode = fillMode.get();
        }

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.noise"));
        ImGui.separator();
        noiseParamSection.render();

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.noise.preview"));
        ImGui.separator();

        if (noisePreviewTex == -1 || !Objects.equal(cachedParams, state.noiseParams)
            || state.crackWidth != cachedCrackWidth) {
            noisePreviewTex = NoisePreviewRenderer.rerenderNoisePreview(state.noiseParams, noisePreviewTex);

            cachedParams = state.noiseParams;
            cachedCrackWidth = state.crackWidth;
        }
    }

}
