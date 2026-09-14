/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.weld;

import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.brushes.GaussianKernel;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.shared.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;

public class WeldBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        WeldToolState s = WeldToolState.INSTANCE;
        SelectedBlockState sbs = SelectedBlockState.INSTANCE;
        Block paint = sbs.getPaintBlock();
        int meta = sbs.getPaintMeta();
        int ox = mop.blockX, oy = mop.blockY, oz = mop.blockZ;
        int sx = Math.min(bs.brushRadius, 12);
        int sy = Math.min(bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius, 12);

        GaussianKernel kernel = GaussianKernel.build(s.weldSmoothStrength * 0.5f + 0.5f);
        int margin = kernel.kR;
        int snStY = 2 * (sx + margin) + 1, snStX = (2 * (sy + margin) + 1) * snStY;
        int[] snapId = BrushUtil.snapshotBlockIds(world, ox, oy, oz, sx, sy, sx, margin);

        final float threshold = s.weldThreshold;
        final float totalW = kernel.totalWeight;
        BrushUtil.forBrush(bs, sx, sy, sx, (dx, dy, dz) -> {
            int wx = ox + dx, wy = oy + dy, wz = oz + dz;
            int existing = snapId[(dx + sx + margin) * snStX + (dy + sy + margin) * snStY + (dz + sx + margin)];
            if (existing != 0 && !s.weldReplaceSolid) return;
            int ix = dx + sx + margin, iy = dy + sy + margin, iz = dz + sx + margin;
            if (kernel.solidWeight(snapId, Vec3DInt.from(ix, iy, iz), snStX, snStY) / totalW > threshold) {
                ChangeProposal.write(world, wx, wy, wz, paint, meta);
            }
        });
    }
}
