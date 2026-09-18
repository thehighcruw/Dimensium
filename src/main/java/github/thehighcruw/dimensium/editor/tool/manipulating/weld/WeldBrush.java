/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.weld;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.brushes.GaussianBrushContext;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class WeldBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        WeldToolState s = WeldToolState.INSTANCE;
        SelectedBlockState sbs = SelectedBlockState.INSTANCE;
        Block paint = sbs.getPaintBlock();
        int meta = sbs.getPaintMeta();
        GaussianBrushContext ctx = GaussianBrushContext.build(world, mop, s.weldSmoothStrength);

        final float threshold = s.weldThreshold;
        final float totalW = ctx.kernel().totalWeight;
        BrushUtil.forBrush(ctx.bs(), ctx.brushSize(), offset -> {
            Vec3DInt snapCoord = ctx.snapCoord(offset);
            int existing = ctx.snapId()[snapCoord.toIndex(ctx.snStX(), ctx.snStY())];
            if (existing != 0 && !s.weldReplaceSolid) return;
            if (ctx.kernel().solidWeight(ctx.snapId(), snapCoord, ctx.snStX(), ctx.snStY()) / totalW > threshold) {
                ChangeProposal.write(world, ctx.origin().plus(offset), paint, meta);
            }
        });
    }
}
