/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.painting;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseParams;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseToolState.NoiseType;
import imgui.ImGui;
import imgui.type.ImInt;

/**
 * Reusable noise parameter controls (type, scale, fbm, jitter, seed).
 * Composes into any tool section that exposes a NoiseParams.
 */
@SideOnly(Side.CLIENT)
public class NoiseParamSection {

    private final Supplier<NoiseParams> getter;
    private final Consumer<NoiseParams> setter;
    private final ImInt noiseTypeIdx = new ImInt();
    private final float[] noiseScale = new float[1];
    private final int[] octaves = new int[1];
    private final float[] lacunarity = new float[1];
    private final float[] gain = new float[1];
    private final float[] jitter = new float[1];
    private final float[] w1 = new float[1];
    private final float[] w2 = new float[1];
    private final float[] w3 = new float[1];
    private final float[] metaballRange = new float[1];
    private final String idSuffix;

    public NoiseParamSection(Supplier<NoiseParams> getter, Consumer<NoiseParams> setter, String idSuffix) {
        this.getter = getter;
        this.setter = setter;
        this.idSuffix = idSuffix;
    }

    public void render() {
        NoiseParams params = getter.get();
        NoiseType[] types = NoiseType.values();
        String[] typeLabels = new String[types.length];
        for (int i = 0; i < types.length; i++) typeLabels[i] = I18n.format(types[i].label);
        noiseTypeIdx.set(
            params.noiseType()
                .ordinal());
        if (ImGui
            .combo(I18n.format("dimensium.ui.noise.type") + "##noise_type_" + idSuffix, noiseTypeIdx, typeLabels)) {
            setter.accept(
                new NoiseParams(
                    types[noiseTypeIdx.get()],
                    params.noiseScale(),
                    params.noiseOctaves(),
                    params.noiseLacunarity(),
                    params.noiseGain(),
                    params.noiseSeed(),
                    params.noiseJitter(),
                    params.noiseW1(),
                    params.noiseW2(),
                    params.noiseW3(),
                    params.noiseMetaballRange()));
            params = getter.get();
        }

        noiseScale[0] = params.noiseScale();
        if (ImGui.sliderFloat(
            I18n.format("dimensium.ui.noise.scale") + "##noise_scale_" + idSuffix,
            noiseScale,
            0.5f,
            100f)) {
            setter.accept(
                new NoiseParams(
                    params.noiseType(),
                    noiseScale[0],
                    params.noiseOctaves(),
                    params.noiseLacunarity(),
                    params.noiseGain(),
                    params.noiseSeed(),
                    params.noiseJitter(),
                    params.noiseW1(),
                    params.noiseW2(),
                    params.noiseW3(),
                    params.noiseMetaballRange()));
            params = getter.get();
        }

        if (params.noiseType() == NoiseType.SIMPLEX || params.noiseType() == NoiseType.PERLIN) {
            octaves[0] = params.noiseOctaves();
            if (ImGui.sliderInt(I18n.format("dimensium.ui.noise.octaves") + "##noise_oct_" + idSuffix, octaves, 1, 8)) {
                setter.accept(
                    new NoiseParams(
                        params.noiseType(),
                        params.noiseScale(),
                        octaves[0],
                        params.noiseLacunarity(),
                        params.noiseGain(),
                        params.noiseSeed(),
                        params.noiseJitter(),
                        params.noiseW1(),
                        params.noiseW2(),
                        params.noiseW3(),
                        params.noiseMetaballRange()));
                params = getter.get();
            }

            lacunarity[0] = params.noiseLacunarity();
            if (ImGui.sliderFloat(
                I18n.format("dimensium.ui.noise.lacunarity") + "##noise_lac_" + idSuffix,
                lacunarity,
                1.0f,
                4.0f)) {
                setter.accept(
                    new NoiseParams(
                        params.noiseType(),
                        params.noiseScale(),
                        params.noiseOctaves(),
                        lacunarity[0],
                        params.noiseGain(),
                        params.noiseSeed(),
                        params.noiseJitter(),
                        params.noiseW1(),
                        params.noiseW2(),
                        params.noiseW3(),
                        params.noiseMetaballRange()));
                params = getter.get();
            }

            gain[0] = params.noiseGain();
            if (ImGui
                .sliderFloat(I18n.format("dimensium.ui.noise.gain") + "##noise_gain_" + idSuffix, gain, 0.0f, 1.0f)) {
                setter.accept(
                    new NoiseParams(
                        params.noiseType(),
                        params.noiseScale(),
                        params.noiseOctaves(),
                        params.noiseLacunarity(),
                        gain[0],
                        params.noiseSeed(),
                        params.noiseJitter(),
                        params.noiseW1(),
                        params.noiseW2(),
                        params.noiseW3(),
                        params.noiseMetaballRange()));
                params = getter.get();
            }
        }

        if (params.noiseType() == NoiseType.VORONOI_EDGES || params.noiseType() == NoiseType.WORLEY) {
            jitter[0] = params.noiseJitter();
            if (ImGui.sliderFloat(
                I18n.format("dimensium.ui.noise.jitter") + "##noise_jitter_" + idSuffix,
                jitter,
                0.0f,
                1.0f)) {
                setter.accept(
                    new NoiseParams(
                        params.noiseType(),
                        params.noiseScale(),
                        params.noiseOctaves(),
                        params.noiseLacunarity(),
                        params.noiseGain(),
                        params.noiseSeed(),
                        jitter[0],
                        params.noiseW1(),
                        params.noiseW2(),
                        params.noiseW3(),
                        params.noiseMetaballRange()));
                params = getter.get();
            }

            w1[0] = params.noiseW1();
            if (ImGui.sliderFloat(I18n.format("dimensium.ui.noise.w1") + "##noise_w1_" + idSuffix, w1, -1.0f, 1.0f)) {
                setter.accept(
                    new NoiseParams(
                        params.noiseType(),
                        params.noiseScale(),
                        params.noiseOctaves(),
                        params.noiseLacunarity(),
                        params.noiseGain(),
                        params.noiseSeed(),
                        params.noiseJitter(),
                        w1[0],
                        params.noiseW2(),
                        params.noiseW3(),
                        params.noiseMetaballRange()));
                params = getter.get();
            }

            w2[0] = params.noiseW2();
            if (ImGui.sliderFloat(I18n.format("dimensium.ui.noise.w2") + "##noise_w2_" + idSuffix, w2, -1.0f, 1.0f)) {
                setter.accept(
                    new NoiseParams(
                        params.noiseType(),
                        params.noiseScale(),
                        params.noiseOctaves(),
                        params.noiseLacunarity(),
                        params.noiseGain(),
                        params.noiseSeed(),
                        params.noiseJitter(),
                        params.noiseW1(),
                        w2[0],
                        params.noiseW3(),
                        params.noiseMetaballRange()));
                params = getter.get();
            }

            w3[0] = params.noiseW3();
            if (ImGui.sliderFloat(I18n.format("dimensium.ui.noise.w3") + "##noise_w3_" + idSuffix, w3, -1.0f, 1.0f)) {
                setter.accept(
                    new NoiseParams(
                        params.noiseType(),
                        params.noiseScale(),
                        params.noiseOctaves(),
                        params.noiseLacunarity(),
                        params.noiseGain(),
                        params.noiseSeed(),
                        params.noiseJitter(),
                        params.noiseW1(),
                        params.noiseW2(),
                        w3[0],
                        params.noiseMetaballRange()));
                params = getter.get();
            }
        }

        if (params.noiseType() == NoiseType.METABALL) {
            metaballRange[0] = params.noiseMetaballRange();
            if (ImGui.sliderFloat(
                I18n.format("dimensium.ui.noise.metaball_range") + "##noise_meta_" + idSuffix,
                metaballRange,
                1.0f,
                20.0f)) {
                setter.accept(
                    new NoiseParams(
                        params.noiseType(),
                        params.noiseScale(),
                        params.noiseOctaves(),
                        params.noiseLacunarity(),
                        params.noiseGain(),
                        params.noiseSeed(),
                        params.noiseJitter(),
                        params.noiseW1(),
                        params.noiseW2(),
                        params.noiseW3(),
                        metaballRange[0]));
            }
        }

        if (ImGui.button(I18n.format("dimensium.ui.noise.randomize_seed") + "##noise_seed_" + idSuffix)) {
            NoiseParams p = getter.get();
            setter.accept(
                new NoiseParams(
                    p.noiseType(),
                    p.noiseScale(),
                    p.noiseOctaves(),
                    p.noiseLacunarity(),
                    p.noiseGain(),
                    ThreadLocalRandom.current()
                        .nextLong(),
                    p.noiseJitter(),
                    p.noiseW1(),
                    p.noiseW2(),
                    p.noiseW3(),
                    p.noiseMetaballRange()));
        }
    }
}
