/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.painting.gradient;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public class GradientBrushInput implements BrushInput {

    public static final GradientBrushInput INSTANCE = new GradientBrushInput();

    private GradientBrushInput() {}

    @Override
    public boolean usesDragLoop() {
        return true;
    }

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        if (button != KeyConstants.LMB) return;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        GradientToolState gs = GradientToolState.INSTANCE;
        gs.gradientHasPos1 = true;
        gs.gradientPos1 = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
    }

    @Override
    public void onBrushDragStart(Minecraft mc, MovingObjectPosition mop) {
        GradientToolState gs = GradientToolState.INSTANCE;
        gs.gradientPos2 = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        gs.gradientHasPos2 = true;
    }

    @Override
    public void onBrushRelease(Minecraft mc) {
        GradientToolState.INSTANCE.gradientHasPos2 = false;
    }
}
