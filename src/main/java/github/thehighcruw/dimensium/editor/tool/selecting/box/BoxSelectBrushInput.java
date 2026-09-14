/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.box;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.SelectionRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.Vec3DDouble;
import github.thehighcruw.dimensium.shared.Vec3DInt;

@SideOnly(Side.CLIENT)
public class BoxSelectBrushInput implements BrushInput {

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        FreecamState fs = FreecamState.INSTANCE;
        int mouseX = (int) fs.cursorX, mouseY = (int) fs.cursorY;

        SelectionState sel = SelectionState.INSTANCE;
        BoxSelectToolState ts = BoxSelectToolState.INSTANCE;

        if (sel.boxConfirmed) {
            if (button == KeyConstants.LMB) {
                EntityLivingBase eye = mc.renderViewEntity;
                if (eye != null) {
                    if (SelectionRenderer.boxPos1Gizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                        double gx = sel.pendingPos.x() + 0.5, gy = sel.pendingPos.y() + 0.5,
                            gz = sel.pendingPos.z() + 0.5;
                        SelectionRenderer.boxPos1Gizmo.startDrag(mouseX, mouseY, gx, gy, gz, gx, gy, gz, 0, 0, 0);
                        return;
                    }
                    if (SelectionRenderer.boxPos1PlaneGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                        double gx = sel.pendingPos.x() + 0.5, gy = sel.pendingPos.y() + 0.5,
                            gz = sel.pendingPos.z() + 0.5;
                        SelectionRenderer.boxPos1PlaneGizmo.startDrag(mouseX, mouseY, gx, gy, gz, gx, gy, gz, 0, 0, 0);
                        return;
                    }
                    if (SelectionRenderer.boxPos2Gizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                        double gx = sel.pendingPos2.x() + 0.5, gy = sel.pendingPos2.y() + 0.5,
                            gz = sel.pendingPos2.z() + 0.5;
                        SelectionRenderer.boxPos2Gizmo.startDrag(mouseX, mouseY, gx, gy, gz, gx, gy, gz, 0, 0, 0);
                        return;
                    }
                    if (SelectionRenderer.boxPos2PlaneGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                        double gx = sel.pendingPos2.x() + 0.5, gy = sel.pendingPos2.y() + 0.5,
                            gz = sel.pendingPos2.z() + 0.5;
                        SelectionRenderer.boxPos2PlaneGizmo.startDrag(mouseX, mouseY, gx, gy, gz, gx, gy, gz, 0, 0, 0);
                        return;
                    }
                    if (SelectionRenderer.boxCenterViewPlaneGizmo.hovered) {
                        double cxW = (sel.pendingPos.x() + sel.pendingPos2.x()) / 2.0 + 0.5;
                        double cyW = (sel.pendingPos.y() + sel.pendingPos2.y()) / 2.0 + 0.5;
                        double czW = (sel.pendingPos.z() + sel.pendingPos2.z()) / 2.0 + 0.5;
                        SelectionRenderer.INSTANCE.boxCenterDragP1 = sel.pendingPos;
                        SelectionRenderer.INSTANCE.boxCenterDragP2 = sel.pendingPos2;
                        SelectionRenderer.boxCenterViewPlaneGizmo
                            .startDrag(mouseX, mouseY, eye, cxW, cyW, czW, cxW, cyW, czW);
                        return;
                    }
                    if (SelectionRenderer.boxCenterGizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                        double cxW = (sel.pendingPos.x() + sel.pendingPos2.x()) / 2.0 + 0.5;
                        double cyW = (sel.pendingPos.y() + sel.pendingPos2.y()) / 2.0 + 0.5;
                        double czW = (sel.pendingPos.z() + sel.pendingPos2.z()) / 2.0 + 0.5;
                        SelectionRenderer.INSTANCE.boxCenterDragP1 = sel.pendingPos;
                        SelectionRenderer.INSTANCE.boxCenterDragP2 = sel.pendingPos2;
                        SelectionRenderer.boxCenterGizmo
                            .startDrag(mouseX, mouseY, cxW, cyW, czW, cxW, cyW, czW, 0, 0, 0);
                        return;
                    }
                    if (SelectionRenderer.boxCenterPlaneGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                        double cxW = (sel.pendingPos.x() + sel.pendingPos2.x()) / 2.0 + 0.5;
                        double cyW = (sel.pendingPos.y() + sel.pendingPos2.y()) / 2.0 + 0.5;
                        double czW = (sel.pendingPos.z() + sel.pendingPos2.z()) / 2.0 + 0.5;
                        SelectionRenderer.INSTANCE.boxCenterDragP1 = sel.pendingPos;
                        SelectionRenderer.INSTANCE.boxCenterDragP2 = sel.pendingPos2;
                        SelectionRenderer.boxCenterPlaneGizmo
                            .startDrag(mouseX, mouseY, cxW, cyW, czW, cxW, cyW, czW, 0, 0, 0);
                        return;
                    }
                }
                GuiDimensiumOverlay.commitBoxSelection(sel, ts);
            }
            return;
        }

        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        if (button == KeyConstants.RMB) {
            sel.pendingPos1 = true;
            sel.pendingPos = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        }
    }

    @Override
    public void onGizmoDrag(int mx, int my, boolean snap) {
        SelectionState bxSel = SelectionState.INSTANCE;
        if (!bxSel.boxConfirmed) return;
        if (SelectionRenderer.boxPos1Gizmo.isDragging()) {
            Vec3DDouble anchor = SelectionRenderer.boxPos1Gizmo.updateDrag(mx, my);
            if (anchor != null) {
                bxSel.pendingPos = Vec3DInt.from(
                    (int) Math.floor(snap ? Math.floor(anchor.x() + 0.5) : anchor.x()),
                    (int) Math.floor(snap ? Math.floor(anchor.y() + 0.5) : anchor.y()),
                    (int) Math.floor(snap ? Math.floor(anchor.z() + 0.5) : anchor.z()));
            }
        } else if (SelectionRenderer.boxPos2Gizmo.isDragging()) {
            Vec3DDouble anchor = SelectionRenderer.boxPos2Gizmo.updateDrag(mx, my);
            if (anchor != null) {
                bxSel.pendingPos2 = Vec3DInt.from(
                    (int) Math.floor(snap ? Math.floor(anchor.x() + 0.5) : anchor.x()),
                    (int) Math.floor(snap ? Math.floor(anchor.y() + 0.5) : anchor.y()),
                    (int) Math.floor(snap ? Math.floor(anchor.z() + 0.5) : anchor.z()));
            }
        } else if (SelectionRenderer.boxCenterViewPlaneGizmo.isDragging()) {
            Vec3DDouble anchor = SelectionRenderer.boxCenterViewPlaneGizmo.updateDrag(mx, my);
            if (anchor != null) {
                double cx0 = (SelectionRenderer.INSTANCE.boxCenterDragP1.x()
                    + SelectionRenderer.INSTANCE.boxCenterDragP2.x()) / 2.0 + 0.5;
                double cy0 = (SelectionRenderer.INSTANCE.boxCenterDragP1.y()
                    + SelectionRenderer.INSTANCE.boxCenterDragP2.y()) / 2.0 + 0.5;
                double cz0 = (SelectionRenderer.INSTANCE.boxCenterDragP1.z()
                    + SelectionRenderer.INSTANCE.boxCenterDragP2.z()) / 2.0 + 0.5;
                int dx = (int) Math.floor(snap ? Math.floor(anchor.x() - cx0 + 0.5) : anchor.x() - cx0);
                int dy = (int) Math.floor(snap ? Math.floor(anchor.y() - cy0 + 0.5) : anchor.y() - cy0);
                int dz = (int) Math.floor(snap ? Math.floor(anchor.z() - cz0 + 0.5) : anchor.z() - cz0);
                bxSel.pendingPos = Vec3DInt.from(
                    SelectionRenderer.INSTANCE.boxCenterDragP1.x() + dx,
                    SelectionRenderer.INSTANCE.boxCenterDragP1.y() + dy,
                    SelectionRenderer.INSTANCE.boxCenterDragP1.z() + dz);
                bxSel.pendingPos2 = Vec3DInt.from(
                    SelectionRenderer.INSTANCE.boxCenterDragP2.x() + dx,
                    SelectionRenderer.INSTANCE.boxCenterDragP2.y() + dy,
                    SelectionRenderer.INSTANCE.boxCenterDragP2.z() + dz);
            }
        } else if (SelectionRenderer.boxCenterGizmo.isDragging()) {
            Vec3DDouble anchor = SelectionRenderer.boxCenterGizmo.updateDrag(mx, my);
            if (anchor != null) {
                double cx0 = (SelectionRenderer.INSTANCE.boxCenterDragP1.x()
                    + SelectionRenderer.INSTANCE.boxCenterDragP2.x()) / 2.0 + 0.5;
                double cy0 = (SelectionRenderer.INSTANCE.boxCenterDragP1.y()
                    + SelectionRenderer.INSTANCE.boxCenterDragP2.y()) / 2.0 + 0.5;
                double cz0 = (SelectionRenderer.INSTANCE.boxCenterDragP1.z()
                    + SelectionRenderer.INSTANCE.boxCenterDragP2.z()) / 2.0 + 0.5;
                int dx = (int) Math.floor(snap ? Math.floor(anchor.x() - cx0 + 0.5) : anchor.x() - cx0);
                int dy = (int) Math.floor(snap ? Math.floor(anchor.y() - cy0 + 0.5) : anchor.y() - cy0);
                int dz = (int) Math.floor(snap ? Math.floor(anchor.z() - cz0 + 0.5) : anchor.z() - cz0);
                bxSel.pendingPos = Vec3DInt.from(
                    SelectionRenderer.INSTANCE.boxCenterDragP1.x() + dx,
                    SelectionRenderer.INSTANCE.boxCenterDragP1.y() + dy,
                    SelectionRenderer.INSTANCE.boxCenterDragP1.z() + dz);
                bxSel.pendingPos2 = Vec3DInt.from(
                    SelectionRenderer.INSTANCE.boxCenterDragP2.x() + dx,
                    SelectionRenderer.INSTANCE.boxCenterDragP2.y() + dy,
                    SelectionRenderer.INSTANCE.boxCenterDragP2.z() + dz);
            }
        }
    }
}
