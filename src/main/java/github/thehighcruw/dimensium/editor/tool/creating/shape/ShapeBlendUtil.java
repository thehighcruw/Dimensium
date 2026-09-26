/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.shape;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class ShapeBlendUtil {

    private ShapeBlendUtil() {}

    /**
     * 26-connected neighbor offsets and their fixed-point (×10) Euclidean-approximate costs:
     * face neighbors cost 10 (≈1.0), edge neighbors 14 (≈√2), corner neighbors 17 (≈√3).
     */
    private static final int[][] NEIGHBOR_OFFSETS;

    private static final int[] NEIGHBOR_COSTS;

    static {
        // 3×3×3 cube minus the center = 26 neighbors.
        NEIGHBOR_OFFSETS = new int[26][3];
        NEIGHBOR_COSTS = new int[26];
        int index = 0;
        for (int deltaX = -1; deltaX <= 1; deltaX++) {
            for (int deltaY = -1; deltaY <= 1; deltaY++) {
                for (int deltaZ = -1; deltaZ <= 1; deltaZ++) {
                    if (deltaX == 0 && deltaY == 0 && deltaZ == 0) continue;
                    NEIGHBOR_OFFSETS[index][0] = deltaX;
                    NEIGHBOR_OFFSETS[index][1] = deltaY;
                    NEIGHBOR_OFFSETS[index][2] = deltaZ;
                    int manhattan = Math.abs(deltaX) + Math.abs(deltaY) + Math.abs(deltaZ);
                    NEIGHBOR_COSTS[index] = manhattan == 1 ? 10 : manhattan == 2 ? 14 : 17;
                    index++;
                }
            }
        }
    }

    /**
     * Returns air positions where the combined quadratic metaball field
     * {@code (1-distToShape/R)² + (1-distToTerrain/R)² >= 1}
     * exceeds the blend threshold — positions that form the smooth neck between shape and terrain.
     */
    public static List<Vec3DInt> computeBlendPositions(
            Set<Vec3DInt> shapeVoxels, Set<Vec3DInt> terrain, Vec3DInt blendMin, Vec3DInt blendMax, int blendRadius) {
        int sizeX = blendMax.x() - blendMin.x() + 1;
        int sizeY = blendMax.y() - blendMin.y() + 1;
        int sizeZ = blendMax.z() - blendMin.z() + 1;

        int[] distToShape = euclidDistanceTransform(shapeVoxels, blendMin, sizeX, sizeY, sizeZ, blendRadius);
        int[] distToTerrain = euclidDistanceTransform(terrain, blendMin, sizeX, sizeY, sizeZ, blendRadius);

        // Radius in fixed-point ×10.
        int radiusScaled = blendRadius * 10;

        List<Vec3DInt> result = new ArrayList<>();
        for (int localX = 0; localX < sizeX; localX++) {
            for (int localY = 0; localY < sizeY; localY++) {
                for (int localZ = 0; localZ < sizeZ; localZ++) {
                    int cellIndex = localX * sizeY * sizeZ + localY * sizeZ + localZ;
                    int distShape = distToShape[cellIndex];
                    int distTerrain = distToTerrain[cellIndex];
                    // Skip positions already inside shape or terrain (distance = 0).
                    if (distShape == 0 || distTerrain == 0) continue;
                    // Skip if outside either influence radius.
                    if (distShape > radiusScaled || distTerrain > radiusScaled) continue;
                    // Quadratic metaball field: f(d) = (1 - d/R)². Fill if f_shape + f_terrain >= 1.
                    // Working in fixed-point to avoid float per voxel: scale numerators by R².
                    // f_shape = (radiusScaled - distShape)² / radiusScaled²
                    // f_terrain = (radiusScaled - distTerrain)² / radiusScaled²
                    // Fill if (radiusScaled - distShape)² + (radiusScaled - distTerrain)² >= radiusScaled²
                    long shapeInfluence = (long) (radiusScaled - distShape);
                    long terrainInfluence = (long) (radiusScaled - distTerrain);
                    if (shapeInfluence * shapeInfluence + terrainInfluence * terrainInfluence
                            < (long) radiusScaled * radiusScaled) continue;
                    result.add(Vec3DInt.from(blendMin.x() + localX, blendMin.y() + localY, blendMin.z() + localZ));
                }
            }
        }
        return result;
    }

    /**
     * Bucket-queue Dijkstra distance transform with 26-connectivity and fixed-point (×10) Euclidean-approximate costs.
     */
    private static int[] euclidDistanceTransform(
            Set<Vec3DInt> seeds, Vec3DInt origin, int sizeX, int sizeY, int sizeZ, int maxDistance) {
        int maxDistanceScaled = maxDistance * 10;
        int[] distances = new int[sizeX * sizeY * sizeZ];
        Arrays.fill(distances, maxDistanceScaled + 1);

        // Bucket queue: indices 0..maxDistanceScaled (distances are multiples of the smallest cost = 10,
        // but intermediate costs 14 and 17 can produce any integer value up to maxDistanceScaled).
        List<List<Integer>> buckets = new ArrayList<>(maxDistanceScaled + 1);
        for (int bucketIndex = 0; bucketIndex <= maxDistanceScaled; bucketIndex++) buckets.add(new ArrayList<>());

        for (Vec3DInt seed : seeds) {
            int localX = seed.x() - origin.x();
            int localY = seed.y() - origin.y();
            int localZ = seed.z() - origin.z();
            if (localX < 0 || localX >= sizeX || localY < 0 || localY >= sizeY || localZ < 0 || localZ >= sizeZ)
                continue;
            int cellIndex = localX * sizeY * sizeZ + localY * sizeZ + localZ;
            if (distances[cellIndex] == 0) continue;
            distances[cellIndex] = 0;
            buckets.get(0).add(cellIndex);
        }

        for (int distance = 0; distance <= maxDistanceScaled; distance++) {
            List<Integer> bucket = buckets.get(distance);
            for (int bucketPos = 0; bucketPos < bucket.size(); bucketPos++) {
                int cellIndex = bucket.get(bucketPos);
                if (distances[cellIndex] != distance) continue; // stale entry from an earlier relaxation
                int localX = cellIndex / (sizeY * sizeZ);
                int localY = (cellIndex / sizeZ) % sizeY;
                int localZ = cellIndex % sizeZ;
                for (int neighborIndex = 0; neighborIndex < NEIGHBOR_OFFSETS.length; neighborIndex++) {
                    int neighborX = localX + NEIGHBOR_OFFSETS[neighborIndex][0];
                    int neighborY = localY + NEIGHBOR_OFFSETS[neighborIndex][1];
                    int neighborZ = localZ + NEIGHBOR_OFFSETS[neighborIndex][2];
                    if (neighborX < 0
                            || neighborX >= sizeX
                            || neighborY < 0
                            || neighborY >= sizeY
                            || neighborZ < 0
                            || neighborZ >= sizeZ) continue;
                    int neighborCellIndex = neighborX * sizeY * sizeZ + neighborY * sizeZ + neighborZ;
                    int newDistance = distance + NEIGHBOR_COSTS[neighborIndex];
                    if (newDistance < distances[neighborCellIndex]) {
                        distances[neighborCellIndex] = newDistance;
                        if (newDistance <= maxDistanceScaled)
                            buckets.get(newDistance).add(neighborCellIndex);
                    }
                }
            }
        }

        return distances;
    }
}
