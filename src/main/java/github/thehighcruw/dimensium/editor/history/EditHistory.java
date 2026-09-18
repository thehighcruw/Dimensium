/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.history;

import github.thehighcruw.dimensium.network.PacketHistoryEntry;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.PerfTrace;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.FakePlayerFactory;

public class EditHistory {

    // ── Block placement ───────────────────────────────────────────────────────

    /**
     * Apply one block op. For "meta-item" blocks (itemDamage > 15) the item
     * damage cannot be stored as 4-bit block metadata. Delegate to the item's
     * own placeBlockAt() so each mod's placement logic runs (TileEntity init,
     * facing, etc.). GT5 machines, GT+Plus, and similar mods all handled
     * automatically this way.
     */
    static void applyBlock(World world, Vec3DInt pos, Block blk, int meta) {
        if (blk == null || blk == Blocks.air) {
            WorldUtils.setBlock(world, pos, Blocks.air, 0, 3);
            return;
        }

        if (meta > 15 && world instanceof WorldServer) {
            Item item = Item.getItemFromBlock(blk);
            if (item instanceof ItemBlock itemBlock) {
                ItemStack stack = new ItemStack(itemBlock, 1, meta);
                // Clear the target position first so placeBlockAt has an air block to fill.
                WorldUtils.setBlock(world, pos, Blocks.air, 0, 3);
                // Use the top face (side=1) as a neutral default; most mods use facing
                // from the player entity, not the side parameter, for actual orientation.
                itemBlock.placeBlockAt(
                        stack,
                        FakePlayerFactory.getMinecraft((WorldServer) world),
                        world,
                        pos.x(),
                        pos.y(),
                        pos.z(),
                        1,
                        0.5f,
                        0.5f,
                        0.5f,
                        itemBlock.getMetadata(meta));
                return;
            }
        }

        // Standard case: meta fits in 4 bits, no special placement needed.
        // Flag 2: notify clients but skip neighbour/lighting cascade (bulk-op performance).
        WorldUtils.setBlock(world, pos, blk, meta, 2);
    }

    /**
     * Bulk-safe variant: writes directly to chunk storage, bypassing World.setBlock entirely.
     * No lighting recalc, no neighbor notify, no client packet. Updates height map and
     * removes stale tile entities via Chunk.func_150807_a.
     *
     * Caller MUST call finalizeChunks() after all fast writes are done.
     * meta > 15 blocks fall back to applyBlock() — placeBlockAt() cannot be bypassed.
     */
    static void applyBlockFast(World world, Vec3DInt pos, Block blk, int meta) {
        if (pos.y() < 0 || pos.y() >= world.getHeight()) return;
        if (blk == null) blk = Blocks.air;
        // Tile-entity blocks must go through world.setBlock so the TE receives a world
        // reference before any constructor logic (e.g. IC2 energy-net registration) fires.
        if (blk.hasTileEntity(meta) || (meta > 15 && world instanceof WorldServer)) {
            applyBlock(world, pos, blk, meta);
            return;
        }
        Chunk chunk = world.getChunkFromBlockCoords(pos.x(), pos.z());
        chunk.func_150807_a(pos.x() & 15, pos.y(), pos.z() & 15, blk, meta);
        chunk.setChunkModified();
    }

    /**
     * Finalize a bulk-fast edit: recalculate sky light per affected chunk column, then
     * notify clients of every changed block. Must be called on the server tick thread.
     *
     * chunkKeys: packed chunk coords as ((long)cx << 32) | (cz & 0xFFFFFFFFL).
     */
    static void finalizeChunks(World world, Set<Long> chunkKeys, List<int[]> ops) {
        PerfTrace.push("generateSkylightMap chunks=" + chunkKeys.size());
        skylightAndLightRecalc(world, chunkKeys, ops);
        PerfTrace.pop();
        PerfTrace.push("markBlockForUpdate ops=" + ops.size());
        for (int[] op : ops) {
            world.markBlockForUpdate(op[0], op[1], op[2]);
        }
        PerfTrace.pop();
    }

    /**
     * Phase 1 of the throttled finalize path: skylight + per-block light recalc only.
     * Does NOT call markBlockForUpdate — caller will drip-feed those over multiple ticks
     * to stay below PlayerManager's S21 threshold (64 per chunk per tick).
     */
    static void skylightAndLightRecalc(World world, Set<Long> chunkKeys, List<int[]> ops) {
        for (long ck : chunkKeys) {
            int cx = (int) (ck >> 32);
            int cz = (int) (ck & 0xFFFFFFFFL);
            Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
            if (chunk != null) chunk.generateSkylightMap();
        }
        for (int[] op : ops) {
            world.func_147451_t(op[0], op[1], op[2]);
        }
    }

    // ── Before-state capture ──────────────────────────────────────────────────

    /**
     * For most blocks: world metadata IS the item damage equivalent.
     * For meta-item blocks (GT5 machines etc.) the world metadata is just a
     * category byte; the real ID lives in the TileEntity's NBT under "mID".
     * Reading it here lets undo restore the correct machine type.
     */
    static int getEffectiveMeta(World world, Vec3DInt pos) {
        int blockMeta = WorldUtils.getBlockMetadata(world, pos);
        if (blockMeta > 15) return blockMeta; // already "full" — shouldn't happen, but safe
        // Only check TileEntity NBT for blocks that have one; avoids a map lookup per block
        // for the common case (standard blocks with no TileEntity).
        if (!WorldUtils.getBlock(world, pos).hasTileEntity(blockMeta)) return blockMeta;
        TileEntity te;
        try {
            te = world.getTileEntity(pos.x(), pos.y(), pos.z());
        } catch (Throwable ignored) {
            return blockMeta;
        }
        if (te == null) return blockMeta;
        try {
            // GT5 / GT+Plus store the machine ID as a short in "mID" NBT tag.
            NBTTagCompound nbt = new NBTTagCompound();
            te.writeToNBT(nbt);
            if (nbt.hasKey("mID")) {
                int machineId = nbt.getShort("mID") & 0xFFFF;
                if (machineId > 0) return machineId;
            }
        } catch (Throwable ignored) {
        }
        return blockMeta;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Overload for server-originated ops (e.g. PacketShapePlacement) where the client
     * has no buffered after-state, so it must be sent explicitly.
     */
    public static void record(
            World world, String action, List<int[]> ops, EntityPlayerMP player, int txId, int[][] after) {
        int[][] before = new int[ops.size()][5];
        for (int i = 0; i < ops.size(); i++) {
            int[] op = ops.get(i);
            Vec3DInt pos = Vec3DInt.from(op[0], op[1], op[2]);
            before[i] = new int[] {
                op[0], op[1], op[2], Block.getIdFromBlock(WorldUtils.getBlock(world, pos)), getEffectiveMeta(world, pos)
            };
        }

        applyBlocks(world, ops);

        PacketHistoryEntry.sendChunked(player, txId, action, before, after);
    }

    /** Applies a block list to the world without recording history (undo/redo replay). */
    public static void applyBlocks(World world, List<int[]> ops) {
        for (int[] op : ops) {
            applyBlock(world, Vec3DInt.from(op[0], op[1], op[2]), Block.getBlockById(op[3]), op[4]);
        }
    }
}
