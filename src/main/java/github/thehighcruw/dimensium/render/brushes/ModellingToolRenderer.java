/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.brushes;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.tool.state.ModellingToolState;

@SideOnly(Side.CLIENT)
public class ModellingToolRenderer implements ToolRenderer {

    public static final ModellingToolRenderer INSTANCE = new ModellingToolRenderer();

    private ModellingToolRenderer() {}

    @Override
    public boolean isBlockAffected(Minecraft mc, int wx, int wy, int wz) {
        return false;
    }

    @Override
    public boolean renderHover(Minecraft mc, MovingObjectPosition mop, double rx, double ry, double rz) {
        return true;
    }

    @Override
    public void renderOverlay(Minecraft mc, int mx, int my, int mx3d, int my3d, int sw, int sh) {
        ModellingToolState mts = ModellingToolState.INSTANCE;
        ModellingToolState.ModelPoint mSelPt = mts.selectedPointObj();
        if (mSelPt == null) return;
        double mgx = mSelPt.x + 0.5, mgy = mSelPt.y + 0.5, mgz = mSelPt.z + 0.5;
        if (mts.gizmo.isDragging()) {
            double[] anchor = mts.gizmo.updateDrag(mx3d, my3d);
            if (anchor != null) {
                boolean snap = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
                mSelPt.x = (int) Math.floor(snap ? Math.floor(anchor[0] + 0.5) : anchor[0]);
                mSelPt.y = (int) Math.floor(snap ? Math.floor(anchor[1] + 0.5) : anchor[1]);
                mSelPt.z = (int) Math.floor(snap ? Math.floor(anchor[2] + 0.5) : anchor[2]);
                mts.invalidate();
            }
        } else if (mc.renderViewEntity != null) {
            mts.gizmo.updateHover(mx3d, my3d, sw, sh, mc.renderViewEntity, mgx, mgy, mgz, 0, 0, 0);
        }
    }
}
