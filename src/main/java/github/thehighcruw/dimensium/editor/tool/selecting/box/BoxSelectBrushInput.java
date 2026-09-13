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

@SideOnly(Side.CLIENT)
public class BoxSelectBrushInput implements BrushInput {

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        FreecamState fs = FreecamState.INSTANCE;
        int mouseX = (int) fs.cursorX, mouseY = (int) fs.cursorY;

        SelectionState sel = SelectionState.INSTANCE;
        BoxSelectToolState ts = BoxSelectToolState.INSTANCE;

        if (sel.boxConfirmed) {
            if (button == KeyConstants.LMB) {
                EntityLivingBase eye = mc.renderViewEntity;
                if (eye != null) {
                    if (SelectionRenderer.boxPos1Gizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                        double gx = sel.pendingX + 0.5, gy = sel.pendingY + 0.5, gz = sel.pendingZ + 0.5;
                        SelectionRenderer.boxPos1Gizmo.startDrag(mouseX, mouseY, gx, gy, gz, gx, gy, gz, 0, 0, 0);
                        return true;
                    }
                    if (SelectionRenderer.boxPos1PlaneGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                        double gx = sel.pendingX + 0.5, gy = sel.pendingY + 0.5, gz = sel.pendingZ + 0.5;
                        SelectionRenderer.boxPos1PlaneGizmo.startDrag(mouseX, mouseY, gx, gy, gz, gx, gy, gz, 0, 0, 0);
                        return true;
                    }
                    if (SelectionRenderer.boxPos2Gizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                        double gx = sel.pendingX2 + 0.5, gy = sel.pendingY2 + 0.5, gz = sel.pendingZ2 + 0.5;
                        SelectionRenderer.boxPos2Gizmo.startDrag(mouseX, mouseY, gx, gy, gz, gx, gy, gz, 0, 0, 0);
                        return true;
                    }
                    if (SelectionRenderer.boxPos2PlaneGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                        double gx = sel.pendingX2 + 0.5, gy = sel.pendingY2 + 0.5, gz = sel.pendingZ2 + 0.5;
                        SelectionRenderer.boxPos2PlaneGizmo.startDrag(mouseX, mouseY, gx, gy, gz, gx, gy, gz, 0, 0, 0);
                        return true;
                    }
                    if (SelectionRenderer.boxCenterViewPlaneGizmo.hovered) {
                        double cxW = (sel.pendingX + sel.pendingX2) / 2.0 + 0.5;
                        double cyW = (sel.pendingY + sel.pendingY2) / 2.0 + 0.5;
                        double czW = (sel.pendingZ + sel.pendingZ2) / 2.0 + 0.5;
                        SelectionRenderer.INSTANCE.boxCenterDragP1X = sel.pendingX;
                        SelectionRenderer.INSTANCE.boxCenterDragP1Y = sel.pendingY;
                        SelectionRenderer.INSTANCE.boxCenterDragP1Z = sel.pendingZ;
                        SelectionRenderer.INSTANCE.boxCenterDragP2X = sel.pendingX2;
                        SelectionRenderer.INSTANCE.boxCenterDragP2Y = sel.pendingY2;
                        SelectionRenderer.INSTANCE.boxCenterDragP2Z = sel.pendingZ2;
                        SelectionRenderer.boxCenterViewPlaneGizmo
                            .startDrag(mouseX, mouseY, eye, cxW, cyW, czW, cxW, cyW, czW);
                        return true;
                    }
                    if (SelectionRenderer.boxCenterGizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                        double cxW = (sel.pendingX + sel.pendingX2) / 2.0 + 0.5;
                        double cyW = (sel.pendingY + sel.pendingY2) / 2.0 + 0.5;
                        double czW = (sel.pendingZ + sel.pendingZ2) / 2.0 + 0.5;
                        SelectionRenderer.INSTANCE.boxCenterDragP1X = sel.pendingX;
                        SelectionRenderer.INSTANCE.boxCenterDragP1Y = sel.pendingY;
                        SelectionRenderer.INSTANCE.boxCenterDragP1Z = sel.pendingZ;
                        SelectionRenderer.INSTANCE.boxCenterDragP2X = sel.pendingX2;
                        SelectionRenderer.INSTANCE.boxCenterDragP2Y = sel.pendingY2;
                        SelectionRenderer.INSTANCE.boxCenterDragP2Z = sel.pendingZ2;
                        SelectionRenderer.boxCenterGizmo
                            .startDrag(mouseX, mouseY, cxW, cyW, czW, cxW, cyW, czW, 0, 0, 0);
                        return true;
                    }
                    if (SelectionRenderer.boxCenterPlaneGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                        double cxW = (sel.pendingX + sel.pendingX2) / 2.0 + 0.5;
                        double cyW = (sel.pendingY + sel.pendingY2) / 2.0 + 0.5;
                        double czW = (sel.pendingZ + sel.pendingZ2) / 2.0 + 0.5;
                        SelectionRenderer.INSTANCE.boxCenterDragP1X = sel.pendingX;
                        SelectionRenderer.INSTANCE.boxCenterDragP1Y = sel.pendingY;
                        SelectionRenderer.INSTANCE.boxCenterDragP1Z = sel.pendingZ;
                        SelectionRenderer.INSTANCE.boxCenterDragP2X = sel.pendingX2;
                        SelectionRenderer.INSTANCE.boxCenterDragP2Y = sel.pendingY2;
                        SelectionRenderer.INSTANCE.boxCenterDragP2Z = sel.pendingZ2;
                        SelectionRenderer.boxCenterPlaneGizmo
                            .startDrag(mouseX, mouseY, cxW, cyW, czW, cxW, cyW, czW, 0, 0, 0);
                        return true;
                    }
                }
                GuiDimensiumOverlay.commitBoxSelection(sel, ts);
            }
            return true;
        }

        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;

        if (button == KeyConstants.RMB) {
            sel.pendingPos1 = true;
            sel.boxConfirmed = false;
            sel.pendingX = mop.blockX;
            sel.pendingY = mop.blockY;
            sel.pendingZ = mop.blockZ;
        }
        return true;
    }

    @Override
    public void onGizmoDrag(int mx, int my, boolean snap) {
        SelectionState bxSel = SelectionState.INSTANCE;
        if (!bxSel.boxConfirmed) return;
        if (SelectionRenderer.boxPos1Gizmo.isDragging()) {
            double[] anchor = SelectionRenderer.boxPos1Gizmo.updateDrag(mx, my);
            if (anchor != null) {
                bxSel.pendingX = (int) Math.floor(snap ? Math.floor(anchor[0] + 0.5) : anchor[0]);
                bxSel.pendingY = (int) Math.floor(snap ? Math.floor(anchor[1] + 0.5) : anchor[1]);
                bxSel.pendingZ = (int) Math.floor(snap ? Math.floor(anchor[2] + 0.5) : anchor[2]);
            }
        } else if (SelectionRenderer.boxPos2Gizmo.isDragging()) {
            double[] anchor = SelectionRenderer.boxPos2Gizmo.updateDrag(mx, my);
            if (anchor != null) {
                bxSel.pendingX2 = (int) Math.floor(snap ? Math.floor(anchor[0] + 0.5) : anchor[0]);
                bxSel.pendingY2 = (int) Math.floor(snap ? Math.floor(anchor[1] + 0.5) : anchor[1]);
                bxSel.pendingZ2 = (int) Math.floor(snap ? Math.floor(anchor[2] + 0.5) : anchor[2]);
            }
        } else if (SelectionRenderer.boxCenterViewPlaneGizmo.isDragging()) {
            double[] anchor = SelectionRenderer.boxCenterViewPlaneGizmo.updateDrag(mx, my);
            if (anchor != null) {
                double cx0 = (SelectionRenderer.INSTANCE.boxCenterDragP1X + SelectionRenderer.INSTANCE.boxCenterDragP2X)
                    / 2.0 + 0.5;
                double cy0 = (SelectionRenderer.INSTANCE.boxCenterDragP1Y + SelectionRenderer.INSTANCE.boxCenterDragP2Y)
                    / 2.0 + 0.5;
                double cz0 = (SelectionRenderer.INSTANCE.boxCenterDragP1Z + SelectionRenderer.INSTANCE.boxCenterDragP2Z)
                    / 2.0 + 0.5;
                int dx = (int) Math.floor(snap ? Math.floor(anchor[0] - cx0 + 0.5) : anchor[0] - cx0);
                int dy = (int) Math.floor(snap ? Math.floor(anchor[1] - cy0 + 0.5) : anchor[1] - cy0);
                int dz = (int) Math.floor(snap ? Math.floor(anchor[2] - cz0 + 0.5) : anchor[2] - cz0);
                bxSel.pendingX = SelectionRenderer.INSTANCE.boxCenterDragP1X + dx;
                bxSel.pendingY = SelectionRenderer.INSTANCE.boxCenterDragP1Y + dy;
                bxSel.pendingZ = SelectionRenderer.INSTANCE.boxCenterDragP1Z + dz;
                bxSel.pendingX2 = SelectionRenderer.INSTANCE.boxCenterDragP2X + dx;
                bxSel.pendingY2 = SelectionRenderer.INSTANCE.boxCenterDragP2Y + dy;
                bxSel.pendingZ2 = SelectionRenderer.INSTANCE.boxCenterDragP2Z + dz;
            }
        } else if (SelectionRenderer.boxCenterGizmo.isDragging()) {
            double[] anchor = SelectionRenderer.boxCenterGizmo.updateDrag(mx, my);
            if (anchor != null) {
                double cx0 = (SelectionRenderer.INSTANCE.boxCenterDragP1X + SelectionRenderer.INSTANCE.boxCenterDragP2X)
                    / 2.0 + 0.5;
                double cy0 = (SelectionRenderer.INSTANCE.boxCenterDragP1Y + SelectionRenderer.INSTANCE.boxCenterDragP2Y)
                    / 2.0 + 0.5;
                double cz0 = (SelectionRenderer.INSTANCE.boxCenterDragP1Z + SelectionRenderer.INSTANCE.boxCenterDragP2Z)
                    / 2.0 + 0.5;
                int dx = (int) Math.floor(snap ? Math.floor(anchor[0] - cx0 + 0.5) : anchor[0] - cx0);
                int dy = (int) Math.floor(snap ? Math.floor(anchor[1] - cy0 + 0.5) : anchor[1] - cy0);
                int dz = (int) Math.floor(snap ? Math.floor(anchor[2] - cz0 + 0.5) : anchor[2] - cz0);
                bxSel.pendingX = SelectionRenderer.INSTANCE.boxCenterDragP1X + dx;
                bxSel.pendingY = SelectionRenderer.INSTANCE.boxCenterDragP1Y + dy;
                bxSel.pendingZ = SelectionRenderer.INSTANCE.boxCenterDragP1Z + dz;
                bxSel.pendingX2 = SelectionRenderer.INSTANCE.boxCenterDragP2X + dx;
                bxSel.pendingY2 = SelectionRenderer.INSTANCE.boxCenterDragP2Y + dy;
                bxSel.pendingZ2 = SelectionRenderer.INSTANCE.boxCenterDragP2Z + dz;
            }
        }
    }
}
