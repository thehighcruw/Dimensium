/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool;

import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseParams;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseToolState;

/**
 * Deterministic noise functions for the noise painter tool.
 * All outputs are in [0, 1]. Same inputs → same output always.
 */
public final class NoiseSampler {

    private NoiseSampler() {}

    // ── Public entry points ───────────────────────────────────────────────────

    /** Raw simplex noise in [-1,1] at the given scaled position and seed. */
    public static float rawSimplex3(float x, float y, float z, long seed) {
        return simplex3(x, y, z, seed);
    }

    /** Returns noise in [0,1] for 2D coordinates using NoiseToolState settings. */
    public static float sample2D(NoiseToolState s, float x, float y) {
        return sample2D(s.noiseParams, x, y);
    }

    /** Returns noise in [0,1] for 3D coordinates using NoiseToolState settings. */
    public static float sample3D(NoiseToolState s, float x, float y, float z) {
        return sample3D(s.noiseParams, x, y, z);
    }

    /** Returns noise in [0,1] for 2D coordinates using NoiseParams settings. */
    public static float sample2D(NoiseParams p, float x, float y) {
        float sx = x / p.noiseScale, sy = y / p.noiseScale;
        // normRange is in normalized (post-scale) space; noiseScale controls zoom,
        // noiseMetaballRange controls blob radius relative to cell spacing independently.
        float normRange = p.noiseMetaballRange;
        float raw = fbm2(p, sx, sy, normRange);
        return saturate(raw);
    }

    /** Returns noise in [0,1] for 3D coordinates using NoiseParams settings. */
    public static float sample3D(NoiseParams p, float x, float y, float z) {
        float sx = x / p.noiseScale, sy = y / p.noiseScale, sz = z / p.noiseScale;
        float normRange = p.noiseMetaballRange;
        float raw = fbm3(p, sx, sy, sz, normRange);
        return saturate(raw);
    }

    // ── FBM wrappers ─────────────────────────────────────────────────────────

    private static float fbm2(NoiseParams p, float x, float y, float normRange) {
        if (p.noiseOctaves <= 1) {
            return base2(p, x, y, p.noiseSeed, normRange);
        }
        float value = 0, amplitude = 1, totalAmp = 0;
        float fx = x, fy = y;
        for (int o = 0; o < p.noiseOctaves; o++) {
            value += amplitude * base2(p, fx, fy, p.noiseSeed + o * 1000003L, normRange);
            totalAmp += amplitude;
            amplitude *= p.noiseGain;
            fx *= p.noiseLacunarity;
            fy *= p.noiseLacunarity;
        }
        return value / totalAmp;
    }

    private static float fbm3(NoiseParams p, float x, float y, float z, float normRange) {
        if (p.noiseOctaves <= 1) {
            return base3(p, x, y, z, p.noiseSeed, normRange);
        }
        float value = 0, amplitude = 1, totalAmp = 0;
        float fx = x, fy = y, fz = z;
        for (int o = 0; o < p.noiseOctaves; o++) {
            value += amplitude * base3(p, fx, fy, fz, p.noiseSeed + o * 1000003L, normRange);
            totalAmp += amplitude;
            amplitude *= p.noiseGain;
            fx *= p.noiseLacunarity;
            fy *= p.noiseLacunarity;
            fz *= p.noiseLacunarity;
        }
        return value / totalAmp;
    }

    private static float base2(NoiseParams p, float x, float y, long seed, float normRange) {
        return switch (p.noiseType) {
            case SIMPLEX -> (simplex2(x, y, seed) + 1f) * 0.5f;
            case PERLIN -> (perlin2(x, y, seed) + 1f) * 0.5f;
            case WORLEY -> worley2(x, y, seed, p.noiseJitter, p.noiseW1, p.noiseW2, p.noiseW3);
            case VORONOI_EDGES -> voronoiEdge2(x, y, seed, p.noiseJitter);
            case METABALL -> metaball2(x, y, seed, p.noiseJitter, normRange);
            case SPLATTER -> splatter2(x, y, seed, p.noiseJitter);
            case WHITE -> white2(x, y, seed);
        };
    }

    private static float base3(NoiseParams p, float x, float y, float z, long seed, float normRange) {
        return switch (p.noiseType) {
            case SIMPLEX -> (simplex3(x, y, z, seed) + 1f) * 0.5f;
            case PERLIN -> (perlin3(x, y, z, seed) + 1f) * 0.5f;
            case WORLEY -> worley3(x, y, z, seed, p.noiseJitter, p.noiseW1, p.noiseW2, p.noiseW3);
            case VORONOI_EDGES -> voronoiEdge3(x, y, z, seed, p.noiseJitter);
            case METABALL -> metaball3(x, y, z, seed, p.noiseJitter, normRange);
            case SPLATTER -> splatter3(x, y, z, seed, p.noiseJitter);
            case WHITE -> white3(x, y, z, seed);
        };
    }

    // ── White noise ───────────────────────────────────────────────────────────

    private static float white2(float x, float y, long seed) {
        long h = hash(fastFloor(x), fastFloor(y), seed);
        return (h & 0xFFFFL) / 65535f;
    }

    private static float white3(float x, float y, float z, long seed) {
        long h = hash3(fastFloor(x), fastFloor(y), fastFloor(z), seed);
        return (h & 0xFFFFL) / 65535f;
    }

    // ── Perlin noise 2D ───────────────────────────────────────────────────────

    private static float perlin2(float x, float y, long seed) {
        int x0 = fastFloor(x), y0 = fastFloor(y);
        int x1 = x0 + 1, y1 = y0 + 1;
        float dx = x - x0, dy = y - y0;
        float u = fade(dx), v = fade(dy);
        float n00 = dotGrad2(x0, y0, dx, dy, seed);
        float n10 = dotGrad2(x1, y0, dx - 1f, dy, seed);
        float n01 = dotGrad2(x0, y1, dx, dy - 1f, seed);
        float n11 = dotGrad2(x1, y1, dx - 1f, dy - 1f, seed);
        float nx0 = lerp(n00, n10, u);
        float nx1 = lerp(n01, n11, u);
        return lerp(nx0, nx1, v) * 1.4142f; // scale to approx [-1,1]
    }

    private static float dotGrad2(int ix, int iy, float dx, float dy, long seed) {
        int g = Math.floorMod(perm(ix, iy, seed), 8);
        return GRAD2[g][0] * dx + GRAD2[g][1] * dy;
    }

    // ── Perlin noise 3D ───────────────────────────────────────────────────────

    private static float perlin3(float x, float y, float z, long seed) {
        int x0 = fastFloor(x), y0 = fastFloor(y), z0 = fastFloor(z);
        int x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;
        float dx = x - x0, dy = y - y0, dz = z - z0;
        float u = fade(dx), v = fade(dy), w = fade(dz);
        float n000 = dotGrad3(x0, y0, z0, dx, dy, dz, seed);
        float n100 = dotGrad3(x1, y0, z0, dx - 1f, dy, dz, seed);
        float n010 = dotGrad3(x0, y1, z0, dx, dy - 1f, dz, seed);
        float n110 = dotGrad3(x1, y1, z0, dx - 1f, dy - 1f, dz, seed);
        float n001 = dotGrad3(x0, y0, z1, dx, dy, dz - 1f, seed);
        float n101 = dotGrad3(x1, y0, z1, dx - 1f, dy, dz - 1f, seed);
        float n011 = dotGrad3(x0, y1, z1, dx, dy - 1f, dz - 1f, seed);
        float n111 = dotGrad3(x1, y1, z1, dx - 1f, dy - 1f, dz - 1f, seed);
        float nx00 = lerp(n000, n100, u);
        float nx10 = lerp(n010, n110, u);
        float nx01 = lerp(n001, n101, u);
        float nx11 = lerp(n011, n111, u);
        float ny0 = lerp(nx00, nx10, v);
        float ny1 = lerp(nx01, nx11, v);
        return lerp(ny0, ny1, w) * 1.7321f; // scale to approx [-1,1]
    }

    private static float dotGrad3(int ix, int iy, int iz, float dx, float dy, float dz, long seed) {
        int g = Math.floorMod(perm3(ix, iy, iz, seed), 12);
        float[] gv = GRAD3[g];
        return gv[0] * dx + gv[1] * dy + gv[2] * dz;
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    // ── Simplex noise 2D ──────────────────────────────────────────────────────

    private static final float F2 = 0.3660254f;
    private static final float G2 = 0.2113249f;

    private static float simplex2(float x, float y, long seed) {
        float s = (x + y) * F2;
        int i = fastFloor(x + s), j = fastFloor(y + s);
        float t = (i + j) * G2;
        float x0 = x - (i - t), y0 = y - (j - t);
        int i1, j1;
        if (x0 > y0) {
            i1 = 1;
            j1 = 0;
        } else {
            i1 = 0;
            j1 = 1;
        }
        float x1 = x0 - i1 + G2, y1 = y0 - j1 + G2;
        float x2 = x0 - 1f + 2f * G2, y2 = y0 - 1f + 2f * G2;
        float n0 = grad2contrib(i, j, x0, y0, seed);
        float n1 = grad2contrib(i + i1, j + j1, x1, y1, seed);
        float n2 = grad2contrib(i + 1, j + 1, x2, y2, seed);
        return 70f * (n0 + n1 + n2);
    }

    private static float grad2contrib(int gi, int gj, float x, float y, long seed) {
        float t = 0.5f - x * x - y * y;
        if (t < 0) return 0;
        t *= t;
        int g = Math.floorMod(perm(gi, gj, seed), 8);
        float gx = GRAD2[g][0], gy = GRAD2[g][1];
        return t * t * (gx * x + gy * y);
    }

    private static final float[][] GRAD2 = { { 1, 1 }, { -1, 1 }, { 1, -1 }, { -1, -1 }, { 1, 0 }, { -1, 0 }, { 0, 1 },
        { 0, -1 } };

    // ── Simplex noise 3D ──────────────────────────────────────────────────────

    private static final float F3 = 1f / 3f;
    private static final float G3 = 1f / 6f;

    private static float simplex3(float x, float y, float z, long seed) {
        float s = (x + y + z) * F3;
        int i = fastFloor(x + s), j = fastFloor(y + s), k = fastFloor(z + s);
        float t = (i + j + k) * G3;
        float x0 = x - (i - t), y0 = y - (j - t), z0 = z - (k - t);
        int i1, j1, k1, i2, j2, k2;
        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            } else if (x0 >= z0) {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            } else {
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            }
        } else {
            if (y0 < z0) {
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 0;
                j2 = 1;
                k2 = 1;
            } else if (x0 < z0) {
                i1 = 0;
                j1 = 1;
                k1 = 0;
                i2 = 0;
                j2 = 1;
                k2 = 1;
            } else {
                i1 = 0;
                j1 = 1;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            }
        }
        float x1 = x0 - i1 + G3, y1 = y0 - j1 + G3, z1 = z0 - k1 + G3;
        float x2 = x0 - i2 + 2 * G3, y2 = y0 - j2 + 2 * G3, z2 = z0 - k2 + 2 * G3;
        float x3 = x0 - 1 + 3 * G3, y3 = y0 - 1 + 3 * G3, z3 = z0 - 1 + 3 * G3;
        float n0 = grad3contrib(i, j, k, x0, y0, z0, seed);
        float n1 = grad3contrib(i + i1, j + j1, k + k1, x1, y1, z1, seed);
        float n2 = grad3contrib(i + i2, j + j2, k + k2, x2, y2, z2, seed);
        float n3 = grad3contrib(i + 1, j + 1, k + 1, x3, y3, z3, seed);
        return 32f * (n0 + n1 + n2 + n3);
    }

    private static float grad3contrib(int gi, int gj, int gk, float x, float y, float z, long seed) {
        float t = 0.6f - x * x - y * y - z * z;
        if (t < 0) return 0;
        t *= t;
        int g = Math.floorMod(perm3(gi, gj, gk, seed), 12);
        float[] gv = GRAD3[g];
        return t * t * (gv[0] * x + gv[1] * y + gv[2] * z);
    }

    private static final float[][] GRAD3 = { { 1, 1, 0 }, { -1, 1, 0 }, { 1, -1, 0 }, { -1, -1, 0 }, { 1, 0, 1 },
        { -1, 0, 1 }, { 1, 0, -1 }, { -1, 0, -1 }, { 0, 1, 1 }, { 0, -1, 1 }, { 0, 1, -1 }, { 0, -1, -1 } };

    // ── Worley (F1/F2/F3 with weights) ───────────────────────────────────────

    private static float worley2(float x, float y, long seed, float jitter, float w1, float w2, float w3) {
        int ix = fastFloor(x), iy = fastFloor(y);
        float f1 = Float.MAX_VALUE, f2 = Float.MAX_VALUE, f3 = Float.MAX_VALUE;
        // Search 2-cell radius to reliably find F2/F3
        for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            int cx = ix + dx, cy = iy + dy;
            long h = hash(cx, cy, seed);
            float px = cx + jitter * ((h & 0xFFFF) / 65535f - 0.5f) * 2f;
            float py = cy + jitter * (((h >> 16) & 0xFFFF) / 65535f - 0.5f) * 2f;
            float d = (float) Math.sqrt(dist2(x - px, y - py));
            if (d < f1) {
                f3 = f2;
                f2 = f1;
                f1 = d;
            } else if (d < f2) {
                f3 = f2;
                f2 = d;
            } else if (d < f3) {
                f3 = d;
            }
        }
        float v = w1 * f1 + w2 * f2 + w3 * f3;
        return saturate(v * 0.5f); // scale so typical F1 (~0.5) maps to mid-range
    }

    private static float worley3(float x, float y, float z, long seed, float jitter, float w1, float w2, float w3) {
        int ix = fastFloor(x), iy = fastFloor(y), iz = fastFloor(z);
        float f1 = Float.MAX_VALUE, f2 = Float.MAX_VALUE, f3 = Float.MAX_VALUE;
        for (int dz = -2; dz <= 2; dz++) for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            int cx = ix + dx, cy = iy + dy, cz = iz + dz;
            long h = hash3(cx, cy, cz, seed);
            float px = cx + jitter * ((h & 0xFFFF) / 65535f - 0.5f) * 2f;
            float py = cy + jitter * (((h >> 16) & 0xFFFF) / 65535f - 0.5f) * 2f;
            float pz = cz + jitter * (((h >> 32) & 0xFFFF) / 65535f - 0.5f) * 2f;
            float d = (float) Math.sqrt(dist2(x - px, y - py) + dist2(z - pz, 0));
            if (d < f1) {
                f3 = f2;
                f2 = f1;
                f1 = d;
            } else if (d < f2) {
                f3 = f2;
                f2 = d;
            } else if (d < f3) {
                f3 = d;
            }
        }
        float v = w1 * f1 + w2 * f2 + w3 * f3;
        return saturate(v * 0.5f);
    }

    // ── Voronoi edges (F2 - F1) ───────────────────────────────────────────────

    private static float voronoiEdge2(float x, float y, long seed, float jitter) {
        int ix = fastFloor(x), iy = fastFloor(y);
        float f1 = Float.MAX_VALUE, f2 = Float.MAX_VALUE;
        for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            int cx = ix + dx, cy = iy + dy;
            long h = hash(cx, cy, seed);
            float px = cx + jitter * ((h & 0xFFFF) / 65535f - 0.5f) * 2f;
            float py = cy + jitter * (((h >> 16) & 0xFFFF) / 65535f - 0.5f) * 2f;
            float d = (float) Math.sqrt(dist2(x - px, y - py));
            if (d < f1) {
                f2 = f1;
                f1 = d;
            } else if (d < f2) {
                f2 = d;
            }
        }
        return saturate((f2 - f1) * 2f);
    }

    private static float voronoiEdge3(float x, float y, float z, long seed, float jitter) {
        int ix = fastFloor(x), iy = fastFloor(y), iz = fastFloor(z);
        float f1 = Float.MAX_VALUE, f2 = Float.MAX_VALUE;
        for (int dz = -2; dz <= 2; dz++) for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            int cx = ix + dx, cy = iy + dy, cz = iz + dz;
            long h = hash3(cx, cy, cz, seed);
            float px = cx + jitter * ((h & 0xFFFF) / 65535f - 0.5f) * 2f;
            float py = cy + jitter * (((h >> 16) & 0xFFFF) / 65535f - 0.5f) * 2f;
            float pz = cz + jitter * (((h >> 32) & 0xFFFF) / 65535f - 0.5f) * 2f;
            float d = (float) Math.sqrt(dist2(x - px, y - py) + dist2(z - pz, 0));
            if (d < f1) {
                f2 = f1;
                f1 = d;
            } else if (d < f2) {
                f2 = d;
            }
        }
        return saturate((f2 - f1) * 2f);
    }

    // ── Metaball ──────────────────────────────────────────────────────────────

    private static float metaball2(float x, float y, long seed, float jitter, float normRange) {
        if (normRange <= 0) normRange = 0.5f;
        // Blob radius = 60% of cell spacing — allows gaps between isolated balls,
        // merging (real metaball look) when high jitter pushes ball centers close together.
        float r = normRange * 0.6f;
        float r2 = r * r;
        int ix = fastFloor(x / normRange), iy = fastFloor(y / normRange);
        float sum = 0;
        for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            int cx = ix + dx, cy = iy + dy;
            long h = hash(cx, cy, seed);
            float px = (cx + 0.5f + jitter * ((h & 0xFFFF) / 65535f - 0.5f)) * normRange;
            float py = (cy + 0.5f + jitter * (((h >> 16) & 0xFFFF) / 65535f - 0.5f)) * normRange;
            float d2 = dist2(x - px, y - py);
            if (d2 < r2) {
                float t = 1f - d2 / r2;
                sum += t * t;
            }
        }
        return saturate(sum);
    }

    private static float metaball3(float x, float y, float z, long seed, float jitter, float normRange) {
        if (normRange <= 0) normRange = 0.5f;
        float r = normRange * 0.6f;
        float r2 = r * r;
        int ix = fastFloor(x / normRange), iy = fastFloor(y / normRange), iz = fastFloor(z / normRange);
        float sum = 0;
        for (int dz = -2; dz <= 2; dz++) for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            int cx = ix + dx, cy = iy + dy, cz = iz + dz;
            long h = hash3(cx, cy, cz, seed);
            float px = (cx + 0.5f + jitter * ((h & 0xFFFF) / 65535f - 0.5f)) * normRange;
            float py = (cy + 0.5f + jitter * (((h >> 16) & 0xFFFF) / 65535f - 0.5f)) * normRange;
            float pz = (cz + 0.5f + jitter * (((h >> 32) & 0xFFFF) / 65535f - 0.5f)) * normRange;
            float d2 = dist2(x - px, y - py) + dist2(z - pz, 0);
            if (d2 < r2) {
                float t = 1f - d2 / r2;
                sum += t * t;
            }
        }
        return saturate(sum);
    }

    // ── Splatter (sharp voronoi, binary per-cell) ─────────────────────────────

    // Splatter uses 3× larger cells than white noise at the same scale so regions are
    // visually distinct (irregular Voronoi blobs vs white's 1-block squares).
    private static final float SPLATTER_CELL = 3f;

    private static float splatter2(float x, float y, long seed, float jitter) {
        float sx = x / SPLATTER_CELL, sy = y / SPLATTER_CELL;
        int ix = fastFloor(sx), iy = fastFloor(sy);
        float minDist = Float.MAX_VALUE;
        long bestHash = 0;
        for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            int cx = ix + dx, cy = iy + dy;
            long h = hash(cx, cy, seed);
            float px = (cx + 0.5f + jitter * ((h & 0xFFFF) / 65535f - 0.5f)) * SPLATTER_CELL;
            float py = (cy + 0.5f + jitter * (((h >> 16) & 0xFFFF) / 65535f - 0.5f)) * SPLATTER_CELL;
            float d = dist2(x - px, y - py);
            if (d < minDist) {
                minDist = d;
                bestHash = h;
            }
        }
        return ((bestHash >> 32) & 0xFFFFL) / 65535f;
    }

    private static float splatter3(float x, float y, float z, long seed, float jitter) {
        float sx = x / SPLATTER_CELL, sy = y / SPLATTER_CELL, sz = z / SPLATTER_CELL;
        int ix = fastFloor(sx), iy = fastFloor(sy), iz = fastFloor(sz);
        float minDist = Float.MAX_VALUE;
        long bestHash = 0;
        for (int dz = -2; dz <= 2; dz++) for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            int cx = ix + dx, cy = iy + dy, cz = iz + dz;
            long h = hash3(cx, cy, cz, seed);
            float px = (cx + 0.5f + jitter * ((h & 0xFFFF) / 65535f - 0.5f)) * SPLATTER_CELL;
            float py = (cy + 0.5f + jitter * (((h >> 16) & 0xFFFF) / 65535f - 0.5f)) * SPLATTER_CELL;
            float pz = (cz + 0.5f + jitter * (((h >> 32) & 0xFFFF) / 65535f - 0.5f)) * SPLATTER_CELL;
            float d = dist2(x - px, y - py) + dist2(z - pz, 0);
            if (d < minDist) {
                minDist = d;
                bestHash = h;
            }
        }
        return ((bestHash >> 48) & 0xFFFFL) / 65535f;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static float dist2(float a, float b) {
        return a * a + b * b;
    }

    private static float saturate(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static int fastFloor(float x) {
        int i = (int) x;
        return x < i ? i - 1 : i;
    }

    private static long hash(long x, long y, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0x6C62272E07BB0142L);
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return h;
    }

    private static long hash(int x, int y, long seed) {
        return hash((long) x, (long) y, seed);
    }

    private static long hash3(int x, int y, int z, long seed) {
        return hash((long) x ^ ((long) z * 0x6C62272E07BB0142L), (long) y, seed ^ z);
    }

    private static int perm(int x, int y, long seed) {
        return (int) (hash(x, y, seed) >>> 32);
    }

    private static int perm3(int x, int y, int z, long seed) {
        return (int) (hash3(x, y, z, seed) >>> 32);
    }
}
