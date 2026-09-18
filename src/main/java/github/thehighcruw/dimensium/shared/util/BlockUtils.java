/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import codechicken.nei.api.ItemInfo;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public final class BlockUtils {

    // MC 1.7.10 sideHit: 0=bottom, 1=top, 2=north, 3=south, 4=west, 5=east
    public static final Vec3DInt[] NEIGHBOUR_OFFSETS = {
        Vec3DInt.from(0, -1, 0),
        Vec3DInt.from(0, 1, 0),
        Vec3DInt.from(0, 0, -1),
        Vec3DInt.from(0, 0, 1),
        Vec3DInt.from(-1, 0, 0),
        Vec3DInt.from(1, 0, 0),
    };
    public static final Vec3DInt[] ADJACENT_OFFSETS = {
        Vec3DInt.from(1, 0, 0), Vec3DInt.from(-1, 0, 0), Vec3DInt.from(0, 0, 1), Vec3DInt.from(0, 0, -1),
    };
    public static final Vec3DInt[] NEIGHBOURS_26_OFFSETS = precomputeNeighbours26();

    private static Vec3DInt[] precomputeNeighbours26() {
        Vec3DInt[] offsets = new Vec3DInt[26];
        int[] i = {0};
        Vec3DInt.forEachInclusive(Vec3DInt.from(-1), Vec3DInt.from(1), offset -> {
            if (!offset.equals(Vec3DInt.ZERO)) offsets[i[0]++] = offset;
        });
        return offsets;
    }

    private BlockUtils() {}

    /** Returns the registry name of the block from the given stack, or null if not a placeable block. */
    public static String getBlockRegistryName(ItemStack stack) {
        if (stack == null) return null;
        Block blk = Block.getBlockFromItem(stack.getItem());
        if (blk == null) return null;
        return (String) Block.blockRegistry.getNameForObject(blk);
    }

    /** Returns {blockId, meta} or null if stack is null or not a placeable block. */
    public static int[] blockToIdMeta(ItemStack stack) {
        if (stack == null) return null;
        Block blk = Block.getBlockFromItem(stack.getItem());
        if (blk == null || blk == Blocks.air) return null;
        return new int[] {Block.getIdFromBlock(blk), stack.getItemDamage()};
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
            permutations.removeIf(s -> s == null
                    || s.getItem() == null
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
            IMetaTileEntity[] mtes = GregTechAPI.METATILEENTITIES;
            Block blockMachines = GregTechAPI.sBlockMachines;
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
        } catch (Throwable ignored) {
        }
    }
}
