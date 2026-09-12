/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.state;

public class SculptToolState {

    public static final SculptToolState INSTANCE = new SculptToolState();

    public float sculptStrength = 1.0f;
    public boolean sculptInvert = false;
    public boolean sculptMaskY = false;
    public boolean sculptDenoise = true;
}
