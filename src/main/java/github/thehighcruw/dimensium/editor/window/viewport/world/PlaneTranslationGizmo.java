/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;

import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.shared.Vec2DDouble;
import github.thehighcruw.dimensium.shared.Vec3DDouble;

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
    private int dragStartMX;
    private int dragStartMY;
    private Vec3DDouble startAnchor = Vec3DDouble.ZERO;
    private float[] worldAxisA = new float[3];
    private float[] worldAxisB = new float[3];
    private Vec2DDouble screenAxisA = Vec2DDouble.ZERO;
    private double pixelsPerUnitA;
    private Vec2DDouble screenAxisB = Vec2DDouble.ZERO;
    private double pixelsPerUnitB;
    // Ray-based drag
    private boolean useRayDrag;
    private Vec3DDouble dragGizmo = Vec3DDouble.ZERO;
    private Vec3DDouble dragPlaneN = Vec3DDouble.ZERO;
    private Vec3DDouble dragStartH = Vec3DDouble.ZERO;

    public boolean isDragging() {
        return dragPlane != Plane.NONE;
    }

    public void reset() {
        hoveredPlane = Plane.NONE;
        dragPlane = Plane.NONE;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    public void render(double gx, double gy, double gz, Vec3DDouble camPos, float rotX, float rotY, float rotZ) {
        proj.capture(camPos);
        float scale = RotationGizmo.computeScale(gx - camPos.x(), gy - camPos.y(), gz - camPos.z());
        RotationGizmo.setupGizmoMatrix(gx, gy, gz, camPos, rotX, rotY, rotZ, scale);

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
            double[][] corners = new double[4][];
            corners[0] = proj.project(
                wcx + (-ha[0] - hb[0]) * (double) scale,
                wcy + (-ha[1] - hb[1]) * (double) scale,
                wcz + (-ha[2] - hb[2]) * (double) scale);
            corners[1] = proj.project(
                wcx + (ha[0] - hb[0]) * (double) scale,
                wcy + (ha[1] - hb[1]) * (double) scale,
                wcz + (ha[2] - hb[2]) * (double) scale);
            corners[2] = proj.project(
                wcx + (ha[0] + hb[0]) * (double) scale,
                wcy + (ha[1] + hb[1]) * (double) scale,
                wcz + (ha[2] + hb[2]) * (double) scale);
            corners[3] = proj.project(
                wcx + (-ha[0] + hb[0]) * (double) scale,
                wcy + (-ha[1] + hb[1]) * (double) scale,
                wcz + (-ha[2] + hb[2]) * (double) scale);
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
    public void startDrag(int mouseX, int mouseY, double gx, double gy, double gz, double anchorX, double anchorY,
        double anchorZ, float rotX, float rotY, float rotZ) {
        if (hoveredPlane == Plane.NONE) return;
        dragPlane = hoveredPlane;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        startAnchor = Vec3DDouble.from(anchorX, anchorY, anchorZ);
        dragGizmo = Vec3DDouble.from(gx, gy, gz);

        int p = dragPlane == Plane.XY ? 0 : dragPlane == Plane.XZ ? 1 : 2;
        float[] R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);
        worldAxisA = RotationGizmo.rotateVec(PLANE_A[p], R);
        worldAxisB = RotationGizmo.rotateVec(PLANE_B[p], R);

        // Plane normal = worldAxisA × worldAxisB
        dragPlaneN = Vec3DDouble.from(
            (double) worldAxisA[1] * worldAxisB[2] - (double) worldAxisA[2] * worldAxisB[1],
            (double) worldAxisA[2] * worldAxisB[0] - (double) worldAxisA[0] * worldAxisB[2],
            (double) worldAxisA[0] * worldAxisB[1] - (double) worldAxisA[1] * worldAxisB[0]);

        // Screen-based fallback setup
        double[] os = proj.project(gx, gy, gz);
        double[] tsA = proj.project(gx + worldAxisA[0], gy + worldAxisA[1], gz + worldAxisA[2]);
        double[] tsB = proj.project(gx + worldAxisB[0], gy + worldAxisB[1], gz + worldAxisB[2]);
        if (os == null || tsA == null || tsB == null) {
            screenAxisA = Vec2DDouble.from(1, 0);
            pixelsPerUnitA = 50;
            screenAxisB = Vec2DDouble.from(0, 1);
            pixelsPerUnitB = 50;
        } else {
            Vec2DDouble da = Vec2DDouble.from(tsA[0] - os[0], tsA[1] - os[1]);
            double lenA = da.length();
            pixelsPerUnitA = Math.max(1.0, lenA);
            screenAxisA = lenA > 0.001 ? da.divide(lenA) : Vec2DDouble.from(1, 0);
            Vec2DDouble db = Vec2DDouble.from(tsB[0] - os[0], tsB[1] - os[1]);
            double lenB = db.length();
            pixelsPerUnitB = Math.max(1.0, lenB);
            screenAxisB = lenB > 0.001 ? db.divide(lenB) : Vec2DDouble.from(0, 1);
        }

        // Ray-based drag: find initial hit on plane
        double[] ray = proj.unprojectRay(mouseX, mouseY);
        double[] hit = ray != null
            ? rayPlaneIntersect(
                ray,
                dragGizmo.x(),
                dragGizmo.y(),
                dragGizmo.z(),
                dragPlaneN.x(),
                dragPlaneN.y(),
                dragPlaneN.z())
            : null;
        if (hit != null) {
            dragStartH = Vec3DDouble.from(hit[0], hit[1], hit[2]);
            useRayDrag = true;
        } else {
            dragStartH = Vec3DDouble.ZERO;
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

    /** Returns updated anchor, or null if not dragging. */
    public Vec3DDouble updateDrag(int mouseX, int mouseY) {
        if (dragPlane == Plane.NONE) return null;
        if (useRayDrag) {
            double[] ray = proj.unprojectRay(mouseX, mouseY);
            if (ray != null) {
                double[] hit = rayPlaneIntersect(
                    ray,
                    dragGizmo.x(),
                    dragGizmo.y(),
                    dragGizmo.z(),
                    dragPlaneN.x(),
                    dragPlaneN.y(),
                    dragPlaneN.z());
                if (hit != null) {
                    return Vec3DDouble.from(
                        startAnchor.x() + (hit[0] - dragStartH.x()),
                        startAnchor.y() + (hit[1] - dragStartH.y()),
                        startAnchor.z() + (hit[2] - dragStartH.z()));
                }
            }
        }
        // Screen-based fallback
        Vec2DDouble dm = Vec2DDouble.from(mouseX - dragStartMX, mouseY - dragStartMY);
        double deltaA = dm.dot(screenAxisA) / pixelsPerUnitA;
        double deltaB = dm.dot(screenAxisB) / pixelsPerUnitB;
        return Vec3DDouble.from(
            startAnchor.x() + deltaA * worldAxisA[0] + deltaB * worldAxisB[0],
            startAnchor.y() + deltaA * worldAxisA[1] + deltaB * worldAxisB[1],
            startAnchor.z() + deltaA * worldAxisA[2] + deltaB * worldAxisB[2]);
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
