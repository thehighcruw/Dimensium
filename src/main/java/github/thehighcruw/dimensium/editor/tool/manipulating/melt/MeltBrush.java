/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.melt;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.brushes.GaussianBrushContext;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MeltBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        MeltToolState s = MeltToolState.INSTANCE;
        GaussianBrushContext ctx = GaussianBrushContext.build(world, mop, s.meltSmoothStrength);

        final float threshold = s.meltThreshold;
        final float totalW = ctx.kernel().totalWeight;
        BrushUtil.forBrush(ctx.bs(), ctx.brushSize(), offset -> {
            Vec3DInt snapCoord = ctx.snapCoord(offset);
            if (ctx.snapId()[snapCoord.toIndex(ctx.snStX(), ctx.snStY())] == 0) return;
            if (ctx.kernel().solidWeight(ctx.snapId(), snapCoord, ctx.snStX(), ctx.snStY()) / totalW < threshold) {
                ChangeProposal.write(world, ctx.origin().plus(offset), Blocks.air, 0);
            }
        });
    }
}
