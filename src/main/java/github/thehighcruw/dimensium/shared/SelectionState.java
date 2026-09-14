/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import com.github.bsideup.jabel.Desugar;

import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;
import github.thehighcruw.dimensium.editor.tool.selecting.magic.MagicSelectToolState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;

public class SelectionState {

    public static final SelectionState INSTANCE = new SelectionState();

    // ── Block-set selection ───────────────────────────────────────────────────

    private final Set<Long> selectedBlocks = new HashSet<>();

    /**
     * Cached bounding box; rebuilt lazily on first access after a mutation.
     */
    private Vec3DInt cachedMin = Vec3DInt.ZERO;
    private Vec3DInt cachedMax = Vec3DInt.ZERO;
    private boolean boundsDirty = true;

    /**
     * Incremented on every block-set mutation; renderers use this to cache wireframes.
     */
    public long renderVersion = 0L;
    /**
     * Incremented when the clipboard content changes; renderers use this to cache hologram wireframes.
     */
    public int clipboardVersion = 0;

    // ── Box-select pending state ──────────────────────────────────────────────
    // Drag phase (pendingPos1=true): RMB held, pos1 anchored, preview follows cursor.
    // Gizmo phase (boxConfirmed=true): RMB released, pos2 locked, gizmos active; Enter commits.

    public boolean pendingPos1 = false;
    public boolean boxConfirmed = false;
    public Vec3DInt pendingPos = Vec3DInt.ZERO;
    public Vec3DInt pendingPos2 = Vec3DInt.ZERO;

    // ── Clipboard ─────────────────────────────────────────────────────────────

    /**
     * Sparse map: key = clipboardKey(x,y,z), value = non-air block. Air positions are absent.
     */
    public Map<Long, BlockData> clipboard = null;
    public int clipW, clipH, clipD;

    // ── Query ────────────────────────────────────────────────────────────────

    public boolean hasSelection() {
        return !selectedBlocks.isEmpty();
    }

    public boolean contains(int x, int y, int z) {
        return selectedBlocks.contains(pack(Vec3DInt.from(x, y, z)));
    }

    public Set<Long> getSelectedBlocks() {
        return Collections.unmodifiableSet(selectedBlocks);
    }

    public int size() {
        return selectedBlocks.size();
    }

    // ── Bounding box (derived, cached) ────────────────────────────────────────

    private void rebuildBounds() {
        if (!boundsDirty) return;
        if (selectedBlocks.isEmpty()) {
            boundsDirty = false;
            return;
        }
        BoundingBox b = computeBounds(selectedBlocks);
        cachedMin = b.minimum();
        cachedMax = b.maximum();
        boundsDirty = false;
    }

    public int minX() {
        rebuildBounds();
        return cachedMin.x();
    }

    public int minY() {
        rebuildBounds();
        return cachedMin.y();
    }

    public int minZ() {
        rebuildBounds();
        return cachedMin.z();
    }

    public int maxX() {
        rebuildBounds();
        return cachedMax.x();
    }

    public int maxY() {
        rebuildBounds();
        return cachedMax.y();
    }

    public int maxZ() {
        rebuildBounds();
        return cachedMax.z();
    }

    public int width() {
        return hasSelection() ? maxX() - minX() + 1 : 0;
    }

    public int height() {
        return hasSelection() ? maxY() - minY() + 1 : 0;
    }

    public int depth() {
        return hasSelection() ? maxZ() - minZ() + 1 : 0;
    }

    // ── Mutation ─────────────────────────────────────────────────────────────

    public void clearSelection() {
        selectedBlocks.clear();
        pendingPos1 = false;
        boxConfirmed = false;
        boundsDirty = true;
        renderVersion++;
    }

    /**
     * Clears only the committed block set; leaves pendingPos1 intact.
     */
    public void clearBlocks() {
        selectedBlocks.clear();
        boundsDirty = true;
        renderVersion++;
    }

    /**
     * Merge an incoming block set into the current selection using the given
     * boolean operation. This is the single mutation point for all selection tools.
     */
    public void applyOp(Set<Long> incoming, BooleanOp op) {
        switch (op) {
            case ADD:
                selectedBlocks.addAll(incoming);
                break;
            case SUBTRACT:
                selectedBlocks.removeAll(incoming);
                break;
            case REPLACE:
                selectedBlocks.clear();
                selectedBlocks.addAll(incoming);
                break;
            case INTERSECT:
                selectedBlocks.retainAll(incoming);
                break;
        }
        boundsDirty = true;
        renderVersion++;
    }

    // ── AABB builder (used by box select before calling applyOp) ─────────────

    public static Set<Long> floodFill(World world, int sx, int sy, int sz, int limit, int range, boolean surfaceOnly,
        boolean corners, MagicSelectToolState.MagicCompareType compareType,
        MagicSelectToolState.MagicDirection direction) {
        Block targetBlock = world.getBlock(sx, sy, sz);
        int targetMeta = world.getBlockMetadata(sx, sy, sz);
        if (targetBlock == Blocks.air) return new HashSet<>();

        int[][] dirs6 = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        int[][] dirs26 = buildDirs26();
        int[][] allDirs = corners ? dirs26 : dirs6;

        Set<Long> visited = new HashSet<>();
        Queue<Vec3DInt> queue = new LinkedList<>();
        Set<Long> result = new HashSet<>();

        Vec3DInt start = Vec3DInt.from(sx, sy, sz);
        visited.add(pack(start));
        queue.add(start);

        while (!queue.isEmpty() && result.size() < limit) {
            Vec3DInt cur = queue.poll();

            if (surfaceOnly && !isExposedToAir(world, cur.x(), cur.y(), cur.z(), dirs6)) continue;

            result.add(pack(cur));

            for (int r = 1; r <= range; r++) {
                for (int[] d : allDirs) {
                    // direction filter
                    if (direction == MagicSelectToolState.MagicDirection.UP_ONLY && d[1] < 0) continue;
                    if (direction == MagicSelectToolState.MagicDirection.DOWN_ONLY && d[1] > 0) continue;

                    Vec3DInt nb = Vec3DInt.from(cur.x() + d[0] * r, cur.y() + d[1] * r, cur.z() + d[2] * r);
                    if (nb.y() < 0 || nb.y() > 255) continue;
                    long nk = pack(nb);
                    if (visited.contains(nk)) continue;
                    visited.add(nk);
                    if (matches(world, nb.x(), nb.y(), nb.z(), targetBlock, targetMeta, compareType)) {
                        queue.add(nb);
                    }
                }
            }
        }
        return result;
    }

    private static boolean matches(World world, int x, int y, int z, Block targetBlock, int targetMeta,
        MagicSelectToolState.MagicCompareType compareType) {
        Block b = world.getBlock(x, y, z);
        return switch (compareType) {
            case BLOCK_STATE -> b == targetBlock && world.getBlockMetadata(x, y, z) == targetMeta;
            case BLOCK -> b == targetBlock;
            case SOLID -> b.isOpaqueCube();
            case ANY -> b != Blocks.air;
        };
    }

    private static boolean isExposedToAir(World world, int x, int y, int z, int[][] dirs6) {
        for (int[] d : dirs6) {
            int nx = x + d[0], ny = y + d[1], nz = z + d[2];
            if (ny < 0 || ny > 255) continue;
            if (world.getBlock(nx, ny, nz) == Blocks.air) return true;
        }
        return false;
    }

    private static int[][] buildDirs26() {
        List<int[]> list = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
            for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dy != 0 || dz != 0) list.add(new int[] { dx, dy, dz });
        return list.toArray(new int[0][]);
    }

    /**
     * Flood-fill air blocks starting from an air block, optionally directional.
     */
    public static Set<Long> floodFillAir(World world, int sx, int sy, int sz, int limit, boolean goDown,
        boolean corners) {
        if (world.getBlock(sx, sy, sz) != Blocks.air) return new HashSet<>();

        int[][] dirs6 = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        int[][] dirs26 = buildDirs26();
        int[][] dirs = corners ? dirs26 : dirs6;

        Set<Long> visited = new HashSet<>();
        Queue<Vec3DInt> queue = new LinkedList<>();
        Set<Long> result = new HashSet<>();

        Vec3DInt start2 = Vec3DInt.from(sx, sy, sz);
        visited.add(pack(start2));
        queue.add(start2);

        while (!queue.isEmpty() && result.size() < limit) {
            Vec3DInt cur = queue.poll();
            result.add(pack(cur));
            for (int[] d : dirs) {
                Vec3DInt nb = Vec3DInt.from(cur.x() + d[0], cur.y() + d[1], cur.z() + d[2]);
                if (nb.y() < 0 || nb.y() > 255) continue;
                if (goDown && nb.y() > cur.y()) continue;
                if (!goDown && nb.y() < cur.y()) continue;
                long nk = pack(nb);
                if (visited.contains(nk)) continue;
                visited.add(nk);
                if (world.getBlock(nb.x(), nb.y(), nb.z()) == Blocks.air) {
                    queue.add(nb);
                }
            }
        }
        return result;
    }

    public static Set<Long> aabbBlocks(int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        Set<Long> set = new HashSet<>();
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) set.add(pack(Vec3DInt.from(x, y, z)));
        return set;
    }

    // ── Clipboard ─────────────────────────────────────────────────────────────

    public static long clipboardKey(int x, int y, int z) {
        return ((long) x << 20) | ((long) y << 10) | z;
    }

    public static Vec3DInt decodeClipboardKey(long key) {
        return Vec3DInt.from((int) (key >> 20) & 0xFFFFF, (int) (key >> 10) & 0x3FF, (int) key & 0x3FF);
    }

    /**
     * Returns the block at clipboard-local (x,y,z), or AIR if absent or out of bounds.
     */
    public BlockData clipboardGet(int x, int y, int z) {
        if (clipboard == null) return BlockData.AIR;
        BlockData bd = clipboard.get(clipboardKey(x, y, z));
        return bd != null ? bd : BlockData.AIR;
    }

    /**
     * Snapshot block data from the client world into a sparse clipboard.
     */
    public void captureFromWorld(World world) {
        if (!hasSelection()) return;
        int limit = DimensiumConfig.maxCopyVolume;
        int w = width(), h = height(), d = depth();
        if (w <= 0 || h <= 0 || d <= 0 || w > limit || h > limit || d > limit) return;
        clipW = w;
        clipH = h;
        clipD = d;
        Map<Long, BlockData> map = new HashMap<>();
        clipboardVersion++;
        int ox = minX(), oy = minY(), oz = minZ();
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) for (int z = 0; z < d; z++) {
            if (contains(ox + x, oy + y, oz + z)) {
                Block block = world.getBlock(ox + x, oy + y, oz + z);
                if (block != Blocks.air) {
                    int meta = world.getBlockMetadata(ox + x, oy + y, oz + z);
                    map.put(clipboardKey(x, y, z), new BlockData(block, meta));
                }
            }
        }
        clipboard = map;
    }

    // ── Bounds utility ────────────────────────────────────────────────────────

    /**
     * Returns int[6] = {minX, minY, minZ, maxX, maxY, maxZ} for an arbitrary block set.
     * Caller must check that blocks is non-empty.
     */
    public static BoundingBox computeBounds(Iterable<Long> keys) {
        Vec3DInt minimum = Vec3DInt.MAX_VALUE, maximum = Vec3DInt.MIN_VALUE;
        for (long key : keys) {
            Vec3DInt coord = unpack(key);
            minimum = minimum.min(coord);
            maximum = maximum.max(coord);
        }
        return BoundingBox.from(minimum, maximum);
    }

    // ── Coordinate packing ────────────────────────────────────────────────────
    // Layout (60 bits total, safe in signed long):
    // bits 59-34: X + 30_000_000 (26 bits, values 0..60M)
    // bits 33-26: Y ( 8 bits, values 0..255)
    // bits 25- 0: Z + 30_000_000 (26 bits, values 0..60M)

    public static long pack(Vec3DInt coord) {
        return ((long) (coord.x() + 30_000_000)) << 34 | ((long) coord.y()) << 26 | (coord.z() + 30_000_000);
    }

    public static Vec3DInt unpack(long key) {
        return new Vec3DInt(unpackX(key), unpackY(key), unpackZ(key));
    }

    private static int unpackX(long key) {
        return (int) (key >> 34) - 30_000_000;
    }

    private static int unpackY(long key) {
        return (int) ((key >> 26) & 0xFF);
    }

    private static int unpackZ(long key) {
        return (int) (key & 0x3FFFFFF) - 30_000_000;
    }

    public static @Nullable BlockInfo unpackBlock(long key) {
        Vec3DInt coord = SelectionState.unpack(key);

        Block block = WorldUtils.getWorldBlock(coord);
        if (block == null || block == Blocks.air) return null;

        Item item = Item.getItemFromBlock(block);
        if (item == null) return new BlockInfo(coord, block, null, -1);

        int meta = WorldUtils.getWorldBlockMeta(coord);
        return new BlockInfo(coord, block, item, meta);
    }

    @Desugar
    public record BlockInfo(@Nonnull Vec3DInt coord, @Nonnull Block block, @Nullable Item item, int meta) {}

    // ── Inner types ───────────────────────────────────────────────────────────

    @Desugar
    public record BlockData(Block block, int meta) {

        public static final BlockData AIR = new BlockData(Blocks.air, 0);
    }
}
