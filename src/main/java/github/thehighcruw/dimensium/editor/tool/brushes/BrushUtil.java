/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.brushes;

import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.shared.math.Vec2DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.BlockUtils;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.function.Consumer;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class BrushUtil {

    private BrushUtil() {}

    public static void writeFromItem(World world, Vec3DInt pos, ItemStack item) {
        if (item == null) return;
        Block blk = Block.getBlockFromItem(item.getItem());
        int meta = item.getItemDamage();
        if (blk != null && blk != Blocks.air) ChangeProposal.write(world, pos, blk, meta);
    }

    public static Vec3DInt faceNormal(int sideHit) {
        return sideHit >= 0 && sideHit < 6 ? BlockUtils.NEIGHBOUR_OFFSETS[sideHit] : BlockUtils.NEIGHBOUR_OFFSETS[1];
    }

    @FunctionalInterface
    public interface VoxelAction {

        void run(Vec3DInt pos);
    }

    public static boolean hasSolidNeighbor(World world, Vec3DInt coord) {
        for (Vec3DInt offset : BlockUtils.NEIGHBOUR_OFFSETS) {
            if (WorldUtils.getBlock(world, coord.plus(offset)) == Blocks.air) return false;
        }
        return true;
    }

    public static void forBrush(BrushState s, VoxelAction action) {
        int r = s.brushRadius, h = s.brushShape.hasHeight ? s.brushHeight : r;
        forBrush(s, Vec3DInt.from(r, h, r), action);
    }

    public static void forBrush(BrushState s, Vec3DInt brushSize, VoxelAction action) {
        Vec3DInt.forEachInclusive(brushSize.negate(), brushSize, (dx, dy, dz) -> {
            Vec3DInt offset = Vec3DInt.from(dx, dy, dz);
            if (!inShape(s.brushShape, offset, brushSize)) return;
            if (s.hollow && isInterior(s.brushShape, offset, brushSize)) return;
            action.run(offset);
        });
    }

    // Epsilon absorbs float rounding at exact-boundary points (e.g. dx=2,dy=2,dz=1,r=3
    // gives 4/9+4/9+1/9 = 1.0 mathematically but ~1.0000002f in float).
    private static final float GEOM_EPS = 1e-6f;

    public static void forEachInShape(BrushShape shape, Vec3DInt brushSize, Consumer<Vec3DInt> action) {
        Vec3DInt.forEachInclusive(brushSize.times(-1), brushSize, (dx, dy, dz) -> {
            Vec3DInt offset = Vec3DInt.from(dx, dy, dz);
            if (inShape(shape, offset, brushSize)) action.accept(offset);
        });
    }

    public static boolean inShape(BrushShape shape, Vec3DInt offset, Vec3DInt brushSize) {
        float thr = DimensiumConfig.shapeThreshold;
        switch (shape) {
            case SPHERE, ELLIPSOID -> {
                Vec3DFloat normalized = offset.toFloat().divide(brushSize.toFloat());
                Vec3DFloat invSize = Vec3DFloat.ONE.divide(brushSize.toFloat());
                float distance = normalized.dot(normalized);
                float vR = 0.5f * (float) Math.sqrt(invSize.dot(invSize));
                return (float) Math.sqrt(distance) <= 1f - vR * (1f - thr) + GEOM_EPS;
            }
            case CUBE, CUBOID -> {
                return offset.inBounds(brushSize.negate(), brushSize);
            }
            case CYLINDER -> {
                int sx = brushSize.x();
                Vec2DFloat xz = Vec2DFloat.from(offset.x(), offset.z()).divide(sx);
                float vR = 0.5f * Vec2DFloat.from(1f / sx, 1f / sx).length();
                return xz.length() <= 1f - vR * (1f - thr) + GEOM_EPS;
            }
            case CAPSULE -> {
                int sx = brushSize.x(), sy = brushSize.y();
                int capH = Math.max(0, sy - sx);
                float vR = 0.5f / sx;
                float cutoff = 1f - vR * (1f - thr) + GEOM_EPS;
                float r = sx * cutoff;
                float xz2 = Vec2DFloat.from(offset.x(), offset.z()).lengthSq();
                if (Math.abs(offset.y()) <= capH) return xz2 <= r * r;
                float capOy = Math.abs(offset.y()) - capH;
                return xz2 + capOy * capOy <= r * r;
            }
            case CONE -> {
                int sx = brushSize.x(), sy = brushSize.y();
                float level = (float) (offset.y() + sy) / (2f * sy);
                float r = sx * (1f - level);
                if (r <= 0) return offset.x() == 0 && offset.z() == 0;
                Vec2DFloat xz = Vec2DFloat.from(offset.x(), offset.z()).divide(r);
                float vR = 0.5f * Vec2DFloat.from(1f / r, 1f / r).length();
                return xz.length() <= 1f - vR * (1f - thr) + GEOM_EPS;
            }
            case OCTAHEDRON -> {
                float norm = offset.toFloat().abs().divide(brushSize.toFloat()).sum();
                float vR = 0.5f * Vec3DFloat.ONE.divide(brushSize.toFloat()).sum();
                return norm <= 1f - vR * (1f - thr) + GEOM_EPS;
            }
            default -> {
                return true;
            }
        }
    }

    public static boolean isInterior(BrushShape shape, Vec3DInt offset, Vec3DInt brushSize) {
        Vec3DInt inner = brushSize.minus(1).max(Vec3DInt.ONE);
        switch (shape) {
            case SPHERE:
            case ELLIPSOID: {
                Vec3DFloat n = offset.toFloat().divide(inner.toFloat());
                return n.dot(n) < 1f;
            }
            case CUBE:
            case CUBOID:
                return offset.abs().inBounds(Vec3DInt.ZERO, inner);
            case CYLINDER: {
                int isx = inner.x();
                return Vec2DFloat.from(offset.x(), offset.z()).divide(isx).lengthSq() < 1f
                        && Math.abs(offset.y()) < brushSize.y();
            }
            case CAPSULE: {
                int isx = inner.x(), isy = inner.y();
                int capH = Math.max(0, isy - isx);
                float r2 = isx * isx;
                float xz2 = Vec2DFloat.from(offset.x(), offset.z()).lengthSq();
                if (Math.abs(offset.y()) <= capH) return xz2 < r2;
                float capOy = Math.abs(offset.y()) - capH;
                return xz2 + capOy * capOy < r2;
            }
            case CONE: {
                int isx = inner.x(), isy = inner.y();
                float level = (float) (offset.y() + isy) / (2f * isy);
                float r = isx * (1f - level);
                if (r <= 0) return false;
                return Vec2DFloat.from(offset.x(), offset.z()).divide(r).lengthSq() < 1f;
            }
            case OCTAHEDRON:
                return offset.toFloat().abs().divide(inner.toFloat()).sum() < 1f;
            default:
                return false;
        }
    }

    public static int[] snapshotBlockIds(World world, Vec3DInt origin, Vec3DInt brushSize, int margin) {
        Vec3DInt snapHalf = brushSize.plus(margin);
        Vec3DInt dims = snapHalf.times(2).plus(1);
        int snStX = dims.y() * dims.z();
        int[] snap = new int[dims.product()];
        int worldMinY = 0, worldMaxY = world.getHeight() - 1;
        Vec3DInt.forEachInclusive(snapHalf.negate(), snapHalf, (dx, dy, dz) -> {
            int idx = Vec3DInt.from(dx, dy, dz).plus(snapHalf).toIndex(snStX, dims.z());
            Vec3DInt worldPos = origin.plus(dx, dy, dz);
            if (worldPos.y() < worldMinY) snap[idx] = -1;
            else if (worldPos.y() > worldMaxY) snap[idx] = 0;
            else snap[idx] = Block.getIdFromBlock(WorldUtils.getBlock(world, worldPos));
        });
        return snap;
    }
}
