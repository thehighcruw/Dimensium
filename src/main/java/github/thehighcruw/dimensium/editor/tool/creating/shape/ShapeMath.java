/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.shape;

/**
 * Pure-math shape geometry — no Minecraft state.
 * Shared between BrushApplicator (server placement) and ShapePlacementState (client ghost).
 */
public class ShapeMath {

    public static boolean inShapeGeom(ShapeToolState.ShapeType type, int dx, int dy, int dz, int w, int h, int d,
        boolean hollow, float exponent, int torusRingR, int torusRingRZ, int torusTubeR, int tubeWallThickness,
        float supersphereExp, int polygonSides, float spiralSpacing, float spiralTurns, float threshold) {
        float cx = (w - 1) / 2f, cy = (h - 1) / 2f, cz = (d - 1) / 2f;
        float rx = w / 2f, ry = h / 2f, rz = d / 2f;
        switch (type) {
            case CUBOID:
                if (!hollow) return true;
                return dx == 0 || dx == w - 1 || dy == 0 || dy == h - 1 || dz == 0 || dz == d - 1;

            case SPHERE: {
                float ex = (dx - cx) / rx, ey = (dy - cy) / ry, ez = (dz - cz) / rz;
                float dist = ex * ex + ey * ey + ez * ez;
                float vR = 0.5f * (float) Math.sqrt(1f / (rx * rx) + 1f / (ry * ry) + 1f / (rz * rz));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                float irx = Math.max(1, rx - 1), iry = Math.max(1, ry - 1), irz = Math.max(1, rz - 1);
                float ix = (dx - cx) / irx, iy = (dy - cy) / iry, iz = (dz - cz) / irz;
                return outer && ix * ix + iy * iy + iz * iz > 1f;
            }
            case CYLINDER: {
                float ex = (dx - cx) / rx, ez = (dz - cz) / rz;
                float dist = ex * ex + ez * ez;
                float vR = 0.5f * (float) Math.sqrt(1f / (rx * rx) + 1f / (rz * rz));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                float irx = Math.max(1, rx - 1), irz = Math.max(1, rz - 1);
                float ix = (dx - cx) / irx, iz = (dz - cz) / irz;
                return outer && (ix * ix + iz * iz > 1f || dy == 0 || dy == h - 1);
            }
            case PYRAMID: {
                float level = (float) dy / Math.max(1, h - 1);
                float hw = (1f - level) * rx;
                float hd = (1f - level) * rz;
                return dx >= cx - hw && dx <= cx + hw && dz >= cz - hd && dz <= cz + hd;
            }
            case CONE: {
                float level = (float) dy / Math.max(1, h - 1);
                float scale = 1f - level;
                float arx = rx * scale, arz = rz * scale;
                if (arx < 0.5f || arz < 0.5f) return Math.abs(dx - cx) < 0.5f && Math.abs(dz - cz) < 0.5f;
                float ex = (dx - cx) / arx, ez = (dz - cz) / arz;
                float dist = ex * ex + ez * ez;
                float vR = 0.5f * (float) Math.sqrt(1f / (arx * arx) + 1f / (arz * arz));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                float irx2 = Math.max(0.5f, arx - 1), irz2 = Math.max(0.5f, arz - 1);
                float iex = (dx - cx) / irx2, iez = (dz - cz) / irz2;
                return outer && (iex * iex + iez * iez > 1f || dy == 0);
            }
            case TORUS: {
                // Elliptic ring: find nearest point on the ring ellipse, then test tube radius
                float lx = dx - cx, lz = dz - cz;
                float angle = (float) Math.atan2(
                    (float) torusRingR > 0 ? lz / (float) torusRingRZ : lz,
                    (float) torusRingRZ > 0 ? lx / (float) torusRingR : lx);
                float nearX = (float) torusRingR * (float) Math.cos(angle);
                float nearZ = (float) torusRingRZ * (float) Math.sin(angle);
                float tubeDist2 = (lx - nearX) * (lx - nearX) + (dy - cy) * (dy - cy) + (lz - nearZ) * (lz - nearZ);
                if (!hollow) return tubeDist2 <= (float) torusTubeR * (float) torusTubeR;
                float ir = Math.max(0.5f, (float) torusTubeR - 1);
                return tubeDist2 <= (float) torusTubeR * (float) torusTubeR && tubeDist2 > ir * ir;
            }
            case OCTAHEDRON: {
                float norm = Math.abs((dx - cx) / rx) + Math.abs((dy - cy) / ry) + Math.abs((dz - cz) / rz);
                float vR = 0.5f * (1f / rx + 1f / ry + 1f / rz);
                boolean outer = passL1(norm, vR, threshold);
                if (!hollow) return outer;
                float irx2 = Math.max(0.5f, rx - 1), iry2 = Math.max(0.5f, ry - 1), irz2 = Math.max(0.5f, rz - 1);
                float inner = Math.abs((dx - cx) / irx2) + Math.abs((dy - cy) / iry2) + Math.abs((dz - cz) / irz2);
                return outer && inner > 1f;
            }
            case DISK: {
                int midY = (h - 1) / 2;
                if (dy != midY) return false;
                float ex = (dx - cx) / rx, ez = (dz - cz) / rz;
                float dist = ex * ex + ez * ez;
                float vR = 0.5f * (float) Math.sqrt(1f / (rx * rx) + 1f / (rz * rz));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                float irx2 = Math.max(0.5f, rx - 1), irz2 = Math.max(0.5f, rz - 1);
                float ix = (dx - cx) / irx2, iz = (dz - cz) / irz2;
                return outer && ix * ix + iz * iz > 1f;
            }
            case PLANE: {
                return dy == (h - 1) / 2;
            }
            case SUPERELLIPSE: {
                int midY = (h - 1) / 2;
                if (dy != midY) return false;
                float ex = (float) Math.pow(Math.abs((dx - cx) / rx), exponent);
                float ez = (float) Math.pow(Math.abs((dz - cz) / rz), exponent);
                float dist = ex + ez;
                float vR = 0.5f * (float) Math.sqrt(1f / (rx * rx) + 1f / (rz * rz));
                float cutoffN = (float) Math.pow(1f - vR * (1f - threshold), exponent);
                boolean outer = dist <= cutoffN;
                if (!hollow) return outer;
                float irx2 = Math.max(0.5f, rx - 1), irz2 = Math.max(0.5f, rz - 1);
                float iex = (float) Math.pow(Math.abs((dx - cx) / irx2), exponent);
                float iez = (float) Math.pow(Math.abs((dz - cz) / irz2), exponent);
                return outer && iex + iez > 1f;
            }
            case SUPERSPHERE: {
                float ex = (float) Math.pow(Math.abs((dx - cx) / rx), supersphereExp);
                float ey = (float) Math.pow(Math.abs((dy - cy) / ry), supersphereExp);
                float ez = (float) Math.pow(Math.abs((dz - cz) / rz), supersphereExp);
                float dist = ex + ey + ez;
                float vR = 0.5f * (float) Math.sqrt(1f / (rx * rx) + 1f / (ry * ry) + 1f / (rz * rz));
                float cutoffN = (float) Math.pow(1f - vR * (1f - threshold), supersphereExp);
                boolean outer = dist <= cutoffN;
                if (!hollow) return outer;
                float irx2 = Math.max(0.5f, rx - 1), iry2 = Math.max(0.5f, ry - 1), irz2 = Math.max(0.5f, rz - 1);
                float iex = (float) Math.pow(Math.abs((dx - cx) / irx2), supersphereExp);
                float iey = (float) Math.pow(Math.abs((dy - cy) / iry2), supersphereExp);
                float iez = (float) Math.pow(Math.abs((dz - cz) / irz2), supersphereExp);
                return outer && iex + iey + iez > 1f;
            }
            case TUBE: {
                float ex = (dx - cx) / rx, ez = (dz - cz) / rz;
                if (ex * ex + ez * ez > 1f) return false;
                float irx = Math.max(0.5f, rx - tubeWallThickness);
                float irz = Math.max(0.5f, rz - tubeWallThickness);
                float ix = (dx - cx) / irx, iz = (dz - cz) / irz;
                return ix * ix + iz * iz >= 1f;
            }
            case DODECAHEDRON:
                return dodecahedronContains(dx - cx, dy - cy, dz - cz, rx, ry, rz, hollow);
            case ICOSAHEDRON:
                return icosahedronContains(dx - cx, dy - cy, dz - cz, rx, ry, rz, hollow);
            case REGULAR_POLYGON: {
                int midY = (h - 1) / 2;
                if (dy != midY) return false;
                return regularPolygonContains(dx - cx, dz - cz, rx, rz, polygonSides, hollow);
            }
            case ARCHIMEDEAN_SPIRAL: {
                int midY = (h - 1) / 2;
                if (dy != midY) return false;
                return spiralHit(dx - cx, dz - cz, spiralSpacing, spiralTurns);
            }
            default:
                return true;
        }
    }

    /**
     * Threshold test for L2-norm shapes. dist = squared normalised distance. voxelHalfR = 0.5*sqrt(1/rx²+...).
     * At threshold=1: standard cutoff. At threshold=0: crops outermost voxel shell only.
     */
    private static boolean passL2(float dist, float voxelHalfR, float threshold) {
        float cutoff = 1f - voxelHalfR * (1f - threshold);
        return (float) Math.sqrt(dist) <= cutoff;
    }

    /** Threshold test for L1-norm shapes (octahedron). norm = unnormalised L1 distance. */
    private static boolean passL1(float norm, float voxelHalfR, float threshold) {
        return norm <= 1f - voxelHalfR * (1f - threshold);
    }

    private static boolean regularPolygonContains(float lpx, float lpz, float rx, float rz, int polygonSides,
        boolean hollow) {
        int nsides = Math.max(3, polygonSides);
        float inr = (float) Math.cos(Math.PI / nsides);
        float maxDot = maxPolygonProjection(lpx / rx, lpz / rz, nsides);
        if (!hollow) return maxDot <= inr;
        float irx = Math.max(0.5f, rx - 1), irz = Math.max(0.5f, rz - 1);
        return maxDot <= inr && maxPolygonProjection(lpx / irx, lpz / irz, nsides) > inr;
    }

    private static float maxPolygonProjection(float px, float pz, int nsides) {
        float maxDot = Float.NEGATIVE_INFINITY;
        for (int k = 0; k < nsides; k++) {
            float fa = (float) ((2.0 * Math.PI * (k + 0.5)) / nsides);
            float d = (float) Math.cos(fa) * px + (float) Math.sin(fa) * pz;
            if (d > maxDot) maxDot = d;
        }
        return maxDot;
    }

    private static boolean spiralHit(float localX, float localZ, float spacing, float turns) {
        float r = (float) Math.sqrt(localX * localX + localZ * localZ);
        float theta = (float) Math.atan2(localZ, localX);
        if (theta < 0) theta += 2f * (float) Math.PI;
        float twoPi = 2f * (float) Math.PI;
        float maxR = spacing * turns;
        for (int k = 0; k <= (int) turns + 1; k++) {
            float armAngle = theta + twoPi * k;
            float armR = spacing * armAngle / twoPi;
            if (armR > maxR + spacing) break;
            if (Math.abs(r - armR) <= 0.5f) return true;
        }
        return false;
    }

    private static boolean dodecahedronContains(float npx, float npy, float npz, float rx, float ry, float rz,
        boolean hollow) {
        float phi = 1.6180339887f;
        float invMag = 1f / (float) Math.sqrt(1f + phi * phi);
        float thresh = phi * phi / ((float) Math.sqrt(3f) * (float) Math.sqrt(1f + phi * phi));
        float px = npx / rx, py = npy / ry, pz = npz / rz;
        float apx = Math.abs(px), apy = Math.abs(py), apz = Math.abs(pz);
        float fa = (apy + phi * apz) * invMag, fb = (apx + phi * apy) * invMag, fc = (phi * apx + apz) * invMag;
        boolean in = Math.max(fa, Math.max(fb, fc)) <= thresh;
        if (!hollow) return in;
        float irx2 = Math.max(0.5f, rx - 1), iry2 = Math.max(0.5f, ry - 1), irz2 = Math.max(0.5f, rz - 1);
        float ipx = npx / irx2, ipy = npy / iry2, ipz = npz / irz2;
        float ifa = (Math.abs(ipy) + phi * Math.abs(ipz)) * invMag;
        float ifb = (Math.abs(ipx) + phi * Math.abs(ipy)) * invMag;
        float ifc = (phi * Math.abs(ipx) + Math.abs(ipz)) * invMag;
        return in && Math.max(ifa, Math.max(ifb, ifc)) > thresh;
    }

    private static boolean icosahedronContains(float npx, float npy, float npz, float rx, float ry, float rz,
        boolean hollow) {
        float phi = 1.6180339887f;
        float inv3 = 1f / (float) Math.sqrt(3f);
        float thresh = phi * phi / ((float) Math.sqrt(3f) * (float) Math.sqrt(1f + phi * phi));
        float px = npx / rx, py = npy / ry, pz = npz / rz;
        float apx = Math.abs(px), apy = Math.abs(py), apz = Math.abs(pz);
        float c1 = (apx + apy + apz) * inv3;
        float c2 = (phi * apy + apz / phi) * inv3;
        float c3 = (apx / phi + phi * apz) * inv3;
        float c4 = (phi * apx + apy / phi) * inv3;
        boolean in = Math.max(c1, Math.max(c2, Math.max(c3, c4))) <= thresh;
        if (!hollow) return in;
        float irx2 = Math.max(0.5f, rx - 1), iry2 = Math.max(0.5f, ry - 1), irz2 = Math.max(0.5f, rz - 1);
        float ipx = npx / irx2, ipy = npy / iry2, ipz = npz / irz2;
        float iapx = Math.abs(ipx), iapy = Math.abs(ipy), iapz = Math.abs(ipz);
        float ic1 = (iapx + iapy + iapz) * inv3;
        float ic2 = (phi * iapy + iapz / phi) * inv3;
        float ic3 = (iapx / phi + phi * iapz) * inv3;
        float ic4 = (phi * iapx + iapy / phi) * inv3;
        return in && Math.max(ic1, Math.max(ic2, Math.max(ic3, ic4))) > thresh;
    }

    /**
     * Build a 3×3 rotation matrix R = Rz(rotZ) * Ry(rotY) * Rx(rotX), angles in degrees.
     * Result is row-major float[9]: [r00,r01,r02, r10,r11,r12, r20,r21,r22].
     * Inverse = transpose (it's orthogonal): R^T[i][j] = R[j][i].
     */
    public static float[] buildRotationMatrix(float rotXDeg, float rotYDeg, float rotZDeg) {
        double rx = Math.toRadians(rotXDeg);
        double ry = Math.toRadians(rotYDeg);
        double rz = Math.toRadians(rotZDeg);
        float cx = (float) Math.cos(rx), sx = (float) Math.sin(rx);
        float cy = (float) Math.cos(ry), sy = (float) Math.sin(ry);
        float cz = (float) Math.cos(rz), sz = (float) Math.sin(rz);
        return new float[] { cy * cz, cz * sx * sy - cx * sz, cx * cz * sy + sx * sz, cy * sz, cx * cz + sx * sy * sz,
            cx * sy * sz - cz * sx, -sy, cy * sx, cx * cy };
    }

    /**
     * Float-coord shape test. Accepts block-center local coordinates (may be non-integer
     * when inverse-transforming a rotated query point). Same formulas as inShapeGeom.
     */
    public static boolean inShapeGeomF(ShapeToolState.ShapeType type, float dx, float dy, float dz, int w, int h, int d,
        boolean hollow, float exponent, int torusRingR, int torusRingRZ, int torusTubeR, int tubeWallThickness,
        float supersphereExp, int polygonSides, float spiralSpacing, float spiralTurns, float threshold) {
        float ccx = w / 2f, ccy = h / 2f, ccz = d / 2f;
        float rx = w / 2f, ry = h / 2f, rz = d / 2f;
        switch (type) {
            case CUBOID: {
                boolean in = dx >= 0 && dx < w && dy >= 0 && dy < h && dz >= 0 && dz < d;
                if (!hollow) return in;
                return in && (dx < 1f || dx > w - 2f || dy < 1f || dy > h - 2f || dz < 1f || dz > d - 2f);
            }
            case SPHERE: {
                float ex = (dx - ccx) / rx, ey = (dy - ccy) / ry, ez = (dz - ccz) / rz;
                float dist = ex * ex + ey * ey + ez * ez;
                float vR = 0.5f * (float) Math.sqrt(1f / (rx * rx) + 1f / (ry * ry) + 1f / (rz * rz));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                float irx2 = Math.max(0.5f, rx - 1), iry2 = Math.max(0.5f, ry - 1), irz2 = Math.max(0.5f, rz - 1);
                return outer && ((dx - ccx) / irx2) * ((dx - ccx) / irx2) + ((dy - ccy) / iry2) * ((dy - ccy) / iry2)
                    + ((dz - ccz) / irz2) * ((dz - ccz) / irz2) > 1f;
            }
            case CYLINDER: {
                float ex = (dx - ccx) / rx, ez = (dz - ccz) / rz;
                float dist = ex * ex + ez * ez;
                float vR = 0.5f * (float) Math.sqrt(1f / (rx * rx) + 1f / (rz * rz));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer && dy >= 0 && dy < h;
                float irx2 = Math.max(0.5f, rx - 1), irz2 = Math.max(0.5f, rz - 1);
                boolean onCap = dy < 1f || dy > h - 2f;
                return outer && dy >= 0
                    && dy < h
                    && (((dx - ccx) / irx2) * ((dx - ccx) / irx2) + ((dz - ccz) / irz2) * ((dz - ccz) / irz2) > 1f
                        || onCap);
            }
            case PYRAMID: {
                if (dy < 0 || dy >= h) return false;
                float level = dy / Math.max(1f, h - 1f);
                float hw = (1f - level) * (w / 2f), hd = (1f - level) * (d / 2f);
                return Math.abs(dx - ccx) <= hw && Math.abs(dz - ccz) <= hd;
            }
            case CONE: {
                if (dy < 0 || dy >= h) return false;
                float level = dy / Math.max(1f, h - 1f);
                float scale = 1f - level;
                float arx = rx * scale, arz = rz * scale;
                if (arx < 0.5f || arz < 0.5f) return Math.abs(dx - ccx) < 0.5f && Math.abs(dz - ccz) < 0.5f;
                float ex = (dx - ccx) / arx, ez = (dz - ccz) / arz;
                float dist = ex * ex + ez * ez;
                float vR = 0.5f * (float) Math.sqrt(1f / (arx * arx) + 1f / (arz * arz));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                float irx2 = Math.max(0.5f, arx - 1), irz2 = Math.max(0.5f, arz - 1);
                boolean onBase = dy < 1f;
                return outer
                    && (((dx - ccx) / irx2) * ((dx - ccx) / irx2) + ((dz - ccz) / irz2) * ((dz - ccz) / irz2) > 1f
                        || onBase);
            }
            case TORUS: {
                float lx = dx - ccx, lz = dz - ccz;
                float angle = (float) Math.atan2(
                    (float) torusRingR > 0 ? lz / (float) torusRingRZ : lz,
                    (float) torusRingRZ > 0 ? lx / (float) torusRingR : lx);
                float nearX = (float) torusRingR * (float) Math.cos(angle);
                float nearZ = (float) torusRingRZ * (float) Math.sin(angle);
                float tubeDist2 = (lx - nearX) * (lx - nearX) + (dy - ccy) * (dy - ccy) + (lz - nearZ) * (lz - nearZ);
                if (!hollow) return tubeDist2 <= (float) torusTubeR * (float) torusTubeR;
                float ir = Math.max(0.5f, (float) torusTubeR - 1f);
                return tubeDist2 <= (float) torusTubeR * (float) torusTubeR && tubeDist2 > ir * ir;
            }
            case OCTAHEDRON: {
                float norm = Math.abs((dx - ccx) / rx) + Math.abs((dy - ccy) / ry) + Math.abs((dz - ccz) / rz);
                float vR = 0.5f * (1f / rx + 1f / ry + 1f / rz);
                boolean outer = passL1(norm, vR, threshold);
                if (!hollow) return outer;
                float irx2 = Math.max(0.5f, rx - 1), iry2 = Math.max(0.5f, ry - 1), irz2 = Math.max(0.5f, rz - 1);
                return outer
                    && (Math.abs((dx - ccx) / irx2) + Math.abs((dy - ccy) / iry2) + Math.abs((dz - ccz) / irz2)) > 1f;
            }
            case DISK: {
                if (Math.abs(dy - ccy) > 0.5f) return false;
                float ex = (dx - ccx) / rx, ez = (dz - ccz) / rz;
                float dist = ex * ex + ez * ez;
                float vR = 0.5f * (float) Math.sqrt(1f / (rx * rx) + 1f / (rz * rz));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                float irx2 = Math.max(0.5f, rx - 1), irz2 = Math.max(0.5f, rz - 1);
                return outer
                    && ((dx - ccx) / irx2) * ((dx - ccx) / irx2) + ((dz - ccz) / irz2) * ((dz - ccz) / irz2) > 1f;
            }
            case PLANE:
                return Math.abs(dy - ccy) <= 0.5f;
            case SUPERELLIPSE: {
                if (Math.abs(dy - ccy) > 0.5f) return false;
                float ex = (float) Math.pow(Math.abs((dx - ccx) / rx), exponent);
                float ez = (float) Math.pow(Math.abs((dz - ccz) / rz), exponent);
                float dist = ex + ez;
                float vR = 0.5f * (float) Math.sqrt(1f / (rx * rx) + 1f / (rz * rz));
                float cutoffN = (float) Math.pow(1f - vR * (1f - threshold), exponent);
                boolean outer = dist <= cutoffN;
                if (!hollow) return outer;
                float irx2 = Math.max(0.5f, rx - 1), irz2 = Math.max(0.5f, rz - 1);
                return outer && ((float) Math.pow(Math.abs((dx - ccx) / irx2), exponent)
                    + (float) Math.pow(Math.abs((dz - ccz) / irz2), exponent)) > 1f;
            }
            case SUPERSPHERE: {
                float ex = (float) Math.pow(Math.abs((dx - ccx) / rx), supersphereExp);
                float ey = (float) Math.pow(Math.abs((dy - ccy) / ry), supersphereExp);
                float ez = (float) Math.pow(Math.abs((dz - ccz) / rz), supersphereExp);
                float dist = ex + ey + ez;
                float vR = 0.5f * (float) Math.sqrt(1f / (rx * rx) + 1f / (ry * ry) + 1f / (rz * rz));
                float cutoffN = (float) Math.pow(1f - vR * (1f - threshold), supersphereExp);
                boolean outer = dist <= cutoffN;
                if (!hollow) return outer;
                float irx2 = Math.max(0.5f, rx - 1), iry2 = Math.max(0.5f, ry - 1), irz2 = Math.max(0.5f, rz - 1);
                return outer && ((float) Math.pow(Math.abs((dx - ccx) / irx2), supersphereExp)
                    + (float) Math.pow(Math.abs((dy - ccy) / iry2), supersphereExp)
                    + (float) Math.pow(Math.abs((dz - ccz) / irz2), supersphereExp)) > 1f;
            }
            case TUBE: {
                float ex = (dx - ccx) / rx, ez = (dz - ccz) / rz;
                if (ex * ex + ez * ez > 1f || dy < 0 || dy >= h) return false;
                float irx = Math.max(0.5f, rx - tubeWallThickness);
                float irz = Math.max(0.5f, rz - tubeWallThickness);
                float ix = (dx - ccx) / irx, iz = (dz - ccz) / irz;
                return ix * ix + iz * iz >= 1f;
            }
            case DODECAHEDRON:
                return dodecahedronContains(dx - ccx, dy - ccy, dz - ccz, rx, ry, rz, hollow);
            case ICOSAHEDRON:
                return icosahedronContains(dx - ccx, dy - ccy, dz - ccz, rx, ry, rz, hollow);
            case REGULAR_POLYGON: {
                if (Math.abs(dy - ccy) > 0.5f) return false;
                return regularPolygonContains(dx - ccx, dz - ccz, rx, rz, polygonSides, hollow);
            }
            case ARCHIMEDEAN_SPIRAL: {
                if (Math.abs(dy - ccy) > 0.5f) return false;
                return spiralHit(dx - ccx, dz - ccz, spiralSpacing, spiralTurns);
            }
            default:
                return true;
        }
    }

    /**
     * Multiply two row-major 3×3 matrices: C = A * B.
     */
    public static float[] multiplyRotationMatrices(float[] A, float[] B) {
        float[] C = new float[9];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++) C[i * 3 + j] += A[i * 3 + k] * B[k * 3 + j];
        return C;
    }

    /**
     * Decompose a row-major 3×3 rotation matrix R = Rz(rz)*Ry(ry)*Rx(rx)
     * back to Euler angles in degrees. Returns [rotX, rotY, rotZ].
     * Uses R[6]=-sin(ry), R[7]=cos(ry)*sin(rx), R[8]=cos(ry)*cos(rx),
     * R[3]=cos(ry)*sin(rz), R[0]=cos(ry)*cos(rz).
     */
    public static float[] decomposeRotationMatrix(float[] R) {
        float sinRy = -R[6];
        float ry = (float) Math.asin(Math.max(-1f, Math.min(1f, sinRy)));
        float cosRy = (float) Math.cos(ry);
        float rx, rz;
        if (cosRy > 0.001f) {
            rx = (float) Math.atan2(R[7], R[8]);
            rz = (float) Math.atan2(R[3], R[0]);
        } else {
            // Gimbal lock: set rz=0, solve rx from remaining terms
            rx = (float) Math.atan2(-R[5], R[4]);
            rz = 0f;
        }
        return new float[] { (float) Math.toDegrees(rx), (float) Math.toDegrees(ry), (float) Math.toDegrees(rz) };
    }
}
