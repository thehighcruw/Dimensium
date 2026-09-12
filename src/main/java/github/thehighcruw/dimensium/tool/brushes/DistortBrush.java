/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.brushes;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.math.NoiseSampler;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.DistortToolState;

public class DistortBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        DistortToolState s = DistortToolState.INSTANCE;
        int ox = mop.blockX, oy = mop.blockY, oz = mop.blockZ;
        int sx = bs.brushRadius;
        int sy = bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius;
        int sz = sx;
        float invScale = 1f / s.distortScale;
        long seed = s.distortSeed;

        int maxPos = (2 * sx + 1) * (2 * sy + 1) * (2 * sz + 1);
        int[] pdx = new int[maxPos], pdy = new int[maxPos], pdz = new int[maxPos];
        int[] srcId = new int[maxPos], srcMeta = new int[maxPos];
        int posCount = 0;

        for (int dx = -sx; dx <= sx; dx++) {
            for (int dy = -sy; dy <= sy; dy++) {
                for (int dz = -sz; dz <= sz; dz++) {
                    if (!BrushUtil.inShape(bs.brushShape, dx, dy, dz, sx, sy, sz)) continue;
                    int wx = ox + dx, wy = oy + dy, wz = oz + dz;

                    float nx = wx * invScale, ny = wy * invScale, nz = wz * invScale;
                    float wx0 = NoiseSampler.rawSimplex3(nx, ny, nz, seed);
                    float wy0 = NoiseSampler.rawSimplex3(nx + 31.7f, ny + 17.3f, nz + 53.1f, seed);
                    float wz0 = NoiseSampler.rawSimplex3(nx + 67.9f, ny + 83.5f, nz + 11.3f, seed);

                    float edgeFade = 1f;
                    if (s.distortSmoothEdges) {
                        float rx = sx > 0 ? (float) Math.abs(dx) / sx : 0f;
                        float ry = sy > 0 ? (float) Math.abs(dy) / sy : 0f;
                        float rz = sz > 0 ? (float) Math.abs(dz) / sz : 0f;
                        float r = Math.max(rx, Math.max(ry, rz));
                        if (r > 0.75f) {
                            float ef = (r - 0.75f) * 4f;
                            edgeFade = 1f - ef * ef * (3f - 2f * ef);
                        }
                    }

                    pdx[posCount] = dx;
                    pdy[posCount] = dy;
                    pdz[posCount] = dz;
                    float warpX = wx0 * s.distortDistanceX * edgeFade;
                    float warpY = wy0 * s.distortDistanceY * edgeFade;
                    float warpZ = wz0 * s.distortDistanceZ * edgeFade;

                    int srcX = (int) Math.round(wx + warpX);
                    int srcY = (int) Math.round(wy + warpY);
                    int srcZ = (int) Math.round(wz + warpZ);
                    Block b = world.getBlock(srcX, srcY, srcZ);
                    srcId[posCount] = Block.getIdFromBlock(b);
                    srcMeta[posCount] = world.getBlockMetadata(srcX, srcY, srcZ);
                    posCount++;
                }
            }
        }

        for (int i = 0; i < posCount; i++) {
            int bid = srcId[i];
            if (bid == 0) continue;
            Block blk = Block.getBlockById(bid);
            if (blk == null) continue;
            int wx = ox + pdx[i], wy = oy + pdy[i], wz = oz + pdz[i];
            if (world.getBlock(wx, wy, wz) == Blocks.air) continue;
            ChangeProposal.write(world, wx, wy, wz, blk, srcMeta[i]);
        }
    }
}
