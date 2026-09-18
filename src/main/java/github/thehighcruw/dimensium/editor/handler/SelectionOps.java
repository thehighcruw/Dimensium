/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.handler;

import com.github.bsideup.jabel.Desugar;
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

    @Desugar
    private record FallingBlock(Vec3DInt pos, int blockId, int meta) {}

    public static List<int[]> selectionToAirOps(SelectionState sel) {
        List<int[]> ops = new ArrayList<>(sel.size());
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt c = SelectionState.unpack(key);
            ops.add(c.toBlockOp(0, 0));
        }
        return ops;
    }

    public static List<int[]> clipboardToPlacements(SelectionState sel, Vec3DInt origin) {
        if (sel.clipboard == null) return new ArrayList<>();
        List<int[]> ops = new ArrayList<>(sel.clipboard.size());
        for (Map.Entry<Long, SelectionState.BlockData> e : sel.clipboard.entrySet()) {
            Vec3DInt dest = origin.plus(SelectionState.decodeClipboardKey(e.getKey()));
            SelectionState.BlockData bd = e.getValue();
            ops.add(dest.toBlockOp(Block.getIdFromBlock(bd.block()), bd.meta()));
        }
        return ops;
    }

    public static List<int[]> drainOps(SelectionState sel, World world) {
        List<int[]> ops = new ArrayList<>();
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt cv = SelectionState.unpack(key);
            Block b = WorldUtils.getBlock(world, cv);
            if (b == Blocks.water || b == Blocks.flowing_water) {
                ops.add(cv.toBlockOp(0, 0));
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
            Block[] nearest = {null};
            int[] nearestMeta = {0};
            for (int r = 1; r <= 16; r++) {
                final int fr = r;
                boolean found =
                        Vec3DInt.anyInclusive(Vec3DInt.from(-r, -r, -r), Vec3DInt.from(r, r, r), (dx, dy, dz) -> {
                            if (Math.abs(dx) != fr && Math.abs(dy) != fr && Math.abs(dz) != fr) return false;
                            Vec3DInt neighbor = cv.plus(dx, dy, dz);
                            if (!selected.contains(SelectionState.pack(neighbor))) return false;
                            Block nb = WorldUtils.getBlock(world, neighbor);
                            if (nb != Blocks.air) {
                                nearest[0] = nb;
                                nearestMeta[0] = WorldUtils.getBlockMetadata(world, neighbor);
                                return true;
                            }
                            return false;
                        });
                if (found) break;
            }
            if (nearest[0] != null) {
                ops.add(cv.toBlockOp(Block.getIdFromBlock(nearest[0]), nearestMeta[0]));
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
                ops.add(cv.toBlockOp(0, 0));
            }
        }
        return ops;
    }

    public static List<int[]> fillGapsOps(SelectionState sel, World world, Block fillBlock, int fillMeta) {
        Set<Long> selected = sel.getSelectedBlocks();
        if (selected.isEmpty()) return new ArrayList<>();

        // Expand bounding box by 1 so flood fill can reach all exterior faces.
        Vec3DInt origin = sel.min().minus(1);
        Vec3DInt end = sel.max().plus(1);
        Vec3DInt dims = end.minus(origin).plus(1);
        boolean[] visited = new boolean[dims.product()];

        Queue<Vec3DInt> queue = new LinkedList<>();
        visited[0] = true;
        queue.add(origin);

        while (!queue.isEmpty()) {
            Vec3DInt cur = queue.poll();
            for (Vec3DInt d : BlockUtils.NEIGHBOUR_OFFSETS) {
                Vec3DInt next = cur.plus(d);
                if (!next.inBounds(origin, end)) continue;
                int i = next.minus(origin).toIndex(dims);
                if (visited[i]) continue;
                // Don't cross through selected (solid) blocks.
                if (selected.contains(SelectionState.pack(next))) continue;
                visited[i] = true;
                queue.add(next);
            }
        }

        // Any non-visited, non-selected block inside the bbox that is air in world = enclosed gap.
        List<int[]> ops = new ArrayList<>();
        Vec3DInt.forEachInclusive(origin.plus(1), end.minus(1), pos -> {
            if (selected.contains(SelectionState.pack(pos))) return;
            int i = pos.minus(origin).toIndex(dims);
            if (!visited[i] && WorldUtils.getBlock(world, pos) == Blocks.air) {
                ops.add(pos.toBlockOp(Block.getIdFromBlock(fillBlock), fillMeta));
            }
        });
        return ops;
    }

    public static List<int[]> simulateGravityOps(SelectionState sel, World world) {
        Set<Long> selected = sel.getSelectedBlocks();
        List<int[]> ops = new ArrayList<>();

        // Collect falling blocks sorted by Y ascending (process lowest first so stacks work).
        List<FallingBlock> falling = new ArrayList<>();
        for (long key : selected) {
            Vec3DInt cv = SelectionState.unpack(key);
            Block b = WorldUtils.getBlock(world, cv);
            if (b instanceof BlockFalling && b != Blocks.air) {
                falling.add(new FallingBlock(cv, Block.getIdFromBlock(b), WorldUtils.getBlockMetadata(world, cv)));
            }
        }
        falling.sort(Comparator.comparingInt(fb -> fb.pos().y()));

        // Simulate: track which positions will be air after movement (applied to our ops list).
        Map<Long, int[]> state = new HashMap<>();
        for (long key : selected) {
            Vec3DInt cv2 = SelectionState.unpack(key);
            Block b = WorldUtils.getBlock(world, cv2);
            state.put(key, new int[] {Block.getIdFromBlock(b), WorldUtils.getBlockMetadata(world, cv2)});
        }

        for (FallingBlock fb : falling) {
            Vec3DInt pos = fb.pos();
            int blockId = fb.blockId(), meta = fb.meta();

            // Find lowest air position below this block (within selection or world).
            int dropY = pos.y();
            for (int ty = pos.y() - 1; ty >= sel.minY() - 1; ty--) {
                Vec3DInt testPos = Vec3DInt.from(pos.x(), ty, pos.z());
                long testKey = SelectionState.pack(testPos);
                int[] cur = state.get(testKey);
                int curId = (cur != null) ? cur[0] : Block.getIdFromBlock(WorldUtils.getBlock(world, testPos));
                if (curId != 0) break; // Hit something solid.
                dropY = ty;
            }

            if (dropY != pos.y()) {
                // Move block down: source becomes air, target gets block.
                state.put(SelectionState.pack(pos), new int[] {0, 0});
                Vec3DInt dropPos = Vec3DInt.from(pos.x(), dropY, pos.z());
                state.put(SelectionState.pack(dropPos), new int[] {blockId, meta});
                ops.add(pos.toBlockOp(0, 0));
                ops.add(dropPos.toBlockOp(blockId, meta));
            }
        }
        return ops;
    }

    public static List<int[]> triggerUpdatesOps(SelectionState sel, World world) {
        List<int[]> ops = new ArrayList<>();
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt cv = SelectionState.unpack(key);
            Block b = WorldUtils.getBlock(world, cv);
            if (b == Blocks.air) continue;
            if (!b.canBlockStay(world, cv.x(), cv.y(), cv.z())) {
                ops.add(cv.toBlockOp(0, 0));
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

        Vec3DInt selMin = sel.min();
        Vec3DInt selMax = sel.max();
        Vec3DInt range = selMax.minus(selMin).max(Vec3DInt.ONE);

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

            Vec3DInt pos = Vec3DInt.from(
                            selMin.x() + (int) Math.round(tX * range.x()),
                            selMin.y() + (int) Math.round(tY * range.y()),
                            selMin.z() + (int) Math.round(tZ * range.z()))
                    .max(selMin)
                    .min(selMax);

            long posKey = SelectionState.pack(pos);
            if (selected.contains(posKey) && claimed.add(posKey)) {
                ops.add(pos.toBlockOp(blockId, meta));
            }
        }
        return ops;
    }
}
