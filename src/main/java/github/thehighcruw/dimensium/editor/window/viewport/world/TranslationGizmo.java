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

    private static final Axis[] AXES = {Axis.X, Axis.Y, Axis.Z};

    private static final Vec3DFloat[] AXIS_DIR = {
        Vec3DFloat.from(1, 0, 0), Vec3DFloat.from(0, 1, 0), Vec3DFloat.from(0, 0, 1)
    };
    // Perpendicular basis pairs for cone base ring, one pair per axis
    private static final Vec3DFloat[] CONE_P1 = {
        Vec3DFloat.from(0, 1, 0), Vec3DFloat.from(1, 0, 0), Vec3DFloat.from(1, 0, 0)
    };
    private static final Vec3DFloat[] CONE_P2 = {
        Vec3DFloat.from(0, 0, 1), Vec3DFloat.from(0, 0, 1), Vec3DFloat.from(0, 1, 0)
    };
    private static final Vec3DFloat[] AXIS_COL = {
        Vec3DFloat.from(1.0f, 0.25f, 0.25f), // X: red
        Vec3DFloat.from(0.25f, 1.0f, 0.25f), // Y: green
        Vec3DFloat.from(0.25f, 0.45f, 1.0f), // Z: blue
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
    public void render(Vec3DDouble pos, Vec3DDouble camPos, Vec3DFloat rot) {
        RotationGizmo.beginRender(proj, pos, camPos, rot);
        Tessellator tessellator = Tessellator.instance;

        for (int a = 0; a < 3; a++) {
            boolean hot = hoveredAxis == AXES[a];
            Vec3DFloat color = AXIS_COL[a];

            Vec3DFloat flippedDir = AXIS_DIR[a].times(axisFlip[a]);
            Vec3DFloat shaft = flippedDir.times(ARM_LEN);
            Vec3DFloat tip = shaft.plus(flippedDir.times(CONE_H));
            Vec3DFloat p1 = CONE_P1[a], p2 = CONE_P2[a];

            if (hot) {
                // White glow pass as a wider billboard quad
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.25f);
                WorldLines.prepareSegmentBatch();
                tessellator.startDrawingQuads();
                WorldLines.addSegment(tessellator, 0, 0, 0, shaft.x(), shaft.y(), shaft.z(), WorldLines.W_HOT);
                tessellator.draw();
            }

            // Main arrow shaft
            GL11.glColor4f(color.x(), color.y(), color.z(), hot ? 1.0f : 0.55f);
            WorldLines.prepareSegmentBatch();
            tessellator.startDrawingQuads();
            WorldLines.addSegment(
                    tessellator, 0, 0, 0, shaft.x(), shaft.y(), shaft.z(), hot ? WorldLines.W_GIZMO : WorldLines.W_SEL);
            tessellator.draw();

            // Cone tip
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex3f(tip.x(), tip.y(), tip.z());
            for (int i = 0; i <= SEG; i++) {
                double ang = 2.0 * Math.PI * i / SEG;
                float cosVal = (float) (Math.cos(ang) * CONE_R);
                float sinVal = (float) (Math.sin(ang) * CONE_R);
                Vec3DFloat ringPt = shaft.plus(p1.times(cosVal)).plus(p2.times(sinVal));
                GL11.glVertex3f(ringPt.x(), ringPt.y(), ringPt.z());
            }
            GL11.glEnd();

            if (hot) {
                // White ring at cone base as billboard quads
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.9f);
                float ringR = CONE_R + 0.03f;
                WorldLines.prepareSegmentBatch();
                tessellator.startDrawingQuads();
                float prevCos = (float) (Math.cos(0) * ringR), prevSin = (float) (Math.sin(0) * ringR);
                for (int i = 1; i <= SEG; i++) {
                    double ang = 2.0 * Math.PI * i / SEG;
                    float cosVal = (float) (Math.cos(ang) * ringR), sinVal = (float) (Math.sin(ang) * ringR);
                    Vec3DFloat segStart = shaft.plus(p1.times(prevCos)).plus(p2.times(prevSin));
                    Vec3DFloat segEnd = shaft.plus(p1.times(cosVal)).plus(p2.times(sinVal));
                    WorldLines.addSegment(
                            tessellator,
                            segStart.x(),
                            segStart.y(),
                            segStart.z(),
                            segEnd.x(),
                            segEnd.y(),
                            segEnd.z(),
                            WorldLines.W_THIN);
                    prevCos = cosVal;
                    prevSin = sinVal;
                }
                tessellator.draw();
            }
        }

        GL11.glPopMatrix();
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    /**
     * Update hoveredAxis from current mouse position.
     * Call every frame from drawScreen (when not dragging).
     */
    public void updateHover(int mouseX, int mouseY, EntityLivingBase player, Vec3DDouble pos, Vec3DFloat rot) {
        RotationGizmo.HoverState hs = RotationGizmo.hoverState(player, pos, rot);
        float scaledArm = (ARM_LEN + CONE_H) * hs.scale();
        Vec2DDouble origin = proj.project(pos);
        if (origin == null) {
            hoveredAxis = Axis.NONE;
            return;
        }

        Mat3DFloat R = hs.R();
        Axis best = Axis.NONE;
        double bestDist = HIT_PX;

        for (int a = 0; a < 3; a++) {
            Vec3DFloat dir = rotatedAxis(R, a);
            Vec2DDouble tip = proj.project(pos.plus(dir.toDouble().times(scaledArm)));
            if (tip == null) continue;

            double dist = RotationGizmo.segDist(origin, tip, Vec2DDouble.from(mouseX, mouseY));
            if (dist < bestDist) {
                bestDist = dist;
                best = AXES[a];
            }
        }
        hoveredAxis = best;
    }

    /**
     * Begin dragging along the currently hovered axis.
     * anchorX/Y/Z is the shape anchor (not center).
     */
    public void startDrag(int mouseX, int mouseY, Vec3DDouble gizmoPos, Vec3DDouble anchor, Vec3DFloat rot) {
        if (hoveredAxis == Axis.NONE) return;
        dragAxis = hoveredAxis;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        startAnchor = anchor;
        dragGizmo = gizmoPos;

        Mat3DFloat R = ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z());
        int a = dragAxis == Axis.X ? 0 : dragAxis == Axis.Y ? 1 : 2;
        rotatedAxisDir = rotatedAxis(R, a);

        // Screen-based fallback (used when ray unprojection fails)
        GizmoProjection.ScreenAxis sa = proj.computeAxisScreenDir(dragGizmo, rotatedAxisDir);
        screenDir = sa.dir();
        pixelsPerBlock = sa.pixelsPerUnit();

        // Ray-based drag: find initial parameter along axis
        GizmoProjection.Ray ray = proj.unprojectRay(mouseX, mouseY);
        if (ray != null) {
            dragStartT = closestAxisT(ray, dragGizmo, rotatedAxisDir);
            useRayDrag = true;
        } else {
            dragStartT = 0;
            useRayDrag = false;
        }
    }

    private Vec3DFloat rotatedAxis(Mat3DFloat R, int a) {
        return R.mul(AXIS_DIR[a].times(axisFlip[a]));
    }

    /** Returns t such that gizmoCenter + t*axisDir is closest to the ray. */
    private static double closestAxisT(GizmoProjection.Ray ray, Vec3DDouble gizmoCenter, Vec3DFloat axisDir) {
        Vec3DDouble axis = axisDir.toDouble();
        double dDotA = ray.dir().dot(axis);
        double aDoA = axis.dot(axis);
        double denom = aDoA - dDotA * dDotA; // = 1 - cos²θ = sin²θ
        if (Math.abs(denom) < 1e-10) return 0; // ray parallel to axis
        Vec3DDouble e = gizmoCenter.minus(ray.origin());
        double eDotA = e.dot(axis);
        double eDotD = e.dot(ray.dir());
        return (dDotA * eDotD - eDotA) / denom;
    }

    /** Returns updated anchor, or null if not dragging. */
    public Vec3DDouble updateDrag(int mouseX, int mouseY) {
        if (dragAxis == Axis.NONE) return null;
        if (useRayDrag) {
            GizmoProjection.Ray ray = proj.unprojectRay(mouseX, mouseY);
            if (ray != null) {
                double t = closestAxisT(ray, dragGizmo, rotatedAxisDir);
                return anchorPlusDelta(t - dragStartT);
            }
        }
        // Screen-based fallback
        double screenProj =
                Vec2DDouble.from(mouseX - dragStartMX, mouseY - dragStartMY).dot(screenDir);
        return anchorPlusDelta(screenProj / pixelsPerBlock);
    }

    private Vec3DDouble anchorPlusDelta(double delta) {
        return startAnchor.plus(rotatedAxisDir.toDouble().times(delta));
    }

    public void endDrag() {
        dragAxis = Axis.NONE;
    }
}
