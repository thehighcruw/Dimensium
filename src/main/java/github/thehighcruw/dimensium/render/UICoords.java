/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render;

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

    public static int guiScale() {
        Minecraft mc = Minecraft.getMinecraft();
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
    }

    public static int guiW() {
        Minecraft mc = Minecraft.getMinecraft();
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledWidth();
    }

    public static int guiH() {
        Minecraft mc = Minecraft.getMinecraft();
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledHeight();
    }

    public static int physW() {
        return Minecraft.getMinecraft().displayWidth;
    }

    public static int physH() {
        return Minecraft.getMinecraft().displayHeight;
    }

    /**
     * When you need multiple values at once, grab a snapshot to avoid
     * constructing ScaledResolution three times.
     */
    public static Snapshot snapshot() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        return new Snapshot(
            sr.getScaleFactor(),
            sr.getScaledWidth(),
            sr.getScaledHeight(),
            mc.displayWidth,
            mc.displayHeight);
    }

    public static final class Snapshot {

        public final int scale, guiW, guiH, physW, physH;

        Snapshot(int scale, int guiW, int guiH, int physW, int physH) {
            this.scale = scale;
            this.guiW = guiW;
            this.guiH = guiH;
            this.physW = physW;
            this.physH = physH;
        }

        public float toGui(float phys) {
            return phys / scale;
        }

        public float toPhys(float gui) {
            return gui * scale;
        }

        public int toGuiI(int phys) {
            return phys / scale;
        }

        public int toPhysI(int gui) {
            return gui * scale;
        }

        /** Convert a GUI-pixel clip rect to GL scissor args (physical px, Y-flipped). */
        public int[] clipToScissor(float x0, float y0, float x1, float y1) {
            return UICoords.clipToScissor(x0, y0, x1, y1, physH);
        }

        /** True if GUI-pixel point (px, py) is inside the GUI-pixel rect. */
        public boolean guiContains(float rx, float ry, float rw, float rh, float px, float py) {
            return px >= rx && px < rx + rw && py >= ry && py < ry + rh;
        }

        public double guiToNdcX(double guiX) {
            return (guiX / guiW) * 2.0 - 1.0;
        }

        public double guiToNdcY(double guiY) {
            return 1.0 - (guiY / guiH) * 2.0;
        }
    }

    // ── Static convenience methods (construct ScaledResolution internally) ────

    /** Physical px → GUI pixels (float-preserving). */
    public static float toGui(float phys) {
        return phys / guiScale();
    }

    /** GUI pixels → physical px (float-preserving). */
    public static float toPhys(float gui) {
        return gui * guiScale();
    }

    public static int toGuiI(int phys) {
        return phys / guiScale();
    }

    public static int toPhysI(int gui) {
        return gui * guiScale();
    }

    /**
     * Convert an ImGui clip rect to GL scissor arguments.
     * ImGui runs in physical-pixel space (displayFramebufferScale=1), so clip rect
     * values are already in physical pixels — only a Y-axis flip is needed.
     * Returns int[4] {x, y, width, height} clamped to the viewport.
     */
    public static int[] clipToScissor(float x0, float y0, float x1, float y1) {
        int ph = Minecraft.getMinecraft().displayHeight;
        return clipToScissor(x0, y0, x1, y1, ph);
    }

    /** True if GUI-pixel point (px, py) lies inside the GUI-pixel axis-aligned rect. */
    public static boolean guiContains(float rx, float ry, float rw, float rh, float px, float py) {
        return px >= rx && px < rx + rw && py >= ry && py < ry + rh;
    }

    /** GUI-pixel X → NDC X in [-1, 1]. Useful for unproject / ray-cast. */
    public static double guiToNdcX(double guiX) {
        return (guiX / guiW()) * 2.0 - 1.0;
    }

    /** GUI-pixel Y → NDC Y in [-1, 1] (Y-up). */
    public static double guiToNdcY(double guiY) {
        return 1.0 - (guiY / guiH()) * 2.0;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    // clip rect is already in physical pixels (ImGui runs with displayFramebufferScale=1)
    private static int[] clipToScissor(float x0, float y0, float x1, float y1, int physH) {
        int pw = Minecraft.getMinecraft().displayWidth;
        int sx = (int) Math.floor(x0);
        int sy = (int) Math.floor(physH - y1);
        int sw = (int) Math.ceil(x1 - x0);
        int sh = (int) Math.ceil(y1 - y0);
        sx = Math.max(0, sx);
        sy = Math.max(0, sy);
        sw = Math.max(0, Math.min(sw, pw - sx));
        sh = Math.max(0, Math.min(sh, physH - sy));
        return new int[] { sx, sy, sw, sh };
    }
}
