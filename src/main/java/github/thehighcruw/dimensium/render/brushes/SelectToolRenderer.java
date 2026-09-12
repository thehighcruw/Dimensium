/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.brushes;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.world.SelectionRenderer;
import github.thehighcruw.dimensium.render.world.TranslationGizmo;
import github.thehighcruw.dimensium.tool.state.SelectionState;

@SideOnly(Side.CLIENT)
public class SelectToolRenderer implements ToolRenderer {

    public static final SelectToolRenderer INSTANCE = new SelectToolRenderer();

    private SelectToolRenderer() {}

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
        SelectionState bxSel = SelectionState.INSTANCE;
        if (!bxSel.boxConfirmed || mc.renderViewEntity == null) return;
        EntityLivingBase bxEye = mc.renderViewEntity;
        boolean snap = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
        if (SelectionRenderer.boxPos1Gizmo.isDragging()) {
            double[] anchor = SelectionRenderer.boxPos1Gizmo.updateDrag(mx3d, my3d);
            if (anchor != null) {
                bxSel.pendingX = (int) Math.floor(snap ? Math.floor(anchor[0] + 0.5) : anchor[0]);
                bxSel.pendingY = (int) Math.floor(snap ? Math.floor(anchor[1] + 0.5) : anchor[1]);
                bxSel.pendingZ = (int) Math.floor(snap ? Math.floor(anchor[2] + 0.5) : anchor[2]);
            }
        } else if (SelectionRenderer.boxPos2Gizmo.isDragging()) {
            double[] anchor = SelectionRenderer.boxPos2Gizmo.updateDrag(mx3d, my3d);
            if (anchor != null) {
                bxSel.pendingX2 = (int) Math.floor(snap ? Math.floor(anchor[0] + 0.5) : anchor[0]);
                bxSel.pendingY2 = (int) Math.floor(snap ? Math.floor(anchor[1] + 0.5) : anchor[1]);
                bxSel.pendingZ2 = (int) Math.floor(snap ? Math.floor(anchor[2] + 0.5) : anchor[2]);
            }
        } else if (SelectionRenderer.boxCenterViewPlaneGizmo.isDragging()) {
            double[] anchor = SelectionRenderer.boxCenterViewPlaneGizmo.updateDrag(mx3d, my3d);
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
            double[] anchor = SelectionRenderer.boxCenterGizmo.updateDrag(mx3d, my3d);
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
        } else {
            SelectionRenderer.boxPos1Gizmo.updateHover(
                mx,
                my,
                sw,
                sh,
                bxEye,
                bxSel.pendingX + 0.5,
                bxSel.pendingY + 0.5,
                bxSel.pendingZ + 0.5,
                0,
                0,
                0);
            if (SelectionRenderer.boxPos1Gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                SelectionRenderer.boxPos2Gizmo.updateHover(
                    mx,
                    my,
                    sw,
                    sh,
                    bxEye,
                    bxSel.pendingX2 + 0.5,
                    bxSel.pendingY2 + 0.5,
                    bxSel.pendingZ2 + 0.5,
                    0,
                    0,
                    0);
            } else {
                SelectionRenderer.boxPos2Gizmo.hoveredAxis = TranslationGizmo.Axis.NONE;
            }
            if (SelectionRenderer.boxPos1Gizmo.hoveredAxis == TranslationGizmo.Axis.NONE
                && SelectionRenderer.boxPos2Gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                double cxW = (bxSel.pendingX + bxSel.pendingX2) / 2.0 + 0.5;
                double cyW = (bxSel.pendingY + bxSel.pendingY2) / 2.0 + 0.5;
                double czW = (bxSel.pendingZ + bxSel.pendingZ2) / 2.0 + 0.5;
                SelectionRenderer.boxCenterViewPlaneGizmo.updateHover(mx, my, sw, sh, bxEye, cxW, cyW, czW, 0, 0, 0);
                SelectionRenderer.boxCenterGizmo.updateHover(mx, my, sw, sh, bxEye, cxW, cyW, czW, 0, 0, 0);
            } else {
                SelectionRenderer.boxCenterViewPlaneGizmo.hovered = false;
                SelectionRenderer.boxCenterGizmo.hoveredAxis = TranslationGizmo.Axis.NONE;
            }
        }
    }
}
