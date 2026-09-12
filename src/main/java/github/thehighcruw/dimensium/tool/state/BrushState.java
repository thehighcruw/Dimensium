/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.state;

public class BrushState {

    public static final BrushState INSTANCE = new BrushState();

    public BrushShape brushShape = BrushShape.SPHERE;
    public int brushRadius = 3;
    public int brushHeight = 5;
    public boolean hollow = false;
    public ReplaceMode replaceMode = ReplaceMode.ANY;
}
