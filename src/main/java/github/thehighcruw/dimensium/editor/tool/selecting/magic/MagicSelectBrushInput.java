/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.magic;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.editor.tool.selecting.box.BoxSelectToolState;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.SelectionState;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public class MagicSelectBrushInput implements BrushInput {

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        if (button != KeyConstants.RMB) return;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
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
        sel.applyOp(ToolMaskRegistry.INSTANCE.filterSelection(flooded), BoxSelectToolState.INSTANCE.booleanOp);
        sel.pendingPos1 = false;
    }
}
