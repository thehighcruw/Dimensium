/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamUtils;
import github.thehighcruw.dimensium.shared.math.Vec2DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

/**
 * View-plane translation gizmo — a white transparent cube at the gizmo origin.
 * Dragging moves the object along the plane perpendicular to the camera's look direction.
 */
@SideOnly(Side.CLIENT)
public class ViewPlaneGizmo {

    private static final float CUBE_H = 0.15f;
    private static final int HIT_PX = 4;

    private static final Vec3DFloat[] CUBE_CORNERS = {
        Vec3DFloat.from(-1, -1, -1),
        Vec3DFloat.from(1, -1, -1),
        Vec3DFloat.from(1, 1, -1),
        Vec3DFloat.from(-1, 1, -1),
        Vec3DFloat.from(-1, -1, 1),
        Vec3DFloat.from(1, -1, 1),
        Vec3DFloat.from(1, 1, 1),
        Vec3DFloat.from(-1, 1, 1),
    };

    private final GizmoProjection proj = new GizmoProjection();

    public boolean hovered = false;
    private boolean dragging = false;
    private int dragStartMX, dragStartMY;
    private Vec3DDouble startAnchor = Vec3DDouble.ZERO;
    private Vec2DDouble screenRight = Vec2DDouble.ZERO;
    private double pixelsPerUnitRight;
    private Vec2DDouble screenUp = Vec2DDouble.ZERO;
    private double pixelsPerUnitUp;
    private Vec3DDouble cameraRight = Vec3DDouble.ZERO;
    private Vec3DDouble cameraUp = Vec3DDouble.ZERO;

    public boolean isDragging() {
        return dragging;
    }

    public void reset() {
        hovered = false;
        dragging = false;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    public void render(Vec3DDouble pos, Vec3DDouble camPos) {
        proj.capture(camPos);
        Vec3DDouble delta = pos.minus(camPos);
        float scale = RotationGizmo.computeScale(delta);
        float halfExtent = CUBE_H * scale;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPushMatrix();
        GL11.glTranslated(delta.x(), delta.y(), delta.z());

        float fill = hovered ? 0.55f : 0.22f;
        float edge = hovered ? 1.0f : 0.70f;

        GL11.glColor4f(1f, 1f, 1f, fill);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-halfExtent, -halfExtent, -halfExtent);
        GL11.glVertex3f(halfExtent, -halfExtent, -halfExtent);
        GL11.glVertex3f(halfExtent, halfExtent, -halfExtent);
        GL11.glVertex3f(-halfExtent, halfExtent, -halfExtent);
        GL11.glVertex3f(-halfExtent, -halfExtent, halfExtent);
        GL11.glVertex3f(-halfExtent, halfExtent, halfExtent);
        GL11.glVertex3f(halfExtent, halfExtent, halfExtent);
        GL11.glVertex3f(halfExtent, -halfExtent, halfExtent);
        GL11.glVertex3f(-halfExtent, -halfExtent, -halfExtent);
        GL11.glVertex3f(-halfExtent, halfExtent, -halfExtent);
        GL11.glVertex3f(-halfExtent, halfExtent, halfExtent);
        GL11.glVertex3f(-halfExtent, -halfExtent, halfExtent);
        GL11.glVertex3f(halfExtent, -halfExtent, -halfExtent);
        GL11.glVertex3f(halfExtent, -halfExtent, halfExtent);
        GL11.glVertex3f(halfExtent, halfExtent, halfExtent);
        GL11.glVertex3f(halfExtent, halfExtent, -halfExtent);
        GL11.glVertex3f(-halfExtent, -halfExtent, -halfExtent);
        GL11.glVertex3f(-halfExtent, -halfExtent, halfExtent);
        GL11.glVertex3f(halfExtent, -halfExtent, halfExtent);
        GL11.glVertex3f(halfExtent, -halfExtent, -halfExtent);
        GL11.glVertex3f(-halfExtent, halfExtent, -halfExtent);
        GL11.glVertex3f(halfExtent, halfExtent, -halfExtent);
        GL11.glVertex3f(halfExtent, halfExtent, halfExtent);
        GL11.glVertex3f(-halfExtent, halfExtent, halfExtent);
        GL11.glEnd();

        GL11.glColor4f(1f, 1f, 1f, edge);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3f(-halfExtent, -halfExtent, -halfExtent);
        GL11.glVertex3f(halfExtent, -halfExtent, -halfExtent);
        GL11.glVertex3f(halfExtent, halfExtent, -halfExtent);
        GL11.glVertex3f(-halfExtent, halfExtent, -halfExtent);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3f(-halfExtent, -halfExtent, halfExtent);
        GL11.glVertex3f(halfExtent, -halfExtent, halfExtent);
        GL11.glVertex3f(halfExtent, halfExtent, halfExtent);
        GL11.glVertex3f(-halfExtent, halfExtent, halfExtent);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3f(-halfExtent, -halfExtent, -halfExtent);
        GL11.glVertex3f(-halfExtent, -halfExtent, halfExtent);
        GL11.glVertex3f(halfExtent, -halfExtent, -halfExtent);
        GL11.glVertex3f(halfExtent, -halfExtent, halfExtent);
        GL11.glVertex3f(halfExtent, halfExtent, -halfExtent);
        GL11.glVertex3f(halfExtent, halfExtent, halfExtent);
        GL11.glVertex3f(-halfExtent, halfExtent, -halfExtent);
        GL11.glVertex3f(-halfExtent, halfExtent, halfExtent);
        GL11.glEnd();

        GL11.glPopMatrix();
    }

    // ── Hover ─────────────────────────────────────────────────────────────────

    public void updateHover(int mouseX, int mouseY, EntityLivingBase player, Vec3DDouble pos) {
        Vec3DDouble eye = Vec3DDouble.from(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        float scale = RotationGizmo.computeScale(pos.minus(eye));
        float halfExtent = CUBE_H * scale;
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean anyValid = false;
        for (Vec3DFloat corner : CUBE_CORNERS) {
            Vec2DDouble s = proj.project(pos.plus(corner.times(halfExtent).toDouble()));
            if (s == null) continue;
            anyValid = true;
            if (s.x() < minX) minX = s.x();
            if (s.x() > maxX) maxX = s.x();
            if (s.y() < minY) minY = s.y();
            if (s.y() > maxY) maxY = s.y();
        }
        hovered = anyValid
                && mouseX >= minX - HIT_PX
                && mouseX <= maxX + HIT_PX
                && mouseY >= minY - HIT_PX
                && mouseY <= maxY + HIT_PX;
    }

    // ── Drag ─────────────────────────────────────────────────────────────────

    public void startDrag(int mouseX, int mouseY, EntityLivingBase player, Vec3DDouble pos, Vec3DDouble anchor) {
        if (!hovered) return;
        dragging = true;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        startAnchor = anchor;

        Vec3DDouble[] basis = FreecamUtils.cameraBasis(player.rotationYaw, player.rotationPitch);
        cameraRight = basis[1];
        cameraUp = basis[2];

        Vec2DDouble s0 = proj.project(pos);
        Vec2DDouble sR = proj.project(pos.plus(cameraRight));
        Vec2DDouble sU = proj.project(pos.plus(cameraUp));

        GizmoProjection.ScreenAxis saRight = computeScreenAxis(s0, sR, Vec2DDouble.from(1, 0));
        screenRight = saRight.dir();
        pixelsPerUnitRight = saRight.pixelsPerUnit();
        GizmoProjection.ScreenAxis saUp = computeScreenAxis(s0, sU, Vec2DDouble.from(0, -1));
        screenUp = saUp.dir();
        pixelsPerUnitUp = saUp.pixelsPerUnit();
    }

    private static GizmoProjection.ScreenAxis computeScreenAxis(
            Vec2DDouble s0, Vec2DDouble s, Vec2DDouble fallbackDir) {
        if (s0 == null || s == null) return new GizmoProjection.ScreenAxis(fallbackDir, 50);
        Vec2DDouble delta = s.minus(s0);
        double scale = Math.max(1.0, delta.length());
        return new GizmoProjection.ScreenAxis(delta.divide(scale), scale);
    }

    /** Returns updated anchor, or null if not dragging. */
    public Vec3DDouble updateDrag(int mouseX, int mouseY) {
        if (!dragging) return null;
        Vec2DDouble mouseDelta = Vec2DDouble.from(mouseX - dragStartMX, mouseY - dragStartMY);
        double deltaRight = mouseDelta.dot(screenRight) / pixelsPerUnitRight;
        double deltaUp = mouseDelta.dot(screenUp) / pixelsPerUnitUp;
        Vec3DDouble delta = cameraRight.times(deltaRight).plus(cameraUp.times(deltaUp));
        return startAnchor.plus(delta);
    }

    public void endDrag() {
        dragging = false;
    }
}
