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
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public class BoxSelectToolRenderer implements ToolRenderer {

    public static final BoxSelectToolRenderer INSTANCE = new BoxSelectToolRenderer();

    private BoxSelectToolRenderer() {}

    @Override
    public boolean isBlockAffected(Minecraft mc, Vec3DInt wc) {
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
            Vec3DDouble pos1Center = bxSel.pendingPos.toDouble().plus(0.5);
            Vec3DDouble pos2Center = bxSel.pendingPos2.toDouble().plus(0.5);
            SelectionRenderer.boxPos1Gizmo.updateHover(mx, my, bxEye, pos1Center, Vec3DFloat.ZERO);
            if (SelectionRenderer.boxPos1Gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                SelectionRenderer.boxPos2Gizmo.updateHover(mx, my, bxEye, pos2Center, Vec3DFloat.ZERO);
            } else {
                SelectionRenderer.boxPos2Gizmo.hoveredAxis = TranslationGizmo.Axis.NONE;
            }
            if (SelectionRenderer.boxPos1Gizmo.hoveredAxis == TranslationGizmo.Axis.NONE
                    && SelectionRenderer.boxPos2Gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                Vec3DDouble boxCenter = bxSel.pendingPos
                        .toDouble()
                        .plus(bxSel.pendingPos2.toDouble())
                        .times(0.5)
                        .plus(0.5);
                SelectionRenderer.boxCenterViewPlaneGizmo.updateHover(mx, my, bxEye, boxCenter);
                SelectionRenderer.boxCenterGizmo.updateHover(mx, my, bxEye, boxCenter, Vec3DFloat.ZERO);
            } else {
                SelectionRenderer.boxCenterViewPlaneGizmo.hovered = false;
                SelectionRenderer.boxCenterGizmo.hoveredAxis = TranslationGizmo.Axis.NONE;
            }
        }
    }
}
