/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import com.github.bsideup.jabel.Desugar;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * Maps full blocks to their stair and slab counterparts.
 * Used by stair/slab smoothing passes in path and shape tools.
 * Mods can call {@link #register} during FML post-init to extend the registry.
 */
public final class BlockFamilyRegistry {

    @Desugar
    public record BlockFamily(Block stairs, Block slab, int slabMeta) {

        /** Meta for a bottom-half slab of this material. */
        public int slabMetaBottom() {
            return slabMeta & 7;
        }

        /** Meta for a top-half slab of this material. */
        public int slabMetaTop() {
            return (slabMeta & 7) | 8;
        }
    }

    private static final Map<Integer, BlockFamily> REGISTRY = new HashMap<>();

    static {
        registerVanilla();
    }

    private static void registerVanilla() {
        // Wood planks
        register(Blocks.planks, 0, Blocks.oak_stairs, Blocks.wooden_slab, 0);
        register(Blocks.planks, 1, Blocks.spruce_stairs, Blocks.wooden_slab, 1);
        register(Blocks.planks, 2, Blocks.birch_stairs, Blocks.wooden_slab, 2);
        register(Blocks.planks, 3, Blocks.jungle_stairs, Blocks.wooden_slab, 3);
        register(Blocks.planks, 4, Blocks.acacia_stairs, Blocks.wooden_slab, 4);
        register(Blocks.planks, 5, Blocks.dark_oak_stairs, Blocks.wooden_slab, 5);
        // Stone types — stone_slab meta:
        // 0=stone,1=sandstone,3=cobblestone,4=brick,5=stone_brick,6=nether_brick,7=quartz
        register(Blocks.cobblestone, 0, Blocks.stone_stairs, Blocks.stone_slab, 3);
        register(Blocks.sandstone, 0, Blocks.sandstone_stairs, Blocks.stone_slab, 1);
        register(Blocks.brick_block, 0, Blocks.brick_stairs, Blocks.stone_slab, 4);
        register(Blocks.stonebrick, 0, Blocks.stone_brick_stairs, Blocks.stone_slab, 5);
        register(Blocks.nether_brick, 0, Blocks.nether_brick_stairs, Blocks.stone_slab, 6);
        register(Blocks.quartz_block, 0, Blocks.quartz_stairs, Blocks.stone_slab, 7);
    }

    /**
     * Registers a full-block → stair/slab family mapping.
     * Call during FML post-init to add modded block families.
     *
     * @param fullBlock the placeable full block
     * @param meta      block metadata variant
     * @param stairs    corresponding stair block
     * @param slab      corresponding slab block
     * @param slabMeta  material bits for the slab (bits 0-2 only; top/bottom bit is set automatically)
     */
    public static void register(Block fullBlock, int meta, Block stairs, Block slab, int slabMeta) {
        REGISTRY.put(packKey(Block.getIdFromBlock(fullBlock), meta), new BlockFamily(stairs, slab, slabMeta));
    }

    /** Returns the family for the given block, or null if none is registered. */
    public static BlockFamily lookup(int blockId, int meta) {
        return REGISTRY.get(packKey(blockId, meta));
    }

    /** Returns true if the given block has a registered stair/slab family. */
    public static boolean hasFamily(int blockId, int meta) {
        return REGISTRY.containsKey(packKey(blockId, meta));
    }

    private static int packKey(int blockId, int meta) {
        return (blockId << 4) | (meta & 0xF);
    }

    private BlockFamilyRegistry() {}
}
