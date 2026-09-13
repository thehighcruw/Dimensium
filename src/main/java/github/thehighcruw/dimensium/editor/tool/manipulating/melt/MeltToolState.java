/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.melt;

public class MeltToolState {

    public static final MeltToolState INSTANCE = new MeltToolState();

    public int meltSmoothStrength = 2;
    public float meltThreshold = 0.5f;
}
