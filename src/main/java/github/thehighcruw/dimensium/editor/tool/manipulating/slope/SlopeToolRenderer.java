/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.slope;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.BrushPreviewRenderer;
import github.thehighcruw.dimensium.shared.math.Vec2DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class SlopeToolRenderer implements ToolRenderer {

    public static final SlopeToolRenderer INSTANCE = new SlopeToolRenderer();

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final int CONE_SEGMENTS = 24;
    private static final int CONE_RADIAL_LINES = 12;

    private SlopeToolRenderer() {}

    @Override
    public boolean isBlockAffected(Minecraft mc, Vec3DInt wc) {
        return false;
    }

    @Override
    public void renderWorldPreview(Minecraft mc, Vec3DDouble camPos) {
        BrushPreviewRenderer.INSTANCE.render(this, mc, camPos);
    }

    @Override
    public boolean renderHover(MovingObjectPosition mop, Vec3DDouble camPos) {
        SlopeToolState state = SlopeToolState.INSTANCE;
        if (!state.hasPos1 || !state.hasPos2) return true;

        Vec3DDouble pos1Center = state.pos1.toDouble().plus(0.5);
        Vec3DDouble pos2Center = state.pos2.toDouble().plus(0.5);

        GL11.glPushMatrix();
        GL11.glTranslated(-camPos.x(), -camPos.y(), -camPos.z());
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(0.2f, 0.9f, 0.3f, 0.65f);

        if (state.slopeShape == SlopeToolState.SlopeShape.PLANE) {
            drawPlaneGrid(pos1Center, pos2Center, state.slopeRadius);
        } else {
            drawConeGrid(pos1Center, pos2Center);
        }

        GL11.glLineWidth(1.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();

        return true;
    }

    private static void drawPlaneGrid(Vec3DDouble pos1, Vec3DDouble pos2, int radius) {
        Vec3DDouble axis = pos2.minus(pos1);
        double axisXZLen = Math.sqrt(axis.x() * axis.x() + axis.z() * axis.z());
        if (axisXZLen < 0.5) return;

        Vec3DDouble fwd = axis.times(1.0 / axisXZLen);
        Vec3DDouble side = Vec3DDouble.from(-fwd.z(), 0, fwd.x());

        double extent = Math.max(radius, axisXZLen) + 4;
        double step = computeGridStep(extent);

        GL11.glBegin(GL11.GL_LINES);

        for (double f = -extent; f <= extent + step * 0.5; f += step) {
            Vec3DDouble lineOrigin = pos1.plus(fwd.times(f));
            Vec3DDouble lineExtent = side.times(extent);
            Vec3DDouble lineStart = lineOrigin.plus(lineExtent);
            Vec3DDouble lineEnd = lineOrigin.minus(lineExtent);
            GL11.glVertex3d(lineStart.x(), lineStart.y(), lineStart.z());
            GL11.glVertex3d(lineEnd.x(), lineEnd.y(), lineEnd.z());
        }

        for (double s = -extent; s <= extent + step * 0.5; s += step) {
            Vec3DDouble sideOffset = side.times(s);
            Vec3DDouble start = pos1.plus(sideOffset).plus(fwd.times(-extent));
            Vec3DDouble end = pos1.plus(sideOffset).plus(fwd.times(extent));
            GL11.glVertex3d(start.x(), start.y(), start.z());
            GL11.glVertex3d(end.x(), end.y(), end.z());
        }

        GL11.glEnd();
    }

    private static void drawConeGrid(Vec3DDouble pos1, Vec3DDouble pos2) {
        double dx = pos2.x() - pos1.x();
        double dz = pos2.z() - pos1.z();
        double coneRadius = Math.sqrt(dx * dx + dz * dz);
        double heightDelta = pos2.y() - pos1.y();
        if (coneRadius < 0.5) return;

        double step = computeGridStep(coneRadius);

        GL11.glBegin(GL11.GL_LINES);

        for (double r = step; r <= coneRadius + step * 0.5; r += step) {
            double ringY = pos1.y() + (r / coneRadius) * heightDelta;
            for (int i = 0; i < CONE_SEGMENTS; i++) {
                Vec2DDouble prev = Vec2DDouble.fromPolar(TWO_PI * (i - 1) / CONE_SEGMENTS, r);
                Vec2DDouble curr = Vec2DDouble.fromPolar(TWO_PI * i / CONE_SEGMENTS, r);
                GL11.glVertex3d(pos1.x() + prev.x(), ringY, pos1.z() + prev.y());
                GL11.glVertex3d(pos1.x() + curr.x(), ringY, pos1.z() + curr.y());
            }
        }

        for (int i = 0; i < CONE_RADIAL_LINES; i++) {
            Vec2DDouble tip = Vec2DDouble.fromPolar(TWO_PI * i / CONE_RADIAL_LINES, coneRadius);
            GL11.glVertex3d(pos1.x(), pos1.y(), pos1.z());
            GL11.glVertex3d(pos1.x() + tip.x(), pos1.y() + heightDelta, pos1.z() + tip.y());
        }

        GL11.glEnd();
    }

    private static double computeGridStep(double extent) {
        if (extent <= 8) return 1.0;
        if (extent <= 20) return 2.0;
        if (extent <= 50) return 5.0;
        return 10.0;
    }
}
