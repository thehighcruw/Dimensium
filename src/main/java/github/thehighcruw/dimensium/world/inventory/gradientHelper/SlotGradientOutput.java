/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.inventory.gradientHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

class SlotGradientOutput extends Slot {

    SlotGradientOutput(IInventory inventory, int slotIndex, int x, int y) {
        super(inventory, slotIndex, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public void putStack(ItemStack stack) {
        // Block all external writes — gradient results are written directly to outputInv.
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        // Server sees an empty output — client handles pickup in mouseClicked.
        return null;
    }

    @Override
    public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {}
}
