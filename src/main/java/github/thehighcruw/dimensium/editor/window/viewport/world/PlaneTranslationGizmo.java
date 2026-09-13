/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;

import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;

/**
 * Plane-translation gizmo — 3 small colored squares between axis arrow pairs.
 * Each square lets the user translate a shape on 2 axes simultaneously.
 * XY = yellow, XZ = magenta, YZ = cyan.
 */
public class PlaneTranslationGizmo {

    public enum Plane {
        NONE,
        XY,
        XZ,
        YZ
    }

    private static final float SQ_POS = 0.50f;
    private static final float SQ_HALF = 0.09f;
    private static final double HIT_PX = 4.0;

    private static final float[][] CENTERS = { { SQ_POS, SQ_POS, 0 }, // XY
        { SQ_POS, 0, SQ_POS }, // XZ
        { 0, SQ_POS, SQ_POS }, // YZ
    };

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
    private int dragStartMX, dragStartMY, dragSW, dragSH;
    private double startAnchorX, startAnchorY, startAnchorZ;
    private float[] worldAxisA = new float[3];
    private float[] worldAxisB = new float[3];
    private double screenAxisAX, screenAxisAY, pixelsPerUnitA;
    private double screenAxisBX, screenAxisBY, pixelsPerUnitB;
    // Ray-based drag
    private boolean useRayDrag;
    private double dragGizmoX, dragGizmoY, dragGizmoZ;
    private double dragPlaneNX, dragPlaneNY, dragPlaneNZ;
    private double dragStartHX, dragStartHY, dragStartHZ;

    public boolean isDragging() {
        return dragPlane != Plane.NONE;
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

    public void updateHover(int mouseX, int mouseY, EntityLivingBase player, double gx, double gy, double gz,
        float rotX, float rotY, float rotZ) {
        double eyeX = player.posX, eyeY = player.posY + player.getEyeHeight(), eyeZ = player.posZ;
        float scale = RotationGizmo.computeScale(gx - eyeX, gy - eyeY, gz - eyeZ);
        float[] R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);

        Plane best = Plane.NONE;
        double bestDist = HIT_PX;

        for (int p = 0; p < 3; p++) {
            float cx = CENTERS[p][0], cy = CENTERS[p][1], cz = CENTERS[p][2];
            float[] a = PLANE_A[p], b = PLANE_B[p];
            float[] ha = RotationGizmo.rotateVec(new float[] { a[0] * SQ_HALF, a[1] * SQ_HALF, a[2] * SQ_HALF }, R);
            float[] hb = RotationGizmo.rotateVec(new float[] { b[0] * SQ_HALF, b[1] * SQ_HALF, b[2] * SQ_HALF }, R);
            float[] cRot = RotationGizmo.rotateVec(new float[] { cx, cy, cz }, R);
            double wcx = gx + cRot[0] * scale, wcy = gy + cRot[1] * scale, wcz = gz + cRot[2] * scale;
            double s = scale;
            double[][] corners = new double[4][];
            corners[0] = proj
                .project(wcx + (-ha[0] - hb[0]) * s, wcy + (-ha[1] - hb[1]) * s, wcz + (-ha[2] - hb[2]) * s);
            corners[1] = proj.project(wcx + (ha[0] - hb[0]) * s, wcy + (ha[1] - hb[1]) * s, wcz + (ha[2] - hb[2]) * s);
            corners[2] = proj.project(wcx + (ha[0] + hb[0]) * s, wcy + (ha[1] + hb[1]) * s, wcz + (ha[2] + hb[2]) * s);
            corners[3] = proj
                .project(wcx + (-ha[0] + hb[0]) * s, wcy + (-ha[1] + hb[1]) * s, wcz + (-ha[2] + hb[2]) * s);
            boolean anyNull = false;
            for (double[] c : corners) if (c == null) {
                anyNull = true;
                break;
            }
            if (anyNull) continue;
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
     * anchorX/Y/Z is the current shape anchor position in world space.
     * Drag moves the anchor along the two axes of the hovered plane.
     */
    public void startDrag(int mouseX, int mouseY, int sw, int sh, double gx, double gy, double gz, double anchorX,
        double anchorY, double anchorZ, float rotX, float rotY, float rotZ) {
        if (hoveredPlane == Plane.NONE) return;
        dragPlane = hoveredPlane;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        dragSW = sw;
        dragSH = sh;
        startAnchorX = anchorX;
        startAnchorY = anchorY;
        startAnchorZ = anchorZ;
        dragGizmoX = gx;
        dragGizmoY = gy;
        dragGizmoZ = gz;

        int p = dragPlane == Plane.XY ? 0 : dragPlane == Plane.XZ ? 1 : 2;
        float[] R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);
        worldAxisA = RotationGizmo.rotateVec(PLANE_A[p], R);
        worldAxisB = RotationGizmo.rotateVec(PLANE_B[p], R);

        // Plane normal = worldAxisA × worldAxisB
        dragPlaneNX = (double) worldAxisA[1] * worldAxisB[2] - (double) worldAxisA[2] * worldAxisB[1];
        dragPlaneNY = (double) worldAxisA[2] * worldAxisB[0] - (double) worldAxisA[0] * worldAxisB[2];
        dragPlaneNZ = (double) worldAxisA[0] * worldAxisB[1] - (double) worldAxisA[1] * worldAxisB[0];

        // Screen-based fallback setup
        double[] os = proj.project(gx, gy, gz);
        double[] tsA = proj.project(gx + worldAxisA[0], gy + worldAxisA[1], gz + worldAxisA[2]);
        double[] tsB = proj.project(gx + worldAxisB[0], gy + worldAxisB[1], gz + worldAxisB[2]);
        if (os == null || tsA == null || tsB == null) {
            screenAxisAX = 1;
            screenAxisAY = 0;
            pixelsPerUnitA = 50;
            screenAxisBX = 0;
            screenAxisBY = 1;
            pixelsPerUnitB = 50;
        } else {
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

        // Ray-based drag: find initial hit on plane
        double[] ray = proj.unprojectRay(mouseX, mouseY);
        double[] hit = ray != null ? rayPlaneIntersect(ray, gx, gy, gz, dragPlaneNX, dragPlaneNY, dragPlaneNZ) : null;
        if (hit != null) {
            dragStartHX = hit[0];
            dragStartHY = hit[1];
            dragStartHZ = hit[2];
            useRayDrag = true;
        } else {
            dragStartHX = dragStartHY = dragStartHZ = 0;
            useRayDrag = false;
        }
    }

    private static double[] rayPlaneIntersect(double[] ray, double px, double py, double pz, double nx, double ny,
        double nz) {
        double dDotN = ray[3] * nx + ray[4] * ny + ray[5] * nz;
        if (Math.abs(dDotN) < 1e-10) return null;
        double t = ((px - ray[0]) * nx + (py - ray[1]) * ny + (pz - ray[2]) * nz) / dDotN;
        return new double[] { ray[0] + t * ray[3], ray[1] + t * ray[4], ray[2] + t * ray[5] };
    }

    /**
     * Returns double[3] {newAnchorX, newAnchorY, newAnchorZ} or null if not dragging.
     */
    public double[] updateDrag(int mouseX, int mouseY) {
        if (dragPlane == Plane.NONE) return null;
        if (useRayDrag) {
            double[] ray = proj.unprojectRay(mouseX, mouseY);
            if (ray != null) {
                double[] hit = rayPlaneIntersect(
                    ray,
                    dragGizmoX,
                    dragGizmoY,
                    dragGizmoZ,
                    dragPlaneNX,
                    dragPlaneNY,
                    dragPlaneNZ);
                if (hit != null) {
                    return new double[] { startAnchorX + (hit[0] - dragStartHX), startAnchorY + (hit[1] - dragStartHY),
                        startAnchorZ + (hit[2] - dragStartHZ) };
                }
            }
        }
        // Screen-based fallback
        double dx = mouseX - dragStartMX, dy = mouseY - dragStartMY;
        double deltaA = (dx * screenAxisAX + dy * screenAxisAY) / pixelsPerUnitA;
        double deltaB = (dx * screenAxisBX + dy * screenAxisBY) / pixelsPerUnitB;
        return new double[] { startAnchorX + deltaA * worldAxisA[0] + deltaB * worldAxisB[0],
            startAnchorY + deltaA * worldAxisA[1] + deltaB * worldAxisB[1],
            startAnchorZ + deltaA * worldAxisA[2] + deltaB * worldAxisB[2] };
    }

    public void endDrag() {
        dragPlane = Plane.NONE;
    }

    private static double quadDist(double[][] corners, double px, double py) {
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
