/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.melt;

import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.brushes.GaussianKernel;
import github.thehighcruw.dimensium.tool.ChangeProposal;

public class MeltBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        MeltToolState s = MeltToolState.INSTANCE;
        int ox = mop.blockX, oy = mop.blockY, oz = mop.blockZ;
        int sx = Math.min(bs.brushRadius, 12);
        int sy = Math.min(bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius, 12);

        GaussianKernel kernel = GaussianKernel.build(s.meltSmoothStrength * 0.5f + 0.5f);
        int margin = kernel.kR;
        int snStY = 2 * (sx + margin) + 1, snStX = (2 * (sy + margin) + 1) * snStY;
        int[] snapId = BrushUtil.snapshotBlockIds(world, ox, oy, oz, sx, sy, sx, margin);

        final float threshold = s.meltThreshold;
        final float totalW = kernel.totalWeight;
        BrushUtil.forBrush(bs, sx, sy, sx, (dx, dy, dz) -> { // sx/sy/sz clamped to snapshot bounds
            int wx = ox + dx, wy = oy + dy, wz = oz + dz;
            int existing = snapId[(dx + sx + margin) * snStX + (dy + sy + margin) * snStY + (dz + sx + margin)];
            if (existing == 0) return;
            int ix = dx + sx + margin, iy = dy + sy + margin, iz = dz + sx + margin;
            if (kernel.solidWeight(snapId, ix, iy, iz, snStX, snStY) / totalW < threshold) {
                ChangeProposal.write(world, wx, wy, wz, Blocks.air, 0);
            }
        });
    }
}
