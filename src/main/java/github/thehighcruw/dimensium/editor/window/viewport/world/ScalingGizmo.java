/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec2DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

/**
 * Single-axis scale gizmo — a colored box at the tip of each axis arrow.
 * Dragging along the axis scales the shape on that axis.
 * X = red, Y = green, Z = blue (matching TranslationGizmo colors).
 */
public class ScalingGizmo {

    public enum Axis {
        NONE,
        X,
        Y,
        Z
    }

    /** Distance from gizmo origin to box center, in gizmo-local units. Must exceed ARC_R (1.5). */
    private static final float BOX_CENTER = 1.85f;
    /** Scale units per world-unit of projected drag distance. Lower = less sensitive. */
    private static final float SCALE_SENSITIVITY = 0.25f;

    private static final float BOX_HALF = 0.10f;
    private static final double HIT_PX = 6.0;

    private static final float[][] AXIS_DIR = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    private static final float[][] AXIS_COL = {
        {1.0f, 0.25f, 0.25f}, // X: red
        {0.25f, 1.0f, 0.25f}, // Y: green
        {0.25f, 0.45f, 1.0f}, // Z: blue
    };

    private final GizmoProjection proj = new GizmoProjection();

    public Axis hoveredAxis = Axis.NONE;
    private Axis dragAxis = Axis.NONE;
    private int dragStartMX, dragStartMY;
    private float startScale;
    private Vec2DDouble screenDir = Vec2DDouble.ZERO;
    private double pixelsPerUnit;

    public boolean isDragging() {
        return dragAxis != Axis.NONE;
    }

    public Axis getDragAxis() {
        return dragAxis;
    }

    public void reset() {
        hoveredAxis = Axis.NONE;
        dragAxis = Axis.NONE;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    public void render(double gx, double gy, double gz, Vec3DDouble camPos, float rotX, float rotY, float rotZ) {
        RotationGizmo.beginRender(proj, gx, gy, gz, camPos, rotX, rotY, rotZ);

        for (int a = 0; a < 3; a++) {
            Axis axis = a == 0 ? Axis.X : a == 1 ? Axis.Y : Axis.Z;
            boolean hot = hoveredAxis == axis;
            float[] col = AXIS_COL[a];
            float[] dir = AXIS_DIR[a];

            // Box center along this axis
            Vec3DFloat bc = Vec3DFloat.from(dir[0] * BOX_CENTER, dir[1] * BOX_CENTER, dir[2] * BOX_CENTER);
            float h = BOX_HALF;

            // Per-axis perpendicular half-extents: for X axis, box extends in Y and Z, etc.
            Vec3DFloat p1, p2;
            if (a == 0) { // X axis: perp = Y, Z
                p1 = Vec3DFloat.from(0, h, 0);
                p2 = Vec3DFloat.from(0, 0, h);
            } else if (a == 1) { // Y axis: perp = X, Z
                p1 = Vec3DFloat.from(h, 0, 0);
                p2 = Vec3DFloat.from(0, 0, h);
            } else { // Z axis: perp = X, Y
                p1 = Vec3DFloat.from(h, 0, 0);
                p2 = Vec3DFloat.from(0, h, 0);
            }
            // Along-axis half extent
            Vec3DFloat p3 = Vec3DFloat.from(dir[0] * h, dir[1] * h, dir[2] * h);

            float alpha = hot ? 0.95f : 0.6f;
            GL11.glColor4f(col[0], col[1], col[2], alpha);

            // 6 faces of the box
            renderBoxFace(bc.minus(p3), p1, p2); // back face
            renderBoxFace(bc.plus(p3), p2, p1); // front face
            renderBoxFace(bc.minus(p1), p3, p2); // left face
            renderBoxFace(bc.plus(p1), p2, p3); // right face
            renderBoxFace(bc.minus(p2), p1, p3); // bottom face
            renderBoxFace(bc.plus(p2), p3, p1); // top face

            // Outline
            if (hot) {
                GL11.glColor4f(1f, 1f, 1f, 0.9f);
            } else {
                GL11.glColor4f(col[0] * 0.7f, col[1] * 0.7f, col[2] * 0.7f, 0.9f);
            }
            renderBoxEdges(bc, p1, p2, p3);
        }

        GL11.glPopMatrix();
    }

    private static void renderBoxFace(Vec3DFloat center, Vec3DFloat a, Vec3DFloat b) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(center.x() - a.x() - b.x(), center.y() - a.y() - b.y(), center.z() - a.z() - b.z());
        GL11.glVertex3f(center.x() + a.x() - b.x(), center.y() + a.y() - b.y(), center.z() + a.z() - b.z());
        GL11.glVertex3f(center.x() + a.x() + b.x(), center.y() + a.y() + b.y(), center.z() + a.z() + b.z());
        GL11.glVertex3f(center.x() - a.x() + b.x(), center.y() - a.y() + b.y(), center.z() - a.z() + b.z());
        GL11.glEnd();
    }

    private static void renderBoxEdges(Vec3DFloat center, Vec3DFloat p1, Vec3DFloat p2, Vec3DFloat p3) {
        // 8 corners
        float[][] v = {
            {
                center.x() - p1.x() - p2.x() - p3.x(),
                center.y() - p1.y() - p2.y() - p3.y(),
                center.z() - p1.z() - p2.z() - p3.z()
            },
            {
                center.x() + p1.x() - p2.x() - p3.x(),
                center.y() + p1.y() - p2.y() - p3.y(),
                center.z() + p1.z() - p2.z() - p3.z()
            },
            {
                center.x() + p1.x() + p2.x() - p3.x(),
                center.y() + p1.y() + p2.y() - p3.y(),
                center.z() + p1.z() + p2.z() - p3.z()
            },
            {
                center.x() - p1.x() + p2.x() - p3.x(),
                center.y() - p1.y() + p2.y() - p3.y(),
                center.z() - p1.z() + p2.z() - p3.z()
            },
            {
                center.x() - p1.x() - p2.x() + p3.x(),
                center.y() - p1.y() - p2.y() + p3.y(),
                center.z() - p1.z() - p2.z() + p3.z()
            },
            {
                center.x() + p1.x() - p2.x() + p3.x(),
                center.y() + p1.y() - p2.y() + p3.y(),
                center.z() + p1.z() - p2.z() + p3.z()
            },
            {
                center.x() + p1.x() + p2.x() + p3.x(),
                center.y() + p1.y() + p2.y() + p3.y(),
                center.z() + p1.z() + p2.z() + p3.z()
            },
            {
                center.x() - p1.x() + p2.x() + p3.x(),
                center.y() - p1.y() + p2.y() + p3.y(),
                center.z() - p1.z() + p2.z() + p3.z()
            }
        };
        GL11.glBegin(GL11.GL_LINES);
        // Bottom ring
        edge(v, 0, 1);
        edge(v, 1, 2);
        edge(v, 2, 3);
        edge(v, 3, 0);
        // Top ring
        edge(v, 4, 5);
        edge(v, 5, 6);
        edge(v, 6, 7);
        edge(v, 7, 4);
        // Pillars
        edge(v, 0, 4);
        edge(v, 1, 5);
        edge(v, 2, 6);
        edge(v, 3, 7);
        GL11.glEnd();
    }

    private static void edge(float[][] v, int i, int j) {
        GL11.glVertex3f(v[i][0], v[i][1], v[i][2]);
        GL11.glVertex3f(v[j][0], v[j][1], v[j][2]);
    }

    // ── Hover ──────────────────────────────────────────────────────────────────

    public void updateHover(
            int mouseX,
            int mouseY,
            EntityLivingBase player,
            double gx,
            double gy,
            double gz,
            float rotX,
            float rotY,
            float rotZ) {
        RotationGizmo.HoverState hs = RotationGizmo.hoverState(player, gx, gy, gz, rotX, rotY, rotZ);
        float scale = hs.scale();
        Mat3DFloat R = hs.R();

        Axis best = Axis.NONE;
        double bestDistSq = HIT_PX * HIT_PX;

        for (int a = 0; a < 3; a++) {
            Vec3DFloat dirRot = R.mul(Vec3DFloat.from(AXIS_DIR[a][0], AXIS_DIR[a][1], AXIS_DIR[a][2]));
            double wcx = gx + dirRot.x() * BOX_CENTER * scale;
            double wcy = gy + dirRot.y() * BOX_CENTER * scale;
            double wcz = gz + dirRot.z() * BOX_CENTER * scale;
            double[] sc = proj.project(wcx, wcy, wcz);
            if (sc == null) continue;
            double distSq = Vec2DDouble.from(sc[0] - mouseX, sc[1] - mouseY).lengthSq();
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = a == 0 ? Axis.X : a == 1 ? Axis.Y : Axis.Z;
            }
        }
        hoveredAxis = best;
    }

    // ── Drag ──────────────────────────────────────────────────────────────────

    /**
     * startScale is the current scale value for the hovered axis.
     */
    public void startDrag(
            int mouseX, int mouseY, double gx, double gy, double gz, float scale, float rotX, float rotY, float rotZ) {
        if (hoveredAxis == Axis.NONE) return;
        dragAxis = hoveredAxis;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        startScale = scale;

        int a = dragAxis == Axis.X ? 0 : dragAxis == Axis.Y ? 1 : 2;
        Mat3DFloat R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);
        Vec3DFloat dir = R.mul(Vec3DFloat.from(AXIS_DIR[a][0], AXIS_DIR[a][1], AXIS_DIR[a][2]));

        GizmoProjection.ScreenAxis sa = proj.computeAxisScreenDir(gx, gy, gz, dir);
        screenDir = sa.dir();
        pixelsPerUnit = sa.pixelsPerUnit();
    }

    /**
     * Returns float[1] {newScale} or null if not dragging.
     */
    public float[] updateDrag(int mouseX, int mouseY) {
        if (dragAxis == Axis.NONE) return null;
        double proj =
                Vec2DDouble.from(mouseX - dragStartMX, mouseY - dragStartMY).dot(screenDir);
        float newScale = Math.max(0.1f, startScale + (float) (proj / pixelsPerUnit) * SCALE_SENSITIVITY);
        return new float[] {newScale};
    }

    public void endDrag() {
        dragAxis = Axis.NONE;
    }
}
