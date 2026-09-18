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
import github.thehighcruw.dimensium.shared.util.WorldUtils;
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

        Vec3DInt brushSize = Vec3DInt.from(sx, sy, sx);
        Vec3DFloat brushSizeF = brushSize.toFloat();
        Vec3DFloat invBrushSize = Vec3DFloat.from(
                brushSizeF.x() > 0 ? 1f / brushSizeF.x() : 0f,
                brushSizeF.y() > 0 ? 1f / brushSizeF.y() : 0f,
                brushSizeF.z() > 0 ? 1f / brushSizeF.z() : 0f);
        int[] pc = {0};
        Vec3DInt.forEachInclusive(brushSize.negate(), brushSize, offset -> {
            if (!BrushUtil.inShape(bs.brushShape, offset, brushSize)) return;
            Vec3DInt worldPos = origin.plus(offset);

            Vec3DFloat noisePos = worldPos.toFloat().times(invScale);
            float[] w0 = NoiseSampler.warpVec3(noisePos.x(), noisePos.y(), noisePos.z(), seed);

            float edgeFade = 1f;
            if (s.distortSmoothEdges) {
                float r = offset.toFloat().abs().times(invBrushSize).max();
                if (r > 0.75f) {
                    float ef = (r - 0.75f) * 4f;
                    edgeFade = 1f - ef * ef * (3f - 2f * ef);
                }
            }

            offsets[pc[0]] = offset;
            Vec3DFloat warp = Vec3DFloat.from(w0[0], w0[1], w0[2])
                    .times(s.distortDistance)
                    .times(edgeFade);
            Vec3DInt srcPos = Vec3DInt.round(worldPos.toFloat().plus(warp));
            srcId[pc[0]] = Block.getIdFromBlock(WorldUtils.getBlock(world, srcPos));
            srcMeta[pc[0]] = WorldUtils.getBlockMetadata(world, srcPos);
            pc[0]++;
        });
        int posCount = pc[0];

        for (int i = 0; i < posCount; i++) {
            int bid = srcId[i];
            if (bid == 0) continue;
            Block blk = Block.getBlockById(bid);
            if (blk == null) continue;
            Vec3DInt wp = origin.plus(offsets[i]);
            if (WorldUtils.getBlock(world, wp) == Blocks.air) continue;
            ChangeProposal.write(world, wp, blk, srcMeta[i]);
        }
    }
}
