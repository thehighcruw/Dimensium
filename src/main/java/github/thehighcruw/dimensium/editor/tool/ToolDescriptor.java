/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Per-tool client-side configuration bundle. Registered in {@link ToolRegistry}.
 * Consolidates brush input, brush view, panel section, and panel accent color.
 */
@SideOnly(Side.CLIENT)
public interface ToolDescriptor {

    /** Panel section factory; returns null if this tool has no options panel. */
    ToolSection createSection(ToolStates states);

    /** Mouse/click input handler; null if this tool has no brush input. */
    BrushInput brushInput();

    /** World-space rendering for this tool. Never null — use {@link ToolRenderer#NONE} for tools with no preview. */
    ToolRenderer toolRenderer();
}
