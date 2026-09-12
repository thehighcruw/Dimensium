package github.thehighcruw.dimensium.render.world;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.ViewportRegistry;
import github.thehighcruw.dimensium.render.ViewportState;

/**
 * Captures GL modelview/projection/viewport matrices at render time and uses
 * them to project world-space points to GUI screen coordinates via gluProject.
 *
 * The GL modelview during RenderWorldLastEvent has camera rotation only —
 * no world-space translation. Gizmos render at (wx-rx, wy-ry, wz-rz) relative
 * to the render offset. capture() stores (rx,ry,rz) so project() can subtract
 * it before calling gluProject, letting callers always pass absolute world coords.
 *
 * Call {@link #capture(double, double, double)} at the start of each gizmo render
 * pass (before setupGizmoMatrix), then call {@link #project} in hit-detection code.
 */
@SideOnly(Side.CLIENT)
public final class GizmoProjection {

    private final FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final FloatBuffer win = BufferUtils.createFloatBuffer(3);

    private double renderOffsetX, renderOffsetY, renderOffsetZ;

    /**
     * Call once per frame before any matrix push/pop, during world render.
     *
     * @param rx/ry/rz interpolated camera render position (same values passed to setupGizmoMatrix)
     */
    public void capture(double rx, double ry, double rz) {
        renderOffsetX = rx;
        renderOffsetY = ry;
        renderOffsetZ = rz;
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
     * @param sw scaled GUI screen width
     * @param sh scaled GUI screen height
     */
    public double[] project(double wx, double wy, double wz, int sw, int sh) {
        modelview.rewind();
        projection.rewind();
        viewport.rewind();
        win.rewind();
        boolean ok = GLU.gluProject(
            (float) (wx - renderOffsetX),
            (float) (wy - renderOffsetY),
            (float) (wz - renderOffsetZ),
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
            return new double[] { guiX, guiY };
        }
        return new double[] { winX / sf, (displayH - winY) / sf };
    }
}
