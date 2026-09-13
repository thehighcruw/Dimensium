/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.editor.tool.NoiseSampler;
import github.thehighcruw.dimensium.editor.tool.brushes.GaussianKernel;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingMath;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingToolState.ModelPoint;

public final class SelectionTransforms {

    private SelectionTransforms() {}

    private static final int MAX_SMOOTH_DIM = 256;

    public static Set<Long> move(Set<Long> blocks, int dx, int dy, int dz) {
        Set<Long> result = new HashSet<>(blocks.size());
        for (long key : blocks) {
            int x = SelectionState.unpackX(key) + dx;
            int y = SelectionState.unpackY(key) + dy;
            int z = SelectionState.unpackZ(key) + dz;
            if (y < 0 || y > 255) continue;
            result.add(SelectionState.pack(x, y, z));
        }
        return result;
    }

    public static Set<Long> expand(Set<Long> blocks, int offset) {
        if (offset <= 0) return new HashSet<>(blocks);
        Set<Long> result = new HashSet<>(blocks);
        int[] dx = { 1, -1, 0, 0, 0, 0 };
        int[] dy = { 0, 0, 1, -1, 0, 0 };
        int[] dz = { 0, 0, 0, 0, 1, -1 };
        Set<Long> frontier = new HashSet<>(blocks);
        for (int step = 0; step < offset; step++) {
            Set<Long> next = new HashSet<>();
            for (long key : frontier) {
                int x = SelectionState.unpackX(key);
                int y = SelectionState.unpackY(key);
                int z = SelectionState.unpackZ(key);
                for (int d = 0; d < 6; d++) {
                    int ny = y + dy[d];
                    if (ny < 0 || ny > 255) continue;
                    long nk = SelectionState.pack(x + dx[d], ny, z + dz[d]);
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
        int[] dx = { 1, -1, 0, 0, 0, 0 };
        int[] dy = { 0, 0, 1, -1, 0, 0 };
        int[] dz = { 0, 0, 0, 0, 1, -1 };
        for (int step = 0; step < offset; step++) {
            Set<Long> toRemove = new HashSet<>();
            for (long key : result) {
                int x = SelectionState.unpackX(key);
                int y = SelectionState.unpackY(key);
                int z = SelectionState.unpackZ(key);
                for (int d = 0; d < 6; d++) {
                    int ny = y + dy[d];
                    if (ny < 0 || ny > 255 || !result.contains(SelectionState.pack(x + dx[d], ny, z + dz[d]))) {
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
            int x = SelectionState.unpackX(key);
            int y = SelectionState.unpackY(key);
            int z = SelectionState.unpackZ(key);
            float nx = x * invScale, ny = y * invScale, nz = z * invScale;
            float wx = NoiseSampler.rawSimplex3(nx, ny, nz, seed);
            float wy = NoiseSampler.rawSimplex3(nx + 31.7f, ny + 17.3f, nz + 53.1f, seed);
            float wz = NoiseSampler.rawSimplex3(nx + 67.9f, ny + 83.5f, nz + 11.3f, seed);
            int rx = (int) Math.round(x + wx * distX);
            int ry = (int) Math.round(y + wy * distY);
            int rz = (int) Math.round(z + wz * distZ);
            if (ry < 0 || ry > 255) continue;
            result.add(SelectionState.pack(rx, ry, rz));
        }
        return result;
    }

    /**
     * @param strength  Gaussian sigma (same scale as SmoothToolState.smoothStrength)
     * @param threshold density cutoff [0,1] — voxels above this density are kept
     */
    public static Set<Long> smooth(Set<Long> blocks, int strength, float threshold) {
        if (blocks.isEmpty()) return new HashSet<>();

        int mnX = Integer.MAX_VALUE, mnY = Integer.MAX_VALUE, mnZ = Integer.MAX_VALUE;
        int mxX = Integer.MIN_VALUE, mxY = Integer.MIN_VALUE, mxZ = Integer.MIN_VALUE;
        for (long key : blocks) {
            int x = SelectionState.unpackX(key), y = SelectionState.unpackY(key), z = SelectionState.unpackZ(key);
            if (x < mnX) mnX = x;
            if (x > mxX) mxX = x;
            if (y < mnY) mnY = y;
            if (y > mxY) mxY = y;
            if (z < mnZ) mnZ = z;
            if (z > mxZ) mxZ = z;
        }

        GaussianKernel kernel = GaussianKernel.build(strength * 0.5f + 0.5f);
        int margin = kernel.kR;
        int dimX = (mxX - mnX) + 2 * margin + 1;
        int dimY = (mxY - mnY) + 2 * margin + 1;
        int dimZ = (mxZ - mnZ) + 2 * margin + 1;
        if (dimX > MAX_SMOOTH_DIM || dimY > MAX_SMOOTH_DIM || dimZ > MAX_SMOOTH_DIM) return new HashSet<>(blocks);
        int snStX = dimY * dimZ, snStY = dimZ;

        int[] snap = new int[dimX * dimY * dimZ];
        for (long key : blocks) {
            int x = SelectionState.unpackX(key) - mnX + margin;
            int y = SelectionState.unpackY(key) - mnY + margin;
            int z = SelectionState.unpackZ(key) - mnZ + margin;
            snap[x * snStX + y * snStY + z] = 1;
        }

        Set<Long> result = new HashSet<>();
        for (long key : blocks) {
            int lx = SelectionState.unpackX(key) - mnX + margin;
            int ly = SelectionState.unpackY(key) - mnY + margin;
            int lz = SelectionState.unpackZ(key) - mnZ + margin;
            float density = kernel.solidWeight(snap, lx, ly, lz, snStX, snStY) / kernel.totalWeight;
            if (density >= threshold) result.add(key);
        }
        // Also check non-selected voxels in the bounding box that might grow in
        for (int lx = margin; lx < dimX - margin; lx++) {
            for (int ly = margin; ly < dimY - margin; ly++) {
                for (int lz = margin; lz < dimZ - margin; lz++) {
                    if (snap[lx * snStX + ly * snStY + lz] != 0) continue; // already handled above
                    float density = kernel.solidWeight(snap, lx, ly, lz, snStX, snStY) / kernel.totalWeight;
                    if (density >= threshold) {
                        int wx = lx - margin + mnX;
                        int wy = ly - margin + mnY;
                        int wz = lz - margin + mnZ;
                        if (wy >= 0 && wy <= 255) result.add(SelectionState.pack(wx, wy, wz));
                    }
                }
            }
        }
        return result;
    }

    public static Set<Long> filter(Set<Long> blocks, World world, Block targetBlock, int targetMeta,
        boolean keepMatching, boolean exactMeta) {
        Set<Long> result = new HashSet<>();
        for (long key : blocks) {
            int x = SelectionState.unpackX(key);
            int y = SelectionState.unpackY(key);
            int z = SelectionState.unpackZ(key);
            Block b = world.getBlock(x, y, z);
            boolean matches = exactMeta ? (b == targetBlock && world.getBlockMetadata(x, y, z) == targetMeta)
                : (b == targetBlock);
            if (matches == keepMatching) result.add(key);
        }
        return result;
    }

    public static Set<Long> convexHull(Set<Long> blocks) {
        if (blocks.isEmpty()) return new HashSet<>();

        List<ModelPoint> pts = new ArrayList<>(blocks.size());
        for (long key : blocks) {
            pts.add(
                new ModelPoint(SelectionState.unpackX(key), SelectionState.unpackY(key), SelectionState.unpackZ(key)));
        }

        List<int[]> faces = ModellingMath.convexHull3DPublic(pts);
        if (faces.isEmpty()) return new HashSet<>(blocks);

        // Voxelize hull surface
        int[] dummy = { 1, 0 };
        Map<Long, int[]> surfaceMap = new HashMap<>();
        double[][] P = new double[pts.size()][3];
        for (int i = 0; i < pts.size(); i++) {
            P[i][0] = pts.get(i).x;
            P[i][1] = pts.get(i).y;
            P[i][2] = pts.get(i).z;
        }
        for (int[] f : faces) {
            ModellingMath.voxelizeTriangleDPublic(surfaceMap, P[f[0]], P[f[1]], P[f[2]], dummy);
        }

        // Scan-line fill: for each (x,z) column, fill from min to max Y in surface
        Map<Long, int[]> xzYRange = new HashMap<>();
        for (long key : surfaceMap.keySet()) {
            int x = SelectionState.unpackX(key);
            int y = SelectionState.unpackY(key);
            int z = SelectionState.unpackZ(key);
            long xzKey = ((long) x << 32) | (z & 0xFFFFFFFFL);
            int[] range = xzYRange.get(xzKey);
            if (range == null) {
                range = new int[] { y, y };
                xzYRange.put(xzKey, range);
            } else {
                if (y < range[0]) range[0] = y;
                if (y > range[1]) range[1] = y;
            }
        }

        Set<Long> result = new HashSet<>(surfaceMap.keySet());
        for (Map.Entry<Long, int[]> e : xzYRange.entrySet()) {
            long xzKey = e.getKey();
            int x = (int) (xzKey >> 32);
            int z = (int) (xzKey);
            int[] range = e.getValue();
            for (int y = range[0]; y <= range[1]; y++) {
                if (y >= 0 && y <= 255) result.add(SelectionState.pack(x, y, z));
            }
        }
        return result;
    }
}
