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
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.FreehandToolState;
import github.thehighcruw.dimensium.tool.state.SelectedBlockState;

public class FreehandBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        FreehandToolState s = FreehandToolState.INSTANCE;
        BrushState bs = BrushState.INSTANCE;
        Block paint = SelectedBlockState.INSTANCE.getPaintBlock();
        int meta = SelectedBlockState.INSTANCE.getPaintMeta();
        int x = mop.blockX, y = mop.blockY, z = mop.blockZ;
        BrushUtil.forBrush(bs, (dx, dy, dz) -> {
            int wx = x + dx, wy = y + dy, wz = z + dz;
            Block existing = world.getBlock(wx, wy, wz);
            if (!s.freehandReplaceSolid && existing != Blocks.air) return;
            if (s.freehandMaskSurface && !BrushUtil.hasAirNeighbor(world, wx, wy, wz)) return;
            ChangeProposal.write(world, wx, wy, wz, paint, meta);
        });
    }
}
