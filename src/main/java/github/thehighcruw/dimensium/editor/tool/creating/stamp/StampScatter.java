/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.stamp;

import github.thehighcruw.dimensium.shared.math.Vec2DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StampScatter {

    public static class StampInstance {

        public final int entryIdx;
        public final Vec3DInt anchor;
        public final float yaw;
        public final boolean flipX, flipZ;
        /** Deterministic seed derived from toolSeed + location, passed to pipeline execution. */
        public final long locationSeed;

        StampInstance(int entryIdx, Vec3DInt anchor, float yaw, boolean flipX, boolean flipZ, long locationSeed) {
            this.entryIdx = entryIdx;
            this.anchor = anchor;
            this.yaw = yaw;
            this.flipX = flipX;
            this.flipZ = flipZ;
            this.locationSeed = locationSeed;
        }
    }

    /**
     * Scatters blueprint instances across the given stroke positions.
     *
     * <p>
     * Each candidate location's placement decisions (chance, entry selection, transforms) are
     * derived deterministically from {@code toolSeed} mixed with the location's x/z coordinates.
     * Restamping the same area with the same tool seed always produces identical results.
     *
     * @param stroke   List of surface block positions visited during the brush drag.
     * @param state    Current stamp tool configuration.
     * @param toolSeed Persistent tool seed from {@link StampToolState#toolSeed}.
     * @return List of instances to place.
     */
    public static List<StampInstance> scatter(List<Vec3DInt> stroke, StampToolState state, long toolSeed) {
        List<StampInstance> result = new ArrayList<>();
        if (state.blueprints.isEmpty() || stroke.isEmpty()) return result;

        float weightSum = 0f;
        for (StampEntry e : state.blueprints) weightSum += Math.max(0f, e.chance);
        if (weightSum <= 0f) return result;

        for (Vec3DInt pos : stroke) {
            long locationSeed = mixLocationSeed(toolSeed, pos.x(), pos.z());
            Random locationRng = new Random(locationSeed);

            if (locationRng.nextFloat() > state.baseChance) continue;

            int entryIdx = pickWeighted(state.blueprints, weightSum, locationRng);
            StampEntry entry = state.blueprints.get(entryIdx);

            float minDist = state.minSpacingPct
                    * Math.max(entry.clipDim().x(), entry.clipDim().z());
            if (minDist > 0f && isTooClose(result, pos.x(), pos.z(), minDist)) continue;

            float yaw = state.randomYaw ? locationRng.nextFloat() * 360f : 0f;
            boolean flipX = state.randomXFlip && locationRng.nextBoolean();
            boolean flipZ = state.randomZFlip && locationRng.nextBoolean();
            // anchorY: top surface of the hit block → place blueprint base one block above
            result.add(new StampInstance(entryIdx, pos.plus(0, 1 + entry.offsetY, 0), yaw, flipX, flipZ, locationSeed));
        }
        return result;
    }

    /** Mixes a base seed with x/z coordinates to produce a unique per-cell seed. */
    private static long mixLocationSeed(long seed, int x, int z) {
        long hash = seed;
        hash ^= (long) x * 0x9E3779B97F4A7C15L;
        hash ^= (long) z * 0x6C62272E07BB0142L;
        hash ^= (hash >>> 30);
        hash *= 0xBF58476D1CE4E5B9L;
        hash ^= (hash >>> 27);
        hash *= 0x94D049BB133111EBL;
        hash ^= (hash >>> 31);
        return hash;
    }

    private static int pickWeighted(List<StampEntry> entries, float weightSum, Random rng) {
        float pick = rng.nextFloat() * weightSum;
        float acc = 0f;
        for (int i = 0; i < entries.size(); i++) {
            acc += Math.max(0f, entries.get(i).chance);
            if (pick < acc) return i;
        }
        return entries.size() - 1;
    }

    private static boolean isTooClose(List<StampInstance> placed, int x, int z, float minDist) {
        float minDist2 = minDist * minDist;
        for (StampInstance inst : placed) {
            if (Vec2DFloat.from(x - inst.anchor.x(), z - inst.anchor.z()).lengthSq() < minDist2) return true;
        }
        return false;
    }

    private StampScatter() {}
}
