/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamEntity;
import github.thehighcruw.dimensium.editor.overlay.ViewState;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;

@SideOnly(Side.CLIENT)
public final class ViewportState {

    public final String label;
    public final FreecamEntity cameraEntity;

    // GL texture owned by this viewport. Managed by ViewportCapture.
    int texId = -1;
    int texW, texH;

    // Content rect of this viewport's image area in scaled GUI pixels, updated each frame by ViewportPanel.
    public float contentX = 0, contentY = 0, contentW = 1, contentH = 1;

    /**
     * Convert a cursor X (scaled GUI pixels) to NDC X in [-1, 1] relative to this viewport's center.
     * contentX/W are in physical pixels (from ImGui); cursor and scaledW are in GUI pixels — scale up before comparing.
     */
    public double cursorToNdcX(double cursorX, int scaledW) {
        Minecraft mc = Minecraft.getMinecraft();
        int sf = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        double physCursorX = cursorX * sf;
        double physW = scaledW * sf;
        if (ViewState.INSTANCE.flipCanvas) physCursorX = (physW - 1) - physCursorX;
        return -(physCursorX - (contentX + contentW * 0.5)) / (physW * 0.5);
    }

    /**
     * Convert a cursor Y (scaled GUI pixels) to NDC Y in [-1, 1] relative to this viewport's center.
     * contentY/H are in physical pixels (from ImGui); cursor and scaledH are in GUI pixels — scale up before comparing.
     */
    public double cursorToNdcY(double cursorY, int scaledH) {
        Minecraft mc = Minecraft.getMinecraft();
        int sf = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        double physCursorY = cursorY * sf;
        double physH = scaledH * sf;
        return -(physCursorY - (contentY + contentH * 0.5)) / (physH * 0.5);
    }

    // Per-viewport camera control state. Swapped into/out of FreecamState on tab switch.
    public float speed = 0.5f;
    public boolean orbiting = false;
    public Vec3DDouble pivot = Vec3DDouble.ZERO;
    public double orbitDist;

    public ViewportState(String label, FreecamEntity cameraEntity) {
        this.label = label;
        this.cameraEntity = cameraEntity;
    }

    /** Delete GL texture immediately. Must be called from the render thread. */
    public void destroy() {
        if (texId != -1) {
            GL11.glDeleteTextures(texId);
            texId = -1;
            texW = 0;
            texH = 0;
        }
    }

    /** Queue GL texture for deletion on the render thread. Safe to call from any thread. */
    public void scheduleDestroy(List<Integer> pendingDeletions) {
        if (texId != -1) {
            pendingDeletions.add(texId);
            texId = -1;
            texW = 0;
            texH = 0;
        }
    }
}
