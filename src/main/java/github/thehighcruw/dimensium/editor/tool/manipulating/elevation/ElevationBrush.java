/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.elevation;

import github.thehighcruw.dimensium.editor.tool.ActiveDragState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.shared.math.Vec2DFloat;
import github.thehighcruw.dimensium.shared.math.Vec2DInt;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

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
        Vec2DInt columnCenter = Vec2DInt.from(mop.blockX, mop.blockZ);
        int flattenTargetY = mop.blockY;
        boolean isOnce = s.elevationApply == ElevationToolState.ElevationApply.ONCE;
        int radius = Math.max(1, s.elevationRadius);

        Vec3DInt.forEachInclusive(Vec3DInt.from(-radius, 0, -radius), Vec3DInt.from(radius, 0, radius), offset -> {
            int dx = offset.x(), dz = offset.z();
            float r = Vec2DFloat.from(dx, dz).length() / radius;
            if (r > 1f) return;

            float weight = falloff(s.elevationFalloff, r);
            weight = edgeSmoothing(weight, r, s.elevationSmoothing);
            if (weight <= 0f) return;

            Vec2DInt column = columnCenter.plus(dx, dz);
            int topY = getEffectiveTopY(world, column);

            long colKey = ((long) (column.x() + 30000000)) << 26 | ((long) (column.y() + 30000000) & 0x3FFFFFFL);

            if (isOnce) {
                int newBlocks = (int) (weight * s.elevationStrength);
                int prevBlocks = elevFireCount.getOrDefault(colKey, 0);
                int delta = newBlocks - prevBlocks;
                if (delta <= 0) return;
                elevFireCount.put(colKey, newBlocks);

                switch (s.elevationMode) {
                    case RAISE:
                        for (int i = 0; i < delta; i++) {
                            int top = getEffectiveTopY(world, column);
                            placeTerrainBlock(world, Vec3DInt.from(column.x(), top + 1, column.y()), top, false);
                        }
                        break;
                    case LOWER:
                        for (int i = 0; i < delta; i++) {
                            int top = getEffectiveTopY(world, column);
                            if (top > 0)
                                ChangeProposal.write(world, Vec3DInt.from(column.x(), top, column.y()), Blocks.air, 0);
                        }
                        break;
                    case FLATTEN: {
                        int steps = Math.min(delta, Math.abs(flattenTargetY - getEffectiveTopY(world, column)));
                        for (int i = 0; i < steps; i++) {
                            int top = getEffectiveTopY(world, column);
                            int dy = flattenTargetY - top;
                            if (dy > 0 && s.flattenDirection != ElevationToolState.FlattenDirection.DOWN)
                                placeTerrainBlock(world, Vec3DInt.from(column.x(), top + 1, column.y()), top, false);
                            else if (dy < 0 && s.flattenDirection != ElevationToolState.FlattenDirection.UP && top > 0)
                                ChangeProposal.write(world, Vec3DInt.from(column.x(), top, column.y()), Blocks.air, 0);
                            else break;
                        }
                        break;
                    }
                }
                return;
            }

            float prev = elevAccum.getOrDefault(colKey, 0f);
            float acc = prev + weight;
            boolean trigger = acc >= 1f;
            elevAccum.put(colKey, trigger ? acc - 1f : acc);
            if (!trigger) return;

            switch (s.elevationMode) {
                case RAISE:
                    placeTerrainBlock(world, Vec3DInt.from(column.x(), topY + 1, column.y()), topY, true);
                    break;
                case LOWER:
                    if (topY > 0)
                        ChangeProposal.write(world, Vec3DInt.from(column.x(), topY, column.y()), Blocks.air, 0);
                    break;
                case FLATTEN: {
                    int dy = flattenTargetY - topY;
                    if (dy > 0 && s.flattenDirection != ElevationToolState.FlattenDirection.DOWN)
                        placeTerrainBlock(world, Vec3DInt.from(column.x(), topY + 1, column.y()), topY, true);
                    else if (dy < 0 && s.flattenDirection != ElevationToolState.FlattenDirection.UP && topY > 0)
                        ChangeProposal.write(world, Vec3DInt.from(column.x(), topY, column.y()), Blocks.air, 0);
                    break;
                }
            }
        });
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

    private static void placeTerrainBlock(World world, Vec3DInt pos, int sourceY, boolean continuous) {
        Block fillWith;
        int fillMeta;
        ChangeProposal drag = ActiveDragState.INSTANCE.activeDrag;
        if (continuous && drag != null) {
            long key = ChangeProposal.packKey(pos.x(), sourceY, pos.z());
            int[] bm = drag.proposed.get(key);
            if (bm != null && bm[0] != 0) {
                fillWith = Block.getBlockById(bm[0]);
                fillMeta = bm[1];
            } else {
                fillWith = world.getBlock(pos.x(), sourceY, pos.z());
                fillMeta = world.getBlockMetadata(pos.x(), sourceY, pos.z());
            }
        } else {
            fillWith = world.getBlock(pos.x(), sourceY, pos.z());
            fillMeta = world.getBlockMetadata(pos.x(), sourceY, pos.z());
        }
        if (fillWith == null || fillWith == Blocks.air) fillWith = Blocks.dirt;
        ChangeProposal.write(world, pos, fillWith, fillMeta);
    }

    private static int getEffectiveTopY(World world, Vec2DInt column) {
        ChangeProposal drag = ActiveDragState.INSTANCE.activeDrag;
        for (int y = 255; y >= 0; y--) {
            if (drag != null) {
                long key = ChangeProposal.packKey(column.x(), y, column.y());
                int[] bm = drag.proposed.get(key);
                if (bm != null) {
                    if (bm[0] != 0) return y;
                    continue;
                }
            }
            if (world.getBlock(column.x(), y, column.y()) != Blocks.air) return y;
        }
        return 0;
    }
}
