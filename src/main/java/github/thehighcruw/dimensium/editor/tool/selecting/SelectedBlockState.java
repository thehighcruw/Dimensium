/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class SelectedBlockState {

    public static final SelectedBlockState INSTANCE = new SelectedBlockState();

    public ItemStack selectedBlock = null;
    public boolean selectedIsAir = false;

    public Block getPaintBlock() {
        if (selectedIsAir) return Blocks.air;
        if (selectedBlock == null) return Blocks.stone;
        Block b = Block.getBlockFromItem(selectedBlock.getItem());
        return (b == null || b == Blocks.air) ? Blocks.stone : b;
    }

    public int getPaintMeta() {
        return selectedBlock == null ? 0 : selectedBlock.getItemDamage();
    }
}
