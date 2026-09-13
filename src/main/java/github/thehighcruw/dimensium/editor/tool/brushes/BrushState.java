/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.brushes;

public class BrushState {

    public static final BrushState INSTANCE = new BrushState();

    public BrushShape brushShape = BrushShape.SPHERE;
    public int brushRadius = 3;
    public int brushHeight = 5;
    public boolean hollow = false;
}
