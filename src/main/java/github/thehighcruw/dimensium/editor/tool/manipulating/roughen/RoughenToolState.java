/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.roughen;

public class RoughenToolState {

    public static final RoughenToolState INSTANCE = new RoughenToolState();

    public int faces = 2;
    public float rougheningRatio = 0.5f;
}
