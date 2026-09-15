/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.distort;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class DistortBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        DistortToolState s = DistortToolState.INSTANCE;
        Vec3DInt origin = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        int sx = bs.brushRadius;
        int sy = bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius;
        float invScale = 1f / s.distortScale;
        long seed = s.distortSeed;

        int maxPos = Vec3DInt.from(sx, sy, sx).times(2).plus(1).product();
        Vec3DInt[] offsets = new Vec3DInt[maxPos];
        int[] srcId = new int[maxPos], srcMeta = new int[maxPos];
        int posCount = 0;

        Vec3DInt brushSize = Vec3DInt.from(sx, sy, sx);
        Vec3DFloat invBrushSize = Vec3DFloat.from(sx > 0 ? 1f / sx : 0f, sy > 0 ? 1f / sy : 0f, sx > 0 ? 1f / sx : 0f);
        for (int dx = -sx; dx <= sx; dx++) {
            for (int dy = -sy; dy <= sy; dy++) {
                for (int dz = -sx; dz <= sx; dz++) {
                    if (!BrushUtil.inShape(bs.brushShape, Vec3DInt.from(dx, dy, dz), brushSize)) continue;
                    Vec3DInt worldPos = origin.plus(dx, dy, dz);

                    float nx = worldPos.x() * invScale, ny = worldPos.y() * invScale, nz = worldPos.z() * invScale;
                    float wx0 = NoiseSampler.rawSimplex3(nx, ny, nz, seed);
                    float wy0 = NoiseSampler.rawSimplex3(nx + 31.7f, ny + 17.3f, nz + 53.1f, seed);
                    float wz0 = NoiseSampler.rawSimplex3(nx + 67.9f, ny + 83.5f, nz + 11.3f, seed);

                    float edgeFade = 1f;
                    if (s.distortSmoothEdges) {
                        float r = Vec3DInt.from(dx, dy, dz)
                                .toFloat()
                                .abs()
                                .times(invBrushSize)
                                .max();
                        if (r > 0.75f) {
                            float ef = (r - 0.75f) * 4f;
                            edgeFade = 1f - ef * ef * (3f - 2f * ef);
                        }
                    }

                    offsets[posCount] = Vec3DInt.from(dx, dy, dz);
                    float warpX = wx0 * s.distortDistanceX * edgeFade;
                    float warpY = wy0 * s.distortDistanceY * edgeFade;
                    float warpZ = wz0 * s.distortDistanceZ * edgeFade;

                    int srcX = Math.round(worldPos.x() + warpX);
                    int srcY = Math.round(worldPos.y() + warpY);
                    int srcZ = Math.round(worldPos.z() + warpZ);
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
            Vec3DInt wp = origin.plus(offsets[i]);
            if (world.getBlock(wp.x(), wp.y(), wp.z()) == Blocks.air) continue;
            ChangeProposal.write(world, wp, blk, srcMeta[i]);
        }
    }
}
