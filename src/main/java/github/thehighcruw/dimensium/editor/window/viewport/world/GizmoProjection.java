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

    /**
     * Project gizmo origin and axis tip to screen to get drag direction + scale.
     * Falls back to (1,0) direction and 50 px/unit when projection fails.
     */
    public ScreenAxis computeAxisScreenDir(double gx, double gy, double gz, Vec3DFloat axisDir) {
        double[] os = project(gx, gy, gz);
        double[] ts = project(gx + axisDir.x(), gy + axisDir.y(), gz + axisDir.z());
        if (os == null || ts == null) return ScreenAxis.FALLBACK;
        return new ScreenAxis(Vec2DDouble.screenDir(os, ts), Vec2DDouble.screenScale(os, ts));
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
    public double[] project(double wx, double wy, double wz) {
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
        int sf = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        int displayW = mc.displayWidth;
        int displayH = mc.displayHeight;
        ViewportState vp = ViewportRegistry.INSTANCE.active();
        if (vp != null && vp.contentW > 1 && vp.contentH > 1) {
            double guiX = (vp.contentX + winX - displayW / 2.0 + vp.contentW / 2.0) / sf;
            double guiY = (vp.contentY + vp.contentH / 2.0 + displayH / 2.0 - winY) / sf;
            return new double[] {guiX, guiY};
        }
        return new double[] {winX / sf, (displayH - winY) / sf};
    }

    /**
     * Unprojects a GUI mouse position to a world-space ray.
     * Returns double[6]: {originX, originY, originZ, dirX, dirY, dirZ}.
     * Origin and direction are in absolute world space.
     * Returns null on failure.
     *
     * @param mouseX/mouseY GUI screen coordinates (same space as cursor3d)
     */
    public double[] unprojectRay(int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();
        int sf = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        int displayW = mc.displayWidth;
        int displayH = mc.displayHeight;

        // Convert GUI coords back to GL window coords (physical pixels, origin bottom-left).
        float winX, winY;
        ViewportState vp = ViewportRegistry.INSTANCE.active();
        if (vp != null && vp.contentW > 1 && vp.contentH > 1) {
            winX = (float) ((mouseX * sf - vp.contentX) + displayW / 2.0 - vp.contentW / 2.0);
            winY = (float) (displayH / 2.0 + vp.contentH / 2.0 - vp.contentY - mouseY * sf);
        } else {
            winX = mouseX * sf;
            winY = displayH - mouseY * sf;
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
        Vec3DDouble dirN = dir.divide(len);
        return new double[] {nx, ny, nz, dirN.x(), dirN.y(), dirN.z()};
    }
}
