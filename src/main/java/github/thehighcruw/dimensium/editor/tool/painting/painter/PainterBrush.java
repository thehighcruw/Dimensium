/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.painting.painter;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class PainterBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        PainterToolState s = PainterToolState.INSTANCE;
        BrushState bs = BrushState.INSTANCE;
        Block paint = SelectedBlockState.INSTANCE.getPaintBlock();
        int meta = SelectedBlockState.INSTANCE.getPaintMeta();
        Vec3DInt coord = WorldUtils.mopToCoord(mop);
        BrushUtil.forBrush(bs, offset -> {
            Vec3DInt pos = coord.plus(offset);
            if (WorldUtils.getBlock(world, pos) == Blocks.air) return;
            if (s.painterMaskSurface && BrushUtil.hasSolidNeighbor(world, pos)) return;
            ChangeProposal.write(world, pos, paint, meta);
        });
    }
}
