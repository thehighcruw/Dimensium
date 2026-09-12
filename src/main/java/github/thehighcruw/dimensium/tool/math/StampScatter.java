package github.thehighcruw.dimensium.tool.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import github.thehighcruw.dimensium.blueprint.Blueprint;
import github.thehighcruw.dimensium.tool.state.StampEntry;
import github.thehighcruw.dimensium.tool.state.StampToolState;

public class StampScatter {

    public static class StampInstance {

        public final int entryIdx;
        public final int anchorX, anchorY, anchorZ;
        public final float yaw;
        public final boolean flipX, flipZ;

        StampInstance(int entryIdx, int ax, int ay, int az, float yaw, boolean flipX, boolean flipZ) {
            this.entryIdx = entryIdx;
            this.anchorX = ax;
            this.anchorY = ay;
            this.anchorZ = az;
            this.yaw = yaw;
            this.flipX = flipX;
            this.flipZ = flipZ;
        }
    }

    /**
     * Scatters blueprint instances across the given stroke positions.
     *
     * @param stroke List of {x, y, z} surface block positions visited during the brush drag.
     * @param state  Current stamp tool configuration.
     * @param rng    Random source.
     * @return List of instances to place.
     */
    public static List<StampInstance> scatter(List<int[]> stroke, StampToolState state, Random rng) {
        List<StampInstance> result = new ArrayList<>();
        if (state.blueprints.isEmpty() || stroke.isEmpty()) return result;

        float weightSum = 0f;
        for (StampEntry e : state.blueprints) weightSum += Math.max(0f, e.chance);
        if (weightSum <= 0f) return result;

        for (int[] pos : stroke) {
            if (rng.nextFloat() > state.baseChance) continue;

            int entryIdx = pickWeighted(state.blueprints, weightSum, rng);
            StampEntry entry = state.blueprints.get(entryIdx);
            Blueprint bp = entry.blueprint;

            float minDist = state.minSpacingPct * Math.max(bp.clipW, bp.clipD);
            if (minDist > 0f && isTooClose(result, pos[0], pos[2], minDist)) continue;

            float yaw = state.randomYaw ? rng.nextFloat() * 360f : 0f;
            boolean flipX = state.randomXFlip && rng.nextBoolean();
            boolean flipZ = state.randomZFlip && rng.nextBoolean();
            // anchorY: top surface of the hit block → place blueprint base one block above
            result.add(new StampInstance(entryIdx, pos[0], pos[1] + 1 + entry.offsetY, pos[2], yaw, flipX, flipZ));
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
            int dx = x - inst.anchorX;
            int dz = z - inst.anchorZ;
            if (dx * dx + dz * dz < minDist2) return true;
        }
        return false;
    }

    private StampScatter() {}
}
