/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.panel.sections;

import java.nio.ByteBuffer;

import net.minecraft.client.resources.I18n;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.panel.ToolSection;
import github.thehighcruw.dimensium.tool.math.NoiseSampler;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.NoiseParams;
import github.thehighcruw.dimensium.tool.state.NoiseToolState;
import github.thehighcruw.dimensium.tool.state.NoiseToolState.NoiseType;
import github.thehighcruw.dimensium.tool.state.PaletteState;
import imgui.ImGui;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class NoiseSection implements ToolSection {

    private final NoiseToolState state;
    private final BrushSection brushSection;
    private final MultiPaletteSection paletteSection;
    private final NoiseParamSection noiseParamSection;
    private final ImBoolean surfaceOnly = new ImBoolean();
    private final ImBoolean noise3D = new ImBoolean();

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

    public NoiseSection(NoiseToolState state, BrushState bs, PaletteState ps) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
        this.paletteSection = new MultiPaletteSection(ps);
        this.noiseParamSection = new NoiseParamSection(state.noiseParams, "noise");
    }

    @Override
    public void render() {
        brushSection.render(false);
        paletteSection.render();

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.noise"));

        noiseParamSection.render();

        surfaceOnly.set(state.noiseSurfaceOnly);
        if (ImGui.checkbox(I18n.format("dimensium.ui.noise.surface_only") + "##noise_surf", surfaceOnly)) {
            state.noiseSurfaceOnly = surfaceOnly.get();
        }

        noise3D.set(state.noise3D);
        if (ImGui.checkbox(I18n.format("dimensium.ui.noise.3d") + "##noise_3d", noise3D)) {
            state.noise3D = noise3D.get();
        }

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.noise.preview"));
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
            || p.noiseType != cachedType) {

            if (noisePreviewTex == -1) noisePreviewTex = GL11.glGenTextures();

            int sz = PREVIEW_TEX_SIZE;
            ByteBuffer buf = BufferUtils.createByteBuffer(sz * sz * 3);
            for (int py = 0; py < sz; py++) {
                for (int px = 0; px < sz; px++) {
                    float wx = px * 50f / sz;
                    float wy = py * 50f / sz;
                    float v = NoiseSampler.sample2D(p, wx, wy);
                    byte b = (byte) (int) (v * 255f);
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
        }

        float displaySize = ImGui.getContentRegionAvailX();
        ImGui.image(noisePreviewTex, displaySize, displaySize, 0, 0, 1, 1);
    }
}
