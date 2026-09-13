/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.weld;

public class WeldToolState {

    public static final WeldToolState INSTANCE = new WeldToolState();

    public int weldSmoothStrength = 2;
    public float weldThreshold = 0.5f;
    public boolean weldReplaceSolid = false;
}
