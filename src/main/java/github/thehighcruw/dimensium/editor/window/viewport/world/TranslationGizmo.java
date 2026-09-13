/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;

import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;

/**
 * Reusable translation gizmo — any tool can instantiate this.
 * Renders 3 colored arrows from a world-space center, hit-tests mouse proximity,
 * and computes integer drag deltas along each axis.
 */
public class TranslationGizmo {

    public enum Axis {
        NONE,
        X,
        Y,
        Z
    }

    private static final float ARM_LEN = 1.1f;
    private static final float CONE_H = 0.18f;
    private static final float CONE_R = 0.07f;
    private static final int SEG = 8;
    private static final int HIT_PX = 4;

    private final GizmoProjection proj = new GizmoProjection();

    private static final float[][] AXIS_DIR = { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
    // Perpendicular basis pairs for cone base ring, one pair per axis
    private static final float[][] CONE_P1 = { { 0, 1, 0 }, { 1, 0, 0 }, { 1, 0, 0 } };
    private static final float[][] CONE_P2 = { { 0, 0, 1 }, { 0, 0, 1 }, { 0, 1, 0 } };
    private static final float[][] AXIS_COL = { { 1.0f, 0.25f, 0.25f }, // X: red
        { 0.25f, 1.0f, 0.25f }, // Y: green
        { 0.25f, 0.45f, 1.0f }, // Z: blue
    };

    // Interaction state
    public Axis hoveredAxis = Axis.NONE;
    private Axis dragAxis = Axis.NONE;
    private int dragStartMX;
    private int dragStartMY;
    private double screenDx, screenDy, pixelsPerBlock;
    private double startAnchorX, startAnchorY, startAnchorZ;
    private float[] rotatedAxisDir = new float[3];
    // Ray-based drag
    private boolean useRayDrag;
    private double dragGizmoX, dragGizmoY, dragGizmoZ;
    private double dragStartT;

    /** Per-axis sign: 1 = arrow points in +axis direction, -1 = flipped. */
    public float[] axisFlip = { 1f, 1f, 1f };

    public GizmoProjection getProjection() {
        return proj;
    }

    public boolean isDragging() {
        return dragAxis != Axis.NONE;
    }

    public void reset() {
        hoveredAxis = Axis.NONE;
        dragAxis = Axis.NONE;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Render 3 arrows at world position (gx,gy,gz).
     * Must be called inside RenderWorldLastEvent with depth-test disabled and
     * the appropriate GL matrix already set up (world space, not translated yet).
     *
     * @param gx/gy/gz world-space gizmo center
     * @param rx/ry/rz interpolated player eye position (for glTranslated offset)
     */
    public void render(double gx, double gy, double gz, double rx, double ry, double rz, float rotX, float rotY,
        float rotZ) {
        proj.capture(rx, ry, rz);
        float scale = RotationGizmo.computeScale(gx - rx, gy - ry, gz - rz);
        RotationGizmo.setupGizmoMatrix(gx, gy, gz, rx, ry, rz, rotX, rotY, rotZ, scale);
        net.minecraft.client.renderer.Tessellator wt = net.minecraft.client.renderer.Tessellator.instance;

        for (int a = 0; a < 3; a++) {
            Axis axis = a == 0 ? Axis.X : a == 1 ? Axis.Y : Axis.Z;
            boolean hot = hoveredAxis == axis;
            float[] col = AXIS_COL[a];

            float[] dir = AXIS_DIR[a];
            float fx = dir[0] * axisFlip[a], fy = dir[1] * axisFlip[a], fz = dir[2] * axisFlip[a];
            float sx = fx * ARM_LEN, sy = fy * ARM_LEN, sz = fz * ARM_LEN;
            float tx = sx + fx * CONE_H, ty = sy + fy * CONE_H, tz = sz + fz * CONE_H;
            float[] p1 = CONE_P1[a], p2 = CONE_P2[a];

            if (hot) {
                // White glow pass as a wider billboard quad
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.25f);
                WorldLines.prepareSegmentBatch();
                wt.startDrawingQuads();
                WorldLines.addSegment(wt, 0, 0, 0, sx, sy, sz, WorldLines.W_HOT);
                wt.draw();
            }

            // Main arrow shaft
            GL11.glColor4f(col[0], col[1], col[2], hot ? 1.0f : 0.55f);
            WorldLines.prepareSegmentBatch();
            wt.startDrawingQuads();
            WorldLines.addSegment(wt, 0, 0, 0, sx, sy, sz, hot ? WorldLines.W_GIZMO : WorldLines.W_SEL);
            wt.draw();

            // Cone tip
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex3f(tx, ty, tz);
            for (int i = 0; i <= SEG; i++) {
                double ang = 2.0 * Math.PI * i / SEG;
                float c = (float) (Math.cos(ang) * CONE_R);
                float s = (float) (Math.sin(ang) * CONE_R);
                GL11.glVertex3f(sx + c * p1[0] + s * p2[0], sy + c * p1[1] + s * p2[1], sz + c * p1[2] + s * p2[2]);
            }
            GL11.glEnd();

            if (hot) {
                // White ring at cone base as billboard quads
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.9f);
                float ringR = CONE_R + 0.03f;
                WorldLines.prepareSegmentBatch();
                wt.startDrawingQuads();
                float prevC = (float) (Math.cos(0) * ringR), prevS = (float) (Math.sin(0) * ringR);
                for (int i = 1; i <= SEG; i++) {
                    double ang = 2.0 * Math.PI * i / SEG;
                    float c = (float) (Math.cos(ang) * ringR), s2 = (float) (Math.sin(ang) * ringR);
                    WorldLines.addSegment(
                        wt,
                        sx + prevC * p1[0] + prevS * p2[0],
                        sy + prevC * p1[1] + prevS * p2[1],
                        sz + prevC * p1[2] + prevS * p2[2],
                        sx + c * p1[0] + s2 * p2[0],
                        sy + c * p1[1] + s2 * p2[1],
                        sz + c * p1[2] + s2 * p2[2],
                        WorldLines.W_THIN);
                    prevC = c;
                    prevS = s2;
                }
                wt.draw();
            }
        }

        GL11.glPopMatrix();
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    /**
     * Update hoveredAxis from current mouse position.
     * Call every frame from drawScreen (when not dragging).
     */
    public void updateHover(int mouseX, int mouseY, EntityLivingBase player, double gx, double gy, double gz,
        float rotX, float rotY, float rotZ) {
        double eyeX = player.posX, eyeY = player.posY + player.getEyeHeight(), eyeZ = player.posZ;
        float scale = RotationGizmo.computeScale(gx - eyeX, gy - eyeY, gz - eyeZ);
        float scaledArm = (ARM_LEN + CONE_H) * scale;
        double[] origin = proj.project(gx, gy, gz);
        if (origin == null) {
            hoveredAxis = Axis.NONE;
            return;
        }

        float[] R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);
        Axis best = Axis.NONE;
        double bestDist = HIT_PX;

        for (int a = 0; a < 3; a++) {
            float[] base = { AXIS_DIR[a][0] * axisFlip[a], AXIS_DIR[a][1] * axisFlip[a], AXIS_DIR[a][2] * axisFlip[a] };
            float[] dir = RotationGizmo.rotateVec(base, R);
            double[] tip = proj.project(gx + dir[0] * scaledArm, gy + dir[1] * scaledArm, gz + dir[2] * scaledArm);
            if (tip == null) continue;

            double dist = RotationGizmo.segDist(origin[0], origin[1], tip[0], tip[1], mouseX, mouseY);
            if (dist < bestDist) {
                bestDist = dist;
                best = a == 0 ? Axis.X : a == 1 ? Axis.Y : Axis.Z;
            }
        }
        hoveredAxis = best;
    }

    /**
     * Begin dragging along the currently hovered axis.
     * anchorX/Y/Z is the shape anchor (not center).
     */
    public void startDrag(int mouseX, int mouseY, double gx, double gy, double gz, double anchorX, double anchorY,
        double anchorZ, float rotX, float rotY, float rotZ) {
        if (hoveredAxis == Axis.NONE) return;
        dragAxis = hoveredAxis;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        startAnchorX = anchorX;
        startAnchorY = anchorY;
        startAnchorZ = anchorZ;
        dragGizmoX = gx;
        dragGizmoY = gy;
        dragGizmoZ = gz;

        float[] R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);
        int a = dragAxis == Axis.X ? 0 : dragAxis == Axis.Y ? 1 : 2;
        float[] base = { AXIS_DIR[a][0] * axisFlip[a], AXIS_DIR[a][1] * axisFlip[a], AXIS_DIR[a][2] * axisFlip[a] };
        float[] dir = RotationGizmo.rotateVec(base, R);
        rotatedAxisDir = dir;

        // Screen-based fallback (used when ray unprojection fails)
        double[] os = proj.project(gx, gy, gz);
        double[] ts = proj.project(gx + dir[0], gy + dir[1], gz + dir[2]);
        if (os == null || ts == null) {
            screenDx = 1;
            screenDy = 0;
            pixelsPerBlock = 50;
        } else {
            double ddx = ts[0] - os[0], ddy = ts[1] - os[1];
            double len = Math.sqrt(ddx * ddx + ddy * ddy);
            pixelsPerBlock = Math.max(1.0, len);
            screenDx = len > 0.001 ? ddx / len : 1;
            screenDy = len > 0.001 ? ddy / len : 0;
        }

        // Ray-based drag: find initial parameter along axis
        double[] ray = proj.unprojectRay(mouseX, mouseY);
        if (ray != null) {
            dragStartT = closestAxisT(ray, gx, gy, gz, dir);
            useRayDrag = true;
        } else {
            dragStartT = 0;
            useRayDrag = false;
        }
    }

    /** Returns t such that gizmoCenter + t*axisDir is closest to the ray. */
    private static double closestAxisT(double[] ray, double px, double py, double pz, float[] axisDir) {
        double ox = ray[0], oy = ray[1], oz = ray[2];
        double dx = ray[3], dy = ray[4], dz = ray[5];
        double ax = axisDir[0], ay = axisDir[1], az = axisDir[2];
        double dDotA = dx * ax + dy * ay + dz * az;
        double aDoA = ax * ax + ay * ay + az * az;
        double denom = aDoA - dDotA * dDotA; // = 1 - cos²θ = sin²θ
        if (Math.abs(denom) < 1e-10) return 0; // ray parallel to axis
        double ex = px - ox, ey = py - oy, ez = pz - oz;
        double eDotA = ex * ax + ey * ay + ez * az;
        double eDotD = ex * dx + ey * dy + ez * dz;
        return (dDotA * eDotD - eDotA) / denom;
    }

    /**
     * Returns updated [anchorX, anchorY, anchorZ] based on mouse delta.
     * Returns null if not dragging.
     */
    /** Returns new float anchor [x, y, z], or null if not dragging. */
    public double[] updateDrag(int mouseX, int mouseY) {
        if (dragAxis == Axis.NONE) return null;
        if (useRayDrag) {
            double[] ray = proj.unprojectRay(mouseX, mouseY);
            if (ray != null) {
                double t = closestAxisT(ray, dragGizmoX, dragGizmoY, dragGizmoZ, rotatedAxisDir);
                double delta = t - dragStartT;
                return new double[] { startAnchorX + delta * rotatedAxisDir[0],
                    startAnchorY + delta * rotatedAxisDir[1], startAnchorZ + delta * rotatedAxisDir[2] };
            }
        }
        // Screen-based fallback
        double screenProj = (mouseX - dragStartMX) * screenDx + (mouseY - dragStartMY) * screenDy;
        double delta = screenProj / pixelsPerBlock;
        return new double[] { startAnchorX + delta * rotatedAxisDir[0], startAnchorY + delta * rotatedAxisDir[1],
            startAnchorZ + delta * rotatedAxisDir[2] };
    }

    public void endDrag() {
        dragAxis = Axis.NONE;
    }

}
