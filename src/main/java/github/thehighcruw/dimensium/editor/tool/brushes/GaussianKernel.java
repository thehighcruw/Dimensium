/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.brushes;

import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;

public final class GaussianKernel {

    public final float[] data;
    public final int kR;
    public final float totalWeight;

    public final int strideX;
    public final int strideY;

    private GaussianKernel(float[] data, int kR, float totalWeight, int strideX, int strideY) {
        this.data = data;
        this.kR = kR;
        this.totalWeight = totalWeight;

        this.strideX = strideX;
        this.strideY = strideY;
    }

    public static GaussianKernel build(float sigma) {
        int kR = (int) Math.ceil(sigma * 2f);
        int kDim = 2 * kR + 1;
        float[] kernel = new float[kDim * kDim * kDim];
        float inv2s2 = 1f / (2f * sigma * sigma);
        int stX = kDim * kDim;
        float[] total = {0f};
        Vec3DInt bound = Vec3DInt.from(kR, kR, kR);
        Vec3DInt.forEachInclusive(bound.negate(), bound, (kx, ky, kz) -> {
            Vec3DFloat k = Vec3DFloat.from(kx, ky, kz);
            float w = (float) Math.exp(-k.dot(k) * inv2s2);
            kernel[Vec3DInt.from(kx + kR, ky + kR, kz + kR).toIndex(stX, kDim)] = w;
            total[0] += w;
        });
        return new GaussianKernel(kernel, kR, total[0], stX, kDim);
    }

    public float solidWeight(int[] snapId, Vec3DInt index, int snStX, int snStY) {
        float[] solidW = {0f};
        Vec3DInt bound = Vec3DInt.from(kR, kR, kR);
        Vec3DInt.forEachInclusive(bound.negate(), bound, (kx, ky, kz) -> {
            int snapIdx = (index.x() + kx) * snStX + (index.y() + ky) * snStY + index.z() + kz;
            if (snapId[snapIdx] != 0) solidW[0] += data[(kx + kR) * strideX + (ky + kR) * strideY + (kz + kR)];
        });
        return solidW[0];
    }
}
