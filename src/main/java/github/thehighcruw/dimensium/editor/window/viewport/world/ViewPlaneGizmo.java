/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamUtils;
import github.thehighcruw.dimensium.shared.Vec2DDouble;
import github.thehighcruw.dimensium.shared.Vec3DDouble;

/**
 * View-plane translation gizmo — a white transparent cube at the gizmo origin.
 * Dragging moves the object along the plane perpendicular to the camera's look direction.
 */
@SideOnly(Side.CLIENT)
public class ViewPlaneGizmo {

    private static final float CUBE_H = 0.15f;
    private static final int HIT_PX = 4;

    private final GizmoProjection proj = new GizmoProjection();

    public boolean hovered = false;
    private boolean dragging = false;
    private int dragStartMX, dragStartMY;
    private Vec3DDouble startAnchor = Vec3DDouble.ZERO;
    private Vec2DDouble scrRight = Vec2DDouble.ZERO;
    private double pixelsPerUnitRight;
    private Vec2DDouble scrUp = Vec2DDouble.ZERO;
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

    public void render(double gx, double gy, double gz, Vec3DDouble camPos) {
        proj.capture(camPos);
        float scale = RotationGizmo.computeScale(gx - camPos.x(), gy - camPos.y(), gz - camPos.z());
        float h = CUBE_H * scale;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPushMatrix();
        GL11.glTranslated(gx - camPos.x(), gy - camPos.y(), gz - camPos.z());

        float fill = hovered ? 0.55f : 0.22f;
        float edge = hovered ? 1.0f : 0.70f;

        GL11.glColor4f(1f, 1f, 1f, fill);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-h, -h, -h);
        GL11.glVertex3f(h, -h, -h);
        GL11.glVertex3f(h, h, -h);
        GL11.glVertex3f(-h, h, -h);
        GL11.glVertex3f(-h, -h, h);
        GL11.glVertex3f(-h, h, h);
        GL11.glVertex3f(h, h, h);
        GL11.glVertex3f(h, -h, h);
        GL11.glVertex3f(-h, -h, -h);
        GL11.glVertex3f(-h, h, -h);
        GL11.glVertex3f(-h, h, h);
        GL11.glVertex3f(-h, -h, h);
        GL11.glVertex3f(h, -h, -h);
        GL11.glVertex3f(h, -h, h);
        GL11.glVertex3f(h, h, h);
        GL11.glVertex3f(h, h, -h);
        GL11.glVertex3f(-h, -h, -h);
        GL11.glVertex3f(-h, -h, h);
        GL11.glVertex3f(h, -h, h);
        GL11.glVertex3f(h, -h, -h);
        GL11.glVertex3f(-h, h, -h);
        GL11.glVertex3f(h, h, -h);
        GL11.glVertex3f(h, h, h);
        GL11.glVertex3f(-h, h, h);
        GL11.glEnd();

        GL11.glColor4f(1f, 1f, 1f, edge);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3f(-h, -h, -h);
        GL11.glVertex3f(h, -h, -h);
        GL11.glVertex3f(h, h, -h);
        GL11.glVertex3f(-h, h, -h);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3f(-h, -h, h);
        GL11.glVertex3f(h, -h, h);
        GL11.glVertex3f(h, h, h);
        GL11.glVertex3f(-h, h, h);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3f(-h, -h, -h);
        GL11.glVertex3f(-h, -h, h);
        GL11.glVertex3f(h, -h, -h);
        GL11.glVertex3f(h, -h, h);
        GL11.glVertex3f(h, h, -h);
        GL11.glVertex3f(h, h, h);
        GL11.glVertex3f(-h, h, -h);
        GL11.glVertex3f(-h, h, h);
        GL11.glEnd();

        GL11.glPopMatrix();
    }

    // ── Hover ─────────────────────────────────────────────────────────────────

    public void updateHover(int mouseX, int mouseY, EntityLivingBase player, double gx, double gy, double gz) {
        float scale = RotationGizmo
            .computeScale(gx - player.posX, gy - (player.posY + player.getEyeHeight()), gz - player.posZ);
        float h = CUBE_H * scale;
        float[] offs = { -h, h };
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean anyValid = false;
        for (float ox : offs) for (float oy : offs) for (float oz : offs) {
            double[] s = proj.project(gx + ox, gy + oy, gz + oz);
            if (s == null) continue;
            anyValid = true;
            if (s[0] < minX) minX = s[0];
            if (s[0] > maxX) maxX = s[0];
            if (s[1] < minY) minY = s[1];
            if (s[1] > maxY) maxY = s[1];
        }
        hovered = anyValid && mouseX >= minX - HIT_PX
            && mouseX <= maxX + HIT_PX
            && mouseY >= minY - HIT_PX
            && mouseY <= maxY + HIT_PX;
    }

    // ── Drag ─────────────────────────────────────────────────────────────────

    public void startDrag(int mouseX, int mouseY, EntityLivingBase player, double gx, double gy, double gz,
        double anchorX, double anchorY, double anchorZ) {
        if (!hovered) return;
        dragging = true;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        startAnchor = Vec3DDouble.from(anchorX, anchorY, anchorZ);

        Vec3DDouble[] basis = FreecamUtils.cameraBasis(player.rotationYaw, player.rotationPitch);
        cameraRight = basis[1];
        cameraUp = basis[2];

        double[] s0 = proj.project(gx, gy, gz);
        double[] sR = proj.project(gx + cameraRight.x(), gy + cameraRight.y(), gz + cameraRight.z());
        double[] sU = proj.project(gx + cameraUp.x(), gy + cameraUp.y(), gz + cameraUp.z());

        if (s0 == null || sR == null) {
            scrRight = Vec2DDouble.from(1, 0);
            pixelsPerUnitRight = 50;
        } else {
            Vec2DDouble dr = Vec2DDouble.from(sR[0] - s0[0], sR[1] - s0[1]);
            pixelsPerUnitRight = Math.max(1.0, dr.length());
            scrRight = dr.divide(pixelsPerUnitRight);
        }
        if (s0 == null || sU == null) {
            scrUp = Vec2DDouble.from(0, -1);
            pixelsPerUnitUp = 50;
        } else {
            Vec2DDouble du = Vec2DDouble.from(sU[0] - s0[0], sU[1] - s0[1]);
            pixelsPerUnitUp = Math.max(1.0, du.length());
            scrUp = du.divide(pixelsPerUnitUp);
        }
    }

    /** Returns updated anchor, or null if not dragging. */
    public Vec3DDouble updateDrag(int mouseX, int mouseY) {
        if (!dragging) return null;
        Vec2DDouble dm = Vec2DDouble.from(mouseX - dragStartMX, mouseY - dragStartMY);
        double deltaRight = dm.dot(scrRight) / pixelsPerUnitRight;
        double deltaUp = dm.dot(scrUp) / pixelsPerUnitUp;
        Vec3DDouble delta = cameraRight.times(deltaRight)
            .plus(cameraUp.times(deltaUp));
        return startAnchor.plus(delta);
    }

    public void endDrag() {
        dragging = false;
    }
}
