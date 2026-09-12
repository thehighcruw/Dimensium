package github.thehighcruw.dimensium.tool.brushes;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.tool.state.BrushShape;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.ReplaceMode;

public final class BrushUtil {

    private BrushUtil() {}

    static final int[][] FACE_DIRS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    // MC 1.7.10 sideHit: 0=bottom, 1=top, 2=north, 3=south, 4=west, 5=east
    private static final int[][] SIDE_NORMALS = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 },
        { 1, 0, 0 } };

    public static int[] faceNormal(int sideHit) {
        return sideHit >= 0 && sideHit < 6 ? SIDE_NORMALS[sideHit] : SIDE_NORMALS[1];
    }

    @FunctionalInterface
    public interface VoxelAction {

        void run(int dx, int dy, int dz);
    }

    public static boolean hasAirNeighbor(World world, int wx, int wy, int wz) {
        for (int[] n : FACE_DIRS) {
            if (world.getBlock(wx + n[0], wy + n[1], wz + n[2]) == Blocks.air) return true;
        }
        return false;
    }

    public static void forBrush(BrushState s, VoxelAction action) {
        int r = s.brushRadius, h = s.brushShape.hasHeight ? s.brushHeight : r;
        for (int dx = -r; dx <= r; dx++) for (int dy = -h; dy <= h; dy++) for (int dz = -r; dz <= r; dz++) {
            if (!inShape(s.brushShape, dx, dy, dz, r, h, r)) continue;
            if (s.hollow && isInterior(s.brushShape, dx, dy, dz, r, h, r)) continue;
            action.run(dx, dy, dz);
        }
    }

    public static void forBrush(BrushState s, int sx, int sy, int sz, VoxelAction action) {
        for (int dx = -sx; dx <= sx; dx++) for (int dy = -sy; dy <= sy; dy++) for (int dz = -sz; dz <= sz; dz++) {
            if (!inShape(s.brushShape, dx, dy, dz, sx, sy, sz)) continue;
            if (s.hollow && isInterior(s.brushShape, dx, dy, dz, sx, sy, sz)) continue;
            action.run(dx, dy, dz);
        }
    }

    public static boolean canReplace(Block existing, Block target, ReplaceMode mode) {
        return switch (mode) {
            case AIR_ONLY -> existing == Blocks.air;
            case SOLID_ONLY -> existing != Blocks.air;
            case SAME_BLOCK -> existing == target;
            case ANY -> true;
        };
    }

    // Epsilon absorbs float rounding at exact-boundary points (e.g. dx=2,dy=2,dz=1,r=3
    // gives 4/9+4/9+1/9 = 1.0 mathematically but ~1.0000002f in float).
    private static final float GEOM_EPS = 1e-6f;

    public static boolean inShape(BrushShape shape, int dx, int dy, int dz, int sx, int sy, int sz) {
        switch (shape) {
            case SPHERE: {
                float ex = (float) dx / sx, ey = (float) dy / sy, ez = (float) dz / sz;
                return ex * ex + ey * ey + ez * ez <= 1f + GEOM_EPS;
            }
            case CUBE:
                return true;
            case CUBOID:
                return true;
            case CYLINDER:
                return (float) dx * dx / (sx * sx) + (float) dz * dz / (sx * sx) <= 1f + GEOM_EPS;
            case CAPSULE: {
                int capH = Math.max(0, sy - sx);
                float r2 = sx * sx;
                float xz2 = dx * dx + dz * dz;
                if (Math.abs(dy) <= capH) return xz2 <= r2;
                float oy = Math.abs(dy) - capH;
                return xz2 + oy * oy <= r2;
            }
            case CONE: {
                float level = (float) (dy + sy) / (2f * sy);
                float r = sx * (1f - level);
                if (r <= 0) return dx == 0 && dz == 0;
                return (float) dx * dx / (r * r) + (float) dz * dz / (r * r) <= 1f + GEOM_EPS;
            }
            case ELLIPSOID: {
                float ex = (float) dx / sx, ey = (float) dy / sy, ez = (float) dz / sz;
                return ex * ex + ey * ey + ez * ez <= 1f + GEOM_EPS;
            }
            case OCTAHEDRON:
                return (float) Math.abs(dx) / sx + (float) Math.abs(dy) / sy + (float) Math.abs(dz) / sz
                    <= 1f + GEOM_EPS;
            default:
                return true;
        }
    }

    public static boolean isInterior(BrushShape shape, int dx, int dy, int dz, int sx, int sy, int sz) {
        int isx = Math.max(1, sx - 1), isy = Math.max(1, sy - 1), isz = Math.max(1, sz - 1);
        switch (shape) {
            case SPHERE: {
                float ex = (float) dx / isx, ey = (float) dy / isy, ez = (float) dz / isz;
                return ex * ex + ey * ey + ez * ez < 1f;
            }
            case CUBE:
                return Math.abs(dx) < sx && Math.abs(dy) < sy && Math.abs(dz) < sz;
            case CUBOID:
                return Math.abs(dx) < sx && Math.abs(dy) < sy && Math.abs(dz) < sz;
            case CYLINDER:
                return (float) dx * dx / (isx * isx) + (float) dz * dz / (isx * isx) < 1f && Math.abs(dy) < sy;
            case CAPSULE: {
                int capH = Math.max(0, isy - isx);
                float r2 = isx * isx;
                float xz2 = dx * dx + dz * dz;
                if (Math.abs(dy) <= capH) return xz2 < r2;
                float oy = Math.abs(dy) - capH;
                return xz2 + oy * oy < r2;
            }
            case CONE: {
                float level = (float) (dy + isy) / (2f * isy);
                float r = isx * (1f - level);
                if (r <= 0) return false;
                return (float) dx * dx / (r * r) + (float) dz * dz / (r * r) < 1f;
            }
            case ELLIPSOID: {
                float ex = (float) dx / isx, ey = (float) dy / isy, ez = (float) dz / isz;
                return ex * ex + ey * ey + ez * ez < 1f;
            }
            case OCTAHEDRON:
                return (float) Math.abs(dx) / isx + (float) Math.abs(dy) / isy + (float) Math.abs(dz) / isz < 1f;
            default:
                return false;
        }
    }

    static int[] snapshotBlockIds(World world, int ox, int oy, int oz, int sx, int sy, int sz, int margin) {
        int dimX = 2 * (sx + margin) + 1;
        int dimY = 2 * (sy + margin) + 1;
        int dimZ = 2 * (sz + margin) + 1;
        int snStY = dimZ, snStX = dimY * dimZ;
        int[] snap = new int[dimX * dimY * dimZ];
        int worldMinY = 0, worldMaxY = world.getHeight() - 1;
        for (int dx = -(sx + margin); dx <= sx + margin; dx++) {
            int ix = dx + sx + margin;
            for (int dy = -(sy + margin); dy <= sy + margin; dy++) {
                int wy = oy + dy;
                int idx0 = ix * snStX + (dy + sy + margin) * snStY;
                for (int dz = -(sz + margin); dz <= sz + margin; dz++) {
                    int idx = idx0 + (dz + sz + margin);
                    if (wy < worldMinY) snap[idx] = -1;
                    else if (wy > worldMaxY) snap[idx] = 0;
                    else snap[idx] = Block.getIdFromBlock(world.getBlock(ox + dx, wy, oz + dz));
                }
            }
        }
        return snap;
    }
}
