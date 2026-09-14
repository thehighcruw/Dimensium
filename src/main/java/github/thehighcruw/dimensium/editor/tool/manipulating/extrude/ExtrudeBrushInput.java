/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.extrude;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.shared.KeyConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public class ExtrudeBrushInput implements BrushInput {

    public static final ExtrudeBrushInput INSTANCE = new ExtrudeBrushInput();

    private ExtrudeBrushInput() {}

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        if (button != KeyConstants.RMB) return;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        ExtrudeHelper.applyExtrudeAt(mc.theWorld, mop.blockX, mop.blockY, mop.blockZ, mop.sideHit);
    }
}
