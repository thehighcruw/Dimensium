/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.path;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.handler.AnchorSnap;
import github.thehighcruw.dimensium.editor.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.InputHandler;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public class PathBrushInput implements BrushInput {

    public static final PathBrushInput INSTANCE = new PathBrushInput();

    private PathBrushInput() {}

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        FreecamState freecamState = FreecamState.INSTANCE;
        int mouseX = (int) freecamState.cursorX, mouseY = (int) freecamState.cursorY;
        PathToolState pathToolState = PathToolState.INSTANCE;

        if (button == KeyConstants.RMB && !InputHandler.isCtrlDown()) {
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                Vec3DInt position = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ)
                        .plus(ExtrudeHelper.sideToOutwardDir(mop.sideHit));
                ItemStack block = SelectedBlockState.INSTANCE.selectedBlock;
                if (block != null) block = block.copy();
                PathToolState.PathPoint point = new PathToolState.PathPoint(position, 0, block);
                pathToolState.points.add(point);
                pathToolState.selectedIndex = pathToolState.points.size() - 1;
                pathToolState.getAxisTranslationGizmo().reset();
                pathToolState.invalidatePath();
            }
            return;
        }

        if (button == KeyConstants.LMB) {
            EntityLivingBase eye = mc.renderViewEntity;
            List<Vec3DInt> pointPositions = new ArrayList<>(pathToolState.points.size());
            for (PathToolState.PathPoint point : pathToolState.points) pointPositions.add(point.pos);
            int bestIdx = GuiDimensiumOverlay.findNearestPointOnScreen(
                    pointPositions,
                    pathToolState.selectedIndex,
                    mouseX,
                    mouseY,
                    pathToolState.getAxisTranslationGizmo().getProjection(),
                    PathMath.POINT_SELECTION_THRESHOLD_PX);
            if (bestIdx >= 0) {
                pathToolState.selectedIndex = bestIdx;
                pathToolState.getAxisTranslationGizmo().reset();
            } else if (pathToolState.getAxisTranslationGizmo().hoveredAxis != TranslationGizmo.Axis.NONE
                    && pathToolState.selectedPoint() != null
                    && eye != null) {
                Vec3DDouble gizmoPos =
                        pathToolState.selectedPoint().pos.toDouble().plus(0.5);
                pathToolState.getAxisTranslationGizmo().startDrag(mouseX, mouseY, gizmoPos, gizmoPos, Vec3DFloat.ZERO);
            } else if (pathToolState.getPlaneTranslationGizmo().hoveredPlane != PlaneTranslationGizmo.Plane.NONE
                    && pathToolState.selectedPoint() != null
                    && eye != null) {
                Vec3DDouble gizmoPos =
                        pathToolState.selectedPoint().pos.toDouble().plus(0.5);
                pathToolState.getPlaneTranslationGizmo().startDrag(mouseX, mouseY, gizmoPos, gizmoPos, Vec3DFloat.ZERO);
            }
        }
    }

    @Override
    public void onGizmoDrag(int mx, int my, boolean snap) {
        PathToolState pathToolState = PathToolState.INSTANCE;
        PathToolState.PathPoint selPt = pathToolState.selectedPoint();
        if (selPt == null) return;
        if (pathToolState.getAxisTranslationGizmo().isDragging()
                || pathToolState.getPlaneTranslationGizmo().isDragging()) {
            Vec3DDouble anchor = pathToolState.getAxisTranslationGizmo().isDragging()
                    ? pathToolState.getAxisTranslationGizmo().updateDrag(mx, my)
                    : pathToolState.getPlaneTranslationGizmo().updateDrag(mx, my);
            if (anchor != null) {
                selPt.pos = Vec3DInt.from(
                        AnchorSnap.toInt(anchor.x(), snap),
                        AnchorSnap.toInt(anchor.y(), snap),
                        AnchorSnap.toInt(anchor.z(), snap));
                pathToolState.invalidatePath();
            }
        }
    }
}
