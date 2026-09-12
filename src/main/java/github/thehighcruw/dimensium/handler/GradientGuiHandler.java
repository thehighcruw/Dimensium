/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.IGuiHandler;
import github.thehighcruw.dimensium.render.gui.GradientHelperContainer;
import github.thehighcruw.dimensium.render.gui.GuiGradientHelper;

public class GradientGuiHandler implements IGuiHandler {

    public static final int GUI_ID = 1;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == GUI_ID) return new GradientHelperContainer(player.inventory);
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == GUI_ID) return new GuiGradientHelper(player);
        return null;
    }
}
