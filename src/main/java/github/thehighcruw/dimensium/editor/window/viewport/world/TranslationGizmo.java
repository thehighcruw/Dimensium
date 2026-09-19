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
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

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

    private static final float[][] AXIS_DIR = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    // Perpendicular basis pairs for cone base ring, one pair per axis
    private static final float[][] CONE_P1 = {{0, 1, 0}, {1, 0, 0}, {1, 0, 0}};
    private static final float[][] CONE_P2 = {{0, 0, 1}, {0, 0, 1}, {0, 1, 0}};
    private static final float[][] AXIS_COL = {
        {1.0f, 0.25f, 0.25f}, // X: red
        {0.25f, 1.0f, 0.25f}, // Y: green
        {0.25f, 0.45f, 1.0f}, // Z: blue
    };

    // Interaction state
    public Axis hoveredAxis = Axis.NONE;
    private Axis dragAxis = Axis.NONE;
    private int dragStartMX;
    private int dragStartMY;
    private Vec2DDouble screenDir = Vec2DDouble.ZERO;
    private double pixelsPerBlock;
    private Vec3DDouble startAnchor = Vec3DDouble.ZERO;
    private Vec3DFloat rotatedAxisDir = Vec3DFloat.ZERO;
    // Ray-based drag
    private boolean useRayDrag;
    private Vec3DDouble dragGizmo = Vec3DDouble.ZERO;
    private double dragStartT;

    /** Per-axis sign: 1 = arrow points in +axis direction, -1 = flipped. */
    public final float[] axisFlip = {1f, 1f, 1f};

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
    public void render(double gx, double gy, double gz, Vec3DDouble camPos, float rotX, float rotY, float rotZ) {
        RotationGizmo.beginRender(proj, gx, gy, gz, camPos, rotX, rotY, rotZ);
        Tessellator wt = Tessellator.instance;

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
        float scaledArm = (ARM_LEN + CONE_H) * hs.scale();
        double[] origin = proj.project(gx, gy, gz);
        if (origin == null) {
            hoveredAxis = Axis.NONE;
            return;
        }

        Mat3DFloat R = hs.R();
        Axis best = Axis.NONE;
        double bestDist = HIT_PX;

        for (int a = 0; a < 3; a++) {
            Vec3DFloat dir = rotatedAxis(R, a);
            double[] tip = proj.project(gx + dir.x() * scaledArm, gy + dir.y() * scaledArm, gz + dir.z() * scaledArm);
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
    public void startDrag(
            int mouseX,
            int mouseY,
            double gx,
            double gy,
            double gz,
            double anchorX,
            double anchorY,
            double anchorZ,
            float rotX,
            float rotY,
            float rotZ) {
        if (hoveredAxis == Axis.NONE) return;
        dragAxis = hoveredAxis;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        startAnchor = Vec3DDouble.from(anchorX, anchorY, anchorZ);
        dragGizmo = Vec3DDouble.from(gx, gy, gz);

        Mat3DFloat R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);
        int a = dragAxis == Axis.X ? 0 : dragAxis == Axis.Y ? 1 : 2;
        rotatedAxisDir = rotatedAxis(R, a);

        // Screen-based fallback (used when ray unprojection fails)
        GizmoProjection.ScreenAxis sa = proj.computeAxisScreenDir(gx, gy, gz, rotatedAxisDir);
        screenDir = sa.dir();
        pixelsPerBlock = sa.pixelsPerUnit();

        // Ray-based drag: find initial parameter along axis
        double[] ray = proj.unprojectRay(mouseX, mouseY);
        if (ray != null) {
            dragStartT = closestAxisTAtGizmo(ray);
            useRayDrag = true;
        } else {
            dragStartT = 0;
            useRayDrag = false;
        }
    }

    private Vec3DFloat rotatedAxis(Mat3DFloat R, int a) {
        return R.mul(Vec3DFloat.from(
                AXIS_DIR[a][0] * axisFlip[a], AXIS_DIR[a][1] * axisFlip[a], AXIS_DIR[a][2] * axisFlip[a]));
    }

    /** Returns t such that gizmoCenter + t*axisDir is closest to the ray. */
    private static double closestAxisT(double[] ray, double px, double py, double pz, Vec3DFloat axisDir) {
        double ox = ray[0], oy = ray[1], oz = ray[2];
        double dx = ray[3], dy = ray[4], dz = ray[5];
        double ax = axisDir.x(), ay = axisDir.y(), az = axisDir.z();
        double dDotA = dx * ax + dy * ay + dz * az;
        double aDoA = ax * ax + ay * ay + az * az;
        double denom = aDoA - dDotA * dDotA; // = 1 - cos²θ = sin²θ
        if (Math.abs(denom) < 1e-10) return 0; // ray parallel to axis
        double ex = px - ox, ey = py - oy, ez = pz - oz;
        double eDotA = ex * ax + ey * ay + ez * az;
        double eDotD = ex * dx + ey * dy + ez * dz;
        return (dDotA * eDotD - eDotA) / denom;
    }

    /** Returns updated anchor, or null if not dragging. */
    public Vec3DDouble updateDrag(int mouseX, int mouseY) {
        if (dragAxis == Axis.NONE) return null;
        if (useRayDrag) {
            double[] ray = proj.unprojectRay(mouseX, mouseY);
            if (ray != null) {
                return anchorPlusDelta(closestAxisTAtGizmo(ray) - dragStartT);
            }
        }
        // Screen-based fallback
        double screenProj =
                Vec2DDouble.from(mouseX - dragStartMX, mouseY - dragStartMY).dot(screenDir);
        return anchorPlusDelta(screenProj / pixelsPerBlock);
    }

    private double closestAxisTAtGizmo(double[] ray) {
        return closestAxisT(ray, dragGizmo.x(), dragGizmo.y(), dragGizmo.z(), rotatedAxisDir);
    }

    private Vec3DDouble anchorPlusDelta(double delta) {
        return startAnchor.plus(rotatedAxisDir.toDouble().times(delta));
    }

    public void endDrag() {
        dragAxis = Axis.NONE;
    }
}
