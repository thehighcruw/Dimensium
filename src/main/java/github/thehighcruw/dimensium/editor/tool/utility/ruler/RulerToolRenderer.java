/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.utility.ruler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.BrushPreviewRenderer;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RulerToolRenderer implements ToolRenderer {

    @Override
    public boolean isBlockAffected(Minecraft mc, Vec3DInt wc) {
        return false;
    }

    @Override
    public void renderWorldPreview(Minecraft mc, Vec3DDouble camPos) {
        BrushPreviewRenderer.INSTANCE.render(this, mc, camPos);
    }

    @Override
    public boolean renderHover(MovingObjectPosition mop, Vec3DDouble camPos) {
        List<Vec3DInt> points = RulerToolState.INSTANCE.points;
        Vec3DInt hover = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);

        GL11.glPushMatrix();
        GL11.glTranslated(-camPos.x(), -camPos.y(), -camPos.z());
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(2.0f);
        GL11.glColor4f(1.0f, 0.85f, 0.1f, 0.9f);

        Vec3DDouble hd = hover.toDouble();
        Vec3DDouble hd1 = hd.plus(1.0);
        Vec3DDouble hc = hd.plus(0.5);

        GL11.glBegin(GL11.GL_LINES);

        // lines between placed points
        for (int i = 0; i + 1 < points.size(); i++) {
            Vec3DDouble ac = points.get(i).toDouble().plus(0.5);
            Vec3DDouble bc = points.get(i + 1).toDouble().plus(0.5);
            GL11.glColor4f(1.0f, 0.85f, 0.1f, 0.9f);
            GL11.glVertex3d(ac.x(), ac.y(), ac.z());
            GL11.glVertex3d(bc.x(), bc.y(), bc.z());
        }

        // line from last point (or cursor cross) to cursor
        if (!points.isEmpty()) {
            Vec3DDouble lastCenter = points.get(points.size() - 1).toDouble().plus(0.5);
            GL11.glColor4f(1.0f, 0.85f, 0.1f, 0.45f);
            GL11.glVertex3d(lastCenter.x(), lastCenter.y(), lastCenter.z());
            GL11.glVertex3d(hc.x(), hc.y(), hc.z());
        }

        GL11.glEnd();

        // cursor cross — always visible so hover is never blank
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(1.0f, 0.85f, 0.1f, 0.9f);
        GL11.glVertex3d(hd.x(), hc.y(), hc.z());
        GL11.glVertex3d(hd1.x(), hc.y(), hc.z());
        GL11.glVertex3d(hc.x(), hd.y(), hc.z());
        GL11.glVertex3d(hc.x(), hd1.y(), hc.z());
        GL11.glVertex3d(hc.x(), hc.y(), hd.z());
        GL11.glVertex3d(hc.x(), hc.y(), hd1.z());
        GL11.glEnd();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(1.0f);
        GL11.glPopMatrix();

        return true;
    }
}
