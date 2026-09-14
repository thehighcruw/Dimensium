/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.rock;

import java.util.concurrent.ThreadLocalRandom;

public class RockToolState {

    public static final RockToolState INSTANCE = new RockToolState();

    public float noiseRadius = 5f;
    public float noisiness = 0.5f;
    public long noiseSeed = ThreadLocalRandom.current().nextLong();
    public float smoothingStdDev = 2f;
    public float meldStrength = 1f;
}
