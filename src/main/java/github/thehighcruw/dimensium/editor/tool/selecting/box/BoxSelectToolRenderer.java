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
import github.thehighcruw.dimensium.editor.tool.ToolRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.SelectionRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.SelectionState;

@SideOnly(Side.CLIENT)
public class BoxSelectToolRenderer implements ToolRenderer {

    public static final BoxSelectToolRenderer INSTANCE = new BoxSelectToolRenderer();

    private BoxSelectToolRenderer() {}

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
        boolean anyDragging = SelectionRenderer.boxPos1Gizmo.isDragging() || SelectionRenderer.boxPos2Gizmo.isDragging()
            || SelectionRenderer.boxCenterViewPlaneGizmo.isDragging()
            || SelectionRenderer.boxCenterGizmo.isDragging();
        if (!anyDragging) {
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
