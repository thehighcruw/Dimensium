/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.shape;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.RotationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.ScalingGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.KeyConstants;

@SideOnly(Side.CLIENT)
public class ShapeBrushInput implements BrushInput {

    public static final ShapeBrushInput INSTANCE = new ShapeBrushInput();

    private ShapeBrushInput() {}

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        if (mc.thePlayer == null) return;
        FreecamState fs = FreecamState.INSTANCE;
        int mouseX = (int) fs.cursorX, mouseY = (int) fs.cursorY;
        ShapePlacementState ps = ShapePlacementState.INSTANCE;

        if (!ps.active) {
            if (button == KeyConstants.RMB) {
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    ps.start(mop.blockX, mop.blockY, mop.blockZ);
                }
            }
            return;
        }

        if (button == KeyConstants.LMB) {
            double cx = ps.centerX(), cy = ps.centerY(), cz = ps.centerZ();
            EntityLivingBase eye = mc.renderViewEntity;
            if (ps.viewPlaneGizmo.hovered) {
                ps.viewPlaneGizmo.startDrag(mouseX, mouseY, eye, cx, cy, cz, ps.anchorFX, ps.anchorFY, ps.anchorFZ);
            } else if (ps.getAxisTranslationGizmo().hoveredAxis != TranslationGizmo.Axis.NONE) {
                ps.getAxisTranslationGizmo()
                    .startDrag(
                        mouseX,
                        mouseY,
                        cx,
                        cy,
                        cz,
                        ps.anchorFX,
                        ps.anchorFY,
                        ps.anchorFZ,
                        ps.rotX,
                        ps.rotY,
                        ps.rotZ);
            } else if (ps.getRotationGizmo().hoveredAxis != RotationGizmo.Axis.NONE) {
                ps.rotDragBaseX = ps.rotX;
                ps.rotDragBaseY = ps.rotY;
                ps.rotDragBaseZ = ps.rotZ;
                ps.getRotationGizmo()
                    .startDrag(mouseX, mouseY, cx, cy, cz, ps.rotX, ps.rotY, ps.rotZ);
            } else if (ps.getScalingGizmo().hoveredAxis != ScalingGizmo.Axis.NONE) {
                float currentScale = ps.getScalingGizmo().hoveredAxis == ScalingGizmo.Axis.X ? ps.scaleX
                    : ps.getScalingGizmo().hoveredAxis == ScalingGizmo.Axis.Y ? ps.scaleY : ps.scaleZ;
                ShapeToolState sts = ShapeToolState.INSTANCE;
                ps.scaleDragBaseW = sts.shapeWidth;
                ps.scaleDragBaseH = sts.shapeHeight;
                ps.scaleDragBaseD = sts.shapeDepth;
                ps.getScalingGizmo()
                    .startDrag(mouseX, mouseY, cx, cy, cz, currentScale, ps.rotX, ps.rotY, ps.rotZ);
            } else if (ps.getPlaneTranslationGizmo().hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                ps.getPlaneTranslationGizmo()
                    .startDrag(
                        mouseX,
                        mouseY,
                        cx,
                        cy,
                        cz,
                        ps.anchorFX,
                        ps.anchorFY,
                        ps.anchorFZ,
                        ps.rotX,
                        ps.rotY,
                        ps.rotZ);
            }
        } else if (button == KeyConstants.RMB) {
            GuiDimensiumOverlay.confirmPlacement();
        }
    }
}
