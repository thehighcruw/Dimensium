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

    private static final Vec3DFloat[] CENTERS = {
        Vec3DFloat.from(SQ_POS, SQ_POS, 0), // XY
        Vec3DFloat.from(SQ_POS, 0, SQ_POS), // XZ
        Vec3DFloat.from(0, SQ_POS, SQ_POS), // YZ
    };

    private static final Vec3DFloat[] PLANE_A = {
        Vec3DFloat.from(1, 0, 0), // XY: A = X
        Vec3DFloat.from(1, 0, 0), // XZ: A = X
        Vec3DFloat.from(0, 1, 0), // YZ: A = Y
    };
    private static final Vec3DFloat[] PLANE_B = {
        Vec3DFloat.from(0, 1, 0), // XY: B = Y
        Vec3DFloat.from(0, 0, 1), // XZ: B = Z
        Vec3DFloat.from(0, 0, 1), // YZ: B = Z
    };

    private static final Plane[] PLANES = {Plane.XY, Plane.XZ, Plane.YZ};

    private static final Vec3DFloat[] PLANE_COL = {
        Vec3DFloat.from(1.0f, 1.0f, 0.2f), // XY: yellow
        Vec3DFloat.from(1.0f, 0.25f, 1.0f), // XZ: magenta
        Vec3DFloat.from(0.25f, 1.0f, 1.0f), // YZ: cyan
    };

    private final GizmoProjection proj = new GizmoProjection();

    /** Per-axis sign, matching TranslationGizmo.axisFlip. Shifts squares into the correct quadrant. */
    public final float[] axisFlip = {1f, 1f, 1f};

    public Plane hoveredPlane = Plane.NONE;
    private Plane dragPlane = Plane.NONE;
    private int dragStartMX;
    private int dragStartMY;
    private Vec3DDouble startAnchor = Vec3DDouble.ZERO;
    private Vec3DFloat worldAxisA = Vec3DFloat.ZERO;
    private Vec3DFloat worldAxisB = Vec3DFloat.ZERO;
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

    public void render(Vec3DDouble pos, Vec3DDouble camPos, Vec3DFloat rot) {
        RotationGizmo.beginRender(proj, pos, camPos, rot);
        Vec3DFloat flipVec = Vec3DFloat.from(axisFlip[0], axisFlip[1], axisFlip[2]);

        for (int p = 0; p < 3; p++) {
            boolean hot = hoveredPlane == PLANES[p];
            Vec3DFloat color = PLANE_COL[p];
            Vec3DFloat center = CENTERS[p].times(flipVec);
            Vec3DFloat ha = PLANE_A[p].times(SQ_HALF), hb = PLANE_B[p].times(SQ_HALF);

            Vec3DFloat c00 = center.minus(ha).minus(hb);
            Vec3DFloat c10 = center.plus(ha).minus(hb);
            Vec3DFloat c11 = center.plus(ha).plus(hb);
            Vec3DFloat c01 = center.minus(ha).plus(hb);

            GL11.glColor4f(color.x(), color.y(), color.z(), hot ? 0.85f : 0.45f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3f(c00.x(), c00.y(), c00.z());
            GL11.glVertex3f(c10.x(), c10.y(), c10.z());
            GL11.glVertex3f(c11.x(), c11.y(), c11.z());
            GL11.glVertex3f(c01.x(), c01.y(), c01.z());
            GL11.glEnd();

            GL11.glColor4f(hot ? 1f : color.x(), hot ? 1f : color.y(), hot ? 1f : color.z(), 0.9f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex3f(c00.x(), c00.y(), c00.z());
            GL11.glVertex3f(c10.x(), c10.y(), c10.z());
            GL11.glVertex3f(c11.x(), c11.y(), c11.z());
            GL11.glVertex3f(c01.x(), c01.y(), c01.z());
            GL11.glEnd();
        }

        GL11.glPopMatrix();
    }

    // ── Hover ──────────────────────────────────────────────────────────────────

    public void updateHover(int mouseX, int mouseY, EntityLivingBase player, Vec3DDouble pos, Vec3DFloat rot) {
        RotationGizmo.HoverState hs = RotationGizmo.hoverState(player, pos, rot);
        float scale = hs.scale();
        Mat3DFloat R = hs.R();

        Plane best = Plane.NONE;
        double bestDist = HIT_PX;

        Vec3DFloat flipVec = Vec3DFloat.from(axisFlip[0], axisFlip[1], axisFlip[2]);
        for (int p = 0; p < 3; p++) {
            Vec3DFloat center = CENTERS[p].times(flipVec);
            Vec3DDouble worldHalfA =
                    rotatedAxis(R, PLANE_A[p]).times(SQ_HALF * scale).toDouble();
            Vec3DDouble worldHalfB =
                    rotatedAxis(R, PLANE_B[p]).times(SQ_HALF * scale).toDouble();
            Vec3DDouble worldCenter = pos.plus(R.mul(center).toDouble().times(scale));
            Vec2DDouble[] corners = new Vec2DDouble[4];
            corners[0] = proj.project(worldCenter.plus(worldHalfA.negate()).minus(worldHalfB));
            corners[1] = proj.project(worldCenter.plus(worldHalfA).minus(worldHalfB));
            corners[2] = proj.project(worldCenter.plus(worldHalfA).plus(worldHalfB));
            corners[3] = proj.project(worldCenter.plus(worldHalfA.negate()).plus(worldHalfB));
            boolean anyNull = false;
            for (Vec2DDouble corner : corners)
                if (corner == null) {
                    anyNull = true;
                    break;
                }
            if (anyNull) continue;
            double dist = quadDist(corners, Vec2DDouble.from(mouseX, mouseY));
            if (dist < bestDist) {
                bestDist = dist;
                best = PLANES[p];
            }
        }
        hoveredPlane = best;
    }

    // ── Drag ──────────────────────────────────────────────────────────────────

    /**
     * anchorX/Y/Z is the current shape anchor position in world space.
     * Drag moves the anchor along the two axes of the hovered plane.
     */
    public void startDrag(int mouseX, int mouseY, Vec3DDouble gizmoPos, Vec3DDouble anchor, Vec3DFloat rot) {
        if (hoveredPlane == Plane.NONE) return;
        dragPlane = hoveredPlane;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        startAnchor = anchor;
        dragGizmo = gizmoPos;

        int p = dragPlane == Plane.XY ? 0 : dragPlane == Plane.XZ ? 1 : 2;
        Mat3DFloat R = ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z());
        worldAxisA = rotatedAxis(R, PLANE_A[p]);
        worldAxisB = rotatedAxis(R, PLANE_B[p]);
        dragPlaneN = worldAxisA.cross(worldAxisB).toDouble();

        // Screen-based fallback setup
        Vec2DDouble os = proj.project(gizmoPos);
        Vec2DDouble tsA = proj.project(gizmoPos.plus(worldAxisA.toDouble()));
        Vec2DDouble tsB = proj.project(gizmoPos.plus(worldAxisB.toDouble()));
        if (os == null || tsA == null || tsB == null) {
            screenAxisA = Vec2DDouble.from(1, 0);
            pixelsPerUnitA = 50;
            screenAxisB = Vec2DDouble.from(0, 1);
            pixelsPerUnitB = 50;
        } else {
            screenAxisA = Vec2DDouble.screenDir(os, tsA);
            pixelsPerUnitA = Vec2DDouble.screenScale(os, tsA);
            Vec2DDouble db = tsB.minus(os);
            double lenB = db.length();
            pixelsPerUnitB = Math.max(1.0, lenB);
            screenAxisB = lenB > 0.001 ? db.divide(lenB) : Vec2DDouble.from(0, 1);
        }

        // Ray-based drag: find initial hit on plane
        GizmoProjection.Ray ray = proj.unprojectRay(mouseX, mouseY);
        Vec3DDouble hit = ray != null ? rayIntersectDragPlane(ray) : null;
        if (hit != null) {
            dragStartH = hit;
            useRayDrag = true;
        } else {
            dragStartH = Vec3DDouble.ZERO;
            useRayDrag = false;
        }
    }

    private Vec3DDouble rayIntersectDragPlane(GizmoProjection.Ray ray) {
        return rayPlaneIntersect(ray, dragGizmo, dragPlaneN);
    }

    private static Vec3DFloat rotatedAxis(Mat3DFloat R, Vec3DFloat axis) {
        return R.mul(axis);
    }

    private static Vec3DDouble rayPlaneIntersect(
            GizmoProjection.Ray ray, Vec3DDouble planePoint, Vec3DDouble planeNormal) {
        double dDotN = ray.dir().dot(planeNormal);
        if (Math.abs(dDotN) < 1e-10) return null;
        double rayParam = planePoint.minus(ray.origin()).dot(planeNormal) / dDotN;
        return ray.origin().plus(ray.dir().times(rayParam));
    }

    /** Returns updated anchor, or null if not dragging. */
    public Vec3DDouble updateDrag(int mouseX, int mouseY) {
        if (dragPlane == Plane.NONE) return null;
        if (useRayDrag) {
            GizmoProjection.Ray ray = proj.unprojectRay(mouseX, mouseY);
            if (ray != null) {
                Vec3DDouble hit = rayIntersectDragPlane(ray);
                if (hit != null) {
                    return startAnchor.plus(hit.minus(dragStartH));
                }
            }
        }
        // Screen-based fallback
        Vec2DDouble mouseDelta = Vec2DDouble.from(mouseX - dragStartMX, mouseY - dragStartMY);
        double deltaA = mouseDelta.dot(screenAxisA) / pixelsPerUnitA;
        double deltaB = mouseDelta.dot(screenAxisB) / pixelsPerUnitB;
        Vec3DDouble delta =
                worldAxisA.toDouble().times(deltaA).plus(worldAxisB.toDouble().times(deltaB));
        return startAnchor.plus(delta);
    }

    public void endDrag() {
        dragPlane = Plane.NONE;
    }

    private static double quadDist(Vec2DDouble[] corners, Vec2DDouble point) {
        int positiveCount = 0, negativeCount = 0;
        for (int i = 0; i < 4; i++) {
            Vec2DDouble cornerA = corners[i], cornerB = corners[(i + 1) % 4];
            double cross = (cornerB.x() - cornerA.x()) * (point.y() - cornerA.y())
                    - (cornerB.y() - cornerA.y()) * (point.x() - cornerA.x());
            if (cross > 0) positiveCount++;
            else if (cross < 0) negativeCount++;
        }
        if (positiveCount == 4 || negativeCount == 4) return 0;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            Vec2DDouble cornerA = corners[i], cornerB = corners[(i + 1) % 4];
            min = Math.min(min, RotationGizmo.segDist(cornerA, cornerB, point));
        }
        return min;
    }
}
