/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.tool.mask.ToolMask;

/**
 * Accumulates block changes for a drag stroke before committing them to the server.
 *
 * While a drag is active ({@link #activeDrag} != null), {@link #write} stores changes
 * here instead of writing to the world. The real world is never modified during the drag,
 * so every brush stroke within the same drag reads the original pre-drag world state.
 * On RMB release the caller flushes the proposal and sends it as a PacketBlockList.
 *
 * Server-side, activeDrag is always null, so write() falls through to world.setBlock().
 */
public class ChangeProposal {

    /** Position key → [blockId, meta]. Later writes to the same position overwrite earlier ones. */
    public final Map<Long, int[]> proposed = new HashMap<>();

    /** Mask snapshot taken at drag-start. Null means no mask is active. */
    private final ToolMask dragMask;

    private ChangeProposal(ToolMask mask) {
        this.dragMask = mask;
    }

    /** Crease-edge wireframe cache. Recomputed when proposed.size() changes. */
    public float[] cachedWire = null;
    public int[] wireOrigin = new int[3];
    public int wireCacheSize = -1;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public static ChangeProposal getActiveDrag() {
        return ActiveDragState.INSTANCE.activeDrag;
    }

    public static void startDrag(ToolMask mask) {
        ActiveDragState.INSTANCE.activeDrag = new ChangeProposal(mask);
    }

    /** Create a proposal for preview/ghost rendering — no mask, never flushed to server. */
    public static ChangeProposal forPreview() {
        return new ChangeProposal(null);
    }

    public static void cancel() {
        ActiveDragState.INSTANCE.activeDrag = null;
    }

    /** Clears activeDrag and returns all accumulated ops as [x, y, z, blockId, meta] rows. */
    public static List<int[]> flush() {
        ChangeProposal drag = ActiveDragState.INSTANCE.activeDrag;
        ActiveDragState.INSTANCE.activeDrag = null;
        if (drag == null) return new ArrayList<>();
        List<int[]> ops = new ArrayList<>(drag.proposed.size());
        for (Map.Entry<Long, int[]> e : drag.proposed.entrySet()) {
            long key = e.getKey();
            int[] bm = e.getValue();
            ops.add(new int[] { unpackX(key), unpackY(key), unpackZ(key), bm[0], bm[1] });
        }
        return ops;
    }

    // ── Write interception ────────────────────────────────────────────────────

    /**
     * If a drag is active, records the change in the proposal (does NOT touch the world).
     * Otherwise, writes directly to the world (normal server-side path).
     */
    public static void write(World world, int x, int y, int z, Block blk, int meta) {
        if (y < 0 || y >= world.getHeight()) return;
        ChangeProposal drag = ActiveDragState.INSTANCE.activeDrag;
        if (drag != null) {
            if (drag.dragMask != null && !drag.dragMask.test(world, x, y, z)) return;
            drag.proposed.put(packKey(x, y, z), new int[] { Block.getIdFromBlock(blk), meta });
        } else {
            world.setBlock(x, y, z, blk, meta, 3);
        }
    }

    // ── Position packing (same layout as ExtrudeHelper.extrudeKey) ───────────

    public static long packKey(int x, int y, int z) {
        return ((long) (x + 30000000)) << 34 | ((long) (y & 0xFF)) << 26 | (long) (z + 30000000);
    }

    public static int unpackX(long k) {
        return (int) (k >> 34) - 30000000;
    }

    public static int unpackY(long k) {
        return (int) ((k >> 26) & 0xFF);
    }

    public static int unpackZ(long k) {
        return (int) (k & 0x3FFFFFFL) - 30000000;
    }
}
