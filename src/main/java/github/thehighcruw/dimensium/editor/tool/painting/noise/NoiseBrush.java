/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.painting.noise;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.editor.tool.state.PaletteState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class NoiseBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        NoiseToolState s = NoiseToolState.INSTANCE;
        BrushState bs = BrushState.INSTANCE;
        PaletteState ps = PaletteState.INSTANCE;
        if (ps.palette.isEmpty()) return;
        Vec3DInt coord = WorldUtils.mopToCoord(mop);

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

        BrushUtil.forBrush(bs, offset -> {
            Vec3DInt pos = coord.plus(offset);
            if (WorldUtils.getBlock(world, pos) == Blocks.air) return;
            if (s.noiseSurfaceOnly && BrushUtil.hasSolidNeighbor(world, pos)) return;

            float noiseVal;
            if (s.noise3D) {
                noiseVal = NoiseSampler.sample3D(s, pos.x(), pos.y(), pos.z());
            } else if (projXZ) {
                noiseVal = NoiseSampler.sample2D(s, pos.x(), pos.z());
            } else {
                float nx = projZY ? pos.z() : pos.x();
                noiseVal = NoiseSampler.sample2D(s, nx, pos.y());
            }

            ItemStack item = samplePaletteByNoise(ps, noiseVal);
            if (item == null) return;
            Block blk = Block.getBlockFromItem(item.getItem());
            int meta = item.getItemDamage();
            if (blk != null && blk != Blocks.air) ChangeProposal.write(world, pos, blk, meta);
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
