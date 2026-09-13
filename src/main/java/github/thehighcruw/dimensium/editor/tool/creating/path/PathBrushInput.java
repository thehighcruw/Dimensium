/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.path;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.InputHandler;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.util.RenderUtils;

@SideOnly(Side.CLIENT)
public class PathBrushInput implements BrushInput {

    public static final PathBrushInput INSTANCE = new PathBrushInput();

    private PathBrushInput() {}

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        FreecamState fs = FreecamState.INSTANCE;
        int sw = RenderUtils.scaledWidth(), sh = RenderUtils.scaledHeight();
        int mouseX = (int) fs.cursorX, mouseY = (int) fs.cursorY;
        PathToolState pts = PathToolState.INSTANCE;

        if (button == KeyConstants.RMB && !InputHandler.isCtrlDown()) {
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
                pts.gizmo.getProjection(),
                18);
            if (bestIdx >= 0) {
                pts.selectedIndex = bestIdx;
                pts.gizmo.reset();
            } else
                if (pts.gizmo.hoveredAxis != TranslationGizmo.Axis.NONE && pts.selectedPoint() != null && eye != null) {
                    PathToolState.PathPoint sel = pts.selectedPoint();
                    double pgx = sel.x + 0.5, pgy = sel.y + 0.5, pgz = sel.z + 0.5;
                    pts.gizmo.startDrag(mouseX, mouseY, sw, sh, pgx, pgy, pgz, pgx, pgy, pgz, 0, 0, 0);
                } else
                    if (pts.planeGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE && pts.selectedPoint() != null
                        && eye != null) {
                            PathToolState.PathPoint sel = pts.selectedPoint();
                            double pgx = sel.x + 0.5, pgy = sel.y + 0.5, pgz = sel.z + 0.5;
                            pts.planeGizmo.startDrag(mouseX, mouseY, sw, sh, pgx, pgy, pgz, pgx, pgy, pgz, 0, 0, 0);
                        }
            return true;
        }

        return false;
    }

    @Override
    public void onGizmoDrag(int mx, int my, boolean snap) {
        PathToolState pts = PathToolState.INSTANCE;
        PathToolState.PathPoint selPt = pts.selectedPoint();
        if (selPt == null) return;
        if (pts.gizmo.isDragging()) {
            double[] anchor = pts.gizmo.updateDrag(mx, my);
            if (anchor != null) {
                selPt.x = (int) Math.floor(snap ? Math.floor(anchor[0] + 0.5) : anchor[0]);
                selPt.y = (int) Math.floor(snap ? Math.floor(anchor[1] + 0.5) : anchor[1]);
                selPt.z = (int) Math.floor(snap ? Math.floor(anchor[2] + 0.5) : anchor[2]);
                pts.invalidatePath();
            }
        } else if (pts.planeGizmo.isDragging()) {
            double[] anchor = pts.planeGizmo.updateDrag(mx, my);
            if (anchor != null) {
                selPt.x = (int) Math.floor(snap ? Math.floor(anchor[0] + 0.5) : anchor[0]);
                selPt.y = (int) Math.floor(snap ? Math.floor(anchor[1] + 0.5) : anchor[1]);
                selPt.z = (int) Math.floor(snap ? Math.floor(anchor[2] + 0.5) : anchor[2]);
                pts.invalidatePath();
            }
        }
    }
}
