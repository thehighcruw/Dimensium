/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.handler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.shared.BlockColorCache;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.BlockUtils;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
public class SelectionOps {

    public static List<int[]> selectionToAirOps(SelectionState sel) {
        List<int[]> ops = new ArrayList<>(sel.size());
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt c = SelectionState.unpack(key);
            ops.add(new int[] {c.x(), c.y(), c.z(), 0, 0});
        }
        return ops;
    }

    public static List<int[]> clipboardToPlacements(SelectionState sel, Vec3DInt origin) {
        if (sel.clipboard == null) return new ArrayList<>();
        List<int[]> ops = new ArrayList<>(sel.clipboard.size());
        for (Map.Entry<Long, SelectionState.BlockData> e : sel.clipboard.entrySet()) {
            Vec3DInt dest = origin.plus(SelectionState.decodeClipboardKey(e.getKey()));
            SelectionState.BlockData bd = e.getValue();
            ops.add(new int[] {dest.x(), dest.y(), dest.z(), Block.getIdFromBlock(bd.block()), bd.meta()});
        }
        return ops;
    }

    public static List<int[]> drainOps(SelectionState sel, World world) {
        List<int[]> ops = new ArrayList<>();
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt cv = SelectionState.unpack(key);
            int x = cv.x(), y = cv.y(), z = cv.z();
            Block b = world.getBlock(x, y, z);
            if (b == Blocks.water || b == Blocks.flowing_water) {
                ops.add(new int[] {x, y, z, 0, 0});
            }
        }
        return ops;
    }

    public static List<int[]> fillNearestOps(SelectionState sel, World world) {
        Set<Long> selected = sel.getSelectedBlocks();
        List<int[]> ops = new ArrayList<>();
        for (long key : selected) {
            Vec3DInt cv = SelectionState.unpack(key);
            if (WorldUtils.getBlock(world, cv) != Blocks.air) continue;
            Block nearest = null;
            int nearestMeta = 0;
            outer:
            for (int r = 1; r <= 16; r++) {
                for (int dx = -r; dx <= r; dx++)
                    for (int dy = -r; dy <= r; dy++)
                        for (int dz = -r; dz <= r; dz++) {
                            if (Math.abs(dx) != r && Math.abs(dy) != r && Math.abs(dz) != r) continue;
                            Vec3DInt neighbor = cv.plus(dx, dy, dz);
                            if (!selected.contains(SelectionState.pack(neighbor))) continue;
                            Block nb = WorldUtils.getBlock(world, neighbor);
                            if (nb != Blocks.air) {
                                nearest = nb;
                                nearestMeta = WorldUtils.getBlockMetadata(world, neighbor);
                                break outer;
                            }
                        }
            }
            if (nearest != null) {
                ops.add(new int[] {cv.x(), cv.y(), cv.z(), Block.getIdFromBlock(nearest), nearestMeta});
            }
        }
        return ops;
    }

    public static List<int[]> hollowOps(SelectionState sel) {
        Set<Long> selected = sel.getSelectedBlocks();
        List<int[]> ops = new ArrayList<>();
        for (long key : selected) {
            Vec3DInt cv = SelectionState.unpack(key);
            boolean isShell = false;
            for (Vec3DInt f : BlockUtils.NEIGHBOUR_OFFSETS) {
                if (!selected.contains(SelectionState.pack(cv.plus(f)))) {
                    isShell = true;
                    break;
                }
            }
            if (!isShell) {
                ops.add(new int[] {cv.x(), cv.y(), cv.z(), 0, 0});
            }
        }
        return ops;
    }

    public static List<int[]> fillGapsOps(SelectionState sel, World world, Block fillBlock, int fillMeta) {
        Set<Long> selected = sel.getSelectedBlocks();
        if (selected.isEmpty()) return new ArrayList<>();

        // Expand bounding box by 1 so flood fill can reach all exterior faces.
        Vec3DInt origin = Vec3DInt.from(sel.minX(), sel.minY(), sel.minZ()).minus(1);
        Vec3DInt end = Vec3DInt.from(sel.maxX(), sel.maxY(), sel.maxZ()).plus(1);
        Vec3DInt dims = end.minus(origin).plus(1);
        int sy = dims.y(), sz = dims.z();

        boolean[] visited = new boolean[dims.product()];

        Queue<Vec3DInt> queue = new LinkedList<>();
        visited[idx(Vec3DInt.ZERO, sy, sz)] = true;
        queue.add(origin);

        while (!queue.isEmpty()) {
            Vec3DInt cur = queue.poll();
            for (Vec3DInt d : BlockUtils.NEIGHBOUR_OFFSETS) {
                Vec3DInt next = cur.plus(d);
                if (!next.inBounds(origin, end)) continue;
                int i = idx(next.minus(origin), sy, sz);
                if (visited[i]) continue;
                // Don't cross through selected (solid) blocks.
                if (selected.contains(SelectionState.pack(next))) continue;
                visited[i] = true;
                queue.add(next);
            }
        }

        // Any non-visited, non-selected block inside the bbox that is air in world = enclosed gap.
        List<int[]> ops = new ArrayList<>();
        Vec3DInt.forEachInclusive(origin.plus(1), end.minus(1), (x, y, z) -> {
            if (selected.contains(SelectionState.pack(Vec3DInt.from(x, y, z)))) return;
            int i = idx(Vec3DInt.from(x, y, z).minus(origin), sy, sz);
            if (!visited[i] && world.getBlock(x, y, z) == Blocks.air) {
                ops.add(new int[] {x, y, z, Block.getIdFromBlock(fillBlock), fillMeta});
            }
        });
        return ops;
    }

    private static int idx(Vec3DInt v, int sy, int sz) {
        return v.x() * sy * sz + v.y() * sz + v.z();
    }

    public static List<int[]> simulateGravityOps(SelectionState sel, World world) {
        Set<Long> selected = sel.getSelectedBlocks();
        List<int[]> ops = new ArrayList<>();

        // Collect falling blocks sorted by Y ascending (process lowest first so stacks work).
        List<int[]> falling = new ArrayList<>();
        for (long key : selected) {
            Vec3DInt cv = SelectionState.unpack(key);
            int x = cv.x(), y = cv.y(), z = cv.z();
            Block b = world.getBlock(x, y, z);
            if (b instanceof BlockFalling && b != Blocks.air) {
                falling.add(new int[] {x, y, z, Block.getIdFromBlock(b), world.getBlockMetadata(x, y, z)});
            }
        }
        falling.sort(Comparator.comparingInt(a -> a[1]));

        // Simulate: track which positions will be air after movement (applied to our ops list).
        Map<Long, int[]> state = new HashMap<>();
        for (long key : selected) {
            Vec3DInt cv2 = SelectionState.unpack(key);
            int x = cv2.x(), y = cv2.y(), z = cv2.z();
            Block b = world.getBlock(x, y, z);
            state.put(key, new int[] {Block.getIdFromBlock(b), world.getBlockMetadata(x, y, z)});
        }

        for (int[] fb : falling) {
            int x = fb[0], y = fb[1], z = fb[2];
            int blockId = fb[3], meta = fb[4];

            // Find lowest air position below this block (within selection or world).
            int dropY = y;
            for (int ty = y - 1; ty >= sel.minY() - 1; ty--) {
                long testKey = SelectionState.pack(Vec3DInt.from(x, ty, z));
                int[] cur = state.get(testKey);
                int curId = (cur != null) ? cur[0] : Block.getIdFromBlock(world.getBlock(x, ty, z));
                if (curId != 0) break; // Hit something solid.
                dropY = ty;
            }

            if (dropY != y) {
                // Move block down: source becomes air, target gets block.
                state.put(SelectionState.pack(Vec3DInt.from(x, y, z)), new int[] {0, 0});
                state.put(SelectionState.pack(Vec3DInt.from(x, dropY, z)), new int[] {blockId, meta});
                ops.add(new int[] {x, y, z, 0, 0});
                ops.add(new int[] {x, dropY, z, blockId, meta});
            }
        }
        return ops;
    }

    public static List<int[]> triggerUpdatesOps(SelectionState sel, World world) {
        List<int[]> ops = new ArrayList<>();
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt cv = SelectionState.unpack(key);
            int x = cv.x(), y = cv.y(), z = cv.z();
            Block b = world.getBlock(x, y, z);
            if (b == Blocks.air) continue;
            if (!b.canBlockStay(world, x, y, z)) {
                ops.add(new int[] {x, y, z, 0, 0});
            }
        }
        return ops;
    }

    // ── Colour Field ──────────────────────────────────────────────────────────

    public static List<int[]> generateColourFieldOps(SelectionState sel, int includeMask) {
        if (!sel.hasSelection()) return new ArrayList<>();

        BlockColorCache cache = BlockColorCache.INSTANCE;
        cache.init();
        List<int[]> candidates = cache.getColourFieldCandidates();
        if (candidates.isEmpty()) return new ArrayList<>();

        int minX = sel.minX(), maxX = sel.maxX();
        int minY = sel.minY(), maxY = sel.maxY();
        int minZ = sel.minZ(), maxZ = sel.maxZ();
        int rangeX = Math.max(1, maxX - minX);
        int rangeY = Math.max(1, maxY - minY);
        int rangeZ = Math.max(1, maxZ - minZ);

        Set<Long> selected = sel.getSelectedBlocks();
        Set<Long> claimed = new HashSet<>();
        List<int[]> ops = new ArrayList<>();

        // Each candidate block maps to exactly one position in the field via its Lab value.
        // X → L (0-100, light→dark), Y → A (-128..127, green→red), Z → B (-128..127, blue→yellow)
        // If that position is unselected or already claimed by another block, the block is skipped.
        for (int[] candidate : candidates) {
            int blockId = candidate[0], meta = candidate[1], rgb = candidate[2], category = candidate[3];
            // Block must have at least one included flag AND no excluded flags.
            int excludeMask = ~includeMask;
            if ((category & includeMask) == 0) continue;
            if ((category & excludeMask) != 0) continue;

            double[] lab = BlockColorCache.rgbToLab(rgb);
            double tX = lab[0] / 100.0;
            double tY = (lab[1] + 128.0) / 255.0;
            double tZ = (lab[2] + 128.0) / 255.0;

            int x = minX + (int) Math.round(tX * rangeX);
            int y = minY + (int) Math.round(tY * rangeY);
            int z = minZ + (int) Math.round(tZ * rangeZ);
            x = Math.max(minX, Math.min(maxX, x));
            y = Math.max(minY, Math.min(maxY, y));
            z = Math.max(minZ, Math.min(maxZ, z));

            long posKey = SelectionState.pack(Vec3DInt.from(x, y, z));
            if (selected.contains(posKey) && claimed.add(posKey)) {
                ops.add(new int[] {x, y, z, blockId, meta});
            }
        }
        return ops;
    }
}
