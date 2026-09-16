/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.imgui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import imgui.ImGui;

/**
 * ImGuiWindow subclass that owns the open/closed toggle state.
 * Eliminates the repeated {@code private boolean open} field and {@code isOpen()} override
 * that every window otherwise duplicates.
 */
@SideOnly(Side.CLIENT)
public abstract class ToggleableWindow extends ImGuiWindow {

    protected boolean open;

    protected ToggleableWindow() {
        this.open = false;
    }

    protected ToggleableWindow(boolean initialOpen) {
        this.open = initialOpen;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    /** Height consumed by a single-button footer row: separator + button + padding. */
    protected static float footerButtonHeight() {
        return ImGui.getStyle().getItemSpacingY()
                + 1f
                + ImGui.getStyle().getItemSpacingY()
                + ImGui.getFrameHeight()
                + ImGui.getStyle().getWindowPaddingY();
    }
}
