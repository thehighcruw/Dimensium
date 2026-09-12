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

    /** Returns the view for a tool, or null if the tool has no brush preview. */
    public static BrushView get(Tool tool) {
        return ToolRegistry.brushView(tool);
    }

    public static boolean hasBrushPreview(Tool tool) {
        return ToolRegistry.hasBrushView(tool);
    }

    private BrushViewRegistry() {}
}
