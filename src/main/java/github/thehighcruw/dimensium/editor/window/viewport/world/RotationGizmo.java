/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import com.github.bsideup.jabel.Desugar;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec2DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

/**
 * Rotation gizmo — 3 colored arcs, one per axis.
 * Dragging an arc rotates by the angle swept around the gizmo center.
 * Hit detection done via screen-space proximity to projected arc segments.
 */
public class RotationGizmo {

    public enum Axis {
        NONE,
        X,
        Y,
        Z
    }

    private static final float ARC_R = 1.5f;
    private static final int ARC_SEG = 24;
    private static final int HIT_PX = 4;
    /** Distance at which gizmo renders at its authored size. */
    private static final float GIZMO_REFERENCE_DIST = 10.0f;

    // Arc drawn in the plane perpendicular to each axis.
    // X arc: YZ plane, basis (0,1,0) × (0,0,1)
    // Y arc: XZ plane, basis (1,0,0) × (0,0,1)
    // Z arc: XY plane, basis (1,0,0) × (0,1,0)
    private static final Axis[] AXES = {Axis.X, Axis.Y, Axis.Z};

    private static final Vec3DFloat[] ARC_P1 = {
        Vec3DFloat.from(0, 1, 0), Vec3DFloat.from(1, 0, 0), Vec3DFloat.from(1, 0, 0)
    };
    private static final Vec3DFloat[] ARC_P2 = {
        Vec3DFloat.from(0, 0, 1), Vec3DFloat.from(0, 0, 1), Vec3DFloat.from(0, 1, 0)
    };
    private static final Vec3DFloat[] AXIS_COL = {
        Vec3DFloat.from(1.0f, 0.25f, 0.25f), Vec3DFloat.from(0.25f, 1.0f, 0.25f), Vec3DFloat.from(0.25f, 0.45f, 1.0f),
    };

    private final GizmoProjection proj = new GizmoProjection();

    public Axis hoveredAxis = Axis.NONE;
    private Axis dragAxis = Axis.NONE;
    // Angular drag state
    private Vec2DDouble centerScr = Vec2DDouble.ZERO;
    private Vec2DDouble arcBasis1 = Vec2DDouble.ZERO;
    private Vec2DDouble arcBasis2 = Vec2DDouble.ZERO;
    private double startAngle;
    private double dragSign;

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

    // ── Render ────────────────────────────────────────────────────────────────

    public void render(Vec3DDouble pos, Vec3DDouble camPos, Vec3DFloat rot) {
        beginRender(proj, pos, camPos, rot);
        Tessellator wt = Tessellator.instance;

        for (int a = 0; a < 3; a++) {
            boolean hot = hoveredAxis == AXES[a];
            Vec3DFloat col = AXIS_COL[a];
            Vec3DFloat p1 = ARC_P1[a], p2 = ARC_P2[a];

            if (hot) {
                GL11.glColor4f(1f, 1f, 1f, 0.20f);
                drawArc(wt, p1, p2, WorldLines.W_HOT);
            }

            GL11.glColor4f(col.x(), col.y(), col.z(), hot ? 1.0f : 0.50f);
            drawArc(wt, p1, p2, hot ? WorldLines.W_GIZMO : WorldLines.W_SEL);
        }

        GL11.glPopMatrix();
    }

    private static void drawArc(Tessellator tessellator, Vec3DFloat p1, Vec3DFloat p2, float halfW) {
        // Render ring as billboard quads via WorldLines — each segment is a consecutive pair
        // of points on the ring.
        WorldLines.prepareSegmentBatch();
        tessellator.startDrawingQuads();
        double prevCos = Math.cos(0) * ARC_R, prevSin = Math.sin(0) * ARC_R;
        for (int i = 1; i <= ARC_SEG; i++) {
            double ang = 2.0 * Math.PI * i / ARC_SEG;
            double cosVal = Math.cos(ang) * ARC_R, sinVal = Math.sin(ang) * ARC_R;
            Vec3DFloat from = p1.times((float) prevCos).plus(p2.times((float) prevSin));
            Vec3DFloat to = p1.times((float) cosVal).plus(p2.times((float) sinVal));
            WorldLines.addSegment(tessellator, from.x(), from.y(), from.z(), to.x(), to.y(), to.z(), halfW);
            prevCos = cosVal;
            prevSin = sinVal;
        }
        tessellator.draw();
    }

    // ── Hover ─────────────────────────────────────────────────────────────────

    public void updateHover(int mouseX, int mouseY, EntityLivingBase player, Vec3DDouble pos, Vec3DFloat rot) {
        HoverState hs = hoverState(player, pos, rot);
        float scaledR = ARC_R * hs.scale();
        Mat3DFloat R = hs.R();
        Axis best = Axis.NONE;
        double bestDist = HIT_PX;

        for (int a = 0; a < 3; a++) {
            Vec3DFloat rp1 = arcBasisP1(R, a);
            Vec3DFloat rp2 = arcBasisP2(R, a);
            Vec2DDouble prev = null;
            double minDist = Double.MAX_VALUE;

            for (int i = 0; i <= ARC_SEG; i++) {
                double ang = 2.0 * Math.PI * i / ARC_SEG;
                float cosVal = (float) (Math.cos(ang) * scaledR);
                float sinVal = (float) (Math.sin(ang) * scaledR);
                Vec2DDouble scr = proj.project(
                        pos.plus(rp1.times(cosVal).plus(rp2.times(sinVal)).toDouble()));
                if (scr == null) {
                    prev = null;
                    continue;
                }
                if (prev != null) {
                    double dist = segDist(prev, scr, Vec2DDouble.from(mouseX, mouseY));
                    if (dist < minDist) minDist = dist;
                }
                prev = scr;
            }

            if (minDist < bestDist) {
                bestDist = minDist;
                best = AXES[a];
            }
        }
        hoveredAxis = best;
    }

    // ── Drag ─────────────────────────────────────────────────────────────────

    /**
     * Begin drag. Records the screen-space axes of the arc plane and the
     * initial mouse angle relative to the projected gizmo center, so that
     * updateDrag can return the swept angle.
     */
    public void startDrag(int mouseX, int mouseY, Vec3DDouble pos, Vec3DFloat rot) {
        if (hoveredAxis == Axis.NONE) return;
        dragAxis = hoveredAxis;
        // Y arc's p1×p2 = -Y, so its atan2 winds opposite to X and Z arcs.
        dragSign = (hoveredAxis == Axis.Y) ? -1.0 : 1.0;

        Mat3DFloat R = ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z());
        int a = dragAxis == Axis.X ? 0 : dragAxis == Axis.Y ? 1 : 2; // index into ARC_P1/P2
        Vec3DFloat rp1 = arcBasisP1(R, a);
        Vec3DFloat rp2 = arcBasisP2(R, a);

        Vec2DDouble cScr = proj.project(pos);
        if (cScr == null) {
            dragAxis = Axis.NONE;
            return;
        }
        centerScr = cScr;

        // Screen-space direction of each arc basis vector (unit length)
        Vec2DDouble s1 = proj.project(pos.plus(rp1.toDouble()));
        Vec2DDouble s2 = proj.project(pos.plus(rp2.toDouble()));

        if (s1 == null || s2 == null) {
            arcBasis1 = Vec2DDouble.from(1, 0);
            arcBasis2 = Vec2DDouble.from(0, 1);
        } else {
            Vec2DDouble delta1 = s1.minus(centerScr);
            Vec2DDouble delta2 = s2.minus(centerScr);
            double len1 = delta1.length(), len2 = delta2.length();
            arcBasis1 = len1 > 0.001 ? delta1.divide(len1) : Vec2DDouble.from(1, 0);
            arcBasis2 = len2 > 0.001 ? delta2.divide(len2) : Vec2DDouble.from(0, 1);
        }

        Vec2DDouble mouseDelta = Vec2DDouble.from(mouseX - centerScr.x(), mouseY - centerScr.y());
        startAngle = Math.atan2(mouseDelta.dot(arcBasis2), mouseDelta.dot(arcBasis1));
    }

    /**
     * Returns total degree delta from drag start based on mouse angle around gizmo center.
     */
    public float updateDrag(int mouseX, int mouseY) {
        if (dragAxis == Axis.NONE) return 0f;
        Vec2DDouble mouseDelta = Vec2DDouble.from(mouseX - centerScr.x(), mouseY - centerScr.y());
        double currentAngle = Math.atan2(mouseDelta.dot(arcBasis2), mouseDelta.dot(arcBasis1));
        double delta = currentAngle - startAngle;
        while (delta > Math.PI) delta -= 2 * Math.PI;
        while (delta < -Math.PI) delta += 2 * Math.PI;
        double degrees = dragSign * Math.toDegrees(delta);
        float snap = DimensiumConfig.rotationSnapDegrees;
        if (snap > 0f) degrees = Math.round(degrees / snap) * snap;
        return (float) degrees;
    }

    public void endDrag() {
        dragAxis = Axis.NONE;
    }

    // ── Util ─────────────────────────────────────────────────────────────────

    @Desugar
    record HoverState(float scale, Mat3DFloat R) {}

    static float beginRender(GizmoProjection proj, Vec3DDouble pos, Vec3DDouble camPos, Vec3DFloat rot) {
        proj.capture(camPos);
        float scale = computeScale(pos.minus(camPos));
        setupGizmoMatrix(pos, camPos, rot, scale);
        return scale;
    }

    static HoverState hoverState(EntityLivingBase player, Vec3DDouble pos, Vec3DFloat rot) {
        Vec3DDouble eye = Vec3DDouble.from(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        float scale = computeScale(pos.minus(eye));
        return new HoverState(scale, ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z()));
    }

    static float computeScale(Vec3DDouble delta) {
        return (float) (delta.length() / GIZMO_REFERENCE_DIST);
    }

    static void setupGizmoMatrix(Vec3DDouble pos, Vec3DDouble camPos, Vec3DFloat rot, float scale) {
        Vec3DDouble delta = pos.minus(camPos);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPushMatrix();
        GL11.glTranslated(delta.x(), delta.y(), delta.z());
        GL11.glRotatef(rot.z(), 0, 0, 1);
        GL11.glRotatef(rot.y(), 0, 1, 0);
        GL11.glRotatef(rot.x(), 1, 0, 0);
        GL11.glScalef(scale, scale, scale);
        Vec3DDouble eyeLocal = delta.divide(scale);
        WorldLines.setEyeRotated(
                ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z()), eyeLocal.x(), eyeLocal.y(), eyeLocal.z());
    }

    private static Vec3DFloat arcBasisP1(Mat3DFloat R, int a) {
        return R.mul(ARC_P1[a]);
    }

    private static Vec3DFloat arcBasisP2(Mat3DFloat R, int a) {
        return R.mul(ARC_P2[a]);
    }

    static double segDist(Vec2DDouble segA, Vec2DDouble segB, Vec2DDouble point) {
        Vec2DDouble ab = segB.minus(segA);
        Vec2DDouble ap = point.minus(segA);
        double lengthSq = ab.lengthSq();
        double rayParam = lengthSq > 0 ? Math.max(0, Math.min(1, ap.dot(ab) / lengthSq)) : 0;
        return point.minus(segA.plus(ab.times(rayParam))).length();
    }
}
