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
    private static final float[][] ARC_P1 = {{0, 1, 0}, {1, 0, 0}, {1, 0, 0}};
    private static final float[][] ARC_P2 = {{0, 0, 1}, {0, 0, 1}, {0, 1, 0}};
    private static final float[][] AXIS_COL = {
        {1.0f, 0.25f, 0.25f}, {0.25f, 1.0f, 0.25f}, {0.25f, 0.45f, 1.0f},
    };

    private final GizmoProjection proj = new GizmoProjection();

    public Axis hoveredAxis = Axis.NONE;
    private Axis dragAxis = Axis.NONE;
    // Angular drag state
    private Vec2DDouble centerScr = Vec2DDouble.ZERO;
    private Vec2DDouble e1 = Vec2DDouble.ZERO;
    private Vec2DDouble e2 = Vec2DDouble.ZERO;
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

    public void render(double gx, double gy, double gz, Vec3DDouble camPos, float rotX, float rotY, float rotZ) {
        beginRender(proj, gx, gy, gz, camPos, rotX, rotY, rotZ);
        Tessellator wt = Tessellator.instance;

        for (int a = 0; a < 3; a++) {
            Axis axis = a == 0 ? Axis.X : a == 1 ? Axis.Y : Axis.Z;
            boolean hot = hoveredAxis == axis;
            float[] col = AXIS_COL[a];
            float[] p1 = ARC_P1[a], p2 = ARC_P2[a];

            if (hot) {
                GL11.glColor4f(1f, 1f, 1f, 0.20f);
                drawArc(wt, p1, p2, WorldLines.W_HOT);
            }

            GL11.glColor4f(col[0], col[1], col[2], hot ? 1.0f : 0.50f);
            drawArc(wt, p1, p2, hot ? WorldLines.W_GIZMO : WorldLines.W_SEL);
        }

        GL11.glPopMatrix();
    }

    private static void drawArc(Tessellator t, float[] p1, float[] p2, float halfW) {
        // Render ring as billboard quads via WorldLines — each segment is a consecutive pair
        // of points on the ring.
        WorldLines.prepareSegmentBatch();
        t.startDrawingQuads();
        double prevC = Math.cos(0) * ARC_R, prevS = Math.sin(0) * ARC_R;
        for (int i = 1; i <= ARC_SEG; i++) {
            double ang = 2.0 * Math.PI * i / ARC_SEG;
            double c = Math.cos(ang) * ARC_R, s = Math.sin(ang) * ARC_R;
            WorldLines.addSegment(
                    t,
                    prevC * p1[0] + prevS * p2[0],
                    prevC * p1[1] + prevS * p2[1],
                    prevC * p1[2] + prevS * p2[2],
                    c * p1[0] + s * p2[0],
                    c * p1[1] + s * p2[1],
                    c * p1[2] + s * p2[2],
                    halfW);
            prevC = c;
            prevS = s;
        }
        t.draw();
    }

    // ── Hover ─────────────────────────────────────────────────────────────────

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
        HoverState hs = hoverState(player, gx, gy, gz, rotX, rotY, rotZ);
        float scaledR = ARC_R * hs.scale();
        Mat3DFloat R = hs.R();
        Axis best = Axis.NONE;
        double bestDist = HIT_PX;

        for (int a = 0; a < 3; a++) {
            Vec3DFloat rp1 = arcBasisP1(R, a);
            Vec3DFloat rp2 = arcBasisP2(R, a);
            double[] prev = null;
            double minD = Double.MAX_VALUE;

            for (int i = 0; i <= ARC_SEG; i++) {
                double ang = 2.0 * Math.PI * i / ARC_SEG;
                float c = (float) (Math.cos(ang) * scaledR);
                float s = (float) (Math.sin(ang) * scaledR);
                double[] scr = proj.project(
                        gx + c * rp1.x() + s * rp2.x(), gy + c * rp1.y() + s * rp2.y(), gz + c * rp1.z() + s * rp2.z());
                if (scr == null) {
                    prev = null;
                    continue;
                }
                if (prev != null) {
                    double d = segDist(prev[0], prev[1], scr[0], scr[1], mouseX, mouseY);
                    if (d < minD) minD = d;
                }
                prev = scr;
            }

            if (minD < bestDist) {
                bestDist = minD;
                best = a == 0 ? Axis.X : a == 1 ? Axis.Y : Axis.Z;
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
    public void startDrag(int mouseX, int mouseY, double gx, double gy, double gz, float rotX, float rotY, float rotZ) {
        if (hoveredAxis == Axis.NONE) return;
        dragAxis = hoveredAxis;
        // Y arc's p1×p2 = -Y, so its atan2 winds opposite to X and Z arcs.
        dragSign = (hoveredAxis == Axis.Y) ? -1.0 : 1.0;

        Mat3DFloat R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);
        int a = dragAxis == Axis.X ? 0 : dragAxis == Axis.Y ? 1 : 2;
        Vec3DFloat rp1 = arcBasisP1(R, a);
        Vec3DFloat rp2 = arcBasisP2(R, a);

        double[] cScr = proj.project(gx, gy, gz);
        if (cScr == null) {
            dragAxis = Axis.NONE;
            return;
        }
        centerScr = Vec2DDouble.from(cScr[0], cScr[1]);

        // Screen-space direction of each arc basis vector (unit length)
        double[] s1 = proj.project(gx + rp1.x(), gy + rp1.y(), gz + rp1.z());
        double[] s2 = proj.project(gx + rp2.x(), gy + rp2.y(), gz + rp2.z());

        if (s1 == null || s2 == null) {
            e1 = Vec2DDouble.from(1, 0);
            e2 = Vec2DDouble.from(0, 1);
        } else {
            Vec2DDouble d1 = Vec2DDouble.from(s1[0] - centerScr.x(), s1[1] - centerScr.y());
            Vec2DDouble d2 = Vec2DDouble.from(s2[0] - centerScr.x(), s2[1] - centerScr.y());
            double l1 = d1.length(), l2 = d2.length();
            e1 = l1 > 0.001 ? d1.divide(l1) : Vec2DDouble.from(1, 0);
            e2 = l2 > 0.001 ? d2.divide(l2) : Vec2DDouble.from(0, 1);
        }

        Vec2DDouble dm = Vec2DDouble.from(mouseX - centerScr.x(), mouseY - centerScr.y());
        startAngle = Math.atan2(dm.dot(e2), dm.dot(e1));
    }

    /**
     * Returns total degree delta from drag start based on mouse angle around gizmo center.
     */
    public float updateDrag(int mouseX, int mouseY) {
        if (dragAxis == Axis.NONE) return 0f;
        Vec2DDouble dm = Vec2DDouble.from(mouseX - centerScr.x(), mouseY - centerScr.y());
        double currentAngle = Math.atan2(dm.dot(e2), dm.dot(e1));
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

    static float beginRender(
            GizmoProjection proj,
            double gx,
            double gy,
            double gz,
            Vec3DDouble camPos,
            float rotX,
            float rotY,
            float rotZ) {
        proj.capture(camPos);
        float scale = computeScale(gx - camPos.x(), gy - camPos.y(), gz - camPos.z());
        setupGizmoMatrix(gx, gy, gz, camPos, rotX, rotY, rotZ, scale);
        return scale;
    }

    static HoverState hoverState(
            EntityLivingBase player, double gx, double gy, double gz, float rotX, float rotY, float rotZ) {
        double eyeX = player.posX, eyeY = player.posY + player.getEyeHeight(), eyeZ = player.posZ;
        float scale = computeScale(gx - eyeX, gy - eyeY, gz - eyeZ);
        return new HoverState(scale, ShapeMath.buildRotationMatrix(rotX, rotY, rotZ));
    }

    static float computeScale(double dgx, double dgy, double dgz) {
        double dist = Math.sqrt(dgx * dgx + dgy * dgy + dgz * dgz);
        return (float) (dist / GIZMO_REFERENCE_DIST);
    }

    static void setupGizmoMatrix(
            double gx, double gy, double gz, Vec3DDouble camPos, float rotX, float rotY, float rotZ, float scale) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPushMatrix();
        GL11.glTranslated(gx - camPos.x(), gy - camPos.y(), gz - camPos.z());
        GL11.glRotatef(rotZ, 0, 0, 1);
        GL11.glRotatef(rotY, 0, 1, 0);
        GL11.glRotatef(rotX, 1, 0, 0);
        GL11.glScalef(scale, scale, scale);
        WorldLines.setEyeRotated(
                ShapeMath.buildRotationMatrix(rotX, rotY, rotZ),
                (gx - camPos.x()) / scale,
                (gy - camPos.y()) / scale,
                (gz - camPos.z()) / scale);
    }

    private static Vec3DFloat arcBasisP1(Mat3DFloat R, int a) {
        return R.mul(Vec3DFloat.from(ARC_P1[a][0], ARC_P1[a][1], ARC_P1[a][2]));
    }

    private static Vec3DFloat arcBasisP2(Mat3DFloat R, int a) {
        return R.mul(Vec3DFloat.from(ARC_P2[a][0], ARC_P2[a][1], ARC_P2[a][2]));
    }

    static double segDist(double ax, double ay, double bx, double by, double px, double py) {
        double abx = bx - ax, aby = by - ay;
        double apx = px - ax, apy = py - ay;
        double len2 = abx * abx + aby * aby;
        double t = len2 > 0 ? Math.max(0, Math.min(1, (apx * abx + apy * aby) / len2)) : 0;
        double nx = ax + t * abx, ny = ay + t * aby;
        double ddx = px - nx, ddy = py - ny;
        return Math.sqrt(ddx * ddx + ddy * ddy);
    }
}
