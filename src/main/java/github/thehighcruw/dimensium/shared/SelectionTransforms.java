/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared;

import github.thehighcruw.dimensium.editor.tool.brushes.GaussianKernel;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingMath;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingToolState.ModelPoint;
import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.BlockUtils;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.world.World;

public final class SelectionTransforms {

    private SelectionTransforms() {}

    private static final int MAX_SMOOTH_DIM = 256;

    public static Set<Long> expand(Set<Long> blocks, int offset) {
        if (offset <= 0) return new HashSet<>(blocks);
        Set<Long> result = new HashSet<>(blocks);
        Set<Long> frontier = new HashSet<>(blocks);
        for (int step = 0; step < offset; step++) {
            Set<Long> next = new HashSet<>();
            for (long key : frontier) {
                Vec3DInt coord = SelectionState.unpack(key);
                for (Vec3DInt d : BlockUtils.NEIGHBOUR_OFFSETS) {
                    Vec3DInt neighbor = coord.plus(d);
                    if (neighbor.y() < 0 || neighbor.y() > 255) continue;
                    long nk = SelectionState.pack(neighbor);
                    if (result.add(nk)) next.add(nk);
                }
            }
            frontier = next;
        }
        return result;
    }

    public static Set<Long> shrink(Set<Long> blocks, int offset) {
        if (offset <= 0) return new HashSet<>(blocks);
        Set<Long> result = new HashSet<>(blocks);
        for (int step = 0; step < offset; step++) {
            Set<Long> toRemove = new HashSet<>();
            for (long key : result) {
                Vec3DInt coord = SelectionState.unpack(key);
                for (Vec3DInt d : BlockUtils.NEIGHBOUR_OFFSETS) {
                    Vec3DInt neighbor = coord.plus(d);
                    if (neighbor.y() < 0 || neighbor.y() > 255 || !result.contains(SelectionState.pack(neighbor))) {
                        toRemove.add(key);
                        break;
                    }
                }
            }
            result.removeAll(toRemove);
            if (result.isEmpty()) break;
        }
        return result;
    }

    public static Set<Long> distort(Set<Long> blocks, float scale, long seed, float distX, float distY, float distZ) {
        Set<Long> result = new HashSet<>(blocks.size());
        float invScale = scale > 0 ? 1f / scale : 1f;
        for (long key : blocks) {
            Vec3DInt coord = SelectionState.unpack(key);
            Vec3DFloat noisePos = coord.toFloat().times(invScale);
            float[] w = NoiseSampler.warpVec3(noisePos.x(), noisePos.y(), noisePos.z(), seed);
            Vec3DFloat warp = Vec3DFloat.from(w[0] * distX, w[1] * distY, w[2] * distZ);
            Vec3DInt rCoord = Vec3DInt.round(coord.toFloat().plus(warp));
            if (rCoord.y() < 0 || rCoord.y() > 255) continue;
            result.add(SelectionState.pack(rCoord));
        }
        return result;
    }

    /**
     * @param strength  Gaussian sigma (same scale as SmoothToolState.smoothStrength)
     * @param threshold density cutoff [0,1] — voxels above this density are kept
     */
    public static Set<Long> smooth(Set<Long> blocks, int strength, float threshold) {
        if (blocks.isEmpty()) return new HashSet<>();

        BoundingBox bb = SelectionState.computeBounds(blocks);

        GaussianKernel kernel = GaussianKernel.build(strength * 0.5f + 0.5f);
        int margin = kernel.kR;

        Vec3DInt dims = bb.maximum().minus(bb.minimum()).plus(2 * margin + 1);
        if (dims.any((x) -> x > MAX_SMOOTH_DIM)) return new HashSet<>(blocks);

        int snStX = dims.y() * dims.z();
        int[] snap = new int[dims.product()];
        for (long key : blocks) {
            Vec3DInt coord = SelectionState.unpack(key);
            Vec3DInt snapper = coord.minus(bb.minimum()).plus(margin).plus(Vec3DInt.from(snStX, dims.z(), 1));
            snap[snapper.sum()] = 1;
        }

        Set<Long> result = new HashSet<>();
        for (long key : blocks) {
            Vec3DInt coord = SelectionState.unpack(key);
            Vec3DInt localizedCoord = coord.minus(bb.minimum()).plus(margin);
            float density = kernel.solidWeight(snap, localizedCoord, snStX, dims.z()) / kernel.totalWeight;
            if (density >= threshold) result.add(key);
        }
        // Also check non-selected voxels in the bounding box that might grow in
        Vec3DInt.forEachInclusive(Vec3DInt.from(margin), dims.minus(margin + 1), (lx, ly, lz) -> {
            if (snap[lx * snStX + ly * dims.z() + lz] != 0) return; // already handled above
            Vec3DInt localizedCoord = Vec3DInt.from(lx, ly, lz);
            float density = kernel.solidWeight(snap, localizedCoord, snStX, dims.z()) / kernel.totalWeight;
            if (density >= threshold) {
                Vec3DInt world = localizedCoord.minus(margin).plus(bb.minimum());
                if (world.y() >= 0 && world.y() <= 255) result.add(SelectionState.pack(world));
            }
        });
        return result;
    }

    public static Set<Long> filter(
            Set<Long> blocks, World world, Block targetBlock, int targetMeta, boolean keepMatching, boolean exactMeta) {
        Set<Long> result = new HashSet<>();
        for (long key : blocks) {
            Vec3DInt coord = SelectionState.unpack(key);
            Block b = WorldUtils.getBlock(world, coord);
            boolean matches = exactMeta
                    ? (b == targetBlock && WorldUtils.getBlockMetadata(world, coord) == targetMeta)
                    : (b == targetBlock);
            if (matches == keepMatching) result.add(key);
        }
        return result;
    }

    public static Set<Long> filterMultiple(
            Set<Long> blocks,
            World world,
            List<Block> targetBlocks,
            List<Integer> targetMetas,
            boolean keepMatching,
            boolean exactMeta) {
        if (keepMatching) {
            Set<Long> result = new HashSet<>();
            for (int i = 0; i < targetBlocks.size(); i++) {
                result.addAll(filter(blocks, world, targetBlocks.get(i), targetMetas.get(i), true, exactMeta));
            }
            return result;
        } else {
            Set<Long> result = new HashSet<>(blocks);
            for (int i = 0; i < targetBlocks.size(); i++) {
                result.removeAll(filter(blocks, world, targetBlocks.get(i), targetMetas.get(i), true, exactMeta));
            }
            return result;
        }
    }

    public static Set<Long> convexHull(Set<Long> blocks) {
        if (blocks.isEmpty()) return new HashSet<>();

        List<ModelPoint> pts = new ArrayList<>(blocks.size());
        for (long key : blocks) {
            Vec3DInt coord = SelectionState.unpack(key);
            pts.add(new ModelPoint(coord));
        }

        List<int[]> faces = ModellingMath.convexHull3DPublic(pts);
        if (faces.isEmpty()) return new HashSet<>(blocks);

        // Voxelize hull surface
        int[] dummy = {1, 0};
        Map<Long, int[]> surfaceMap = new HashMap<>();
        Vec3DDouble[] P = new Vec3DDouble[pts.size()];
        for (int i = 0; i < pts.size(); i++) P[i] = pts.get(i).pos().toDouble();
        for (int[] f : faces) {
            ModellingMath.voxelizeTriangleDPublic(surfaceMap, P[f[0]], P[f[1]], P[f[2]], dummy);
        }

        // Scan-line fill: for each (x,z) column, fill from min to max Y in surface
        Map<Long, int[]> xzYRange = new HashMap<>();
        for (long key : surfaceMap.keySet()) {
            Vec3DInt c = SelectionState.unpack(key);
            long xzKey = ((long) c.x() << 32) | (c.z() & 0xFFFFFFFFL);
            int[] range = xzYRange.get(xzKey);
            if (range == null) {
                range = new int[] {c.y(), c.y()};
                xzYRange.put(xzKey, range);
            } else {
                if (c.y() < range[0]) range[0] = c.y();
                if (c.y() > range[1]) range[1] = c.y();
            }
        }

        Set<Long> result = new HashSet<>(surfaceMap.keySet());
        for (Map.Entry<Long, int[]> e : xzYRange.entrySet()) {
            long xzKey = e.getKey();
            int x = (int) (xzKey >> 32);
            int z = (int) (xzKey);
            int[] range = e.getValue();
            for (int y = range[0]; y <= range[1]; y++) {
                if (y >= 0 && y <= 255) result.add(SelectionState.pack(Vec3DInt.from(x, y, z)));
            }
        }
        return result;
    }
}
