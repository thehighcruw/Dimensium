/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.melt;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.brushes.GaussianKernel;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MeltBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        MeltToolState s = MeltToolState.INSTANCE;
        Vec3DInt origin = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        int sx = Math.min(bs.brushRadius, 12);
        int sy = Math.min(bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius, 12);

        GaussianKernel kernel = GaussianKernel.build(s.meltSmoothStrength * 0.5f + 0.5f);
        int margin = kernel.kR;
        Vec3DInt brushSize = Vec3DInt.from(sx, sy, sx);
        int snStY = 2 * (sx + margin) + 1, snStX = (2 * (sy + margin) + 1) * snStY;
        int[] snapId = BrushUtil.snapshotBlockIds(world, origin, brushSize, margin);

        final float threshold = s.meltThreshold;
        final float totalW = kernel.totalWeight;
        BrushUtil.forBrush(
                bs,
                brushSize,
                offset -> { // sx/sy/sz clamped to snapshot bounds
                    Vec3DInt world3 = origin.plus(offset);
                    Vec3DInt snapCoord = offset.plus(sx + margin, sy + margin, sx + margin);
                    if (snapId[snapCoord.toIndex(snStX, snStY)] == 0) return;
                    if (kernel.solidWeight(snapId, snapCoord, snStX, snStY) / totalW < threshold) {
                        ChangeProposal.write(world, world3, Blocks.air, 0);
                    }
                });
    }
}
