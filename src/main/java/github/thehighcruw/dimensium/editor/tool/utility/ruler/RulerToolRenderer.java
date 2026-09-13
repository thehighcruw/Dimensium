/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.utility.ruler;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.BrushPreviewRenderer;

@SideOnly(Side.CLIENT)
public class RulerToolRenderer implements ToolRenderer {

    @Override
    public boolean isBlockAffected(Minecraft mc, int wx, int wy, int wz) {
        return false;
    }

    @Override
    public void renderWorldPreview(Minecraft mc, double rx, double ry, double rz) {
        BrushPreviewRenderer.INSTANCE.render(this, mc, rx, ry, rz);
    }

    @Override
    public boolean renderHover(MovingObjectPosition mop, double rx, double ry, double rz) {
        List<int[]> points = RulerToolState.INSTANCE.points;
        int hx = mop.blockX, hy = mop.blockY, hz = mop.blockZ;

        GL11.glPushMatrix();
        GL11.glTranslated(-rx, -ry, -rz);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(2.0f);
        GL11.glColor4f(1.0f, 0.85f, 0.1f, 0.9f);

        GL11.glBegin(GL11.GL_LINES);

        // lines between placed points
        for (int i = 0; i + 1 < points.size(); i++) {
            int[] a = points.get(i);
            int[] b = points.get(i + 1);
            GL11.glColor4f(1.0f, 0.85f, 0.1f, 0.9f);
            GL11.glVertex3d(a[0] + 0.5, a[1] + 0.5, a[2] + 0.5);
            GL11.glVertex3d(b[0] + 0.5, b[1] + 0.5, b[2] + 0.5);
        }

        // line from last point (or cursor cross) to cursor
        if (!points.isEmpty()) {
            int[] last = points.get(points.size() - 1);
            GL11.glColor4f(1.0f, 0.85f, 0.1f, 0.45f);
            GL11.glVertex3d(last[0] + 0.5, last[1] + 0.5, last[2] + 0.5);
            GL11.glVertex3d(hx + 0.5, hy + 0.5, hz + 0.5);
        }

        GL11.glEnd();

        // cursor cross — always visible so hover is never blank
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(1.0f, 0.85f, 0.1f, 0.9f);
        GL11.glVertex3d(hx, hy + 0.5, hz + 0.5);
        GL11.glVertex3d(hx + 1.0, hy + 0.5, hz + 0.5);
        GL11.glVertex3d(hx + 0.5, hy, hz + 0.5);
        GL11.glVertex3d(hx + 0.5, hy + 1.0, hz + 0.5);
        GL11.glVertex3d(hx + 0.5, hy + 0.5, hz);
        GL11.glVertex3d(hx + 0.5, hy + 0.5, hz + 1.0);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(1.0f);
        GL11.glPopMatrix();

        return true;
    }
}
