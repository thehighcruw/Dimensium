/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.imgui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import imgui.ImGui;

/**
 * Base class for all floating ImGui windows. Owns pixel-space bounds tracking
 * so containsMouse() is available for the InputHandler onPanel check without
 * any per-window boilerplate.
 *
 * Subclasses must call captureBounds() immediately after ImGui.begin() each frame.
 * Registration with ImGuiWindowRegistry happens automatically in the constructor.
 */
@SideOnly(Side.CLIENT)
public abstract class ImGuiWindow {

    private float minX, minY, maxX, maxY;

    protected ImGuiWindow() {
        ImGuiWindowRegistry.INSTANCE.register(this);
    }

    /**
     * Call immediately after ImGui.begin() each frame to capture the window bounds.
     */
    protected void captureBounds() {
        minX = ImGui.getWindowPosX();
        minY = ImGui.getWindowPosY();
        maxX = minX + ImGui.getWindowWidth();
        maxY = minY + ImGui.getWindowHeight();
    }

    /**
     * Returns true when the window is open and the given pixel-space point is inside it.
     * mx/my must be in the same pixel coordinate space as ImGui (cursor * scaleFactor).
     */
    public abstract boolean isOpen();

    public boolean containsMouse(float mx, float my) {
        return isOpen() && mx >= minX && mx < maxX && my >= minY && my < maxY;
    }
}
