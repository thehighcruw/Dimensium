/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.rock;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class RockBrush implements BrushStrategy {

    // Threshold on the post-blur density volume to decide fill/no-fill.
    // Meld factor and blur boundary effects reduce effective density ~30-40% vs pre-blur,
    // so 0.20 reliably places the core rock body while excluding near-zero edge voxels.
    private static final float FILL_THRESHOLD = 0.20f;

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        RockToolState s = RockToolState.INSTANCE;
        BrushState bs = BrushState.INSTANCE;
        SelectedBlockState sbs = SelectedBlockState.INSTANCE;
        Block paint = sbs.getPaintBlock();
        int paintMeta = sbs.getPaintMeta();

        Vec3DInt n = BrushUtil.faceNormal(mop.sideHit);
        Vec3DInt neighbour = n.plus(mop.blockX, mop.blockY, mop.blockZ);
        Vec3DInt brushSize = Vec3DInt.from(
                bs.brushRadius, bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius, bs.brushRadius);
        Vec3DInt dims = brushSize.times(2).plus(1);

        float[] density = buildDensity(s, bs, brushSize, neighbour, dims);

        if (s.smoothingStdDev > 0f) {
            int minRadius = brushSize.min();
            float clampedStdDev = Math.min(s.smoothingStdDev, minRadius / 2.5f);
            if (clampedStdDev > 0f) {
                density = gaussianBlur3D(density, dims, clampedStdDev);
            }
        }

        final float[] finalDensity = density;
        BrushUtil.forEachInShape(bs.brushShape, brushSize, offset -> {
            int idx = offset.plus(brushSize).toIndex(dims);
            if (finalDensity[idx] < FILL_THRESHOLD) return;
            Vec3DInt worldPos = neighbour.plus(offset);
            if (WorldUtils.getBlock(world, worldPos) != Blocks.air) return;
            ChangeProposal.write(world, worldPos, paint, paintMeta);
        });
    }

    private static float[] buildDensity(
            RockToolState s, BrushState bs, Vec3DInt brushSize, Vec3DInt neighbour, Vec3DInt dims) {

        float[] density = new float[dims.product()];
        float noiseRadius = Math.max(0.01f, s.noiseRadius);
        float noisiness = s.noisiness;
        float meldStrength = s.meldStrength;
        long seed = s.noiseSeed;

        Vec3DFloat brushSizeF = brushSize.toFloat();
        Vec3DInt.forEachInclusive(brushSize.negate(), brushSize, offset -> {
            if (!BrushUtil.inShape(bs.brushShape, offset, brushSize)) return;

            // Normalized distance from center [0,1] using ellipsoid metric
            float dist = Math.min(1f, offset.toFloat().divide(brushSizeF).length());

            // Meld factor: fades density to 0 at brush edge
            float meld = meldStrength <= 0f ? 1f : (float) Math.pow(1f - dist, meldStrength);

            // Sphere falloff: strongest at center, zero at edge
            float sphereDensity = 1f - dist;

            // Simplex noise sample in [0,1]
            Vec3DFloat noiseCoord = neighbour.plus(offset).toFloat().divide(noiseRadius);
            float noise = (NoiseSampler.rawSimplex3(noiseCoord.x(), noiseCoord.y(), noiseCoord.z(), seed) + 1f) * 0.5f;

            // Blend sphere shape and noise according to noisiness
            float d = sphereDensity + (noise - sphereDensity) * noisiness;

            density[offset.plus(brushSize).toIndex(dims)] = d * meld;
        });
        return density;
    }

    private static float[] gaussianBlur3D(float[] src, Vec3DInt dims, float stdDev) {
        int kr = (int) Math.ceil(stdDev * 2.5f);
        int kSize = kr * 2 + 1;

        float[] kernel = new float[kSize];
        float sum = 0f;
        for (int i = 0; i < kSize; i++) {
            float x = i - kr;
            kernel[i] = (float) Math.exp(-x * x / (2f * stdDev * stdDev));
            sum += kernel[i];
        }

        for (int i = 0; i < kSize; i++) kernel[i] /= sum;

        float[] tmp1 = blurAxis(src, dims, kernel, kr, 0);
        float[] tmp2 = blurAxis(tmp1, dims, kernel, kr, 1);
        return blurAxis(tmp2, dims, kernel, kr, 2);
    }

    private static float[] blurAxis(float[] src, Vec3DInt dims, float[] kernel, int kr, int axis) {
        float[] dst = new float[dims.product()];

        dims.forEach(pos -> {
            float val = 0f;
            float wSum = 0f;
            for (int k = -kr; k <= kr; k++) {
                Vec3DInt neighbor = pos.withAxis(axis, pos.get(axis) + k);
                if (!neighbor.inBounds(Vec3DInt.ZERO, dims.minus(1))) continue;
                float w = kernel[k + kr];
                val += src[neighbor.toIndex(dims)] * w;
                wSum += w;
            }
            dst[pos.toIndex(dims)] = wSum > 0f ? val / wSum : 0f;
        });

        return dst;
    }
}
