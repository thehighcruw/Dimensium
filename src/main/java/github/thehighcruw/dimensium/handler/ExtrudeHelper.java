package github.thehighcruw.dimensium.handler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.state.ExtrudeToolState;

@SideOnly(Side.CLIENT)
public class ExtrudeHelper {

    public static final ExtrudeHelper INSTANCE = new ExtrudeHelper();

    public static void applyExtrudeAt(World world, int tx, int ty, int tz, int sideHit) {
        int[] dir = sideToOutwardDir(sideHit);

        Block targetBlock = world.getBlock(tx, ty, tz);
        int targetMeta = world.getBlockMetadata(tx, ty, tz);
        if (targetBlock == Blocks.air) return;

        ExtrudeToolState s = ExtrudeToolState.INSTANCE;
        boolean expand = s.extrudeMode == ExtrudeToolState.ExtrudeMode.EXPAND;
        int count = Math.max(1, s.extrudeCount);

        List<int[]> connected = floodFillFace(
            world,
            tx,
            ty,
            tz,
            dir,
            targetBlock,
            targetMeta,
            s.extrudeLimit,
            s.extrudeCorners);

        List<int[]> ops = new ArrayList<>();
        if (expand) {
            for (int layer = 1; layer <= count; layer++) {
                for (int[] pos : connected) {
                    int nx = pos[0] + dir[0] * layer;
                    int ny = pos[1] + dir[1] * layer;
                    int nz = pos[2] + dir[2] * layer;
                    if (world.getBlock(nx, ny, nz) == Blocks.air) {
                        ops.add(new int[] { nx, ny, nz, Block.getIdFromBlock(targetBlock), targetMeta });
                    }
                }
            }
        } else {
            for (int layer = 0; layer < count; layer++) {
                for (int[] pos : connected) {
                    int rx = pos[0] - dir[0] * layer;
                    int ry = pos[1] - dir[1] * layer;
                    int rz = pos[2] - dir[2] * layer;
                    if (world.getBlock(rx, ry, rz) != Blocks.air) {
                        ops.add(new int[] { rx, ry, rz, 0, 0 });
                    }
                }
            }
            if (s.extrudeDisplace) {
                for (int[] pos : connected) {
                    int lx = pos[0] - dir[0] * (count - 1);
                    int ly = pos[1] - dir[1] * (count - 1);
                    int lz = pos[2] - dir[2] * (count - 1);
                    int bx = pos[0] - dir[0] * count;
                    int by = pos[1] - dir[1] * count;
                    int bz = pos[2] - dir[2] * count;
                    if (world.getBlock(lx, ly, lz) != Blocks.air && world.getBlock(bx, by, bz) == Blocks.air) {
                        ops.add(new int[] { bx, by, bz, Block.getIdFromBlock(targetBlock), targetMeta });
                    }
                }
            }
        }

        if (!ops.isEmpty()) BlockSender.sendChunked(
            ops,
            StatCollector.translateToLocal("dimensium.action.extrude") + " ("
                + StatCollector.translateToLocal(s.extrudeMode.label)
                + ")");
    }

    public static List<int[]> floodFillFace(World world, int sx, int sy, int sz, int[] outDir, Block matchBlock,
        int matchMeta, int limit, boolean corners) {
        int[][] perp = perpAxes(outDir);

        Set<Long> visited = new HashSet<>();
        Queue<int[]> queue = new LinkedList<>();
        List<int[]> result = new ArrayList<>();

        queue.add(new int[] { sx, sy, sz });
        visited.add(extrudeKey(sx, sy, sz));

        while (!queue.isEmpty() && result.size() < limit) {
            int[] cur = queue.poll();
            result.add(cur);

            int[][] steps = corners ? diagonalSteps(perp) : orthogonalSteps(perp);

            for (int[] step : steps) {
                int nx = cur[0] + step[0];
                int ny = cur[1] + step[1];
                int nz = cur[2] + step[2];
                long k = extrudeKey(nx, ny, nz);
                if (visited.contains(k)) continue;
                visited.add(k);
                if (world.getBlock(nx, ny, nz) == matchBlock && world.getBlockMetadata(nx, ny, nz) == matchMeta
                    && world.getBlock(nx + outDir[0], ny + outDir[1], nz + outDir[2]) == Blocks.air) {
                    queue.add(new int[] { nx, ny, nz });
                }
            }
        }

        return result;
    }

    private static int[][] orthogonalSteps(int[][] perp) {
        return new int[][] { { perp[0][0], perp[0][1], perp[0][2] }, { -perp[0][0], -perp[0][1], -perp[0][2] },
            { perp[1][0], perp[1][1], perp[1][2] }, { -perp[1][0], -perp[1][1], -perp[1][2] } };
    }

    private static int[][] diagonalSteps(int[][] perp) {
        int[][] ortho = orthogonalSteps(perp);
        return new int[][] { ortho[0], ortho[1], ortho[2], ortho[3],
            { perp[0][0] + perp[1][0], perp[0][1] + perp[1][1], perp[0][2] + perp[1][2] },
            { perp[0][0] - perp[1][0], perp[0][1] - perp[1][1], perp[0][2] - perp[1][2] },
            { -perp[0][0] + perp[1][0], -perp[0][1] + perp[1][1], -perp[0][2] + perp[1][2] },
            { -perp[0][0] - perp[1][0], -perp[0][1] - perp[1][1], -perp[0][2] - perp[1][2] } };
    }

    public static int[] sideToOutwardDir(int side) {
        return switch (side) {
            case 0 -> new int[] { 0, -1, 0 };
            case 1 -> new int[] { 0, 1, 0 };
            case 2 -> new int[] { 0, 0, -1 };
            case 3 -> new int[] { 0, 0, 1 };
            case 4 -> new int[] { -1, 0, 0 };
            case 5 -> new int[] { 1, 0, 0 };
            default -> throw new RuntimeException("Unknown side: " + side);
        };
    }

    public static int[][] perpAxes(int[] dir) {
        if (dir[1] != 0) return new int[][] { { 1, 0, 0 }, { 0, 0, 1 } };
        if (dir[2] != 0) return new int[][] { { 1, 0, 0 }, { 0, 1, 0 } };
        return new int[][] { { 0, 1, 0 }, { 0, 0, 1 } };
    }

    public static long extrudeKey(int x, int y, int z) {
        return ((long) (x + 30000000)) << 34 | ((long) (y & 0xFF)) << 26 | (long) (z + 30000000);
    }

    private int lastExtrudeX = Integer.MIN_VALUE;
    private int lastExtrudeY = Integer.MIN_VALUE;
    private int lastExtrudeZ = Integer.MIN_VALUE;
    private int lastExtrudeSide = -1;

    public void resetExtrudeDedup() {
        lastExtrudeX = Integer.MIN_VALUE;
    }

    /** Builds the extrude preview proposal for the block under the cursor. Called each render frame. */
    public void buildExtrudeProposal(Minecraft mc) {
        FreecamState fs = FreecamState.INSTANCE;
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(
            mc,
            mc.displayWidth,
            mc.displayHeight);
        MovingObjectPosition mop = GuiDimensiumOverlay
            .raycastFromMouse((int) fs.cursorX, (int) fs.cursorY, sr.getScaledWidth(), sr.getScaledHeight());

        BuilderToolState bts = BuilderToolState.INSTANCE;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            bts.extrudePreview = null;
            lastExtrudeX = Integer.MIN_VALUE;
            return;
        }

        if (mop.blockX == lastExtrudeX && mop.blockY == lastExtrudeY
            && mop.blockZ == lastExtrudeZ
            && mop.sideHit == lastExtrudeSide) return;
        lastExtrudeX = mop.blockX;
        lastExtrudeY = mop.blockY;
        lastExtrudeZ = mop.blockZ;
        lastExtrudeSide = mop.sideHit;

        int tx = mop.blockX, ty = mop.blockY, tz = mop.blockZ;
        int[] dir = sideToOutwardDir(mop.sideHit);
        Block targetBlock = mc.theWorld.getBlock(tx, ty, tz);
        if (targetBlock == Blocks.air) {
            bts.extrudePreview = null;
            return;
        }
        int targetMeta = mc.theWorld.getBlockMetadata(tx, ty, tz);

        ExtrudeToolState s = ExtrudeToolState.INSTANCE;
        int count = Math.max(1, s.extrudeCount);
        boolean expand = s.extrudeMode == ExtrudeToolState.ExtrudeMode.EXPAND;

        List<int[]> connected = floodFillFace(
            mc.theWorld,
            tx,
            ty,
            tz,
            dir,
            targetBlock,
            targetMeta,
            Math.min(s.extrudeLimit, 4096),
            s.extrudeCorners);

        ChangeProposal p = ChangeProposal.forPreview();
        int targetId = Block.getIdFromBlock(targetBlock);

        if (expand) {
            for (int layer = 1; layer <= count; layer++) {
                for (int[] pos : connected) {
                    int nx = pos[0] + dir[0] * layer;
                    int ny = pos[1] + dir[1] * layer;
                    int nz = pos[2] + dir[2] * layer;
                    if (mc.theWorld.getBlock(nx, ny, nz) == Blocks.air)
                        p.proposed.put(ChangeProposal.packKey(nx, ny, nz), new int[] { targetId, targetMeta });
                }
            }
        } else {
            for (int layer = 0; layer < count; layer++) {
                for (int[] pos : connected) {
                    int px = pos[0] - dir[0] * layer;
                    int py = pos[1] - dir[1] * layer;
                    int pz = pos[2] - dir[2] * layer;
                    if (mc.theWorld.getBlock(px, py, pz) != Blocks.air)
                        p.proposed.put(ChangeProposal.packKey(px, py, pz), new int[] { 0, 0 });
                }
            }
            if (s.extrudeDisplace) {
                for (int[] pos : connected) {
                    int lx = pos[0] - dir[0] * (count - 1);
                    int ly = pos[1] - dir[1] * (count - 1);
                    int lz = pos[2] - dir[2] * (count - 1);
                    int bx = pos[0] - dir[0] * count;
                    int by = pos[1] - dir[1] * count;
                    int bz = pos[2] - dir[2] * count;
                    if (mc.theWorld.getBlock(lx, ly, lz) != Blocks.air
                        && mc.theWorld.getBlock(bx, by, bz) == Blocks.air)
                        p.proposed.put(ChangeProposal.packKey(bx, by, bz), new int[] { targetId, targetMeta });
                }
            }
        }

        bts.extrudePreview = p;
    }
}
