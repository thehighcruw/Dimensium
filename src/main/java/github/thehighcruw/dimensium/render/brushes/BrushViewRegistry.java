/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.brushes;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.tool.Tool;
import github.thehighcruw.dimensium.tool.ToolRegistry;

@SideOnly(Side.CLIENT)
public final class BrushViewRegistry {

    public static ToolRenderer get(Tool tool) {
        return ToolRegistry.toolRenderer(tool);
    }

    public static boolean hasBrushPreview(Tool tool) {
        return ToolRegistry.hasBrushPreview(tool);
    }

    private BrushViewRegistry() {}
}
