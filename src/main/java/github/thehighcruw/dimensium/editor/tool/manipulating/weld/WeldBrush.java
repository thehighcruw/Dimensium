/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.weld;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.brushes.GaussianKernel;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class WeldBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        WeldToolState s = WeldToolState.INSTANCE;
        SelectedBlockState sbs = SelectedBlockState.INSTANCE;
        Block paint = sbs.getPaintBlock();
        int meta = sbs.getPaintMeta();
        Vec3DInt origin = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        int sx = Math.min(bs.brushRadius, 12);
        int sy = Math.min(bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius, 12);

        GaussianKernel kernel = GaussianKernel.build(s.weldSmoothStrength * 0.5f + 0.5f);
        int margin = kernel.kR;
        Vec3DInt brushSize = Vec3DInt.from(sx, sy, sx);
        int snStY = 2 * (sx + margin) + 1, snStX = (2 * (sy + margin) + 1) * snStY;
        int[] snapId = BrushUtil.snapshotBlockIds(world, origin, brushSize, margin);

        final float threshold = s.weldThreshold;
        final float totalW = kernel.totalWeight;
        BrushUtil.forBrush(bs, brushSize, offset -> {
            Vec3DInt world3 = origin.plus(offset);
            Vec3DInt snapCoord = offset.plus(sx + margin, sy + margin, sx + margin);
            int existing = snapId[snapCoord.toIndex(snStX, snStY)];
            if (existing != 0 && !s.weldReplaceSolid) return;
            if (kernel.solidWeight(snapId, snapCoord, snStX, snStY) / totalW > threshold) {
                ChangeProposal.write(world, world3, paint, meta);
            }
        });
    }
}
