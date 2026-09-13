/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.freehand;

public class FreehandToolState {

    public static final FreehandToolState INSTANCE = new FreehandToolState();

    public boolean freehandMaskSurface = false;
    public boolean freehandReplaceSolid = false;
    public boolean includeAir = false;
}
