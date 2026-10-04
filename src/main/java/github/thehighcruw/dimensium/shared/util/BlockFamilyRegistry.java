/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizon.gtnhlib.reflect.Fields.LookupType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

/**
 * Maps full blocks to their stair, slab, and fence counterparts, and back.
 *
 * <p>
 * Stair mappings are built automatically at FML post-init by {@link #scanAllStairs()}, which
 * reads the base-block reference stored in every {@link BlockStairs} subclass. Slab and fence
 * mappings must be registered explicitly via {@link #registerSlab} / {@link #registerFence}.
 *
 * <p>
 * Mods can call any of the register methods during FML post-init to extend the registry.
 */
public final class BlockFamilyRegistry {

    @Desugar
    public record BlockFamily(
            @Nullable Block stairs,
            @Nullable Block slab,
            int slabMeta,
            @Nullable Block fence) {

        /** Meta for a bottom-half slab of this material. */
        public int slabMetaBottom() {
            return slabMeta & 7;
        }

        /** Meta for a top-half slab of this material. */
        public int slabMetaTop() {
            return (slabMeta & 7) | 8;
        }
    }

    public enum BlockShapeType {
        FULL,
        STAIR,
        SLAB,
        FENCE
    }

    // GTNHLib field accessors for BlockStairs' private base-block references.
    private static final Fields.ClassFields<BlockStairs> STAIR_CLASS_FIELDS = Fields.ofClass(BlockStairs.class);
    private static final Fields.ClassFields<BlockStairs>.Field<Block> STAIR_MODEL_BLOCK =
            STAIR_CLASS_FIELDS.getField(LookupType.DECLARED, "field_150149_b", Block.class);
    private static final Fields.ClassFields<BlockStairs>.Field<Integer> STAIR_MODEL_META =
            STAIR_CLASS_FIELDS.getIntField(LookupType.DECLARED, "field_150151_M");

    // Forward: packKey(fullBlockId, fullMeta) → BlockFamily
    private static final Map<Integer, BlockFamily> REGISTRY = new HashMap<>();
    // Reverse for slabs: packKey(slabBlockId, slabMeta & 7) → {fullBlockId, fullMeta}
    private static final Map<Integer, int[]> SLAB_REVERSE = new HashMap<>();
    // Reverse for fences: packKey(fenceBlockId, 0) → {fullBlockId, fullMeta}
    private static final Map<Integer, int[]> FENCE_REVERSE = new HashMap<>();

    static {
        registerVanilla();
    }

    private static void registerVanilla() {
        // Wood planks — stairs are also auto-detected by scanAllStairs(), but explicit
        // registration here ensures they are present before post-init.
        register(Blocks.planks, 0, Blocks.oak_stairs, Blocks.wooden_slab, 0);
        register(Blocks.planks, 1, Blocks.spruce_stairs, Blocks.wooden_slab, 1);
        register(Blocks.planks, 2, Blocks.birch_stairs, Blocks.wooden_slab, 2);
        register(Blocks.planks, 3, Blocks.jungle_stairs, Blocks.wooden_slab, 3);
        register(Blocks.planks, 4, Blocks.acacia_stairs, Blocks.wooden_slab, 4);
        register(Blocks.planks, 5, Blocks.dark_oak_stairs, Blocks.wooden_slab, 5);
        // Stone types
        register(Blocks.cobblestone, 0, Blocks.stone_stairs, Blocks.stone_slab, 3);
        register(Blocks.sandstone, 0, Blocks.sandstone_stairs, Blocks.stone_slab, 1);
        register(Blocks.brick_block, 0, Blocks.brick_stairs, Blocks.stone_slab, 4);
        register(Blocks.stonebrick, 0, Blocks.stone_brick_stairs, Blocks.stone_slab, 5);
        register(Blocks.nether_brick, 0, Blocks.nether_brick_stairs, Blocks.stone_slab, 6);
        register(Blocks.quartz_block, 0, Blocks.quartz_stairs, Blocks.stone_slab, 7);
        // Fences
        registerFence(Blocks.planks, 0, Blocks.fence);
        registerFence(Blocks.nether_brick, 0, Blocks.nether_brick_fence);
    }

    /**
     * Scans the entire block registry for {@link BlockStairs} subclasses and registers each
     * discovered stair's base-block mapping. Existing explicit entries take precedence.
     * Call this during FML post-init, after all mods have registered their blocks.
     */
    @SuppressWarnings("unchecked")
    public static void scanAllStairs() {
        if (STAIR_MODEL_BLOCK == null || STAIR_MODEL_META == null) return;
        for (Object obj : Block.blockRegistry) {
            if (!(obj instanceof BlockStairs)) continue;
            BlockStairs stair = (BlockStairs) obj;
            Block base = STAIR_MODEL_BLOCK.getValue(stair);
            int baseMeta = STAIR_MODEL_META.getValue(stair);
            if (base == null || base == Blocks.air) continue;
            int fullId = Block.getIdFromBlock(base);
            int forwardKey = packKey(fullId, baseMeta);
            BlockFamily existing = REGISTRY.get(forwardKey);
            if (existing == null) {
                REGISTRY.put(forwardKey, new BlockFamily(stair, null, 0, null));
            } else if (existing.stairs() == null) {
                REGISTRY.put(
                        forwardKey, new BlockFamily(stair, existing.slab(), existing.slabMeta(), existing.fence()));
            }
            // Stairs reverse is handled via instanceof + direct field read in resolveToBase /
            // shapeTypeOf, but we also populate a forward hint here so hasFamily() works.
        }
    }

    /**
     * Registers a full-block → stair + slab family mapping. Entries registered here are not
     * overwritten by {@link #scanAllStairs()}. May be called during static init or FML post-init.
     */
    public static void register(Block fullBlock, int meta, Block stairs, Block slab, int slabMeta) {
        int fullId = Block.getIdFromBlock(fullBlock);
        int[] base = {fullId, meta};
        BlockFamily existing = REGISTRY.get(packKey(fullId, meta));
        Block fence = existing != null ? existing.fence() : null;
        REGISTRY.put(packKey(fullId, meta), new BlockFamily(stairs, slab, slabMeta, fence));
        SLAB_REVERSE.put(packKey(Block.getIdFromBlock(slab), slabMeta & 7), base);
    }

    /**
     * Registers only the slab variant for a full block.
     * Use when the stair is handled by auto-scan but the slab needs explicit registration.
     */
    public static void registerSlab(Block fullBlock, int meta, Block slab, int slabMeta) {
        int fullId = Block.getIdFromBlock(fullBlock);
        int[] base = {fullId, meta};
        BlockFamily existing = REGISTRY.get(packKey(fullId, meta));
        Block stairs = existing != null ? existing.stairs() : null;
        Block fence = existing != null ? existing.fence() : null;
        REGISTRY.put(packKey(fullId, meta), new BlockFamily(stairs, slab, slabMeta, fence));
        SLAB_REVERSE.put(packKey(Block.getIdFromBlock(slab), slabMeta & 7), base);
    }

    /**
     * Registers the fence variant for a full block. Fence blocks have no base-block reference, so
     * this always requires explicit registration.
     */
    public static void registerFence(Block fullBlock, int meta, Block fence) {
        int fullId = Block.getIdFromBlock(fullBlock);
        int[] base = {fullId, meta};
        BlockFamily existing = REGISTRY.get(packKey(fullId, meta));
        Block stairs = existing != null ? existing.stairs() : null;
        Block slab = existing != null ? existing.slab() : null;
        int slabMeta = existing != null ? existing.slabMeta() : 0;
        REGISTRY.put(packKey(fullId, meta), new BlockFamily(stairs, slab, slabMeta, fence));
        FENCE_REVERSE.put(packKey(Block.getIdFromBlock(fence), 0), base);
    }

    /** Returns the family for the given full block, or null if none is registered. */
    public static @Nullable BlockFamily lookup(int blockId, int meta) {
        return REGISTRY.get(packKey(blockId, meta));
    }

    /** Returns true if the given full block has any registered family entry. */
    public static boolean hasFamily(int blockId, int meta) {
        return REGISTRY.containsKey(packKey(blockId, meta));
    }

    /**
     * Returns the {@link BlockShapeType} of the given world block, or null if it belongs to no
     * known family (neither as a full block nor as a variant of one).
     *
     * <p>
     * Stair detection uses {@code instanceof} so it works for unregistered modded stairs.
     */
    public static @Nullable BlockShapeType shapeTypeOf(Block block, int meta) {
        if (block instanceof BlockStairs) return BlockShapeType.STAIR;
        int id = Block.getIdFromBlock(block);
        if (SLAB_REVERSE.containsKey(packKey(id, meta & 7))) return BlockShapeType.SLAB;
        if (FENCE_REVERSE.containsKey(packKey(id, 0))) return BlockShapeType.FENCE;
        if (REGISTRY.containsKey(packKey(id, meta))) return BlockShapeType.FULL;
        return null;
    }

    /**
     * If the given block/meta is a stair, slab, or fence variant, returns {baseBlockId, baseMeta}.
     * Returns null if the block is not a known variant.
     *
     * <p>
     * Stair resolution reads directly from the {@link BlockStairs} instance, so unregistered
     * modded stairs are supported.
     */
    public static @Nullable int[] resolveToBase(int blockId, int meta) {
        Block block = Block.getBlockById(blockId);
        if (block instanceof BlockStairs && STAIR_MODEL_BLOCK != null && STAIR_MODEL_META != null) {
            Block base = STAIR_MODEL_BLOCK.getValue((BlockStairs) block);
            int baseMeta = STAIR_MODEL_META.getValue((BlockStairs) block);
            if (base != null && base != Blocks.air) {
                return new int[] {Block.getIdFromBlock(base), baseMeta};
            }
        }
        int[] fromSlab = SLAB_REVERSE.get(packKey(blockId, meta & 7));
        if (fromSlab != null) return fromSlab.clone();
        int[] fromFence = FENCE_REVERSE.get(packKey(blockId, 0));
        return fromFence != null ? fromFence.clone() : null;
    }

    /**
     * Returns true if every block in {@code palette} has a registered family entry, and the
     * palette is non-empty. Used by painting tools to decide whether to show the Type Replace
     * option.
     */
    public static boolean allHaveFamily(List<ItemStack> palette) {
        if (palette.isEmpty()) return false;
        for (ItemStack stack : palette) {
            if (stack == null) return false;
            Block block = Block.getBlockFromItem(stack.getItem());
            if (block == null || block == Blocks.air) return false;
            if (!hasFamily(Block.getIdFromBlock(block), stack.getItemDamage())) return false;
        }
        return true;
    }

    /**
     * Given the existing block at a position and the target block to paint, returns {blockId,
     * meta} to write so that the shape (stair/slab/fence) of the existing block is preserved.
     *
     * <p>
     * Returns null when type replacement is not applicable — the caller should fall back to
     * writing the target block directly. This includes the case where the existing block is a
     * full block (no shape to preserve).
     */
    public static @Nullable int[] applyTypeReplace(Block existing, int existingMeta, Block target, int targetMeta) {
        BlockShapeType shape = shapeTypeOf(existing, existingMeta);
        if (shape == null) return null;
        BlockFamily family = lookup(Block.getIdFromBlock(target), targetMeta);
        if (family == null) return null;
        switch (shape) {
            case FULL:
                return null;
            case STAIR:
                // existingMeta encodes orientation (bits 0-1 = facing, bit 2 = upside-down) per the
                // vanilla BlockStairs contract, which all known BlockStairs subclasses follow.
                return family.stairs() != null ? new int[] {Block.getIdFromBlock(family.stairs()), existingMeta} : null;
            case SLAB:
                return family.slab() != null
                        ? new int[] {Block.getIdFromBlock(family.slab()), family.slabMetaBottom() | (existingMeta & 8)}
                        : null;
            case FENCE:
                return family.fence() != null ? new int[] {Block.getIdFromBlock(family.fence()), 0} : null;
            default:
                return null;
        }
    }

    private static int packKey(int blockId, int meta) {
        return (blockId << 4) | (meta & 0xF);
    }

    private BlockFamilyRegistry() {}
}
