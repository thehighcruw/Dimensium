/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.modelling;

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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class ModellingBrushInput implements BrushInput {

    public static final ModellingBrushInput INSTANCE = new ModellingBrushInput();

    private ModellingBrushInput() {}

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        FreecamState fs = FreecamState.INSTANCE;
        int mouseX = (int) fs.cursorX, mouseY = (int) fs.cursorY;
        ModellingToolState mts = ModellingToolState.INSTANCE;

        if (button == KeyConstants.RMB) {
            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
            Vec3DInt p = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
            if (mts.offsetTargetPoint) {
                p = p.plus(ExtrudeHelper.sideToOutwardDir(mop.sideHit));
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && mts.mode.usesRows()) {
                mts.addRow();
            }
            mts.addPoint(p);
            mts.selectedRow = mts.currentRowIndex;
            mts.selectedPoint = mts.rows.get(mts.currentRowIndex).size() - 1;
            mts.getAxisTranslationGizmo().reset();
            mts.invalidate();
            return;
        }

        if (button == KeyConstants.LMB) {
            EntityLivingBase eye = mc.renderViewEntity;
            List<Vec3DInt> positions = new ArrayList<>();
            int skipFlat = -1;
            for (int r = 0; r < mts.rows.size(); r++) {
                List<ModellingToolState.ModelPoint> row = mts.rows.get(r);
                for (int c = 0; c < row.size(); c++) {
                    if (r == mts.selectedRow && c == mts.selectedPoint) skipFlat = positions.size();
                    positions.add(mts.rows.get(r).get(c).pos());
                }
            }
            int bestFlat = GuiDimensiumOverlay.findNearestPointOnScreen(
                    positions,
                    skipFlat,
                    mouseX,
                    mouseY,
                    mts.getAxisTranslationGizmo().getProjection(),
                    18);
            if (bestFlat >= 0) {
                int flat = 0;
                done:
                for (int r = 0; r < mts.rows.size(); r++) {
                    for (int c = 0; c < mts.rows.get(r).size(); c++, flat++) {
                        if (flat == bestFlat) {
                            mts.selectedRow = r;
                            mts.selectedPoint = c;
                            mts.currentRowIndex = r;
                            mts.getAxisTranslationGizmo().reset();
                            break done;
                        }
                    }
                }
            } else if (mts.getAxisTranslationGizmo().hoveredAxis != TranslationGizmo.Axis.NONE
                    && mts.selectedPointObj() != null
                    && eye != null) {
                Vec3DDouble gp = mts.selectedPointObj().pos().toDouble().plus(0.5);
                mts.getAxisTranslationGizmo().startDrag(mouseX, mouseY, gp, gp, Vec3DFloat.ZERO);
            } else if (mts.getPlaneTranslationGizmo().hoveredPlane != PlaneTranslationGizmo.Plane.NONE
                    && mts.selectedPointObj() != null
                    && eye != null) {
                Vec3DDouble gp = mts.selectedPointObj().pos().toDouble().plus(0.5);
                mts.getPlaneTranslationGizmo().startDrag(mouseX, mouseY, gp, gp, Vec3DFloat.ZERO);
            }
        }
    }

    @Override
    public void onGizmoDrag(int mx, int my, boolean snap) {
        ModellingToolState mts = ModellingToolState.INSTANCE;
        ModellingToolState.ModelPoint mSelPt = mts.selectedPointObj();
        if (mSelPt == null) return;
        if (mts.getAxisTranslationGizmo().isDragging()
                || mts.getPlaneTranslationGizmo().isDragging()) {
            Vec3DDouble anchor = mts.getAxisTranslationGizmo().isDragging()
                    ? mts.getAxisTranslationGizmo().updateDrag(mx, my)
                    : mts.getPlaneTranslationGizmo().updateDrag(mx, my);
            if (anchor != null) {
                mts.rows
                        .get(mts.selectedRow)
                        .set(
                                mts.selectedPoint,
                                new ModellingToolState.ModelPoint(Vec3DInt.from(
                                        AnchorSnap.toInt(anchor.x(), snap),
                                        AnchorSnap.toInt(anchor.y(), snap),
                                        AnchorSnap.toInt(anchor.z(), snap))));
                mts.invalidate();
            }
        }
    }
}
