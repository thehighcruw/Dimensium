/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Coordinate-space utilities for Dimensium's editor UI.
 *
 * Two spaces are in play:
 * GUI pixels — scaledWidth × scaledHeight; matches ImGui displaySize and
 * the orthographic projection used by DeferredItemRender.
 * All cursor positions, panel widths, and ImGui getCursorScreenPos
 * values are in this space.
 * Physical px — mc.displayWidth × mc.displayHeight; used by glScissor, LWJGL
 * raw mouse deltas, and the GL viewport.
 *
 * Relationship: physical = gui × guiScale (integer factor, usually 1–4).
 *
 * All methods derive state from Minecraft.getMinecraft() on every call — no
 * per-frame snapshot needed, and safe to call at any point on the client thread.
 */
@SideOnly(Side.CLIENT)
public final class UICoords {

    private UICoords() {}

    // ── Accessors ─────────────────────────────────────────────────────────────

    public static int guiW() {
        Minecraft mc = Minecraft.getMinecraft();
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledWidth();
    }

    public static int guiH() {
        Minecraft mc = Minecraft.getMinecraft();
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledHeight();
    }

    // ── Static convenience methods (construct ScaledResolution internally) ────

    /** GUI-pixel X → NDC X in [-1, 1]. Useful for unproject / ray-cast. */
    public static double guiToNdcX(double guiX) {
        return (guiX / guiW()) * 2.0 - 1.0;
    }

    /** GUI-pixel Y → NDC Y in [-1, 1] (Y-up). */
    public static double guiToNdcY(double guiY) {
        return 1.0 - (guiY / guiH()) * 2.0;
    }

}
