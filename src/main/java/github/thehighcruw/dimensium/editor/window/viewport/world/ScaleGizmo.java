/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;

import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;

/**
 * Single-axis scale gizmo — a colored box at the tip of each axis arrow.
 * Dragging along the axis scales the shape on that axis.
 * X = red, Y = green, Z = blue (matching TranslationGizmo colors).
 */
public class ScaleGizmo {

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

    private static final float[][] AXIS_DIR = { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
    private static final float[][] AXIS_COL = { { 1.0f, 0.25f, 0.25f }, // X: red
        { 0.25f, 1.0f, 0.25f }, // Y: green
        { 0.25f, 0.45f, 1.0f }, // Z: blue
    };

    private final GizmoProjection proj = new GizmoProjection();

    public Axis hoveredAxis = Axis.NONE;
    private Axis dragAxis = Axis.NONE;
    private int dragStartMX, dragStartMY;
    private float startScale;
    private double screenDx, screenDy, pixelsPerUnit;

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

    public void render(double gx, double gy, double gz, double rx, double ry, double rz, float rotX, float rotY,
        float rotZ) {
        proj.capture(rx, ry, rz);
        float scale = RotationGizmo.computeScale(gx - rx, gy - ry, gz - rz);
        RotationGizmo.setupGizmoMatrix(gx, gy, gz, rx, ry, rz, rotX, rotY, rotZ, scale);

        for (int a = 0; a < 3; a++) {
            Axis axis = a == 0 ? Axis.X : a == 1 ? Axis.Y : Axis.Z;
            boolean hot = hoveredAxis == axis;
            float[] col = AXIS_COL[a];
            float[] dir = AXIS_DIR[a];

            // Box center along this axis
            float bcx = dir[0] * BOX_CENTER, bcy = dir[1] * BOX_CENTER, bcz = dir[2] * BOX_CENTER;
            float h = BOX_HALF;

            // Per-axis perpendicular half-extents: for X axis, box extends in Y and Z, etc.
            // Compute two perpendicular half-extents
            float p1x, p1y, p1z, p2x, p2y, p2z;
            if (a == 0) { // X axis: perp = Y, Z
                p1x = 0;
                p1y = h;
                p1z = 0;
                p2x = 0;
                p2y = 0;
                p2z = h;
            } else if (a == 1) { // Y axis: perp = X, Z
                p1x = h;
                p1y = 0;
                p1z = 0;
                p2x = 0;
                p2y = 0;
                p2z = h;
            } else { // Z axis: perp = X, Y
                p1x = h;
                p1y = 0;
                p1z = 0;
                p2x = 0;
                p2y = h;
                p2z = 0;
            }
            // Along-axis half extent
            float p3x = dir[0] * h, p3y = dir[1] * h, p3z = dir[2] * h;

            float alpha = hot ? 0.95f : 0.6f;
            GL11.glColor4f(col[0], col[1], col[2], alpha);

            // 6 faces of the box
            renderBoxFace(bcx - p3x, bcy - p3y, bcz - p3z, p1x, p1y, p1z, p2x, p2y, p2z); // back face
            renderBoxFace(bcx + p3x, bcy + p3y, bcz + p3z, p2x, p2y, p2z, p1x, p1y, p1z); // front face
            renderBoxFace(bcx - p1x, bcy - p1y, bcz - p1z, p3x, p3y, p3z, p2x, p2y, p2z); // left face
            renderBoxFace(bcx + p1x, bcy + p1y, bcz + p1z, p2x, p2y, p2z, p3x, p3y, p3z); // right face
            renderBoxFace(bcx - p2x, bcy - p2y, bcz - p2z, p1x, p1y, p1z, p3x, p3y, p3z); // bottom face
            renderBoxFace(bcx + p2x, bcy + p2y, bcz + p2z, p3x, p3y, p3z, p1x, p1y, p1z); // top face

            // Outline
            if (hot) {
                GL11.glColor4f(1f, 1f, 1f, 0.9f);
            } else {
                GL11.glColor4f(col[0] * 0.7f, col[1] * 0.7f, col[2] * 0.7f, 0.9f);
            }
            renderBoxEdges(bcx, bcy, bcz, p1x, p1y, p1z, p2x, p2y, p2z, p3x, p3y, p3z);
        }

        GL11.glPopMatrix();
    }

    private static void renderBoxFace(float cx, float cy, float cz, float ax, float ay, float az, float bx, float by,
        float bz) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(cx - ax - bx, cy - ay - by, cz - az - bz);
        GL11.glVertex3f(cx + ax - bx, cy + ay - by, cz + az - bz);
        GL11.glVertex3f(cx + ax + bx, cy + ay + by, cz + az + bz);
        GL11.glVertex3f(cx - ax + bx, cy - ay + by, cz - az + bz);
        GL11.glEnd();
    }

    private static void renderBoxEdges(float cx, float cy, float cz, float p1x, float p1y, float p1z, float p2x,
        float p2y, float p2z, float p3x, float p3y, float p3z) {
        // 8 corners
        float[][] v = { { cx - p1x - p2x - p3x, cy - p1y - p2y - p3y, cz - p1z - p2z - p3z },
            { cx + p1x - p2x - p3x, cy + p1y - p2y - p3y, cz + p1z - p2z - p3z },
            { cx + p1x + p2x - p3x, cy + p1y + p2y - p3y, cz + p1z + p2z - p3z },
            { cx - p1x + p2x - p3x, cy - p1y + p2y - p3y, cz - p1z + p2z - p3z },
            { cx - p1x - p2x + p3x, cy - p1y - p2y + p3y, cz - p1z - p2z + p3z },
            { cx + p1x - p2x + p3x, cy + p1y - p2y + p3y, cz + p1z - p2z + p3z },
            { cx + p1x + p2x + p3x, cy + p1y + p2y + p3y, cz + p1z + p2z + p3z },
            { cx - p1x + p2x + p3x, cy - p1y + p2y + p3y, cz - p1z + p2z + p3z } };
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

    public void updateHover(int mouseX, int mouseY, int sw, int sh, EntityLivingBase player, double gx, double gy,
        double gz, float rotX, float rotY, float rotZ) {
        double eyeX = player.posX, eyeY = player.posY + player.getEyeHeight(), eyeZ = player.posZ;
        float scale = RotationGizmo.computeScale(gx - eyeX, gy - eyeY, gz - eyeZ);
        float[] R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);

        Axis best = Axis.NONE;
        double bestDist = HIT_PX;

        for (int a = 0; a < 3; a++) {
            float[] dirRot = RotationGizmo.rotateVec(AXIS_DIR[a], R);
            double wcx = gx + dirRot[0] * BOX_CENTER * scale;
            double wcy = gy + dirRot[1] * BOX_CENTER * scale;
            double wcz = gz + dirRot[2] * BOX_CENTER * scale;
            double[] sc = proj.project(wcx, wcy, wcz, sw, sh);
            if (sc == null) continue;
            double dx = sc[0] - mouseX, dy = sc[1] - mouseY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < bestDist) {
                bestDist = dist;
                best = a == 0 ? Axis.X : a == 1 ? Axis.Y : Axis.Z;
            }
        }
        hoveredAxis = best;
    }

    // ── Drag ──────────────────────────────────────────────────────────────────

    /**
     * startScale is the current scale value for the hovered axis.
     */
    public void startDrag(int mouseX, int mouseY, int sw, int sh, EntityLivingBase player, double gx, double gy,
        double gz, float scale, float rotX, float rotY, float rotZ) {
        if (hoveredAxis == Axis.NONE) return;
        dragAxis = hoveredAxis;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        startScale = scale;

        int a = dragAxis == Axis.X ? 0 : dragAxis == Axis.Y ? 1 : 2;
        float[] R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);
        float[] dir = RotationGizmo.rotateVec(AXIS_DIR[a], R);

        double[] os = proj.project(gx, gy, gz, sw, sh);
        double[] ts = proj.project(gx + dir[0], gy + dir[1], gz + dir[2], sw, sh);
        if (os == null || ts == null) {
            screenDx = 1;
            screenDy = 0;
            pixelsPerUnit = 50;
            return;
        }
        double ddx = ts[0] - os[0], ddy = ts[1] - os[1];
        double len = Math.sqrt(ddx * ddx + ddy * ddy);
        pixelsPerUnit = Math.max(1.0, len);
        screenDx = len > 0.001 ? ddx / len : 1;
        screenDy = len > 0.001 ? ddy / len : 0;
    }

    /**
     * Returns float[1] {newScale} or null if not dragging.
     */
    public float[] updateDrag(int mouseX, int mouseY) {
        if (dragAxis == Axis.NONE) return null;
        double proj = (mouseX - dragStartMX) * screenDx + (mouseY - dragStartMY) * screenDy;
        float newScale = Math.max(0.1f, startScale + (float) (proj / pixelsPerUnit) * SCALE_SENSITIVITY);
        return new float[] { newScale };
    }

    public void endDrag() {
        dragAxis = Axis.NONE;
    }
}
