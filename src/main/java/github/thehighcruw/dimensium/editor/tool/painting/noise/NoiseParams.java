/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.painting.noise;

import java.util.concurrent.ThreadLocalRandom;

import com.github.bsideup.jabel.Desugar;

import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseToolState.NoiseType;

@Desugar
public record NoiseParams(NoiseType noiseType, float noiseScale, int noiseOctaves, float noiseLacunarity,
    float noiseGain, long noiseSeed, float noiseJitter, float noiseW1, float noiseW2, float noiseW3,
    float noiseMetaballRange) {

    public static NoiseParams withDefaults(NoiseType type) {
        return new NoiseParams(
            type,
            10f,
            1,
            2.0f,
            0.5f,
            ThreadLocalRandom.current()
                .nextLong(),
            0.5f,
            1.0f,
            0.0f,
            0.0f,
            5.0f);
    }

    public NoiseParams withSeed(long seed) {
        return new NoiseParams(
            noiseType,
            noiseScale,
            noiseOctaves,
            noiseLacunarity,
            noiseGain,
            seed,
            noiseJitter,
            noiseW1,
            noiseW2,
            noiseW3,
            noiseMetaballRange);
    }

    public NoiseParams withOctaves(int octaves) {
        return new NoiseParams(
            noiseType,
            noiseScale,
            octaves,
            noiseLacunarity,
            noiseGain,
            noiseSeed,
            noiseJitter,
            noiseW1,
            noiseW2,
            noiseW3,
            noiseMetaballRange);
    }
}
