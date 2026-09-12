package github.thehighcruw.dimensium.render.world;

import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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
    private double startAnchorX, startAnchorY, startAnchorZ;
    private double scrRightX, scrRightY, pixelsPerUnitRight;
    private double scrUpX, scrUpY, pixelsPerUnitUp;
    private final double[] cameraRight = new double[3];
    private final double[] cameraUp = new double[3];

    public GizmoProjection getProjection() {
        return proj;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void reset() {
        hovered = false;
        dragging = false;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    public void render(double gx, double gy, double gz, double rx, double ry, double rz, float rotX, float rotY,
        float rotZ) {
        proj.capture(rx, ry, rz);
        float scale = RotationGizmo.computeScale(gx - rx, gy - ry, gz - rz);
        float h = CUBE_H * scale;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPushMatrix();
        GL11.glTranslated(gx - rx, gy - ry, gz - rz);

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

    public void updateHover(int mouseX, int mouseY, int sw, int sh, EntityLivingBase player, double gx, double gy,
        double gz, float rotX, float rotY, float rotZ) {
        float scale = RotationGizmo
            .computeScale(gx - player.posX, gy - (player.posY + player.getEyeHeight()), gz - player.posZ);
        float h = CUBE_H * scale;
        float[] offs = { -h, h };
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean anyValid = false;
        for (float ox : offs) for (float oy : offs) for (float oz : offs) {
            double[] s = proj.project(gx + ox, gy + oy, gz + oz, sw, sh);
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

    public void startDrag(int mouseX, int mouseY, int sw, int sh, EntityLivingBase player, double gx, double gy,
        double gz, double anchorX, double anchorY, double anchorZ) {
        if (!hovered) return;
        dragging = true;
        dragStartMX = mouseX;
        dragStartMY = mouseY;
        startAnchorX = anchorX;
        startAnchorY = anchorY;
        startAnchorZ = anchorZ;

        // Camera right = (cos(yaw), 0, sin(yaw))
        // Camera up = (-sin(pitch)*sin(yaw), cos(pitch), sin(pitch)*cos(yaw))
        double yaw = Math.toRadians(player.rotationYaw);
        double pitch = Math.toRadians(player.rotationPitch);
        cameraRight[0] = Math.cos(yaw);
        cameraRight[1] = 0;
        cameraRight[2] = Math.sin(yaw);
        cameraUp[0] = -Math.sin(pitch) * Math.sin(yaw);
        cameraUp[1] = Math.cos(pitch);
        cameraUp[2] = Math.sin(pitch) * Math.cos(yaw);

        double[] s0 = proj.project(gx, gy, gz, sw, sh);
        double[] sR = proj.project(gx + cameraRight[0], gy + cameraRight[1], gz + cameraRight[2], sw, sh);
        double[] sU = proj.project(gx + cameraUp[0], gy + cameraUp[1], gz + cameraUp[2], sw, sh);

        if (s0 == null || sR == null) {
            scrRightX = 1;
            scrRightY = 0;
            pixelsPerUnitRight = 50;
        } else {
            double dx = sR[0] - s0[0], dy = sR[1] - s0[1];
            pixelsPerUnitRight = Math.max(1.0, Math.sqrt(dx * dx + dy * dy));
            scrRightX = dx / pixelsPerUnitRight;
            scrRightY = dy / pixelsPerUnitRight;
        }
        if (s0 == null || sU == null) {
            scrUpX = 0;
            scrUpY = -1;
            pixelsPerUnitUp = 50;
        } else {
            double dx = sU[0] - s0[0], dy = sU[1] - s0[1];
            pixelsPerUnitUp = Math.max(1.0, Math.sqrt(dx * dx + dy * dy));
            scrUpX = dx / pixelsPerUnitUp;
            scrUpY = dy / pixelsPerUnitUp;
        }
    }

    /**
     * Returns updated [anchorX, anchorY, anchorZ] or null if not dragging.
     */
    public double[] updateDrag(int mouseX, int mouseY) {
        if (!dragging) return null;
        double dmx = mouseX - dragStartMX, dmy = mouseY - dragStartMY;
        double deltaRight = (dmx * scrRightX + dmy * scrRightY) / pixelsPerUnitRight;
        double deltaUp = (dmx * scrUpX + dmy * scrUpY) / pixelsPerUnitUp;
        return new double[] { startAnchorX + deltaRight * cameraRight[0] + deltaUp * cameraUp[0],
            startAnchorY + deltaRight * cameraRight[1] + deltaUp * cameraUp[1],
            startAnchorZ + deltaRight * cameraRight[2] + deltaUp * cameraUp[2] };
    }

    public void endDrag() {
        dragging = false;
    }
}
