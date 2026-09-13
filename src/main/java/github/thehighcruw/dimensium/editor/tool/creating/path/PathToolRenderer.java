/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.path;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolRenderer;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState;

@SideOnly(Side.CLIENT)
public class PathToolRenderer implements ToolRenderer {

    public static final PathToolRenderer INSTANCE = new PathToolRenderer();

    private PathToolRenderer() {}

    @Override
    public boolean isBlockAffected(Minecraft mc, int wx, int wy, int wz) {
        return false;
    }

    @Override
    public boolean renderHover(MovingObjectPosition mop, double rx, double ry, double rz) {
        return true;
    }

    @Override
    public void renderOverlay(Minecraft mc, int mx, int my, int mx3d, int my3d, int sw, int sh) {
        PathToolState pathState = PathToolState.INSTANCE;
        if (pathState.selectedIndex < 0 || pathState.points.isEmpty()) return;
        PathToolState.PathPoint selPt = pathState.selectedPoint();
        if (selPt == null) return;
        if (!pathState.gizmo.isDragging() && !pathState.planeGizmo.isDragging() && mc.renderViewEntity != null) {
            double pgx = selPt.x + 0.5, pgy = selPt.y + 0.5, pgz = selPt.z + 0.5;
            pathState.gizmo.updateHover(mx3d, my3d, mc.renderViewEntity, pgx, pgy, pgz, 0, 0, 0);
        }
    }
}
