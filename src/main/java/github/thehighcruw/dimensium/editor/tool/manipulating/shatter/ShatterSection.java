/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.shatter;

import java.nio.ByteBuffer;

import net.minecraft.client.resources.I18n;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.NoiseSampler;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.manipulating.shatter.ShatterToolState.AxisMode;
import github.thehighcruw.dimensium.editor.tool.painting.NoiseParamSection;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseParams;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseToolState.NoiseType;
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

    private static final int PREVIEW_TEX_SIZE = 64;

    private int noisePreviewTex = -1;
    private long cachedSeed = Long.MIN_VALUE;
    private float cachedScale = Float.NaN;
    private int cachedOctaves = -1;
    private float cachedLacunarity = Float.NaN;
    private float cachedGain = Float.NaN;
    private float cachedJitter = Float.NaN;
    private float cachedRange = Float.NaN;
    private float cachedW1 = Float.NaN;
    private float cachedW2 = Float.NaN;
    private float cachedW3 = Float.NaN;
    private NoiseType cachedType = null;
    private float cachedCrackWidth = Float.NaN;

    public ShatterSection(ShatterToolState state, BrushState bs) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
        this.noiseParamSection = new NoiseParamSection(state.noiseParams, "shat");
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
        renderNoisePreview();
    }

    private void renderNoisePreview() {
        NoiseParams p = state.noiseParams;
        if (noisePreviewTex == -1 || p.noiseSeed != cachedSeed
            || p.noiseScale != cachedScale
            || p.noiseOctaves != cachedOctaves
            || p.noiseLacunarity != cachedLacunarity
            || p.noiseGain != cachedGain
            || p.noiseJitter != cachedJitter
            || p.noiseMetaballRange != cachedRange
            || p.noiseW1 != cachedW1
            || p.noiseW2 != cachedW2
            || p.noiseW3 != cachedW3
            || p.noiseType != cachedType
            || state.crackWidth != cachedCrackWidth) {

            if (noisePreviewTex == -1) noisePreviewTex = GL11.glGenTextures();

            int sz = PREVIEW_TEX_SIZE;
            ByteBuffer buf = BufferUtils.createByteBuffer(sz * sz * 3);
            for (int py = 0; py < sz; py++) {
                for (int px = 0; px < sz; px++) {
                    float wx = px * 50f / sz;
                    float wy = py * 50f / sz;
                    float v = NoiseSampler.sample2D(p, wx, wy);
                    float crack = v < state.crackWidth ? 0f : 1f;
                    byte b = (byte) (int) (crack * 255f);
                    buf.put(b)
                        .put(b)
                        .put(b);
                }
            }
            buf.flip();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, noisePreviewTex);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, sz, sz, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buf);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            cachedSeed = p.noiseSeed;
            cachedScale = p.noiseScale;
            cachedOctaves = p.noiseOctaves;
            cachedLacunarity = p.noiseLacunarity;
            cachedGain = p.noiseGain;
            cachedJitter = p.noiseJitter;
            cachedRange = p.noiseMetaballRange;
            cachedW1 = p.noiseW1;
            cachedW2 = p.noiseW2;
            cachedW3 = p.noiseW3;
            cachedType = p.noiseType;
            cachedCrackWidth = state.crackWidth;
        }

        float displaySize = ImGui.getContentRegionAvailX();
        ImGui.image(noisePreviewTex, displaySize, displaySize, 0, 0, 1, 1);
    }
}
