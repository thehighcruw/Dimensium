/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.move;

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
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.KeyConstants;

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
            double gx = ms.gizmoX(), gy = ms.gizmoY(), gz = ms.gizmoZ();
            if (ms.getAxisTranslationGizmo().hoveredAxis != TranslationGizmo.Axis.NONE) {
                ms.getAxisTranslationGizmo()
                    .startDrag(mouseX, mouseY, gx, gy, gz, gx, gy, gz, ms.rotX, ms.rotY, ms.rotZ);
            } else if (ms.getPlaneTranslationGizmo().hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                ms.getPlaneTranslationGizmo()
                    .startDrag(mouseX, mouseY, gx, gy, gz, gx, gy, gz, ms.rotX, ms.rotY, ms.rotZ);
            } else if (ms.getRotationGizmo().hoveredAxis != RotationGizmo.Axis.NONE) {
                ms.rotDragBaseX = ms.rotX;
                ms.rotDragBaseY = ms.rotY;
                ms.rotDragBaseZ = ms.rotZ;
                ms.getRotationGizmo()
                    .startDrag(mouseX, mouseY, gx, gy, gz, ms.rotX, ms.rotY, ms.rotZ);
            }
        } else if (button == KeyConstants.RMB) {
            GuiDimensiumOverlay.confirmMove();
        }
    }
}
