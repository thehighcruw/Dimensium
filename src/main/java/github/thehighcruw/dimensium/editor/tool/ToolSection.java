/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Strategy interface for per-tool panel sections.
 * Implementations call ImGui widgets directly in render().
 * No coordinates, no hit-testing — ImGui handles all of that.
 */
@SideOnly(Side.CLIENT)
public interface ToolSection {

    void render();

}
