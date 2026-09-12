/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler.brushes;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.tool.state.MagicSelectToolState;
import github.thehighcruw.dimensium.tool.state.SelectToolState;
import github.thehighcruw.dimensium.tool.state.SelectionState;

@SideOnly(Side.CLIENT)
public class MagicSelectBrushInput implements BrushInput {

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        if (button != KeyConstants.RMB) return false;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        SelectionState sel = SelectionState.INSTANCE;
        MagicSelectToolState ts = MagicSelectToolState.INSTANCE;
        Set<Long> flooded = SelectionState.floodFill(
            mc.theWorld,
            mop.blockX,
            mop.blockY,
            mop.blockZ,
            ts.magicSelectLimit,
            ts.magicSelectRange,
            ts.magicSelectSurface,
            ts.magicSelectCorners,
            ts.magicCompareType,
            ts.magicDirection);
        sel.applyOp(ToolMaskRegistry.INSTANCE.filterSelection(flooded), SelectToolState.INSTANCE.booleanOp);
        sel.pendingPos1 = false;
        return true;
    }
}
