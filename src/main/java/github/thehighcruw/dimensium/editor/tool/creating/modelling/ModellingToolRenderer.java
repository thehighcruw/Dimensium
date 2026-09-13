/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.modelling;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolRenderer;

@SideOnly(Side.CLIENT)
public class ModellingToolRenderer implements ToolRenderer {

    public static final ModellingToolRenderer INSTANCE = new ModellingToolRenderer();

    private ModellingToolRenderer() {}

    @Override
    public boolean isBlockAffected(Minecraft mc, int wx, int wy, int wz) {
        return false;
    }

    @Override
    public boolean renderHover(MovingObjectPosition mop, double rx, double ry, double rz) {
        return true;
    }

    @Override
    public void renderOverlay(Minecraft mc, int mx, int my, int mx3d, int my3d) {
        ModellingToolState mts = ModellingToolState.INSTANCE;
        ModellingToolState.ModelPoint mSelPt = mts.selectedPointObj();
        if (mSelPt == null) return;
        if (!mts.gizmo.isDragging() && !mts.planeGizmo.isDragging() && mc.renderViewEntity != null) {
            double mgx = mSelPt.x + 0.5, mgy = mSelPt.y + 0.5, mgz = mSelPt.z + 0.5;
            mts.gizmo.updateHover(mx3d, my3d, mc.renderViewEntity, mgx, mgy, mgz, 0, 0, 0);
        }
    }
}
