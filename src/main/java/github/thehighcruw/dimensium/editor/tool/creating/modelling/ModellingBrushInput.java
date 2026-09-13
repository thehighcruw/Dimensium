/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.modelling;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.editor.handler.AnchorSnap;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.KeyConstants;

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
            int px = mop.blockX, py = mop.blockY, pz = mop.blockZ;
            if (mts.offsetTargetPoint) {
                int[] off = ExtrudeHelper.sideToOutwardDir(mop.sideHit);
                px += off[0];
                py += off[1];
                pz += off[2];
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && mts.mode.usesRows()) {
                mts.addRow();
            }
            mts.addPoint(px, py, pz);
            mts.selectedRow = mts.currentRowIndex;
            mts.selectedPoint = mts.rows.get(mts.currentRowIndex)
                .size() - 1;
            mts.gizmo.reset();
            mts.invalidate();
            return;
        }

        if (button == KeyConstants.LMB) {
            EntityLivingBase eye = mc.renderViewEntity;
            List<int[]> positions = new ArrayList<>();
            int skipFlat = -1;
            for (int r = 0; r < mts.rows.size(); r++) {
                List<ModellingToolState.ModelPoint> row = mts.rows.get(r);
                for (int c = 0; c < row.size(); c++) {
                    if (r == mts.selectedRow && c == mts.selectedPoint) skipFlat = positions.size();
                    ModellingToolState.ModelPoint p = row.get(c);
                    positions.add(new int[] { p.x, p.y, p.z });
                }
            }
            int bestFlat = GuiDimensiumOverlay
                .findNearestPointOnScreen(positions, skipFlat, mouseX, mouseY, mts.gizmo.getProjection(), 18);
            if (bestFlat >= 0) {
                int flat = 0;
                done: for (int r = 0; r < mts.rows.size(); r++) {
                    for (int c = 0; c < mts.rows.get(r)
                        .size(); c++, flat++) {
                        if (flat == bestFlat) {
                            mts.selectedRow = r;
                            mts.selectedPoint = c;
                            mts.currentRowIndex = r;
                            mts.gizmo.reset();
                            break done;
                        }
                    }
                }
            } else if (mts.gizmo.hoveredAxis != TranslationGizmo.Axis.NONE && mts.selectedPointObj() != null
                && eye != null) {
                    ModellingToolState.ModelPoint mSelPt = mts.selectedPointObj();
                    double mgx = mSelPt.x + 0.5, mgy = mSelPt.y + 0.5, mgz = mSelPt.z + 0.5;
                    mts.gizmo.startDrag(mouseX, mouseY, mgx, mgy, mgz, mgx, mgy, mgz, 0, 0, 0);
                } else
                if (mts.planeGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE && mts.selectedPointObj() != null
                    && eye != null) {
                        ModellingToolState.ModelPoint mSelPt = mts.selectedPointObj();
                        double mgx = mSelPt.x + 0.5, mgy = mSelPt.y + 0.5, mgz = mSelPt.z + 0.5;
                        mts.planeGizmo.startDrag(mouseX, mouseY, mgx, mgy, mgz, mgx, mgy, mgz, 0, 0, 0);
                    }
        }

    }

    @Override
    public void onGizmoDrag(int mx, int my, boolean snap) {
        ModellingToolState mts = ModellingToolState.INSTANCE;
        ModellingToolState.ModelPoint mSelPt = mts.selectedPointObj();
        if (mSelPt == null) return;
        if (mts.gizmo.isDragging() || mts.planeGizmo.isDragging()) {
            double[] anchor = mts.gizmo.isDragging() ? mts.gizmo.updateDrag(mx, my)
                : mts.planeGizmo.updateDrag(mx, my);
            if (anchor != null) {
                mSelPt.x = AnchorSnap.toInt(anchor[0], snap);
                mSelPt.y = AnchorSnap.toInt(anchor[1], snap);
                mSelPt.z = AnchorSnap.toInt(anchor[2], snap);
                mts.invalidate();
            }
        }
    }
}
