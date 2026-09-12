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
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.tool.state.GradientToolState;

@SideOnly(Side.CLIENT)
public class GradientBrushInput implements BrushInput {

    public static final GradientBrushInput INSTANCE = new GradientBrushInput();

    private GradientBrushInput() {}

    @Override
    public boolean usesDragLoop() {
        return true;
    }

    @Override
    public boolean requiresBlockTarget() {
        return false;
    }

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition ignored) {
        if (button != KeyConstants.LMB) return false;
        FreecamState fs = FreecamState.INSTANCE;
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        MovingObjectPosition mop = GuiDimensiumOverlay
            .raycastFromMouse((int) fs.cursorX, (int) fs.cursorY, sr.getScaledWidth(), sr.getScaledHeight());
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        GradientToolState gs = GradientToolState.INSTANCE;
        gs.gradientHasPos1 = true;
        gs.gradientPos1X = mop.blockX;
        gs.gradientPos1Y = mop.blockY;
        gs.gradientPos1Z = mop.blockZ;
        return true;
    }

    @Override
    public void onBrushDragStart(Minecraft mc, MovingObjectPosition mop) {
        GradientToolState gs = GradientToolState.INSTANCE;
        gs.gradientPos2X = mop.blockX;
        gs.gradientPos2Y = mop.blockY;
        gs.gradientPos2Z = mop.blockZ;
        gs.gradientHasPos2 = true;
    }

    @Override
    public void onBrushRelease(Minecraft mc) {
        GradientToolState.INSTANCE.gradientHasPos2 = false;
    }
}
