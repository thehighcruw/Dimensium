/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import codechicken.nei.api.ItemInfo;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

public final class BlockUtils {

    private BlockUtils() {}

    /** Returns {blockId, meta} or null if stack is null or not a placeable block. */
    public static int[] blockToIdMeta(ItemStack stack) {
        if (stack == null) return null;
        Block blk = Block.getBlockFromItem(stack.getItem());
        if (blk == null || blk == Blocks.air) return null;
        return new int[] { Block.getIdFromBlock(blk), stack.getItemDamage() };
    }

    @SuppressWarnings("unchecked")
    public static List<ItemStack> collectPlaceableBlocks() {
        List<ItemStack> result = new ArrayList<>();
        for (Item item : (Iterable<Item>) Item.itemRegistry) {
            if (!(item instanceof ItemBlock)) continue;

            List<ItemStack> permutations = new ArrayList<>(ItemInfo.itemOverrides.get(item));
            if (permutations.isEmpty()) {
                item.getSubItems(item, null, permutations);
            }
            permutations.addAll(ItemInfo.itemVariants.get(item));
            permutations.removeIf(
                s -> s == null || s.getItem() == null
                    || s.getItemDamage() == OreDictionary.WILDCARD_VALUE
                    || Block.getBlockFromItem(s.getItem()) == null
                    || Block.getBlockFromItem(s.getItem()) == Blocks.air);
            result.addAll(permutations);
        }
        addGT5Machines(result);
        return result;
    }

    private static void addGT5Machines(List<ItemStack> out) {
        try {
            IMetaTileEntity[] mtes = gregtech.api.GregTechAPI.METATILEENTITIES;
            Block blockMachines = gregtech.api.GregTechAPI.sBlockMachines;
            if (blockMachines == null) return;
            Item blockItem = Item.getItemFromBlock(blockMachines);
            if (blockItem == null) return;
            BitSet covered = new BitSet(Short.MAX_VALUE);
            for (ItemStack s : out) {
                if (s != null && s.getItem() == blockItem) covered.set(s.getItemDamage());
            }
            for (int i = 0; i < mtes.length; i++) {
                if (mtes[i] == null || covered.get(i)) continue;
                out.add(new ItemStack(blockItem, 1, i));
            }
        } catch (Throwable ignored) {}
    }
}
