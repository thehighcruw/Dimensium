package github.thehighcruw.dimensium.render.gui;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

class SlotGradientInput extends Slot {

    SlotGradientInput(IInventory inventory, int slotIndex, int x, int y) {
        super(inventory, slotIndex, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return true;
    }
}
