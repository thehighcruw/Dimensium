/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler.brushes;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.tool.state.RulerToolState;

@SideOnly(Side.CLIENT)
public class RulerBrushInput implements BrushInput {

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        if (button != KeyConstants.RMB) return false;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        RulerToolState.INSTANCE.points.add(new int[] { mop.blockX, mop.blockY, mop.blockZ });
        return true;
    }
}
