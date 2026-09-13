/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class BlockUtils {

    /** Returns {blockId, meta} or null if stack is null or not a placeable block. */
    public static int[] blockToIdMeta(ItemStack stack) {
        if (stack == null) return null;
        Block blk = Block.getBlockFromItem(stack.getItem());
        if (blk == null || blk == net.minecraft.init.Blocks.air) return null;
        return new int[] { Block.getIdFromBlock(blk), stack.getItemDamage() };
    }
}
