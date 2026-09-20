/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.asm;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportRegistry;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportState;
import java.nio.FloatBuffer;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Called immediately after {@code EntityRenderer.setupCameraTransform} invokes
 * {@code gluPerspective}, while {@code GL_PROJECTION} is still the active matrix.
 *
 * When the editor viewport is active this does two things:
 * 1. Captures the untiled half-tangent values into {@link FreecamState} for raycasting.
 * 2. Replaces the projection matrix with an off-axis tile transform so that the full
 * {@code displayW × displayH} framebuffer covers only the viewport panel's angular
 * extent — giving supersampled quality for the panel content.
 *
 * The tile is centered at the viewport panel's center direction. Camera forward (screen
 * center) appears slightly off-centre in the panel for asymmetric layouts, but the error
 * is < 2° for typical side-panel widths and is unnoticeable in practice.
 */
@SideOnly(Side.CLIENT)
public final class ViewportTileHook {

    private static final FloatBuffer PROJ_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer TILE_BUFFER = BufferUtils.createFloatBuffer(16);

    private ViewportTileHook() {}

    public static void afterProjectionSetup() {
        // Capture the untiled projection tangents every frame so FreecamState stays current
        // regardless of whether the tile is applied.
        PROJ_BUFFER.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJ_BUFFER);
        float m0 = PROJ_BUFFER.get(0);
        float m5 = PROJ_BUFFER.get(5);
        if (m0 > 0f && m5 > 0f) {
            FreecamState.INSTANCE.projTanHX = 1f / m0;
            FreecamState.INSTANCE.projTanHY = 1f / m5;
        }

        if (!DimensiumEditorMode.INSTANCE.isActive()) return;

        ViewportState vp = ViewportRegistry.INSTANCE.active();
        if (vp == null || vp.contentW < 10f || vp.contentH < 10f) return;

        Minecraft mc = Minecraft.getMinecraft();
        int displayW = mc.displayWidth;
        int displayH = mc.displayHeight;

        float scaleX = (float) displayW / vp.contentW;
        float scaleY = (float) displayH / vp.contentH;

        // Panel centre in normalised screen NDC [-1, 1], where screen centre = 0.
        float halfDisplayW = displayW * 0.5f;
        float halfDisplayH = displayH * 0.5f;
        float panelCenterNdcX = (vp.contentX + vp.contentW * 0.5f - halfDisplayW) / halfDisplayW;
        // ImGui Y goes top-down; GL NDC Y goes bottom-up.
        float panelCenterNdcY = (halfDisplayH - (vp.contentY + vp.contentH * 0.5f)) / halfDisplayH;

        // Translation that shifts panel centre to tiled NDC (0, 0).
        float tx = -panelCenterNdcX * scaleX;
        float ty = -panelCenterNdcY * scaleY;

        // Off-axis tile matrix T (column-major, OpenGL convention):
        // [ scaleX 0 0 0 ]
        // [ 0 scaleY 0 0 ]
        // [ 0 0 1 0 ]
        // [ tx ty 0 1 ]
        // Multiplied as: T × P_full (glLoadMatrix T, then glMultMatrix P_full)
        TILE_BUFFER.clear();
        TILE_BUFFER.put(scaleX).put(0f).put(0f).put(0f); // column 0
        TILE_BUFFER.put(0f).put(scaleY).put(0f).put(0f); // column 1
        TILE_BUFFER.put(0f).put(0f).put(1f).put(0f); // column 2
        TILE_BUFFER.put(tx).put(ty).put(0f).put(1f); // column 3
        TILE_BUFFER.flip();

        GL11.glLoadMatrix(TILE_BUFFER); // current = T
        PROJ_BUFFER.rewind();
        GL11.glMultMatrix(PROJ_BUFFER); // current = T × P_full
    }
}
