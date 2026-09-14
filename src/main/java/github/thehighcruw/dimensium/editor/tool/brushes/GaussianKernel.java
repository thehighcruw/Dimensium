/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.brushes;

import github.thehighcruw.dimensium.shared.Vec3DInt;

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
        float total = 0f;
        int stX = kDim * kDim;
        for (int kx = -kR; kx <= kR; kx++) for (int ky = -kR; ky <= kR; ky++) for (int kz = -kR; kz <= kR; kz++) {
            float w = (float) Math.exp(-(kx * kx + ky * ky + kz * kz) * inv2s2);
            kernel[(kx + kR) * stX + (ky + kR) * kDim + (kz + kR)] = w;
            total += w;
        }
        return new GaussianKernel(kernel, kR, total, stX, kDim);
    }

    public float solidWeight(int[] snapId, Vec3DInt index, int snStX, int snStY) {
        float solidW = 0f;
        for (int kx = -kR; kx <= kR; kx++) {
            int nxBase = (index.x() + kx) * snStX;
            int kxBase = (kx + kR) * strideX;
            for (int ky = -kR; ky <= kR; ky++) {
                int nyBase = nxBase + (index.y() + ky) * snStY;
                int kyBase = kxBase + (ky + kR) * strideY;
                for (int kz = -kR; kz <= kR; kz++) {
                    if (snapId[nyBase + index.z() + kz] != 0) solidW += data[kyBase + (kz + kR)];
                }
            }
        }
        return solidW;
    }
}
