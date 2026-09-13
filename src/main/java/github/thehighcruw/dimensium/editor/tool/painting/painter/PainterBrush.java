/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.painting.painter;

import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.tool.ChangeProposal;

public class PainterBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        PainterToolState s = PainterToolState.INSTANCE;
        BrushState bs = BrushState.INSTANCE;
        net.minecraft.block.Block paint = SelectedBlockState.INSTANCE.getPaintBlock();
        int meta = SelectedBlockState.INSTANCE.getPaintMeta();
        int x = mop.blockX, y = mop.blockY, z = mop.blockZ;
        BrushUtil.forBrush(bs, (dx, dy, dz) -> {
            int wx = x + dx, wy = y + dy, wz = z + dz;
            if (world.getBlock(wx, wy, wz) == Blocks.air) return;
            if (s.painterMaskSurface && !BrushUtil.hasAirNeighbor(world, wx, wy, wz)) return;
            ChangeProposal.write(world, wx, wy, wz, paint, meta);
        });
    }
}
