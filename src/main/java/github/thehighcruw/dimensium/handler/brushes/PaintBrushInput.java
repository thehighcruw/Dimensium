/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler.brushes;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PaintBrushInput implements BrushInput {

    public static final PaintBrushInput INSTANCE = new PaintBrushInput();

    @Override
    public boolean usesDragLoop() {
        return true;
    }

    private PaintBrushInput() {}
}
