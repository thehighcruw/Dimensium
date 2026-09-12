package github.thehighcruw.dimensium.render.gui;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ColourPickerContainer extends Container {

    static final int PANEL_W = 246;
    static final int SLOT_Y = 136;
    static final int GRID_COLS = 2;
    static final int GRID_ROWS = 6;
    static final int GRID_PALETTE = GRID_COLS * GRID_ROWS;
    static final int GRID_REL_X = 8;
    static final int CONTENT_REL_Y = 20;
    static final int CELL = 16;

    private final InventoryBasic paletteInv;

    public ColourPickerContainer(InventoryPlayer playerInv) {
        paletteInv = new InventoryBasic("palette", false, GRID_PALETTE);

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                addSlotToContainer(
                    new SlotPaletteResult(
                        paletteInv,
                        col + row * GRID_COLS,
                        GRID_REL_X + col * CELL,
                        CONTENT_REL_Y + row * CELL));
            }
        }

        int startX = (PANEL_W - 9 * 18) / 2;
        for (int i = 0; i < 9; i++) {
            // +1 offset so item renders centred inside the 18×18 frame (engine renders at slot.x, not slot.x+1)
            addSlotToContainer(new Slot(playerInv, i, startX + i * 18 + 1, SLOT_Y + 1));
        }
    }

    public void updatePalette(List<ItemStack> results, int scrollOffset) {
        for (int i = 0; i < GRID_PALETTE; i++) {
            int idx = scrollOffset * GRID_COLS + i;
            paletteInv.setInventorySlotContents(i, idx < results.size() ? results.get(idx) : null);
        }
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
