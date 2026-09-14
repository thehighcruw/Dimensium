/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import github.thehighcruw.dimensium.editor.tool.brushes.GaussianKernel;
import github.thehighcruw.dimensium.shared.Vec3DInt;

public class GaussianKernelTest {

    private static final float EPSILON = 1e-4f;

    // ── build invariants ──────────────────────────────────────────────────────

    @Test
    public void totalWeightEqualsSumOfKernelData() {
        GaussianKernel k = GaussianKernel.build(1.0f);
        float sum = 0f;
        for (float v : k.data) sum += v;
        assertEquals(k.totalWeight, sum, EPSILON);
    }

    @Test
    public void totalWeightPositive() {
        for (float sigma : new float[] { 0.5f, 1.0f, 2.0f, 3.0f }) {
            GaussianKernel k = GaussianKernel.build(sigma);
            assertTrue("totalWeight must be > 0 for sigma=" + sigma, k.totalWeight > 0f);
        }
    }

    @Test
    public void kernelRadiusMatchesCeilSigmaTimesTwo() {
        for (float sigma : new float[] { 0.5f, 1.0f, 1.5f, 2.0f }) {
            GaussianKernel k = GaussianKernel.build(sigma);
            int expected = (int) Math.ceil(sigma * 2f);
            assertEquals("kR for sigma=" + sigma, expected, k.kR);
        }
    }

    @Test
    public void kernelDataSizeMatchesDimension() {
        GaussianKernel k = GaussianKernel.build(1.0f);
        int dim = 2 * k.kR + 1;
        assertEquals(dim * dim * dim, k.data.length);
    }

    @Test
    public void allKernelValuesPositive() {
        GaussianKernel k = GaussianKernel.build(1.5f);
        for (float v : k.data) assertTrue("kernel value must be > 0", v > 0f);
    }

    @Test
    public void centreTapIsMaximumValue() {
        GaussianKernel k = GaussianKernel.build(1.0f);
        float centre = k.data[k.kR * k.strideX + k.kR * k.strideY + k.kR];
        float max = 0f;
        for (float v : k.data) if (v > max) max = v;
        assertEquals("centre tap must equal max value", max, centre, EPSILON);
    }

    @Test
    public void largeSigmaHasLargerRadius() {
        GaussianKernel small = GaussianKernel.build(0.5f);
        GaussianKernel large = GaussianKernel.build(2.0f);
        assertTrue("larger sigma → larger kR", large.kR > small.kR);
    }

    // ── solidWeight ───────────────────────────────────────────────────────────

    /**
     * Build a snap volume where every voxel is solid, with margin padding.
     * The centre voxel should return solidWeight == totalWeight (all neighbours solid).
     */
    @Test
    public void solidWeightFullyFilledEqualsTotal() {
        GaussianKernel k = GaussianKernel.build(1.0f);
        int margin = k.kR;
        // Volume must be at least (2*margin+1)^3 to avoid out-of-bounds
        int dim = 2 * margin + 3; // one extra layer on each side
        int stX = dim * dim;
        int[] snap = new int[dim * dim * dim];
        // Fill entirely with solid
        Arrays.fill(snap, 1);

        // Sample at the centre of the volume
        int cx = dim / 2, cy = dim / 2, cz = dim / 2;
        float sw = k.solidWeight(snap, Vec3DInt.from(cx, cy, cz), stX, dim);
        assertEquals("all-solid neighbourhood → solidWeight equals totalWeight", k.totalWeight, sw, EPSILON);
    }

    @Test
    public void solidWeightEmptyNeighbourhoodIsZero() {
        GaussianKernel k = GaussianKernel.build(1.0f);
        int margin = k.kR;
        int dim = 2 * margin + 3;
        int stX = dim * dim;
        int[] snap = new int[dim * dim * dim]; // all zeros (air)

        int cx = dim / 2, cy = dim / 2, cz = dim / 2;
        float sw = k.solidWeight(snap, Vec3DInt.from(cx, cy, cz), stX, dim);
        assertEquals("all-air neighbourhood → solidWeight is 0", 0f, sw, EPSILON);
    }

    @Test
    public void solidWeightOnlyCentreSolidIsCentreTapWeight() {
        GaussianKernel k = GaussianKernel.build(1.0f);
        int margin = k.kR;
        int dim = 2 * margin + 3;
        int stX = dim * dim;
        int[] snap = new int[dim * dim * dim];

        int cx = dim / 2, cy = dim / 2, cz = dim / 2;
        snap[cx * stX + cy * dim + cz] = 1;

        float sw = k.solidWeight(snap, Vec3DInt.from(cx, cy, cz), stX, dim);
        float centreTap = k.data[k.kR * k.strideX + k.kR * k.strideY + k.kR];
        assertEquals("only centre solid → solidWeight equals centre kernel tap", centreTap, sw, EPSILON);
    }

    @Test
    public void solidWeightNeverExceedsTotal() {
        GaussianKernel k = GaussianKernel.build(1.0f);
        int margin = k.kR;
        int dim = 2 * margin + 3;
        int stX = dim * dim;
        int[] snap = new int[dim * dim * dim];
        Arrays.fill(snap, 1);

        int cx = dim / 2, cy = dim / 2, cz = dim / 2;
        float sw = k.solidWeight(snap, Vec3DInt.from(cx, cy, cz), stX, dim);
        assertTrue("solidWeight must not exceed totalWeight", sw <= k.totalWeight + EPSILON);
    }
}
