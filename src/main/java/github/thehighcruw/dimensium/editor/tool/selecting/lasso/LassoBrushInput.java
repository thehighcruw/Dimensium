/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.lasso;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec2DFloat;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import java.util.Set;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class LassoBrushInput implements BrushInput {

    public static final LassoBrushInput INSTANCE = new LassoBrushInput();

    private LassoBrushInput() {}

    @Override
    public boolean onDragTick(Minecraft mc, int sw, int sh) {
        FreecamState fs = FreecamState.INSTANCE;
        LassoSelectToolState lasso = LassoSelectToolState.INSTANCE;
        if (Mouse.isButtonDown(KeyConstants.RMB) && !fs.isMoving()) {
            lasso.dragging = true;
            float cx = fs.cursorX, cy = fs.cursorY;
            if (lasso.polygonPoints.isEmpty()) {
                lasso.polygonPoints.add(new float[] {cx, cy});
            } else {
                float[] last = lasso.polygonPoints.get(lasso.polygonPoints.size() - 1);
                if (Vec2DFloat.from(cx - last[0], cy - last[1]).lengthSq() >= 4.0f) {
                    lasso.polygonPoints.add(new float[] {cx, cy});
                }
            }
        } else if (lasso.dragging) {
            lasso.dragging = false;
            if (lasso.polygonPoints.size() >= 3) {
                Set<Long> blocks = LassoComputer.compute(
                        mc,
                        lasso.polygonPoints,
                        lasso.lassoDepth,
                        lasso.lassoIncludeNonSolid,
                        RenderUtils.scaledWidth(),
                        RenderUtils.scaledHeight());
                SelectionState.INSTANCE.applyOp(ToolMaskRegistry.INSTANCE.filterSelection(blocks), BooleanOp.REPLACE);
            }
            lasso.polygonPoints.clear();
        }
        return true;
    }
}
