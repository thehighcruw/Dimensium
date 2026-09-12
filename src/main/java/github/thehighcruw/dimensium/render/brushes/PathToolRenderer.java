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
import github.thehighcruw.dimensium.tool.state.PathToolState;

@SideOnly(Side.CLIENT)
public class PathToolRenderer implements ToolRenderer {

    public static final PathToolRenderer INSTANCE = new PathToolRenderer();

    private PathToolRenderer() {}

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
        PathToolState pathState = PathToolState.INSTANCE;
        if (pathState.selectedIndex < 0 || pathState.points.isEmpty()) return;
        PathToolState.PathPoint selPt = pathState.selectedPoint();
        if (selPt == null) return;
        double pgx = selPt.x + 0.5, pgy = selPt.y + 0.5, pgz = selPt.z + 0.5;
        if (pathState.gizmo.isDragging()) {
            double[] anchor = pathState.gizmo.updateDrag(mx3d, my3d);
            if (anchor != null) {
                boolean snap = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
                selPt.x = (int) Math.floor(snap ? Math.floor(anchor[0] + 0.5) : anchor[0]);
                selPt.y = (int) Math.floor(snap ? Math.floor(anchor[1] + 0.5) : anchor[1]);
                selPt.z = (int) Math.floor(snap ? Math.floor(anchor[2] + 0.5) : anchor[2]);
                pathState.invalidatePath();
            }
        } else if (mc.renderViewEntity != null) {
            pathState.gizmo.updateHover(mx3d, my3d, sw, sh, mc.renderViewEntity, pgx, pgy, pgz, 0, 0, 0);
        }
    }
}
