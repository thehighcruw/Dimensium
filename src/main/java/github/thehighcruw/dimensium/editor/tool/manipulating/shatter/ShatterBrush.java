/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.shatter;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseParams;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.tool.ChangeProposal;

public class ShatterBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        ShatterToolState s = ShatterToolState.INSTANCE;
        BrushState bs = BrushState.INSTANCE;
        SelectedBlockState sel = SelectedBlockState.INSTANCE;

        int ox = mop.blockX, oy = mop.blockY, oz = mop.blockZ;

        BrushUtil.forBrush(bs, (dx, dy, dz) -> {
            int wx = ox + dx, wy = oy + dy, wz = oz + dz;
            if (world.getBlock(wx, wy, wz) == Blocks.air) return;

            float edge = sampleEdge(s.noiseParams, s.axisMode, wx, wy, wz);
            if (edge >= s.crackWidth) return;

            if (s.fillMode) {
                Block blk = sel.getPaintBlock();
                int meta = sel.getPaintMeta();
                ChangeProposal.write(world, wx, wy, wz, blk, meta);
            } else {
                ChangeProposal.write(world, wx, wy, wz, Blocks.air, 0);
            }
        });
    }

    private static float sampleEdge(NoiseParams p, ShatterToolState.AxisMode axisMode, int wx, int wy, int wz) {
        return switch (axisMode) {
            case XYZ -> NoiseSampler.sample3D(p, wx, wy, wz);
            case X -> NoiseSampler.sample2D(p, wy, wz);
            case Y -> NoiseSampler.sample2D(p, wx, wz);
            case Z -> NoiseSampler.sample2D(p, wx, wy);
        };
    }
}
