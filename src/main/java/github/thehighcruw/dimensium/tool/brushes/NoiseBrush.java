/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.brushes;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.math.NoiseSampler;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.NoiseToolState;
import github.thehighcruw.dimensium.tool.state.PaletteState;

public class NoiseBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        NoiseToolState s = NoiseToolState.INSTANCE;
        BrushState bs = BrushState.INSTANCE;
        PaletteState ps = PaletteState.INSTANCE;
        if (ps.palette.isEmpty()) return;
        int x = mop.blockX, y = mop.blockY, z = mop.blockZ;
        Block target = world.getBlock(x, y, z);

        boolean useXZ = false, useZY = false;
        if (!s.noise3D) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                double yawRad = Math.toRadians(mc.thePlayer.rotationYaw);
                double pitchRad = Math.toRadians(mc.thePlayer.rotationPitch);
                double cosP = Math.cos(pitchRad);
                double absX = Math.abs(-Math.sin(yawRad) * cosP);
                double absY = Math.abs(Math.sin(pitchRad));
                double absZ = Math.abs(Math.cos(yawRad) * cosP);
                if (absY >= absX && absY >= absZ) useXZ = true;
                else if (absX >= absZ) useZY = true;
            }
        }
        final boolean projXZ = useXZ, projZY = useZY;

        BrushUtil.forBrush(bs, (dx, dy, dz) -> {
            int wx = x + dx, wy = y + dy, wz = z + dz;
            if (world.getBlock(wx, wy, wz) == Blocks.air) return;
            if (s.noiseSurfaceOnly && !BrushUtil.hasAirNeighbor(world, wx, wy, wz)) return;
            if (!BrushUtil.canReplace(world.getBlock(wx, wy, wz), target, bs.replaceMode)) return;

            float noiseVal;
            if (s.noise3D) {
                noiseVal = NoiseSampler.sample3D(s, wx, wy, wz);
            } else if (projXZ) {
                noiseVal = NoiseSampler.sample2D(s, wx, wz);
            } else {
                float nx = projZY ? wz : wx;
                noiseVal = NoiseSampler.sample2D(s, nx, wy);
            }

            ItemStack item = samplePaletteByNoise(ps, noiseVal);
            if (item == null) return;
            Block blk = Block.getBlockFromItem(item.getItem());
            int meta = item.getItemDamage();
            if (blk != null && blk != Blocks.air) ChangeProposal.write(world, wx, wy, wz, blk, meta);
        });
    }

    private static ItemStack samplePaletteByNoise(PaletteState ps, float v) {
        if (ps.palette.isEmpty()) return null;
        int total = ps.totalPaletteWeight();
        if (total == 0) return null;
        int target = (int) (v * total);
        if (target >= total) target = total - 1;
        int cum = 0;
        for (int i = 0; i < ps.palette.size(); i++) {
            cum += ps.getWeight(i);
            if (target < cum) return ps.palette.get(i);
        }
        return ps.palette.get(ps.palette.size() - 1);
    }
}
