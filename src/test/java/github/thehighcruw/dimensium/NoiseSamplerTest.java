/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import org.junit.Test;

import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseParams;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseToolState.NoiseType;

public class NoiseSamplerTest {

    private static final float EPSILON = 1e-6f;

    // ── rawSimplex3 ───────────────────────────────────────────────────────────

    @Test
    public void rawSimplex3IsDeterministic() {
        float a = NoiseSampler.rawSimplex3(1.5f, 2.3f, -0.7f, 42L);
        float b = NoiseSampler.rawSimplex3(1.5f, 2.3f, -0.7f, 42L);
        assertEquals(a, b, EPSILON);
    }

    @Test
    public void rawSimplex3OutputInMinusOneToOne() {
        long seed = 12345L;
        float[] xs = { 0f, 0.5f, 1.23f, -3.7f, 100f };
        float[] ys = { 0f, -1f, 5.1f, 0.001f, -99f };
        float[] zs = { 0f, 2f, -0.5f, 77f, 3.14f };
        for (float x : xs) for (float y : ys) for (float z : zs) {
            float v = NoiseSampler.rawSimplex3(x, y, z, seed);
            assertTrue("rawSimplex3 out of [-1,1] at (" + x + "," + y + "," + z + "): " + v, v >= -1f && v <= 1f);
        }
    }

    @Test
    public void rawSimplex3SensitiveToSeed() {
        // Use non-integer coords to avoid lattice-point zero contributions.
        float a = NoiseSampler.rawSimplex3(0.3f, 0.7f, 0.5f, 1L);
        float b = NoiseSampler.rawSimplex3(0.3f, 0.7f, 0.5f, 1000003L);
        assertNotEquals("different seeds should produce different noise", a, b, EPSILON);
    }

    @Test
    public void rawSimplex3VariesAcrossPositions() {
        // Verify noise is not constant — at least two of these coords must differ.
        long seed = 99L;
        float first = NoiseSampler.rawSimplex3(0.3f, 0.3f, 0.3f, seed);
        boolean anyDiffers = false;
        float[] offsets = { 0.1f, 0.37f, 0.8f, 1.5f, 3.14f };
        for (float o : offsets) {
            if (Math.abs(NoiseSampler.rawSimplex3(0.3f + o, 0.3f, 0.3f, seed) - first) > EPSILON) {
                anyDiffers = true;
                break;
            }
        }
        assertTrue("rawSimplex3 must not be constant across all sampled positions", anyDiffers);
    }

    // ── sample2D ─────────────────────────────────────────────────────────────

    @Test
    public void sample2DOutputInZeroToOne() {
        for (NoiseType type : NoiseType.values()) {
            NoiseParams p = NoiseParams.withDefaults(type)
                .withSeed(7L);
            float[] coords = { 0f, 1f, 5f, -3f, 12.5f };
            for (float x : coords) for (float y : coords) {
                float v = NoiseSampler.sample2D(p, x, y);
                assertTrue(type + ": sample2D out of [0,1] at (" + x + "," + y + "): " + v, v >= 0f && v <= 1f);
            }
        }
    }

    @Test
    public void sample2DIsDeterministic() {
        NoiseParams p = NoiseParams.withDefaults(NoiseType.SIMPLEX)
            .withSeed(55L);
        float a = NoiseSampler.sample2D(p, 3.3f, -1.1f);
        float b = NoiseSampler.sample2D(p, 3.3f, -1.1f);
        assertEquals(a, b, EPSILON);
    }

    @Test
    public void sample2DMultiOctaveStillInRange() {
        NoiseParams p = NoiseParams.withDefaults(NoiseType.PERLIN)
            .withOctaves(4)
            .withSeed(1L);
        for (int i = 0; i < 20; i++) {
            float v = NoiseSampler.sample2D(p, i * 1.3f, i * 0.7f);
            assertTrue("multi-octave sample2D out of [0,1]: " + v, v >= 0f && v <= 1f);
        }
    }

    // ── sample3D ─────────────────────────────────────────────────────────────

    @Test
    public void sample3DOutputInZeroToOne() {
        for (NoiseType type : NoiseType.values()) {
            NoiseParams p = NoiseParams.withDefaults(type)
                .withSeed(3L);
            float[] coords = { 0f, 1f, 5f, -3f };
            for (float x : coords) for (float y : coords) for (float z : coords) {
                float v = NoiseSampler.sample3D(p, x, y, z);
                assertTrue(
                    type + ": sample3D out of [0,1] at (" + x + "," + y + "," + z + "): " + v,
                    v >= 0f && v <= 1f);
            }
        }
    }

    @Test
    public void sample3DIsDeterministic() {
        NoiseParams p = NoiseParams.withDefaults(NoiseType.WORLEY)
            .withSeed(77L);
        float a = NoiseSampler.sample3D(p, 1f, 2f, 3f);
        float b = NoiseSampler.sample3D(p, 1f, 2f, 3f);
        assertEquals(a, b, EPSILON);
    }

    @Test
    public void sample3DMultiOctaveStillInRange() {
        NoiseParams p = NoiseParams.withDefaults(NoiseType.SIMPLEX)
            .withOctaves(6)
            .withSeed(42L);
        for (int i = 0; i < 20; i++) {
            float v = NoiseSampler.sample3D(p, i * 1.1f, i * 0.9f, i * 0.5f);
            assertTrue("multi-octave sample3D out of [0,1]: " + v, v >= 0f && v <= 1f);
        }
    }

    @Test
    public void sample3DSensitiveToSeed() {
        NoiseParams p1 = NoiseParams.withDefaults(NoiseType.SIMPLEX)
            .withSeed(1L);
        NoiseParams p2 = NoiseParams.withDefaults(NoiseType.SIMPLEX)
            .withSeed(1000003L);
        // Use non-integer world coords to avoid lattice-point degeneracy.
        float a = NoiseSampler.sample3D(p1, 5.3f, 5.7f, 5.1f);
        float b = NoiseSampler.sample3D(p2, 5.3f, 5.7f, 5.1f);
        assertNotEquals("different seeds must produce different results", a, b, EPSILON);
    }
}
