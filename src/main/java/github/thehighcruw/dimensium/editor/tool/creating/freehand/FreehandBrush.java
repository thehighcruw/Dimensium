/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.freehand;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class FreehandBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        FreehandToolState s = FreehandToolState.INSTANCE;
        BrushState bs = BrushState.INSTANCE;
        Block paint = SelectedBlockState.INSTANCE.getPaintBlock();
        int meta = SelectedBlockState.INSTANCE.getPaintMeta();

        var coord = WorldUtils.mopToCoord(mop);

        BrushUtil.forBrush(bs, offset -> {
            var pos = coord.plus(offset);

            Block existing = WorldUtils.getBlock(world, pos);
            if (!s.freehandReplaceSolid && existing != Blocks.air) return;
            if (s.freehandMaskSurface && BrushUtil.hasSolidNeighbor(world, pos)) return;

            ChangeProposal.write(world, pos, paint, meta);
        });
    }
}
