package github.thehighcruw.dimensium.handler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.tool.BlockColorCache;
import github.thehighcruw.dimensium.tool.state.SelectionState;

@SideOnly(Side.CLIENT)
public class SelectionOps {

    public static List<int[]> selectionToAirOps(SelectionState sel) {
        List<int[]> ops = new ArrayList<>(sel.size());
        for (long key : sel.getSelectedBlocks()) {
            ops.add(
                new int[] { SelectionState.unpackX(key), SelectionState.unpackY(key), SelectionState.unpackZ(key), 0,
                    0 });
        }
        return ops;
    }

    public static List<int[]> clipboardToPlacements(SelectionState sel, int ox, int oy, int oz) {
        if (sel.clipboard == null) return new ArrayList<>();
        List<int[]> ops = new ArrayList<>(sel.clipboard.size());
        for (java.util.Map.Entry<Long, SelectionState.BlockData> e : sel.clipboard.entrySet()) {
            long key = e.getKey();
            int lx = (int) (key >> 20) & 0xFFFFF;
            int ly = (int) (key >> 10) & 0x3FF;
            int lz = (int) key & 0x3FF;
            SelectionState.BlockData bd = e.getValue();
            ops.add(new int[] { ox + lx, oy + ly, oz + lz, Block.getIdFromBlock(bd.block), bd.meta });
        }
        return ops;
    }

    public static List<int[]> drainOps(SelectionState sel, World world) {
        List<int[]> ops = new ArrayList<>();
        for (long key : sel.getSelectedBlocks()) {
            int x = SelectionState.unpackX(key), y = SelectionState.unpackY(key), z = SelectionState.unpackZ(key);
            Block b = world.getBlock(x, y, z);
            if (b == Blocks.water || b == Blocks.flowing_water) {
                ops.add(new int[] { x, y, z, 0, 0 });
            }
        }
        return ops;
    }

    public static List<int[]> fillNearestOps(SelectionState sel, World world) {
        Set<Long> selected = sel.getSelectedBlocks();
        List<int[]> ops = new ArrayList<>();
        for (long key : selected) {
            int x = SelectionState.unpackX(key), y = SelectionState.unpackY(key), z = SelectionState.unpackZ(key);
            if (world.getBlock(x, y, z) != Blocks.air) continue;
            Block nearest = null;
            int nearestMeta = 0;
            outer: for (int r = 1; r <= 16; r++) {
                for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r && Math.abs(dz) != r) continue;
                    long nKey = SelectionState.pack(x + dx, y + dy, z + dz);
                    if (!selected.contains(nKey)) continue;
                    Block nb = world.getBlock(x + dx, y + dy, z + dz);
                    if (nb != Blocks.air) {
                        nearest = nb;
                        nearestMeta = world.getBlockMetadata(x + dx, y + dy, z + dz);
                        break outer;
                    }
                }
            }
            if (nearest != null) {
                ops.add(new int[] { x, y, z, Block.getIdFromBlock(nearest), nearestMeta });
            }
        }
        return ops;
    }

    public static List<int[]> hollowOps(SelectionState sel) {
        Set<Long> selected = sel.getSelectedBlocks();
        int[][] faces = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        List<int[]> ops = new ArrayList<>();
        for (long key : selected) {
            int x = SelectionState.unpackX(key), y = SelectionState.unpackY(key), z = SelectionState.unpackZ(key);
            boolean isShell = false;
            for (int[] f : faces) {
                if (!selected.contains(SelectionState.pack(x + f[0], y + f[1], z + f[2]))) {
                    isShell = true;
                    break;
                }
            }
            if (!isShell) {
                ops.add(new int[] { x, y, z, 0, 0 });
            }
        }
        return ops;
    }

    public static List<int[]> fillGapsOps(SelectionState sel, World world, Block fillBlock, int fillMeta) {
        Set<Long> selected = sel.getSelectedBlocks();
        if (selected.isEmpty()) return new ArrayList<>();

        int minX = sel.minX(), maxX = sel.maxX();
        int minY = sel.minY(), maxY = sel.maxY();
        int minZ = sel.minZ(), maxZ = sel.maxZ();

        // Expand bounding box by 1 so flood fill can reach all exterior faces.
        int ox = minX - 1, oy = minY - 1, oz = minZ - 1;
        int ex = maxX + 1, ey = maxY + 1, ez = maxZ + 1;
        int sx = ex - ox + 1, sy = ey - oy + 1, sz = ez - oz + 1;

        boolean[] visited = new boolean[sx * sy * sz];

        Queue<int[]> queue = new LinkedList<>();
        int startIdx = idx(0, 0, 0, sx, sy, sz);
        visited[startIdx] = true;
        queue.add(new int[] { ox, oy, oz });

        int[][] dirs = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int[] d : dirs) {
                int nx = cur[0] + d[0], ny = cur[1] + d[1], nz = cur[2] + d[2];
                if (nx < ox || nx > ex || ny < oy || ny > ey || nz < oz || nz > ez) continue;
                int i = idx(nx - ox, ny - oy, nz - oz, sx, sy, sz);
                if (visited[i]) continue;
                // Don't cross through selected (solid) blocks.
                if (selected.contains(SelectionState.pack(nx, ny, nz))) continue;
                visited[i] = true;
                queue.add(new int[] { nx, ny, nz });
            }
        }

        // Any non-visited, non-selected block inside the bbox that is air in world = enclosed gap.
        List<int[]> ops = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            if (selected.contains(SelectionState.pack(x, y, z))) continue;
            int i = idx(x - ox, y - oy, z - oz, sx, sy, sz);
            if (!visited[i] && world.getBlock(x, y, z) == Blocks.air) {
                ops.add(new int[] { x, y, z, Block.getIdFromBlock(fillBlock), fillMeta });
            }
        }
        return ops;
    }

    private static int idx(int x, int y, int z, int sx, int sy, int sz) {
        return x * sy * sz + y * sz + z;
    }

    public static List<int[]> simulateGravityOps(SelectionState sel, World world) {
        Set<Long> selected = sel.getSelectedBlocks();
        List<int[]> ops = new ArrayList<>();

        // Collect falling blocks sorted by Y ascending (process lowest first so stacks work).
        List<int[]> falling = new ArrayList<>();
        for (long key : selected) {
            int x = SelectionState.unpackX(key), y = SelectionState.unpackY(key), z = SelectionState.unpackZ(key);
            Block b = world.getBlock(x, y, z);
            if (b instanceof BlockFalling && b != Blocks.air) {
                falling.add(new int[] { x, y, z, Block.getIdFromBlock(b), world.getBlockMetadata(x, y, z) });
            }
        }
        falling.sort((a, b) -> Integer.compare(a[1], b[1]));

        // Simulate: track which positions will be air after movement (applied to our ops list).
        java.util.Map<Long, int[]> state = new java.util.HashMap<>();
        for (long key : selected) {
            int x = SelectionState.unpackX(key), y = SelectionState.unpackY(key), z = SelectionState.unpackZ(key);
            Block b = world.getBlock(x, y, z);
            state.put(key, new int[] { Block.getIdFromBlock(b), world.getBlockMetadata(x, y, z) });
        }

        for (int[] fb : falling) {
            int x = fb[0], y = fb[1], z = fb[2];
            int blockId = fb[3], meta = fb[4];

            // Find lowest air position below this block (within selection or world).
            int dropY = y;
            for (int ty = y - 1; ty >= sel.minY() - 1; ty--) {
                long testKey = SelectionState.pack(x, ty, z);
                int[] cur = state.get(testKey);
                int curId = (cur != null) ? cur[0] : Block.getIdFromBlock(world.getBlock(x, ty, z));
                if (curId != 0) break; // Hit something solid.
                dropY = ty;
            }

            if (dropY != y) {
                // Move block down: source becomes air, target gets block.
                state.put(SelectionState.pack(x, y, z), new int[] { 0, 0 });
                state.put(SelectionState.pack(x, dropY, z), new int[] { blockId, meta });
                ops.add(new int[] { x, y, z, 0, 0 });
                ops.add(new int[] { x, dropY, z, blockId, meta });
            }
        }
        return ops;
    }

    public static List<int[]> triggerUpdatesOps(SelectionState sel, World world) {
        List<int[]> ops = new ArrayList<>();
        for (long key : sel.getSelectedBlocks()) {
            int x = SelectionState.unpackX(key), y = SelectionState.unpackY(key), z = SelectionState.unpackZ(key);
            Block b = world.getBlock(x, y, z);
            if (b == Blocks.air) continue;
            if (!b.canBlockStay(world, x, y, z)) {
                ops.add(new int[] { x, y, z, 0, 0 });
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
        Set<Long> claimed = new java.util.HashSet<>();
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

            long posKey = SelectionState.pack(x, y, z);
            if (selected.contains(posKey) && claimed.add(posKey)) {
                ops.add(new int[] { x, y, z, blockId, meta });
            }
        }
        return ops;
    }
}
