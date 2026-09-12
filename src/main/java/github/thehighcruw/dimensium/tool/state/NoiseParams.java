/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.state;

import java.util.concurrent.ThreadLocalRandom;

public class NoiseParams {

    public NoiseToolState.NoiseType noiseType;
    public float noiseScale = 10f;
    public int noiseOctaves = 1;
    public float noiseLacunarity = 2.0f;
    public float noiseGain = 0.5f;
    public long noiseSeed = ThreadLocalRandom.current()
        .nextLong();
    public float noiseJitter = 0.5f;
    public float noiseW1 = 1.0f;
    public float noiseW2 = 0.0f;
    public float noiseW3 = 0.0f;
    public float noiseMetaballRange = 5.0f;

    public NoiseParams(NoiseToolState.NoiseType defaultType) {
        this.noiseType = defaultType;
    }
}
