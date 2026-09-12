/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.brushes;

import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.tool.ActiveDragState;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.state.ElevationToolState;

public class ElevationBrush implements BrushStrategy {

    private static final HashMap<Long, Float> elevAccum = new HashMap<>();
    private static final HashMap<Long, Integer> elevFireCount = new HashMap<>();

    public static void clearAccum() {
        elevAccum.clear();
        elevFireCount.clear();
    }

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        ElevationToolState s = ElevationToolState.INSTANCE;
        int cx = mop.blockX, cz = mop.blockZ;
        int flattenTargetY = mop.blockY;
        boolean isOnce = s.elevationApply == ElevationToolState.ElevationApply.ONCE;
        int radius = Math.max(1, s.elevationRadius);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                float r = (float) Math.sqrt((double) (dx * dx + dz * dz)) / radius;
                if (r > 1f) continue;

                float weight = falloff(s.elevationFalloff, r);
                weight = edgeSmoothing(weight, r, s.elevationSmoothing);
                if (weight <= 0f) continue;

                int wx = cx + dx, wz = cz + dz;
                int topY = getEffectiveTopY(world, wx, wz);

                long colKey = ((long) (wx + 30000000)) << 26 | ((long) (wz + 30000000) & 0x3FFFFFFL);

                if (isOnce) {
                    int newBlocks = (int) (weight * s.elevationStrength);
                    int prevBlocks = elevFireCount.getOrDefault(colKey, 0);
                    int delta = newBlocks - prevBlocks;
                    if (delta <= 0) continue;
                    elevFireCount.put(colKey, newBlocks);

                    switch (s.elevationMode) {
                        case RAISE:
                            for (int i = 0; i < delta; i++) {
                                int top = getEffectiveTopY(world, wx, wz);
                                placeTerrainBlock(world, wx, top + 1, wz, top, false);
                            }
                            break;
                        case LOWER:
                            for (int i = 0; i < delta; i++) {
                                int top = getEffectiveTopY(world, wx, wz);
                                if (top > 0) ChangeProposal.write(world, wx, top, wz, Blocks.air, 0);
                            }
                            break;
                        case FLATTEN: {
                            int steps = Math.min(delta, Math.abs(flattenTargetY - getEffectiveTopY(world, wx, wz)));
                            for (int i = 0; i < steps; i++) {
                                int top = getEffectiveTopY(world, wx, wz);
                                int dy = flattenTargetY - top;
                                if (dy > 0 && s.flattenDirection != ElevationToolState.FlattenDirection.DOWN)
                                    placeTerrainBlock(world, wx, top + 1, wz, top, false);
                                else if (dy < 0 && s.flattenDirection != ElevationToolState.FlattenDirection.UP
                                    && top > 0) ChangeProposal.write(world, wx, top, wz, Blocks.air, 0);
                                else break;
                            }
                            break;
                        }
                    }
                    continue;
                }

                float prev = elevAccum.getOrDefault(colKey, 0f);
                float acc = prev + weight;
                boolean trigger = acc >= 1f;
                elevAccum.put(colKey, trigger ? acc - 1f : acc);
                if (!trigger) continue;

                switch (s.elevationMode) {
                    case RAISE:
                        placeTerrainBlock(world, wx, topY + 1, wz, topY, true);
                        break;
                    case LOWER:
                        if (topY > 0) ChangeProposal.write(world, wx, topY, wz, Blocks.air, 0);
                        break;
                    case FLATTEN: {
                        int dy = flattenTargetY - topY;
                        if (dy > 0 && s.flattenDirection != ElevationToolState.FlattenDirection.DOWN)
                            placeTerrainBlock(world, wx, topY + 1, wz, topY, true);
                        else if (dy < 0 && s.flattenDirection != ElevationToolState.FlattenDirection.UP && topY > 0)
                            ChangeProposal.write(world, wx, topY, wz, Blocks.air, 0);
                        break;
                    }
                }
            }
        }
    }

    public static float falloff(ElevationToolState.ElevationFalloff profile, float r) {
        return switch (profile) {
            case FLAT -> 1f;
            case SPHERICAL -> (float) Math.sqrt(Math.max(0, 1f - r * r));
            case LINEAR -> 1f - r;
            case LOGARITHMIC -> (float) Math.pow(Math.max(0, 1f - r * r * r * r), 0.25);
            case NORMAL -> (float) Math.exp(-4.5 * r * r);
            case PEAK -> (float) Math.pow(1f - r, 4);
        };
    }

    public static float edgeSmoothing(float weight, float r, float smoothing) {
        if (smoothing <= 0f) return weight;
        float smoothStart = 1f - smoothing;
        if (r <= smoothStart) return weight;
        float t = (r - smoothStart) / smoothing;
        float softFactor = 1f - t * t * (3f - 2f * t);
        return weight * softFactor;
    }

    private static void placeTerrainBlock(World world, int wx, int wy, int wz, int sourceY, boolean continuous) {
        Block fillWith;
        int fillMeta;
        ChangeProposal drag = ActiveDragState.INSTANCE.activeDrag;
        if (continuous && drag != null) {
            long key = ChangeProposal.packKey(wx, sourceY, wz);
            int[] bm = drag.proposed.get(key);
            if (bm != null && bm[0] != 0) {
                fillWith = Block.getBlockById(bm[0]);
                fillMeta = bm[1];
            } else {
                fillWith = world.getBlock(wx, sourceY, wz);
                fillMeta = world.getBlockMetadata(wx, sourceY, wz);
            }
        } else {
            fillWith = world.getBlock(wx, sourceY, wz);
            fillMeta = world.getBlockMetadata(wx, sourceY, wz);
        }
        if (fillWith == null || fillWith == Blocks.air) fillWith = Blocks.dirt;
        ChangeProposal.write(world, wx, wy, wz, fillWith, fillMeta);
    }

    private static int getEffectiveTopY(World world, int wx, int wz) {
        ChangeProposal drag = ActiveDragState.INSTANCE.activeDrag;
        for (int y = 255; y >= 0; y--) {
            if (drag != null) {
                long key = ChangeProposal.packKey(wx, y, wz);
                int[] bm = drag.proposed.get(key);
                if (bm != null) {
                    if (bm[0] != 0) return y;
                    continue;
                }
            }
            if (world.getBlock(wx, y, wz) != Blocks.air) return y;
        }
        return 0;
    }
}
