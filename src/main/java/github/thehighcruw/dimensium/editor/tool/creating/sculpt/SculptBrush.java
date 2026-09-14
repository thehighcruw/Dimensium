/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.sculpt;

import github.thehighcruw.dimensium.editor.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class SculptBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        SculptToolState s = SculptToolState.INSTANCE;
        int cx = mop.blockX, centerY = mop.blockY, cz = mop.blockZ;

        Vec3DFloat normal;
        if (s.sculptMaskY) {
            normal = Vec3DFloat.from(0f, 1f, 0f);
        } else {
            normal = computeSobelNormal(world, cx, centerY, cz, Math.max(1, bs.brushRadius));
            if (normal == null) {
                int[] rawN = ExtrudeHelper.sideToOutwardDir(mop.sideHit);
                normal = Vec3DFloat.from(rawN[0], rawN[1], rawN[2]);
            }
        }
        Vec3DFloat pa1 = normal.cross(Vec3DFloat.from(0f, 1f, 0f));
        if (pa1.lengthSq() < 0.001f) pa1 = normal.cross(Vec3DFloat.from(1f, 0f, 0f));
        pa1 = pa1.normalize();
        Vec3DFloat pa2 = normal.cross(pa1).normalize();

        int radius = Math.max(1, bs.brushRadius);
        int dim = 2 * radius + 1;
        int[] disp = new int[dim * dim];
        Vec3DInt[] basePos = new Vec3DInt[dim * dim];
        Vec3DInt center = Vec3DInt.from(cx, centerY, cz);

        for (int d1 = -radius; d1 <= radius; d1++) {
            for (int d2 = -radius; d2 <= radius; d2++) {
                int idx = (d1 + radius) * dim + (d2 + radius);
                float dist = (float) Math.sqrt(d1 * d1 + d2 * d2) / radius;
                if (dist > 1f) {
                    disp[idx] = -1;
                    continue;
                }
                float falloff = (float) Math.sqrt(Math.max(0f, 1f - dist * dist));
                disp[idx] = Math.max(0, Math.round(s.sculptStrength * falloff));
                basePos[idx] = center.plus(pa1.times(d1).plus(pa2.times(d2)).round());
            }
        }

        if (s.sculptDenoise) {
            int[] smoothed = new int[dim * dim];
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    int idx = i * dim + j;
                    if (disp[idx] < 0) {
                        smoothed[idx] = -1;
                        continue;
                    }
                    int sum = 0, cnt = 0;
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            int ni = i + di, nj = j + dj;
                            if (ni < 0 || ni >= dim || nj < 0 || nj >= dim) continue;
                            int nidx = ni * dim + nj;
                            if (disp[nidx] < 0) continue;
                            sum += disp[nidx];
                            cnt++;
                        }
                    }
                    smoothed[idx] = cnt > 0 ? Math.round((float) sum / cnt) : 0;
                }
            }
            disp = smoothed;
        }

        int searchRange = radius + (int) Math.ceil(s.sculptStrength) + 2;
        for (int idx = 0; idx < dim * dim; idx++) {
            if (disp[idx] <= 0) continue;
            int depth = disp[idx];

            Vec3DInt surf = findSculptSurface(world, basePos[idx], normal, searchRange);
            if (surf == null) continue;

            if (!s.sculptInvert) {
                Block surfBlock = world.getBlock(surf.x(), surf.y(), surf.z());
                int surfMeta = world.getBlockMetadata(surf.x(), surf.y(), surf.z());
                if (surfBlock == null || surfBlock == Blocks.air) surfBlock = Blocks.dirt;
                Vec3DInt prev = surf;
                for (int d = 1; d <= depth; d++) {
                    Vec3DInt t = surf.plus(normal.times(d).round());
                    if (t.y() < 0 || t.y() > 255) break;
                    if (t.equals(prev)) continue;
                    prev = t;
                    if (world.getBlock(t.x(), t.y(), t.z()) != Blocks.air) break;
                    ChangeProposal.write(world, t.x(), t.y(), t.z(), surfBlock, surfMeta);
                }
            } else {
                Vec3DInt prev = surf;
                for (int d = 0; d < depth; d++) {
                    Vec3DInt t = surf.minus(normal.times(d).round());
                    if (t.y() < 0 || t.y() > 255) break;
                    if (t.equals(prev)) continue;
                    prev = t;
                    if (world.getBlock(t.x(), t.y(), t.z()) == Blocks.air) break;
                    ChangeProposal.write(world, t.x(), t.y(), t.z(), Blocks.air, 0);
                }
            }
        }
    }

    /**
     * Sobel gradient on topmost-solid-block height map, falling back to weighted
     * face-average normal when the gradient is too flat to be informative.
     * Returns null if no surface found at all.
     */
    private static Vec3DFloat computeSobelNormal(World world, int cx, int cy, int cz, int radius) {
        int search = radius + 8;
        float[] h = new float[9];
        boolean anyFound = false;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int idx = (dz + 1) * 3 + (dx + 1);
                int top = findTopY(world, cx + dx, cz + dz, cy, search);
                if (top == Integer.MIN_VALUE) {
                    h[idx] = cy;
                } else {
                    h[idx] = top;
                    anyFound = true;
                }
            }
        }
        if (!anyFound) return null;

        // Sobel kernels (divide by 8 for normalization)
        float dX = (-h[0] + h[2] - 2 * h[3] + 2 * h[5] - h[6] + h[8]) / 8f;
        float dZ = (-h[0] - 2 * h[1] - h[2] + h[6] + 2 * h[7] + h[8]) / 8f;

        float gradMag = (float) Math.sqrt(dX * dX + dZ * dZ);
        if (gradMag < 0.15f) {
            return Vec3DFloat.from(0f, 1f, 0f);
        }

        // Normal from height gradient: surface z = h(x,z), tangents are (1,dX,0) and (0,dZ,1)
        // normal = cross(tangents) = (-dX, 1, -dZ) normalized
        return Vec3DFloat.from(-dX, 1f, -dZ).normalize();
    }

    private static int findTopY(World world, int x, int z, int cy, int search) {
        for (int y = Math.min(255, cy + search); y >= Math.max(0, cy - search); y--) {
            if (world.getBlock(x, y, z) != Blocks.air) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static Vec3DInt findSculptSurface(World world, Vec3DInt base, Vec3DFloat normal, int range) {
        Vec3DInt last = null;
        for (float step = range; step >= -range; step -= 0.5f) {
            Vec3DInt t = base.plus(normal.times(step).round());
            if (t.y() < 0 || t.y() > 255) continue;
            if (t.equals(last)) continue;
            last = t;
            if (world.getBlock(t.x(), t.y(), t.z()) != Blocks.air) return t;
        }
        return null;
    }
}
