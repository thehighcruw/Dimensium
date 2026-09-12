/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler.brushes;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.tool.Tool;
import github.thehighcruw.dimensium.tool.ToolRegistry;

@SideOnly(Side.CLIENT)
public final class BrushInputRegistry {

    /** Returns the input handler for a tool, or null if the tool has no registered input. */
    public static BrushInput get(Tool tool) {
        return ToolRegistry.brushInput(tool);
    }

    public static boolean usesDragLoop(Tool tool) {
        return ToolRegistry.usesDragLoop(tool);
    }

    private BrushInputRegistry() {}
}
