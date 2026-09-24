/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import com.github.bsideup.jabel.Desugar;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportRegistry;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportState;
import github.thehighcruw.dimensium.shared.math.Vec2DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

/**
 * Captures GL modelview/projection/viewport matrices at render time and uses
 * them to project world-space points to GUI screen coordinates via gluProject.
 *
 * The GL modelview during RenderWorldLastEvent has camera rotation only —
 * no world-space translation. Gizmos render at (wx-rx, wy-ry, wz-rz) relative
 * to the render offset. capture() stores (rx,ry,rz) so project() can subtract
 * it before calling gluProject, letting callers always pass absolute world coords.
 *
 * Call {@link #capture(Vec3DDouble)} at the start of each gizmo render
 * pass (before setupGizmoMatrix), then call {@link #project} in hit-detection code.
 */
@SideOnly(Side.CLIENT)
public final class GizmoProjection {

    @Desugar
    public record ScreenAxis(Vec2DDouble dir, double pixelsPerUnit) {

        static final ScreenAxis FALLBACK = new ScreenAxis(Vec2DDouble.from(1, 0), 50);
    }

    @Desugar
    public record Ray(Vec3DDouble origin, Vec3DDouble dir) {}

    /**
     * Project gizmo origin and axis tip to screen to get drag direction + scale.
     * Falls back to (1,0) direction and 50 px/unit when projection fails.
     */
    public ScreenAxis computeAxisScreenDir(Vec3DDouble pos, Vec3DFloat axisDir) {
        return computeAxisScreenDir(pos.x(), pos.y(), pos.z(), axisDir);
    }

    public ScreenAxis computeAxisScreenDir(double gx, double gy, double gz, Vec3DFloat axisDir) {
        Vec2DDouble originScreen = project(gx, gy, gz);
        Vec2DDouble tipScreen = project(gx + axisDir.x(), gy + axisDir.y(), gz + axisDir.z());
        if (originScreen == null || tipScreen == null) return ScreenAxis.FALLBACK;
        return new ScreenAxis(
                Vec2DDouble.screenDir(originScreen, tipScreen), Vec2DDouble.screenScale(originScreen, tipScreen));
    }

    private final FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final FloatBuffer win = BufferUtils.createFloatBuffer(3);
    private final FloatBuffer unprojectResult = BufferUtils.createFloatBuffer(3);

    private Vec3DDouble renderOffset = Vec3DDouble.ZERO;

    /**
     * Call once per frame before any matrix push/pop, during world render.
     *
     * @param camPos interpolated camera render position (same values passed to setupGizmoMatrix)
     */
    public void capture(Vec3DDouble camPos) {
        renderOffset = camPos;
        modelview.rewind();
        projection.rewind();
        viewport.rewind();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
    }

    /**
     * Projects an absolute world-space point to scaled-GUI screen coordinates.
     * Returns null if the point is behind the camera or projection failed.
     *
     */
    public Vec2DDouble project(Vec3DDouble pos) {
        return project(pos.x(), pos.y(), pos.z());
    }

    public Vec2DDouble project(double wx, double wy, double wz) {
        modelview.rewind();
        projection.rewind();
        viewport.rewind();
        win.rewind();
        boolean ok = GLU.gluProject(
                (float) (wx - renderOffset.x()),
                (float) (wy - renderOffset.y()),
                (float) (wz - renderOffset.z()),
                modelview,
                projection,
                viewport,
                win);
        if (!ok) return null;
        float winX = win.get(0);
        float winY = win.get(1);
        float winZ = win.get(2);
        // winZ in [0,1]: 0 = near plane, 1 = far plane. < 0 or >= 1 = clipped / behind.
        if (winZ < 0f || winZ >= 1f) return null;
        // The viewport panel captures the full display but shows only the central
        // contentW×contentH portion (UV-cropped, centered at displayW/2, displayH/2).
        // Map GL window coords (physical pixels, origin bottom-left) to panel GUI coords.
        Minecraft mc = Minecraft.getMinecraft();
        int scaleFactor = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        int displayW = mc.displayWidth;
        int displayH = mc.displayHeight;
        ViewportState viewportState = ViewportRegistry.INSTANCE.active();
        if (viewportState != null && viewportState.contentW > 1 && viewportState.contentH > 1) {
            double guiX = (viewportState.contentX + winX - displayW / 2.0 + viewportState.contentW / 2.0) / scaleFactor;
            double guiY = (viewportState.contentY + viewportState.contentH / 2.0 + displayH / 2.0 - winY) / scaleFactor;
            return Vec2DDouble.from(guiX, guiY);
        }
        return Vec2DDouble.from(winX / scaleFactor, (displayH - winY) / scaleFactor);
    }

    /**
     * Unprojects a GUI mouse position to a world-space ray.
     * Returns double[6]: {originX, originY, originZ, dirX, dirY, dirZ}.
     * Origin and direction are in absolute world space.
     * Returns null on failure.
     *
     * @param mouseX/mouseY GUI screen coordinates (same space as cursor3d)
     */
    public Ray unprojectRay(int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();
        int scaleFactor = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        int displayW = mc.displayWidth;
        int displayH = mc.displayHeight;

        // Convert GUI coords back to GL window coords (physical pixels, origin bottom-left).
        float winX, winY;
        ViewportState viewportState = ViewportRegistry.INSTANCE.active();
        if (viewportState != null && viewportState.contentW > 1 && viewportState.contentH > 1) {
            winX = (float)
                    ((mouseX * scaleFactor - viewportState.contentX) + displayW / 2.0 - viewportState.contentW / 2.0);
            winY = (float)
                    (viewportState.contentY + viewportState.contentH / 2.0 + displayH / 2.0 - mouseY * scaleFactor);
        } else {
            winX = mouseX * scaleFactor;
            winY = displayH - mouseY * scaleFactor;
        }

        // Unproject at near plane (winZ=0) and far plane (winZ=1) to get ray endpoints.
        modelview.rewind();
        projection.rewind();
        viewport.rewind();

        unprojectResult.rewind();
        boolean okNear = GLU.gluUnProject(winX, winY, 0f, modelview, projection, viewport, unprojectResult);
        if (!okNear) return null;
        double nx = (double) unprojectResult.get(0) + renderOffset.x();
        double ny = (double) unprojectResult.get(1) + renderOffset.y();
        double nz = (double) unprojectResult.get(2) + renderOffset.z();

        modelview.rewind();
        projection.rewind();
        viewport.rewind();
        unprojectResult.rewind();
        boolean okFar = GLU.gluUnProject(winX, winY, 1f, modelview, projection, viewport, unprojectResult);
        if (!okFar) return null;
        double fx = (double) unprojectResult.get(0) + renderOffset.x();
        double fy = (double) unprojectResult.get(1) + renderOffset.y();
        double fz = (double) unprojectResult.get(2) + renderOffset.z();

        Vec3DDouble dir = Vec3DDouble.from(fx - nx, fy - ny, fz - nz);
        double len = dir.length();
        if (len < 1e-10) return null;
        return new Ray(Vec3DDouble.from(nx, ny, nz), dir.divide(len));
    }
}
