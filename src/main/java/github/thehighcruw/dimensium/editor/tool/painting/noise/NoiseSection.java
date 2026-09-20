/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.painting.noise;

import com.google.common.base.Objects;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.noise.NoisePreviewRenderer;
import github.thehighcruw.dimensium.editor.tool.painting.MultiPaletteSection;
import github.thehighcruw.dimensium.editor.tool.painting.NoiseParamSection;
import github.thehighcruw.dimensium.editor.tool.state.PaletteState;
import imgui.ImGui;
import imgui.type.ImBoolean;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

@SideOnly(Side.CLIENT)
public class NoiseSection implements ToolSection {

    private final NoiseToolState state;
    private final PaletteState paletteState;
    private final BrushSection brushSection;
    private final MultiPaletteSection paletteSection;
    private final NoiseParamSection noiseParamSection;
    private final ImBoolean surfaceOnly = new ImBoolean();
    private final ImBoolean noise3D = new ImBoolean();
    private final ImBoolean showGrayscaleNoise = new ImBoolean();

    private NoiseParams cachedParams = null;
    private int cachedPaletteHash = 0;
    private boolean cachedShowGrayscale = false;
    private int noisePreviewTex = -1;

    public NoiseSection(NoiseToolState state, BrushState bs, PaletteState ps) {
        this.state = state;
        this.paletteState = ps;
        this.brushSection = new BrushSection(bs);
        this.paletteSection = new MultiPaletteSection(ps);
        this.noiseParamSection = new NoiseParamSection(() -> state.noiseParams, p -> state.noiseParams = p, "noise");
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

        showGrayscaleNoise.set(state.showGrayscaleNoise);
        if (ImGui.checkbox(
                I18n.format("dimensium.ui.noise.show_grayscale") + "##noise_grayscale", showGrayscaleNoise)) {
            state.showGrayscaleNoise = showGrayscaleNoise.get();
        }

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.noise.preview"));

        int paletteHash = paletteHash();
        if (noisePreviewTex == -1
                || !Objects.equal(cachedParams, state.noiseParams)
                || cachedPaletteHash != paletteHash
                || cachedShowGrayscale != state.showGrayscaleNoise) {
            noisePreviewTex = NoisePreviewRenderer.rerenderNoisePreview(
                    state.noiseParams, noisePreviewTex, paletteState, state.showGrayscaleNoise);
            cachedParams = state.noiseParams;
            cachedPaletteHash = paletteHash;
            cachedShowGrayscale = state.showGrayscaleNoise;
        }
        NoisePreviewRenderer.drawPreviewImage(noisePreviewTex);
    }

    private int paletteHash() {
        int hash = paletteState.palette.size();
        for (int i = 0; i < paletteState.palette.size(); i++) {
            ItemStack stack = paletteState.palette.get(i);
            hash = 31 * hash + System.identityHashCode(stack.getItem());
            hash = 31 * hash + stack.getItemDamage();
            hash = 31 * hash + paletteState.getWeight(i);
        }
        return hash;
    }
}
