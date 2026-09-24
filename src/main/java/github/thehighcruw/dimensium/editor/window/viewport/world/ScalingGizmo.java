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
    private static final double HIT_PX = 10.0;

    private static final Axis[] AXES = {Axis.X, Axis.Y, Axis.Z};

    private static final Vec3DFloat[] AXIS_DIR = {
        Vec3DFloat.from(1, 0, 0), Vec3DFloat.from(0, 1, 0), Vec3DFloat.from(0, 0, 1)
    };
    private static final Vec3DFloat[] AXIS_COL = {
        Vec3DFloat.from(1.0f, 0.25f, 0.25f), // X: red
        Vec3DFloat.from(0.25f, 1.0f, 0.25f), // Y: green
        Vec3DFloat.from(0.25f, 0.45f, 1.0f), // Z: blue
    };
    // Perpendicular half-extent basis per axis: BOX_HALF-scaled unit vecs in the 2 orthogonal dirs
    private static final Vec3DFloat[] PERP_1 = {
        Vec3DFloat.from(0, 1, 0), // X axis: p1 = Y
        Vec3DFloat.from(1, 0, 0), // Y axis: p1 = X
        Vec3DFloat.from(1, 0, 0), // Z axis: p1 = X
    };
    private static final Vec3DFloat[] PERP_2 = {
        Vec3DFloat.from(0, 0, 1), // X axis: p2 = Z
        Vec3DFloat.from(0, 0, 1), // Y axis: p2 = Z
        Vec3DFloat.from(0, 1, 0), // Z axis: p2 = Y
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

    public void render(Vec3DDouble pos, Vec3DDouble camPos, Vec3DFloat rot) {
        RotationGizmo.beginRender(proj, pos, camPos, rot);

        for (int a = 0; a < 3; a++) {
            boolean hot = hoveredAxis == AXES[a];
            Vec3DFloat color = AXIS_COL[a];
            Vec3DFloat dir = AXIS_DIR[a];

            Vec3DFloat bc = dir.times(BOX_CENTER);
            Vec3DFloat p1 = PERP_1[a].times(BOX_HALF);
            Vec3DFloat p2 = PERP_2[a].times(BOX_HALF);
            Vec3DFloat p3 = dir.times(BOX_HALF);

            float alpha = hot ? 0.95f : 0.6f;
            GL11.glColor4f(color.x(), color.y(), color.z(), alpha);

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
                GL11.glColor4f(color.x() * 0.7f, color.y() * 0.7f, color.z() * 0.7f, 0.9f);
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
        Vec3DFloat[] v = {
            center.minus(p1).minus(p2).minus(p3),
            center.plus(p1).minus(p2).minus(p3),
            center.plus(p1).plus(p2).minus(p3),
            center.minus(p1).plus(p2).minus(p3),
            center.minus(p1).minus(p2).plus(p3),
            center.plus(p1).minus(p2).plus(p3),
            center.plus(p1).plus(p2).plus(p3),
            center.minus(p1).plus(p2).plus(p3),
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

    private static void edge(Vec3DFloat[] v, int i, int j) {
        GL11.glVertex3f(v[i].x(), v[i].y(), v[i].z());
        GL11.glVertex3f(v[j].x(), v[j].y(), v[j].z());
    }

    // ── Hover ──────────────────────────────────────────────────────────────────

    public void updateHover(int mouseX, int mouseY, EntityLivingBase player, Vec3DDouble pos, Vec3DFloat rot) {
        RotationGizmo.HoverState hs = RotationGizmo.hoverState(player, pos, rot);
        float scale = hs.scale();
        Mat3DFloat R = hs.R();

        Axis best = Axis.NONE;
        double bestDist = HIT_PX;

        for (int a = 0; a < 3; a++) {
            Vec3DDouble wc = pos.plus(R.mul(AXIS_DIR[a]).toDouble().times(BOX_CENTER * scale));
            Vec2DDouble sc = proj.project(wc);
            if (sc == null) continue;
            double dist = Math.max(Math.abs(sc.x() - mouseX), Math.abs(sc.y() - mouseY));
            if (dist < bestDist) {
                bestDist = dist;
                best = AXES[a];
            }
        }
        hoveredAxis = best;
    }

    // ── Drag ──────────────────────────────────────────────────────────────────

    /**
     * startScale is the current scale value for the hovered axis.
     */
    public void startDrag(int mouseX, int mouseY, Vec3DDouble pos, float scale, Vec3DFloat rot) {
        if (hoveredAxis == Axis.NONE) return;
        dragAxis = hoveredAxis;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        startScale = scale;

        int a = dragAxis == Axis.X ? 0 : dragAxis == Axis.Y ? 1 : 2;
        Mat3DFloat R = ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z());
        Vec3DFloat dir = R.mul(AXIS_DIR[a]);

        GizmoProjection.ScreenAxis sa = proj.computeAxisScreenDir(pos, dir);
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
