/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.path;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolRenderer;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public class PathToolRenderer implements ToolRenderer {

    public static final PathToolRenderer INSTANCE = new PathToolRenderer();

    private PathToolRenderer() {}

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
        PathToolState pathState = PathToolState.INSTANCE;
        if (pathState.selectedIndex < 0 || pathState.points.isEmpty()) return;
        PathToolState.PathPoint selPt = pathState.selectedPoint();
        if (selPt == null) return;
        if (!pathState.getAxisTranslationGizmo().isDragging()
                && !pathState.getPlaneTranslationGizmo().isDragging()
                && mc.renderViewEntity != null) {
            Vec3DDouble gp = Vec3DDouble.from(selPt.pos.x() + 0.5, selPt.pos.y() + 0.5, selPt.pos.z() + 0.5);
            pathState
                    .getAxisTranslationGizmo()
                    .updateHover(mx3d, my3d, mc.renderViewEntity, gp.x(), gp.y(), gp.z(), 0, 0, 0);
        }
    }
}
