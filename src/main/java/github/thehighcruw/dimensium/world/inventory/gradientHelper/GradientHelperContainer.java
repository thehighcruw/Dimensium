/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.inventory.gradientHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class GradientHelperContainer extends Container {

    static final int PANEL_W = 178;
    static final int PANEL_H = 133;
    static final int SLOT_SIZE = 18;
    static final int INPUT_SLOTS = 9;
    static final int OUTPUT_SLOTS = 9;

    // Slot rows — relative to guiTop (GuiContainer adds guiTop automatically)
    static final int CONTENT_X = 8;
    static final int INPUT_Y = 26;
    static final int OUTPUT_Y = 62;
    static final int HOTBAR_Y = 111;

    private final InventoryBasic inputInv;
    private final InventoryBasic outputInv;

    public GradientHelperContainer(InventoryPlayer playerInv) {
        inputInv = new InventoryBasic("gradient_input", true, INPUT_SLOTS);
        outputInv = new InventoryBasic("gradient_output", false, OUTPUT_SLOTS);

        for (int i = 0; i < INPUT_SLOTS; i++) {
            addSlotToContainer(new SlotGradientInput(inputInv, i, CONTENT_X + i * SLOT_SIZE, INPUT_Y));
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            addSlotToContainer(new SlotGradientOutput(outputInv, i, CONTENT_X + i * SLOT_SIZE, OUTPUT_Y));
        }

        int hotbarStartX = (PANEL_W - 9 * 18) / 2;
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(playerInv, i, hotbarStartX + i * 18, HOTBAR_Y));
        }
    }

    public InventoryBasic getInputInv() {
        return inputInv;
    }

    public InventoryBasic getOutputInv() {
        return outputInv;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        return null;
    }
}
