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
import github.thehighcruw.dimensium.shared.util.RenderUtils;

@SideOnly(Side.CLIENT)
public class MoveBrushInput implements BrushInput {

    public static final MoveBrushInput INSTANCE = new MoveBrushInput();

    private MoveBrushInput() {}

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition ignored) {
        MoveToolState ms = MoveToolState.INSTANCE;
        if (!ms.active) return false;
        EntityLivingBase eye = mc.renderViewEntity;
        if (eye == null) return false;
        FreecamState fs = FreecamState.INSTANCE;
        int sw = RenderUtils.scaledWidth(), sh = RenderUtils.scaledHeight();
        int mouseX = (int) fs.cursorX, mouseY = (int) fs.cursorY;

        if (button == KeyConstants.LMB) {
            double gx = ms.gizmoX(), gy = ms.gizmoY(), gz = ms.gizmoZ();
            if (ms.gizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                ms.gizmo.startDrag(mouseX, mouseY, sw, sh, eye, gx, gy, gz, gx, gy, gz, ms.rotX, ms.rotY, ms.rotZ);
            } else if (ms.planeGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                ms.planeGizmo.startDrag(mouseX, mouseY, sw, sh, eye, gx, gy, gz, gx, gy, gz, ms.rotX, ms.rotY, ms.rotZ);
            } else if (ms.rotGizmo.hoveredAxis != RotationGizmo.Axis.NONE) {
                ms.rotDragBaseX = ms.rotX;
                ms.rotDragBaseY = ms.rotY;
                ms.rotDragBaseZ = ms.rotZ;
                ms.rotGizmo.startDrag(mouseX, mouseY, sw, sh, eye, gx, gy, gz, ms.rotX, ms.rotY, ms.rotZ);
            }
        } else if (button == KeyConstants.RMB) {
            GuiDimensiumOverlay.confirmMove();
        }
        return true;
    }
}
