/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.lasso;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolRenderer;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;

@SideOnly(Side.CLIENT)
public class LassoSelectToolRenderer implements ToolRenderer {

    public static final LassoSelectToolRenderer INSTANCE = new LassoSelectToolRenderer();

    private LassoSelectToolRenderer() {}

    @Override
    public boolean isBlockAffected(Minecraft mc, int wx, int wy, int wz) {
        return false;
    }

    @Override
    public boolean renderHover(MovingObjectPosition mop, Vec3DDouble camPos) {
        return true;
    }

    @Override
    public void renderOverlay(Minecraft mc, int mx, int my, int mx3d, int my3d) {
        LassoSelectToolState lasso = LassoSelectToolState.INSTANCE;
        if (!lasso.dragging || lasso.polygonPoints.size() < 2) return;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(1.5f);

        Tessellator t = Tessellator.instance;

        // Drop shadow
        GL11.glColor4f(0f, 0f, 0f, 0.5f);
        t.startDrawing(GL11.GL_LINE_LOOP);
        for (float[] p : lasso.polygonPoints) t.addVertex(p[0] + 1, p[1] + 1, 0);
        t.draw();

        // Lasso outline in cyan-white
        GL11.glColor4f(0.4f, 0.9f, 0.8f, 0.9f);
        t.startDrawing(GL11.GL_LINE_LOOP);
        for (float[] p : lasso.polygonPoints) t.addVertex(p[0], p[1], 0);
        t.draw();

        // Fill tint
        GL11.glColor4f(0.4f, 0.9f, 0.8f, 0.07f);
        t.startDrawingQuads();
        float[] first = lasso.polygonPoints.get(0);
        for (int i = 1; i < lasso.polygonPoints.size() - 1; i++) {
            float[] a = lasso.polygonPoints.get(i);
            float[] b = lasso.polygonPoints.get(i + 1);
            t.addVertex(first[0], first[1], 0);
            t.addVertex(a[0], a[1], 0);
            t.addVertex(b[0], b[1], 0);
            t.addVertex(b[0], b[1], 0);
        }
        t.draw();

        GL11.glPopAttrib();
    }
}
