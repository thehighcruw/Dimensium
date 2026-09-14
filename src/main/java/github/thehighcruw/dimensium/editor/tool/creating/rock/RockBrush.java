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

        int[] n = BrushUtil.faceNormal(mop.sideHit);
        int ox = mop.blockX + n[0], oy = mop.blockY + n[1], oz = mop.blockZ + n[2];
        int sx = bs.brushRadius;
        int sy = bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius;
        int dimX = 2 * sx + 1, dimY = 2 * sy + 1, dimZ = 2 * sx + 1;
        int strideX = dimY * dimZ;

        float[] density = buildDensity(s, bs, sx, sy, sx, ox, oy, oz, dimX, dimY, dimZ, strideX, dimZ);

        if (s.smoothingStdDev > 0f) {
            int minRadius = Math.min(sy, sx);
            float clampedStdDev = Math.min(s.smoothingStdDev, minRadius / 2.5f);
            if (clampedStdDev > 0f) {
                density = gaussianBlur3D(density, dimX, dimY, dimZ, clampedStdDev);
            }
        }

        for (int dx = -sx; dx <= sx; dx++) {
            for (int dy = -sy; dy <= sy; dy++) {
                for (int dz = -sx; dz <= sx; dz++) {
                    if (!BrushUtil.inShape(bs.brushShape, dx, dy, dz, sx, sy, sx)) continue;
                    int idx = (dx + sx) * strideX + (dy + sy) * dimZ + (dz + sx);
                    if (density[idx] < FILL_THRESHOLD) continue;

                    int wx = ox + dx, wy = oy + dy, wz = oz + dz;
                    if (world.getBlock(wx, wy, wz) != Blocks.air) continue;

                    ChangeProposal.write(world, wx, wy, wz, paint, paintMeta);
                }
            }
        }
    }

    private static float[] buildDensity(
            RockToolState s,
            BrushState bs,
            int sx,
            int sy,
            int sz,
            int ox,
            int oy,
            int oz,
            int dimX,
            int dimY,
            int dimZ,
            int strideX,
            int strideY) {

        float[] density = new float[dimX * dimY * dimZ];
        float noiseRadius = Math.max(0.01f, s.noiseRadius);
        float noisiness = s.noisiness;
        float meldStrength = s.meldStrength;
        long seed = s.noiseSeed;

        for (int dx = -sx; dx <= sx; dx++) {
            for (int dy = -sy; dy <= sy; dy++) {
                for (int dz = -sz; dz <= sz; dz++) {
                    if (!BrushUtil.inShape(bs.brushShape, dx, dy, dz, sx, sy, sz)) continue;

                    // Normalized distance from center [0,1] using ellipsoid metric
                    float ex = sx > 0 ? (float) dx / sx : 0f;
                    float ey = sy > 0 ? (float) dy / sy : 0f;
                    float ez = sz > 0 ? (float) dz / sz : 0f;
                    float dist = Math.min(1f, (float) Math.sqrt(ex * ex + ey * ey + ez * ez));

                    // Meld factor: fades density to 0 at brush edge
                    float meld = meldStrength <= 0f ? 1f : (float) Math.pow(1f - dist, meldStrength);

                    // Sphere falloff: strongest at center, zero at edge
                    float sphereDensity = 1f - dist;

                    // Simplex noise sample in [0,1]
                    float nx = (ox + dx) / noiseRadius;
                    float ny = (oy + dy) / noiseRadius;
                    float nz = (oz + dz) / noiseRadius;
                    float noise = (NoiseSampler.rawSimplex3(nx, ny, nz, seed) + 1f) * 0.5f;

                    // Blend sphere shape and noise according to noisiness
                    float d = sphereDensity + (noise - sphereDensity) * noisiness;

                    density[(dx + sx) * strideX + (dy + sy) * strideY + (dz + sz)] = d * meld;
                }
            }
        }
        return density;
    }

    private static float[] gaussianBlur3D(float[] src, int dimX, int dimY, int dimZ, float stdDev) {
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

        float[] tmp1 = blurAxis(src, dimX, dimY, dimZ, kernel, kr, 0);
        float[] tmp2 = blurAxis(tmp1, dimX, dimY, dimZ, kernel, kr, 1);
        return blurAxis(tmp2, dimX, dimY, dimZ, kernel, kr, 2);
    }

    private static float[] blurAxis(float[] src, int dimX, int dimY, int dimZ, float[] kernel, int kr, int axis) {
        int strideX = dimY * dimZ;
        float[] dst = new float[dimX * dimY * dimZ];
        for (int x = 0; x < dimX; x++) {
            for (int y = 0; y < dimY; y++) {
                for (int z = 0; z < dimZ; z++) {
                    float val = 0f;
                    float wSum = 0f;
                    for (int k = -kr; k <= kr; k++) {
                        int nx = x, ny = y, nz = z;
                        if (axis == 0) nx += k;
                        else if (axis == 1) ny += k;
                        else nz += k;
                        if (nx < 0 || nx >= dimX || ny < 0 || ny >= dimY || nz < 0 || nz >= dimZ) continue;
                        float w = kernel[k + kr];
                        val += src[nx * strideX + ny * dimZ + nz] * w;
                        wSum += w;
                    }
                    dst[x * strideX + y * dimZ + z] = wSum > 0f ? val / wSum : 0f;
                }
            }
        }
        return dst;
    }
}
