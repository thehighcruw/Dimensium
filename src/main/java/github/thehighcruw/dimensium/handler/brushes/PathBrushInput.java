/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler.brushes;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.handler.InputHandler;
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.render.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.render.world.TranslationGizmo;
import github.thehighcruw.dimensium.tool.state.PathToolState;
import github.thehighcruw.dimensium.tool.state.SelectedBlockState;

@SideOnly(Side.CLIENT)
public class PathBrushInput implements BrushInput {

    public static final PathBrushInput INSTANCE = new PathBrushInput();

    private PathBrushInput() {}

    @Override
    public boolean requiresBlockTarget() {
        return false;
    }

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition ignored) {
        FreecamState fs = FreecamState.INSTANCE;
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
        int mouseX = (int) fs.cursorX, mouseY = (int) fs.cursorY;
        PathToolState pts = PathToolState.INSTANCE;

        if (button == KeyConstants.RMB && !InputHandler.isCtrlDown()) {
            MovingObjectPosition mop = GuiDimensiumOverlay.raycastFromMouse((int) fs.cursorX, (int) fs.cursorY, sw, sh);
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                int[] off = ExtrudeHelper.sideToOutwardDir(mop.sideHit);
                int px = mop.blockX + off[0], py = mop.blockY + off[1], pz = mop.blockZ + off[2];
                net.minecraft.item.ItemStack blk = SelectedBlockState.INSTANCE.selectedBlock;
                if (blk != null) blk = blk.copy();
                PathToolState.PathPoint pt = new PathToolState.PathPoint(px, py, pz, 0, blk);
                pts.points.add(pt);
                pts.selectedIndex = pts.points.size() - 1;
                pts.gizmo.reset();
                pts.invalidatePath();
            }
            return true;
        }

        if (button == KeyConstants.LMB) {
            EntityLivingBase eye = mc.renderViewEntity;
            List<int[]> ptPositions = new ArrayList<>(pts.points.size());
            for (PathToolState.PathPoint pt : pts.points) ptPositions.add(new int[] { pt.x, pt.y, pt.z });
            int bestIdx = GuiDimensiumOverlay.findNearestPointOnScreen(
                ptPositions,
                pts.selectedIndex,
                mouseX,
                mouseY,
                sw,
                sh,
                pts.gizmo.getProjection(),
                18);
            if (bestIdx >= 0) {
                pts.selectedIndex = bestIdx;
                pts.gizmo.reset();
            } else
                if (pts.gizmo.hoveredAxis != TranslationGizmo.Axis.NONE && pts.selectedPoint() != null && eye != null) {
                    PathToolState.PathPoint sel = pts.selectedPoint();
                    double pgx = sel.x + 0.5, pgy = sel.y + 0.5, pgz = sel.z + 0.5;
                    pts.gizmo.startDrag(mouseX, mouseY, sw, sh, eye, pgx, pgy, pgz, pgx, pgy, pgz, 0, 0, 0);
                } else
                    if (pts.planeGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE && pts.selectedPoint() != null
                        && eye != null) {
                            PathToolState.PathPoint sel = pts.selectedPoint();
                            double pgx = sel.x + 0.5, pgy = sel.y + 0.5, pgz = sel.z + 0.5;
                            pts.planeGizmo
                                .startDrag(mouseX, mouseY, sw, sh, eye, pgx, pgy, pgz, pgx, pgy, pgz, 0, 0, 0);
                        }
            return true;
        }

        return false;
    }
}
