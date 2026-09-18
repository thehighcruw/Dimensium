/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.stamp;

import github.thehighcruw.dimensium.editor.blueprint.Blueprint;
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

        StampInstance(int entryIdx, Vec3DInt anchor, float yaw, boolean flipX, boolean flipZ) {
            this.entryIdx = entryIdx;
            this.anchor = anchor;
            this.yaw = yaw;
            this.flipX = flipX;
            this.flipZ = flipZ;
        }
    }

    /**
     * Scatters blueprint instances across the given stroke positions.
     *
     * @param stroke List of surface block positions visited during the brush drag.
     * @param state  Current stamp tool configuration.
     * @param rng    Random source.
     * @return List of instances to place.
     */
    public static List<StampInstance> scatter(List<Vec3DInt> stroke, StampToolState state, Random rng) {
        List<StampInstance> result = new ArrayList<>();
        if (state.blueprints.isEmpty() || stroke.isEmpty()) return result;

        float weightSum = 0f;
        for (StampEntry e : state.blueprints) weightSum += Math.max(0f, e.chance);
        if (weightSum <= 0f) return result;

        for (Vec3DInt pos : stroke) {
            if (rng.nextFloat() > state.baseChance) continue;

            int entryIdx = pickWeighted(state.blueprints, weightSum, rng);
            StampEntry entry = state.blueprints.get(entryIdx);
            Blueprint bp = entry.blueprint;

            float minDist = state.minSpacingPct
                    * Math.max(bp.clipDim().x(), bp.clipDim().z());
            if (minDist > 0f && isTooClose(result, pos.x(), pos.z(), minDist)) continue;

            float yaw = state.randomYaw ? rng.nextFloat() * 360f : 0f;
            boolean flipX = state.randomXFlip && rng.nextBoolean();
            boolean flipZ = state.randomZFlip && rng.nextBoolean();
            // anchorY: top surface of the hit block → place blueprint base one block above
            result.add(new StampInstance(entryIdx, pos.plus(0, 1 + entry.offsetY, 0), yaw, flipX, flipZ));
        }
        return result;
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
