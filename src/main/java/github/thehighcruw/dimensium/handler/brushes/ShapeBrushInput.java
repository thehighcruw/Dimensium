/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler.brushes;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.render.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.render.world.RotationGizmo;
import github.thehighcruw.dimensium.render.world.ScaleGizmo;
import github.thehighcruw.dimensium.render.world.TranslationGizmo;
import github.thehighcruw.dimensium.tool.state.ShapePlacementState;
import github.thehighcruw.dimensium.tool.state.ShapeToolState;
import github.thehighcruw.dimensium.util.RenderUtils;

@SideOnly(Side.CLIENT)
public class ShapeBrushInput implements BrushInput {

    public static final ShapeBrushInput INSTANCE = new ShapeBrushInput();

    private ShapeBrushInput() {}

    @Override
    public boolean requiresBlockTarget() {
        return false;
    }

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition ignored) {
        if (mc.thePlayer == null) return false;
        FreecamState fs = FreecamState.INSTANCE;
        int sw = RenderUtils.scaledWidth(), sh = RenderUtils.scaledHeight();
        int mouseX = (int) fs.cursorX, mouseY = (int) fs.cursorY;
        ShapePlacementState ps = ShapePlacementState.INSTANCE;

        if (!ps.active) {
            if (button == KeyConstants.RMB) {
                MovingObjectPosition mop = GuiDimensiumOverlay
                    .raycastFromMouse((int) fs.cursorX, (int) fs.cursorY, sw, sh);
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    ps.start(mop.blockX, mop.blockY, mop.blockZ);
                }
            }
            return true;
        }

        if (button == KeyConstants.LMB) {
            double cx = ps.centerX(), cy = ps.centerY(), cz = ps.centerZ();
            EntityLivingBase eye = mc.renderViewEntity;
            if (ps.viewPlaneGizmo.hovered) {
                ps.viewPlaneGizmo
                    .startDrag(mouseX, mouseY, sw, sh, eye, cx, cy, cz, ps.anchorFX, ps.anchorFY, ps.anchorFZ);
            } else if (ps.gizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                ps.gizmo.startDrag(
                    mouseX,
                    mouseY,
                    sw,
                    sh,
                    eye,
                    cx,
                    cy,
                    cz,
                    ps.anchorFX,
                    ps.anchorFY,
                    ps.anchorFZ,
                    ps.rotX,
                    ps.rotY,
                    ps.rotZ);
            } else if (ps.rotGizmo.hoveredAxis != RotationGizmo.Axis.NONE) {
                ps.rotDragBaseX = ps.rotX;
                ps.rotDragBaseY = ps.rotY;
                ps.rotDragBaseZ = ps.rotZ;
                ps.rotGizmo.startDrag(mouseX, mouseY, sw, sh, eye, cx, cy, cz, ps.rotX, ps.rotY, ps.rotZ);
            } else if (ps.scaleGizmo.hoveredAxis != ScaleGizmo.Axis.NONE) {
                float currentScale = ps.scaleGizmo.hoveredAxis == ScaleGizmo.Axis.X ? ps.scaleX
                    : ps.scaleGizmo.hoveredAxis == ScaleGizmo.Axis.Y ? ps.scaleY : ps.scaleZ;
                ShapeToolState sts = ShapeToolState.INSTANCE;
                ps.scaleDragBaseW = sts.shapeWidth;
                ps.scaleDragBaseH = sts.shapeHeight;
                ps.scaleDragBaseD = sts.shapeDepth;
                ps.scaleGizmo
                    .startDrag(mouseX, mouseY, sw, sh, eye, cx, cy, cz, currentScale, ps.rotX, ps.rotY, ps.rotZ);
            } else if (ps.planeGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                ps.planeGizmo.startDrag(
                    mouseX,
                    mouseY,
                    sw,
                    sh,
                    eye,
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
        return true;
    }
}
