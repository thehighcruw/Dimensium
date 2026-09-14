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
import github.thehighcruw.dimensium.editor.handler.AnchorSnap;
import github.thehighcruw.dimensium.editor.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.InputHandler;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;

@SideOnly(Side.CLIENT)
public class PathBrushInput implements BrushInput {

    public static final PathBrushInput INSTANCE = new PathBrushInput();

    private PathBrushInput() {}

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        FreecamState fs = FreecamState.INSTANCE;
        int mouseX = (int) fs.cursorX, mouseY = (int) fs.cursorY;
        PathToolState pts = PathToolState.INSTANCE;

        if (button == KeyConstants.RMB && !InputHandler.isCtrlDown()) {
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                int[] off = ExtrudeHelper.sideToOutwardDir(mop.sideHit);
                int px = mop.blockX + off[0], py = mop.blockY + off[1], pz = mop.blockZ + off[2];
                net.minecraft.item.ItemStack blk = SelectedBlockState.INSTANCE.selectedBlock;
                if (blk != null) blk = blk.copy();
                PathToolState.PathPoint pt = new PathToolState.PathPoint(Vec3DInt.from(px, py, pz), 0, blk);
                pts.points.add(pt);
                pts.selectedIndex = pts.points.size() - 1;
                pts.getAxisTranslationGizmo()
                    .reset();
                pts.invalidatePath();
            }
            return;
        }

        if (button == KeyConstants.LMB) {
            EntityLivingBase eye = mc.renderViewEntity;
            List<int[]> ptPositions = new ArrayList<>(pts.points.size());
            for (PathToolState.PathPoint pt : pts.points)
                ptPositions.add(new int[] { pt.pos.x(), pt.pos.y(), pt.pos.z() });
            int bestIdx = GuiDimensiumOverlay.findNearestPointOnScreen(
                ptPositions,
                pts.selectedIndex,
                mouseX,
                mouseY,
                pts.getAxisTranslationGizmo()
                    .getProjection(),
                18);
            if (bestIdx >= 0) {
                pts.selectedIndex = bestIdx;
                pts.getAxisTranslationGizmo()
                    .reset();
            } else if (pts.getAxisTranslationGizmo().hoveredAxis != TranslationGizmo.Axis.NONE
                && pts.selectedPoint() != null
                && eye != null) {
                    Vec3DDouble gp = Vec3DDouble.from(
                        pts.selectedPoint().pos.x() + 0.5,
                        pts.selectedPoint().pos.y() + 0.5,
                        pts.selectedPoint().pos.z() + 0.5);
                    pts.getAxisTranslationGizmo()
                        .startDrag(mouseX, mouseY, gp.x(), gp.y(), gp.z(), gp.x(), gp.y(), gp.z(), 0, 0, 0);
                } else if (pts.getPlaneTranslationGizmo().hoveredPlane != PlaneTranslationGizmo.Plane.NONE
                    && pts.selectedPoint() != null
                    && eye != null) {
                        Vec3DDouble gp = Vec3DDouble.from(
                            pts.selectedPoint().pos.x() + 0.5,
                            pts.selectedPoint().pos.y() + 0.5,
                            pts.selectedPoint().pos.z() + 0.5);
                        pts.getPlaneTranslationGizmo()
                            .startDrag(mouseX, mouseY, gp.x(), gp.y(), gp.z(), gp.x(), gp.y(), gp.z(), 0, 0, 0);
                    }
        }

    }

    @Override
    public void onGizmoDrag(int mx, int my, boolean snap) {
        PathToolState pts = PathToolState.INSTANCE;
        PathToolState.PathPoint selPt = pts.selectedPoint();
        if (selPt == null) return;
        if (pts.getAxisTranslationGizmo()
            .isDragging()
            || pts.getPlaneTranslationGizmo()
                .isDragging()) {
            Vec3DDouble anchor = pts.getAxisTranslationGizmo()
                .isDragging()
                    ? pts.getAxisTranslationGizmo()
                        .updateDrag(mx, my)
                    : pts.getPlaneTranslationGizmo()
                        .updateDrag(mx, my);
            if (anchor != null) {
                selPt.pos = Vec3DInt.from(
                    AnchorSnap.toInt(anchor.x(), snap),
                    AnchorSnap.toInt(anchor.y(), snap),
                    AnchorSnap.toInt(anchor.z(), snap));
                pts.invalidatePath();
            }
        }
    }
}
