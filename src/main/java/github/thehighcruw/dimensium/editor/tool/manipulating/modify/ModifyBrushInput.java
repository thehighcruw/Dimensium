/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.modify;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.handler.AnchorSnap;
import github.thehighcruw.dimensium.editor.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public class ModifyBrushInput implements BrushInput {

    public static final ModifyBrushInput INSTANCE = new ModifyBrushInput();

    private ModifyBrushInput() {}

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        ModifyToolState mods = ModifyToolState.INSTANCE;

        if (button == KeyConstants.RMB) {
            if (mods.mode == ModifyToolState.ModifyMode.REVOLVE) {
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    mods.revolveCenter = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ)
                            .plus(ExtrudeHelper.sideToOutwardDir(mop.sideHit));
                    mods.getAxisTranslationGizmo().reset();
                    mods.getPlaneTranslationGizmo().reset();
                    mods.invalidatePreview();
                }
            } else {
                GuiDimensiumOverlay.confirmModify();
            }
            return;
        }

        if (button == KeyConstants.LMB
                && mods.mode == ModifyToolState.ModifyMode.REVOLVE
                && mods.revolveCenter != null) {
            FreecamState freecamState = FreecamState.INSTANCE;
            int mouseX = (int) freecamState.cursorX;
            int mouseY = (int) freecamState.cursorY;
            Vec3DDouble gizmoPos = mods.revolveCenter.toDouble().plus(0.5);
            Vec3DFloat rot = Vec3DFloat.ZERO;
            if (mods.getAxisTranslationGizmo().hoveredAxis != TranslationGizmo.Axis.NONE) {
                mods.getAxisTranslationGizmo().startDrag(mouseX, mouseY, gizmoPos, gizmoPos, rot);
            } else if (mods.getPlaneTranslationGizmo().hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                mods.getPlaneTranslationGizmo().startDrag(mouseX, mouseY, gizmoPos, gizmoPos, rot);
            }
        }
    }

    @Override
    public void onGizmoDrag(int mx, int my, boolean snap) {
        ModifyToolState mods = ModifyToolState.INSTANCE;
        if (mods.mode != ModifyToolState.ModifyMode.REVOLVE || mods.revolveCenter == null) return;

        if (mods.getAxisTranslationGizmo().isDragging()
                || mods.getPlaneTranslationGizmo().isDragging()) {
            Vec3DDouble anchor = mods.getAxisTranslationGizmo().isDragging()
                    ? mods.getAxisTranslationGizmo().updateDrag(mx, my)
                    : mods.getPlaneTranslationGizmo().updateDrag(mx, my);
            if (anchor != null) {
                mods.revolveCenter = Vec3DInt.from(
                        AnchorSnap.toInt(anchor.x(), snap),
                        AnchorSnap.toInt(anchor.y(), snap),
                        AnchorSnap.toInt(anchor.z(), snap));
                mods.invalidatePreview();
            }
        }
    }
}
