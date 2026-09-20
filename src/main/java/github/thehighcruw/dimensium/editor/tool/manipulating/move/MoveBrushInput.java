/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.move;

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
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public class MoveBrushInput implements BrushInput {

    public static final MoveBrushInput INSTANCE = new MoveBrushInput();

    private MoveBrushInput() {}

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition ignored) {
        MoveToolState ms = MoveToolState.INSTANCE;
        if (!ms.active) return;
        EntityLivingBase eye = mc.renderViewEntity;
        if (eye == null) return;
        FreecamState fs = FreecamState.INSTANCE;
        int mouseX = (int) fs.cursorX, mouseY = (int) fs.cursorY;

        if (button == KeyConstants.LMB) {
            Vec3DDouble gizmoPos = ms.gizmoPos();
            Vec3DFloat rot = ms.rot;
            if (ms.getAxisTranslationGizmo().hoveredAxis != TranslationGizmo.Axis.NONE) {
                ms.getAxisTranslationGizmo().startDrag(mouseX, mouseY, gizmoPos, gizmoPos, rot);
            } else if (ms.getPlaneTranslationGizmo().hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                ms.getPlaneTranslationGizmo().startDrag(mouseX, mouseY, gizmoPos, gizmoPos, rot);
            } else if (ms.getRotationGizmo().hoveredAxis != RotationGizmo.Axis.NONE) {
                ms.rotDragBase = rot;
                ms.getRotationGizmo().startDrag(mouseX, mouseY, gizmoPos, rot);
            } else if (ms.getScalingGizmo().hoveredAxis != ScalingGizmo.Axis.NONE) {
                ScalingGizmo.Axis axis = ms.getScalingGizmo().hoveredAxis;
                float currentScale = axis == ScalingGizmo.Axis.X
                        ? ms.scale.x()
                        : axis == ScalingGizmo.Axis.Y ? ms.scale.y() : ms.scale.z();
                ms.getScalingGizmo().startDrag(mouseX, mouseY, gizmoPos, currentScale, rot);
            }
        } else if (button == KeyConstants.RMB) {
            GuiDimensiumOverlay.confirmMove();
        }
    }
}
