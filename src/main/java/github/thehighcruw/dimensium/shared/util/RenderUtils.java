/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;

/**
 * Client-side rendering utilities.
 * Centralises ScaledResolution construction and cursor-based raycasting.
 */
@SideOnly(Side.CLIENT)
public final class RenderUtils {

    private RenderUtils() {}

    public static ScaledResolution scaledResolution() {
        Minecraft mc = Minecraft.getMinecraft();
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
    }

    public static int scaledWidth() {
        return scaledResolution().getScaledWidth();
    }

    public static int scaledHeight() {
        return scaledResolution().getScaledHeight();
    }

    public static int scaleFactor() {
        return scaledResolution().getScaleFactor();
    }

    /**
     * Raycast using cursorX3d (Flip Canvas aware) for the X axis,
     * cursorY for the Y axis.
     */
    public static MovingObjectPosition raycastAtCursor() {
        ScaledResolution sr = scaledResolution();
        return GuiDimensiumOverlay.raycastFromMouse(
            (int) FreecamState.INSTANCE.cursorX,
            (int) FreecamState.INSTANCE.cursorY,
            sr.getScaledWidth(),
            sr.getScaledHeight());
    }

    public static void unsetGhostRendering() {
        GL11.glDepthMask(true);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(0.0f, 0.0f);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }
}
