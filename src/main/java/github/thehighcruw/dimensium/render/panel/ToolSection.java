package github.thehighcruw.dimensium.render.panel;

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

    /** Called every tick while this tool is active (for tools that need continuous logic). */
    default void onTick() {}
}
