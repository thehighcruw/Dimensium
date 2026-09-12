/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler.brushes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;

@SideOnly(Side.CLIENT)
public class ExtrudeBrushInput implements BrushInput {

    public static final ExtrudeBrushInput INSTANCE = new ExtrudeBrushInput();

    private ExtrudeBrushInput() {}

    @Override
    public boolean requiresBlockTarget() {
        return false;
    }

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition ignored) {
        if (button != KeyConstants.RMB) return false;
        FreecamState fs = FreecamState.INSTANCE;
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        MovingObjectPosition mop = GuiDimensiumOverlay
            .raycastFromMouse((int) fs.cursorX, (int) fs.cursorY, sr.getScaledWidth(), sr.getScaledHeight());
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        ExtrudeHelper.applyExtrudeAt(mc.theWorld, mop.blockX, mop.blockY, mop.blockZ, mop.sideHit);
        return true;
    }
}
