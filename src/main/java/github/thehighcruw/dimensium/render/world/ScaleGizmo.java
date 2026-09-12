/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.world;

import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;

import github.thehighcruw.dimensium.tool.math.ShapeMath;

/**
 * Plane-scale gizmo — 3 small colored squares between axis arrow pairs.
 * Each square lets the user stretch a shape on 2 axes simultaneously.
 * XY = yellow, XZ = magenta, YZ = cyan.
 */
public class ScaleGizmo {

    public enum Plane {
        NONE,
        XY,
        XZ,
        YZ
    }

    private static final float SQ_POS = 0.50f;
    private static final float SQ_HALF = 0.09f;
    private static final double HIT_PX = 4.0;

    // Center of each plane-square in local (pre-rotation) gizmo space
    private static final float[][] CENTERS = { { SQ_POS, SQ_POS, 0 }, // XY
        { SQ_POS, 0, SQ_POS }, // XZ
        { 0, SQ_POS, SQ_POS }, // YZ
    };

    // In-plane basis vectors for quad rendering
    private static final float[][] PLANE_A = { { 1, 0, 0 }, // XY: A = X
        { 1, 0, 0 }, // XZ: A = X
        { 0, 1, 0 }, // YZ: A = Y
    };
    private static final float[][] PLANE_B = { { 0, 1, 0 }, // XY: B = Y
        { 0, 0, 1 }, // XZ: B = Z
        { 0, 0, 1 }, // YZ: B = Z
    };

    private static final float[][] PLANE_COL = { { 1.0f, 1.0f, 0.2f }, // XY: yellow
        { 1.0f, 0.25f, 1.0f }, // XZ: magenta
        { 0.25f, 1.0f, 1.0f }, // YZ: cyan
    };

    private final GizmoProjection proj = new GizmoProjection();

    public Plane hoveredPlane = Plane.NONE;
    private Plane dragPlane = Plane.NONE;
    private int dragStartMX, dragStartMY;
    private float startScaleA, startScaleB;
    private double screenAxisAX, screenAxisAY, pixelsPerUnitA;
    private double screenAxisBX, screenAxisBY, pixelsPerUnitB;

    public boolean isDragging() {
        return dragPlane != Plane.NONE;
    }

    public Plane getDragPlane() {
        return dragPlane;
    }

    public void reset() {
        hoveredPlane = Plane.NONE;
        dragPlane = Plane.NONE;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    public void render(double gx, double gy, double gz, double rx, double ry, double rz, float rotX, float rotY,
        float rotZ) {
        proj.capture(rx, ry, rz);
        float scale = RotationGizmo.computeScale(gx - rx, gy - ry, gz - rz);
        RotationGizmo.setupGizmoMatrix(gx, gy, gz, rx, ry, rz, rotX, rotY, rotZ, scale);

        for (int p = 0; p < 3; p++) {
            Plane plane = p == 0 ? Plane.XY : p == 1 ? Plane.XZ : Plane.YZ;
            boolean hot = hoveredPlane == plane;
            float[] col = PLANE_COL[p];
            float cx = CENTERS[p][0], cy = CENTERS[p][1], cz = CENTERS[p][2];
            float[] a = PLANE_A[p], b = PLANE_B[p];

            float alpha = hot ? 0.85f : 0.45f;
            GL11.glColor4f(col[0], col[1], col[2], alpha);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3f(
                cx - SQ_HALF * a[0] - SQ_HALF * b[0],
                cy - SQ_HALF * a[1] - SQ_HALF * b[1],
                cz - SQ_HALF * a[2] - SQ_HALF * b[2]);
            GL11.glVertex3f(
                cx + SQ_HALF * a[0] - SQ_HALF * b[0],
                cy + SQ_HALF * a[1] - SQ_HALF * b[1],
                cz + SQ_HALF * a[2] - SQ_HALF * b[2]);
            GL11.glVertex3f(
                cx + SQ_HALF * a[0] + SQ_HALF * b[0],
                cy + SQ_HALF * a[1] + SQ_HALF * b[1],
                cz + SQ_HALF * a[2] + SQ_HALF * b[2]);
            GL11.glVertex3f(
                cx - SQ_HALF * a[0] + SQ_HALF * b[0],
                cy - SQ_HALF * a[1] + SQ_HALF * b[1],
                cz - SQ_HALF * a[2] + SQ_HALF * b[2]);
            GL11.glEnd();

            GL11.glColor4f(hot ? 1f : col[0], hot ? 1f : col[1], hot ? 1f : col[2], 0.9f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex3f(
                cx - SQ_HALF * a[0] - SQ_HALF * b[0],
                cy - SQ_HALF * a[1] - SQ_HALF * b[1],
                cz - SQ_HALF * a[2] - SQ_HALF * b[2]);
            GL11.glVertex3f(
                cx + SQ_HALF * a[0] - SQ_HALF * b[0],
                cy + SQ_HALF * a[1] - SQ_HALF * b[1],
                cz + SQ_HALF * a[2] - SQ_HALF * b[2]);
            GL11.glVertex3f(
                cx + SQ_HALF * a[0] + SQ_HALF * b[0],
                cy + SQ_HALF * a[1] + SQ_HALF * b[1],
                cz + SQ_HALF * a[2] + SQ_HALF * b[2]);
            GL11.glVertex3f(
                cx - SQ_HALF * a[0] + SQ_HALF * b[0],
                cy - SQ_HALF * a[1] + SQ_HALF * b[1],
                cz - SQ_HALF * a[2] + SQ_HALF * b[2]);
            GL11.glEnd();
        }

        GL11.glPopMatrix();
    }

    // ── Hover ──────────────────────────────────────────────────────────────────

    public void updateHover(int mouseX, int mouseY, int sw, int sh, EntityLivingBase player, double gx, double gy,
        double gz, float rotX, float rotY, float rotZ) {
        double eyeX = player.posX, eyeY = player.posY + player.getEyeHeight(), eyeZ = player.posZ;
        float scale = RotationGizmo.computeScale(gx - eyeX, gy - eyeY, gz - eyeZ);
        float[] R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);

        Plane best = Plane.NONE;
        double bestDist = HIT_PX;

        for (int p = 0; p < 3; p++) {
            float cx = CENTERS[p][0], cy = CENTERS[p][1], cz = CENTERS[p][2];
            float[] a = PLANE_A[p], b = PLANE_B[p];
            // Project all 4 corners of the square.
            float[] ha = RotationGizmo.rotateVec(new float[] { a[0] * SQ_HALF, a[1] * SQ_HALF, a[2] * SQ_HALF }, R);
            float[] hb = RotationGizmo.rotateVec(new float[] { b[0] * SQ_HALF, b[1] * SQ_HALF, b[2] * SQ_HALF }, R);
            float[] cRot = RotationGizmo.rotateVec(new float[] { cx, cy, cz }, R);
            double wcx = gx + cRot[0] * scale, wcy = gy + cRot[1] * scale, wcz = gz + cRot[2] * scale;
            double s = scale;
            double[][] corners = new double[4][];
            corners[0] = proj
                .project(wcx + (-ha[0] - hb[0]) * s, wcy + (-ha[1] - hb[1]) * s, wcz + (-ha[2] - hb[2]) * s, sw, sh);
            corners[1] = proj
                .project(wcx + (ha[0] - hb[0]) * s, wcy + (ha[1] - hb[1]) * s, wcz + (ha[2] - hb[2]) * s, sw, sh);
            corners[2] = proj
                .project(wcx + (ha[0] + hb[0]) * s, wcy + (ha[1] + hb[1]) * s, wcz + (ha[2] + hb[2]) * s, sw, sh);
            corners[3] = proj
                .project(wcx + (-ha[0] + hb[0]) * s, wcy + (-ha[1] + hb[1]) * s, wcz + (-ha[2] + hb[2]) * s, sw, sh);
            boolean anyNull = false;
            for (double[] c : corners) if (c == null) {
                anyNull = true;
                break;
            }
            if (anyNull) continue;
            // Test: inside quad or within HIT_PX of any edge.
            double dist = quadDist(corners, mouseX, mouseY);
            if (dist < bestDist) {
                bestDist = dist;
                best = p == 0 ? Plane.XY : p == 1 ? Plane.XZ : Plane.YZ;
            }
        }
        hoveredPlane = best;
    }

    // ── Drag ──────────────────────────────────────────────────────────────────

    /**
     * scaleA and scaleB are the current scales for the two axes of the hovered plane.
     * For XY: scaleA=scaleX, scaleB=scaleY. For XZ: scaleA=scaleX, scaleB=scaleZ. For YZ: scaleA=scaleY, scaleB=scaleZ.
     */
    public void startDrag(int mouseX, int mouseY, int sw, int sh, EntityLivingBase player, double gx, double gy,
        double gz, float scaleA, float scaleB, float rotX, float rotY, float rotZ) {
        if (hoveredPlane == Plane.NONE) return;
        dragPlane = hoveredPlane;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        startScaleA = scaleA;
        startScaleB = scaleB;

        int p = dragPlane == Plane.XY ? 0 : dragPlane == Plane.XZ ? 1 : 2;
        float[] R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);
        float[] axisAWorld = RotationGizmo.rotateVec(PLANE_A[p], R);
        float[] axisBWorld = RotationGizmo.rotateVec(PLANE_B[p], R);

        double[] os = proj.project(gx, gy, gz, sw, sh);
        double[] tsA = proj.project(gx + axisAWorld[0], gy + axisAWorld[1], gz + axisAWorld[2], sw, sh);
        double[] tsB = proj.project(gx + axisBWorld[0], gy + axisBWorld[1], gz + axisBWorld[2], sw, sh);
        if (os == null || tsA == null || tsB == null) {
            screenAxisAX = 1;
            screenAxisAY = 0;
            pixelsPerUnitA = 50;
            screenAxisBX = 0;
            screenAxisBY = 1;
            pixelsPerUnitB = 50;
            return;
        }
        double dax = tsA[0] - os[0], day = tsA[1] - os[1];
        double lenA = Math.sqrt(dax * dax + day * day);
        pixelsPerUnitA = Math.max(1.0, lenA);
        screenAxisAX = lenA > 0.001 ? dax / lenA : 1;
        screenAxisAY = lenA > 0.001 ? day / lenA : 0;

        double dbx = tsB[0] - os[0], dby = tsB[1] - os[1];
        double lenB = Math.sqrt(dbx * dbx + dby * dby);
        pixelsPerUnitB = Math.max(1.0, lenB);
        screenAxisBX = lenB > 0.001 ? dbx / lenB : 0;
        screenAxisBY = lenB > 0.001 ? dby / lenB : 1;
    }

    /**
     * Returns float[2] {newScaleA, newScaleB} or null if not dragging.
     */
    public float[] updateDrag(int mouseX, int mouseY) {
        if (dragPlane == Plane.NONE) return null;
        double dx = mouseX - dragStartMX, dy = mouseY - dragStartMY;
        float newScaleA = Math
            .max(0.1f, startScaleA + (float) ((dx * screenAxisAX + dy * screenAxisAY) / pixelsPerUnitA));
        float newScaleB = Math
            .max(0.1f, startScaleB + (float) ((dx * screenAxisBX + dy * screenAxisBY) / pixelsPerUnitB));
        return new float[] { newScaleA, newScaleB };
    }

    public void endDrag() {
        dragPlane = Plane.NONE;
    }

    /**
     * Returns 0 if (px,py) is inside the convex quad defined by 4 projected corners (in order),
     * otherwise returns the minimum distance to any of the 4 edges.
     */
    private static double quadDist(double[][] corners, double px, double py) {
        // Point-in-convex-quad: all cross products same sign (works for either winding).
        int pos = 0, neg = 0;
        for (int i = 0; i < 4; i++) {
            double[] a = corners[i], b = corners[(i + 1) % 4];
            double cross = (b[0] - a[0]) * (py - a[1]) - (b[1] - a[1]) * (px - a[0]);
            if (cross > 0) pos++;
            else if (cross < 0) neg++;
        }
        boolean inside = pos == 4 || neg == 4;
        if (inside) return 0;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double[] a = corners[i], b = corners[(i + 1) % 4];
            min = Math.min(min, RotationGizmo.segDist(a[0], a[1], b[0], b[1], px, py));
        }
        return min;
    }
}
