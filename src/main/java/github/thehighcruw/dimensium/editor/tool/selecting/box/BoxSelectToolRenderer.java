/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.box;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.SelectionRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public class BoxSelectToolRenderer implements ToolRenderer {

    public static final BoxSelectToolRenderer INSTANCE = new BoxSelectToolRenderer();

    private BoxSelectToolRenderer() {}

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
        SelectionState bxSel = SelectionState.INSTANCE;
        if (!bxSel.boxConfirmed || mc.renderViewEntity == null) return;
        EntityLivingBase bxEye = mc.renderViewEntity;
        boolean anyDragging = SelectionRenderer.boxPos1Gizmo.isDragging()
                || SelectionRenderer.boxPos2Gizmo.isDragging()
                || SelectionRenderer.boxCenterViewPlaneGizmo.isDragging()
                || SelectionRenderer.boxCenterGizmo.isDragging();
        if (!anyDragging) {
            SelectionRenderer.boxPos1Gizmo.updateHover(
                    mx,
                    my,
                    bxEye,
                    bxSel.pendingPos.x() + 0.5,
                    bxSel.pendingPos.y() + 0.5,
                    bxSel.pendingPos.z() + 0.5,
                    0,
                    0,
                    0);
            if (SelectionRenderer.boxPos1Gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                SelectionRenderer.boxPos2Gizmo.updateHover(
                        mx,
                        my,
                        bxEye,
                        bxSel.pendingPos2.x() + 0.5,
                        bxSel.pendingPos2.y() + 0.5,
                        bxSel.pendingPos2.z() + 0.5,
                        0,
                        0,
                        0);
            } else {
                SelectionRenderer.boxPos2Gizmo.hoveredAxis = TranslationGizmo.Axis.NONE;
            }
            if (SelectionRenderer.boxPos1Gizmo.hoveredAxis == TranslationGizmo.Axis.NONE
                    && SelectionRenderer.boxPos2Gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                double cxW = (bxSel.pendingPos.x() + bxSel.pendingPos2.x()) / 2.0 + 0.5;
                double cyW = (bxSel.pendingPos.y() + bxSel.pendingPos2.y()) / 2.0 + 0.5;
                double czW = (bxSel.pendingPos.z() + bxSel.pendingPos2.z()) / 2.0 + 0.5;
                SelectionRenderer.boxCenterViewPlaneGizmo.updateHover(mx, my, bxEye, cxW, cyW, czW);
                SelectionRenderer.boxCenterGizmo.updateHover(mx, my, bxEye, cxW, cyW, czW, 0, 0, 0);
            } else {
                SelectionRenderer.boxCenterViewPlaneGizmo.hovered = false;
                SelectionRenderer.boxCenterGizmo.hoveredAxis = TranslationGizmo.Axis.NONE;
            }
        }
    }
}
