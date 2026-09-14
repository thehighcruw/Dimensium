/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.distort;

public class DistortToolState {

    public static final DistortToolState INSTANCE = new DistortToolState();

    public float distortScale = 10f;
    public long distortSeed = java.util.concurrent.ThreadLocalRandom.current().nextLong();
    public float distortDistanceX = 3f;
    public float distortDistanceY = 3f;
    public float distortDistanceZ = 3f;
    public boolean distortSeparateAxis = false;
    public boolean distortSmoothEdges = true;
}
