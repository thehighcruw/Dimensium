/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler.brushes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.tool.state.BooleanOp;
import github.thehighcruw.dimensium.tool.state.LassoSelectToolState;
import github.thehighcruw.dimensium.tool.state.SelectionState;

@SideOnly(Side.CLIENT)
public class LassoBrushInput implements BrushInput {

    public static final LassoBrushInput INSTANCE = new LassoBrushInput();

    private LassoBrushInput() {}

    @Override
    public boolean onDragTick(Minecraft mc, int sw, int sh) {
        FreecamState fs = FreecamState.INSTANCE;
        LassoSelectToolState lasso = LassoSelectToolState.INSTANCE;
        boolean altDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        boolean rmbHeld = org.lwjgl.input.Mouse.isButtonDown(KeyConstants.RMB) && !fs.rmbDragging && !altDown;
        if (rmbHeld) {
            lasso.dragging = true;
            float cx = (float) fs.cursorX, cy = (float) fs.cursorY;
            if (lasso.polygonPoints.isEmpty()) {
                lasso.polygonPoints.add(new float[] { cx, cy });
            } else {
                float[] last = lasso.polygonPoints.get(lasso.polygonPoints.size() - 1);
                float dx = cx - last[0], dy = cy - last[1];
                if (dx * dx + dy * dy >= 4.0f) {
                    lasso.polygonPoints.add(new float[] { cx, cy });
                }
            }
        } else if (lasso.dragging) {
            lasso.dragging = false;
            if (lasso.polygonPoints.size() >= 3) {
                ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
                java.util.Set<Long> blocks = LassoComputer.compute(
                    mc,
                    lasso.polygonPoints,
                    lasso.lassoDepth,
                    lasso.lassoIncludeNonSolid,
                    sr.getScaledWidth(),
                    sr.getScaledHeight());
                SelectionState.INSTANCE.applyOp(ToolMaskRegistry.INSTANCE.filterSelection(blocks), BooleanOp.REPLACE);
            }
            lasso.polygonPoints.clear();
        }
        return true;
    }
}
