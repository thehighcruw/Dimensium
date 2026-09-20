/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.shape;

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
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

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
                    ps.start(Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ));
                }
            }
            return;
        }

        if (button == KeyConstants.LMB) {
            Vec3DDouble psCenter = ps.center();
            Vec3DDouble anchor = ps.anchorF.toDouble();
            Vec3DFloat rot = ps.rot;
            EntityLivingBase eye = mc.renderViewEntity;
            if (ps.viewPlaneGizmo.hovered) {
                ps.viewPlaneGizmo.startDrag(mouseX, mouseY, eye, psCenter, anchor);
            } else if (ps.getPlaneTranslationGizmo().hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                ps.getPlaneTranslationGizmo().startDrag(mouseX, mouseY, psCenter, anchor, rot);
            } else if (ps.getAxisTranslationGizmo().hoveredAxis != TranslationGizmo.Axis.NONE) {
                ps.getAxisTranslationGizmo().startDrag(mouseX, mouseY, psCenter, anchor, rot);
            } else if (ps.getScalingGizmo().hoveredAxis != ScalingGizmo.Axis.NONE) {
                float currentScale = ps.getScalingGizmo().hoveredAxis == ScalingGizmo.Axis.X
                        ? ps.scale.x()
                        : ps.getScalingGizmo().hoveredAxis == ScalingGizmo.Axis.Y ? ps.scale.y() : ps.scale.z();
                ShapeToolState sts = ShapeToolState.INSTANCE;
                ps.scaleDragBase = Vec3DInt.from(sts.shapeWidth, sts.shapeHeight, sts.shapeDepth);
                ps.getScalingGizmo().startDrag(mouseX, mouseY, psCenter, currentScale, rot);
            } else if (ps.getRotationGizmo().hoveredAxis != RotationGizmo.Axis.NONE) {
                ps.rotDragBase = rot;
                ps.getRotationGizmo().startDrag(mouseX, mouseY, psCenter, rot);
            }
        } else if (button == KeyConstants.RMB) {
            GuiDimensiumOverlay.confirmPlacement();
        }
    }
}
