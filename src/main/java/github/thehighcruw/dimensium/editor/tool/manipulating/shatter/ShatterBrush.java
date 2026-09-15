/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.shatter;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseParams;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class ShatterBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        ShatterToolState s = ShatterToolState.INSTANCE;
        BrushState bs = BrushState.INSTANCE;
        SelectedBlockState sel = SelectedBlockState.INSTANCE;

        Vec3DInt coord = WorldUtils.mopToCoord(mop);

        BrushUtil.forBrush(bs, offset -> {
            Vec3DInt pos = coord.plus(offset);
            if (WorldUtils.getBlock(world, pos) == Blocks.air) return;

            float edge = sampleEdge(s.noiseParams, s.axisMode, pos);
            if (edge >= s.crackWidth) return;

            if (s.fillMode) {
                Block blk = sel.getPaintBlock();
                int meta = sel.getPaintMeta();
                ChangeProposal.write(world, pos, blk, meta);
            } else {
                ChangeProposal.write(world, pos, Blocks.air, 0);
            }
        });
    }

    private static float sampleEdge(NoiseParams p, ShatterToolState.AxisMode axisMode, Vec3DInt wc) {
        return switch (axisMode) {
            case XYZ -> NoiseSampler.sample3D(p, wc.x(), wc.y(), wc.z());
            case X -> NoiseSampler.sample2D(p, wc.y(), wc.z());
            case Y -> NoiseSampler.sample2D(p, wc.x(), wc.z());
            case Z -> NoiseSampler.sample2D(p, wc.x(), wc.y());
        };
    }
}
