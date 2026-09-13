/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.painting;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.BrushInput;

@SideOnly(Side.CLIENT)
public final class PaintBrushInput implements BrushInput {

    public static final PaintBrushInput INSTANCE = new PaintBrushInput();

    @Override
    public boolean usesDragLoop() {
        return true;
    }

    private PaintBrushInput() {}
}
