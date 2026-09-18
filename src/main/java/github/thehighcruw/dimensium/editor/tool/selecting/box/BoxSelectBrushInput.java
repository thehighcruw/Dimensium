/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.box;

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
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

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
                    Vec3DDouble g1 = sel.pendingPos.toDouble().plus(0.5);
                    if (tryStartPointGizmos(
                            mouseX, mouseY, g1, SelectionRenderer.boxPos1Gizmo, SelectionRenderer.boxPos1PlaneGizmo))
                        return;
                    Vec3DDouble g2 = sel.pendingPos2.toDouble().plus(0.5);
                    if (tryStartPointGizmos(
                            mouseX, mouseY, g2, SelectionRenderer.boxPos2Gizmo, SelectionRenderer.boxPos2PlaneGizmo))
                        return;
                    Vec3DDouble c = sel.pendingPos
                            .toDouble()
                            .plus(sel.pendingPos2.toDouble())
                            .divide(2.0)
                            .plus(0.5);
                    if (SelectionRenderer.boxCenterViewPlaneGizmo.hovered) {
                        SelectionRenderer.INSTANCE.boxCenterDragP1 = sel.pendingPos;
                        SelectionRenderer.INSTANCE.boxCenterDragP2 = sel.pendingPos2;
                        SelectionRenderer.boxCenterViewPlaneGizmo.startDrag(mouseX, mouseY, eye, c, c);
                        return;
                    }
                    if (SelectionRenderer.boxCenterGizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                        SelectionRenderer.INSTANCE.boxCenterDragP1 = sel.pendingPos;
                        SelectionRenderer.INSTANCE.boxCenterDragP2 = sel.pendingPos2;
                        SelectionRenderer.boxCenterGizmo.startDrag(mouseX, mouseY, c, c, Vec3DFloat.ZERO);
                        return;
                    }
                    if (SelectionRenderer.boxCenterPlaneGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                        SelectionRenderer.INSTANCE.boxCenterDragP1 = sel.pendingPos;
                        SelectionRenderer.INSTANCE.boxCenterDragP2 = sel.pendingPos2;
                        SelectionRenderer.boxCenterPlaneGizmo.startDrag(mouseX, mouseY, c, c, Vec3DFloat.ZERO);
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
                bxSel.pendingPos = snap ? Vec3DInt.round(anchor) : Vec3DInt.floor(anchor);
            }
        } else if (SelectionRenderer.boxPos2Gizmo.isDragging()) {
            Vec3DDouble anchor = SelectionRenderer.boxPos2Gizmo.updateDrag(mx, my);
            if (anchor != null) {
                bxSel.pendingPos2 = snap ? Vec3DInt.round(anchor) : Vec3DInt.floor(anchor);
            }
        } else if (SelectionRenderer.boxCenterViewPlaneGizmo.isDragging()) {
            Vec3DDouble anchor = SelectionRenderer.boxCenterViewPlaneGizmo.updateDrag(mx, my);
            if (anchor != null) {
                applyCenter(bxSel, anchor, snap);
            }
        } else if (SelectionRenderer.boxCenterGizmo.isDragging()) {
            Vec3DDouble anchor = SelectionRenderer.boxCenterGizmo.updateDrag(mx, my);
            if (anchor != null) {
                applyCenter(bxSel, anchor, snap);
            }
        }
    }

    private static boolean tryStartPointGizmos(
            int mouseX, int mouseY, Vec3DDouble g, TranslationGizmo axisGizmo, PlaneTranslationGizmo planeGizmo) {
        if (axisGizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
            axisGizmo.startDrag(mouseX, mouseY, g, g, Vec3DFloat.ZERO);
            return true;
        }
        if (planeGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
            planeGizmo.startDrag(mouseX, mouseY, g, g, Vec3DFloat.ZERO);
            return true;
        }
        return false;
    }

    private static void applyCenter(SelectionState bxSel, Vec3DDouble anchor, boolean snap) {
        Vec3DDouble center0 = SelectionRenderer.INSTANCE
                .boxCenterDragP1
                .toDouble()
                .plus(SelectionRenderer.INSTANCE.boxCenterDragP2.toDouble())
                .divide(2.0)
                .plus(0.5);
        Vec3DDouble delta = anchor.minus(center0);
        Vec3DInt d = snap ? Vec3DInt.round(delta) : Vec3DInt.floor(delta);
        bxSel.pendingPos = SelectionRenderer.INSTANCE.boxCenterDragP1.plus(d);
        bxSel.pendingPos2 = SelectionRenderer.INSTANCE.boxCenterDragP2.plus(d);
    }
}
