/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler.brushes;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.render.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.render.world.TranslationGizmo;
import github.thehighcruw.dimensium.tool.state.ModellingToolState;
import github.thehighcruw.dimensium.util.RenderUtils;

@SideOnly(Side.CLIENT)
public class ModellingBrushInput implements BrushInput {

    public static final ModellingBrushInput INSTANCE = new ModellingBrushInput();

    private ModellingBrushInput() {}

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        FreecamState fs = FreecamState.INSTANCE;
        int sw = RenderUtils.scaledWidth(), sh = RenderUtils.scaledHeight();
        int mouseX = (int) fs.cursorX, mouseY = (int) fs.cursorY;
        ModellingToolState mts = ModellingToolState.INSTANCE;

        if (button == KeyConstants.RMB) {
            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
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
            return true;
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
                .findNearestPointOnScreen(positions, skipFlat, mouseX, mouseY, sw, sh, mts.gizmo.getProjection(), 18);
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
                    mts.gizmo.startDrag(mouseX, mouseY, sw, sh, eye, mgx, mgy, mgz, mgx, mgy, mgz, 0, 0, 0);
                } else
                if (mts.planeGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE && mts.selectedPointObj() != null
                    && eye != null) {
                        ModellingToolState.ModelPoint mSelPt = mts.selectedPointObj();
                        double mgx = mSelPt.x + 0.5, mgy = mSelPt.y + 0.5, mgz = mSelPt.z + 0.5;
                        mts.planeGizmo.startDrag(mouseX, mouseY, sw, sh, eye, mgx, mgy, mgz, mgx, mgy, mgz, 0, 0, 0);
                    }
            return true;
        }

        return false;
    }
}
