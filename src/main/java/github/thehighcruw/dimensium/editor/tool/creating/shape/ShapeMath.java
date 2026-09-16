/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.shape;

import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec2DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;

/**
 * Pure-math shape geometry — no Minecraft state.
 * Shared between BrushApplicator (server placement) and ShapePlacementState (client ghost).
 */
public class ShapeMath {

    public static boolean inShapeGeom(
            ShapeToolState.ShapeType type,
            Vec3DInt offset,
            Vec3DInt dims,
            boolean hollow,
            float exponent,
            int torusRingR,
            int torusRingRZ,
            int torusTubeR,
            int tubeWallThickness,
            float supersphereExp,
            int polygonSides,
            float spiralSpacing,
            float spiralTurns,
            float threshold) {
        // center = (dims - 1) / 2, radius = dims / 2 (integer voxel convention)
        Vec3DFloat center = dims.toFloat().minus(Vec3DFloat.ONE).times(0.5f);
        Vec3DFloat radius = dims.toFloat().times(0.5f);
        switch (type) {
            case CUBOID: {
                if (!hollow) return true;
                return offset.x() == 0
                        || offset.x() == dims.x() - 1
                        || offset.y() == 0
                        || offset.y() == dims.y() - 1
                        || offset.z() == 0
                        || offset.z() == dims.z() - 1;
            }
            case SPHERE: {
                Vec3DFloat n = offset.toFloat().minus(center).divide(radius);
                float dist = n.dot(n);
                Vec3DFloat invR = Vec3DFloat.ONE.divide(radius);
                float vR = 0.5f * (float) Math.sqrt(invR.dot(invR));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                Vec3DFloat innerR = radius.minus(Vec3DFloat.ONE).max(Vec3DFloat.ONE);
                Vec3DFloat inner = offset.toFloat().minus(center).divide(innerR);
                return outer && inner.dot(inner) > 1f;
            }
            case CYLINDER: {
                int dy = offset.y(), h = dims.y();
                Vec2DFloat n =
                        Vec2DFloat.from((offset.x() - center.x()) / radius.x(), (offset.z() - center.z()) / radius.z());
                float dist = n.dot(n);
                Vec2DFloat invR = Vec2DFloat.from(1f / radius.x(), 1f / radius.z());
                float vR = 0.5f * (float) Math.sqrt(invR.dot(invR));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                Vec2DFloat innerR = Vec2DFloat.from(Math.max(1, radius.x() - 1), Math.max(1, radius.z() - 1));
                Vec2DFloat inner =
                        Vec2DFloat.from((offset.x() - center.x()) / innerR.x(), (offset.z() - center.z()) / innerR.y());
                return outer && (inner.dot(inner) > 1f || dy == 0 || dy == h - 1);
            }
            case PYRAMID: {
                int dy = offset.y(), h = dims.y();
                float level = (float) dy / Math.max(1, h - 1);
                float hw = (1f - level) * radius.x();
                float hd = (1f - level) * radius.z();
                float lx = offset.x() - center.x(), lz = offset.z() - center.z();
                return lx >= -hw && lx <= hw && lz >= -hd && lz <= hd;
            }
            case CONE: {
                int dy = offset.y(), h = dims.y();
                float level = (float) dy / Math.max(1, h - 1);
                float scale = 1f - level;
                float arx = radius.x() * scale, arz = radius.z() * scale;
                float lx = offset.x() - center.x(), lz = offset.z() - center.z();
                if (arx < 0.5f || arz < 0.5f) return Math.abs(lx) < 0.5f && Math.abs(lz) < 0.5f;
                Vec2DFloat n = Vec2DFloat.from(lx / arx, lz / arz);
                float dist = n.dot(n);
                Vec2DFloat invR = Vec2DFloat.from(1f / arx, 1f / arz);
                float vR = 0.5f * (float) Math.sqrt(invR.dot(invR));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                float irx = Math.max(0.5f, arx - 1), irz = Math.max(0.5f, arz - 1);
                Vec2DFloat inner = Vec2DFloat.from(lx / irx, lz / irz);
                return outer && (inner.dot(inner) > 1f || dy == 0);
            }
            case TORUS: {
                // Elliptic ring: find nearest point on the ring ellipse, then test tube radius
                Vec3DFloat local = offset.toFloat().minus(center);
                float angle = (float) Math.atan2(
                        torusRingR > 0 ? local.z() / torusRingRZ : local.z(),
                        torusRingRZ > 0 ? local.x() / torusRingR : local.x());
                float nearX = torusRingR * (float) Math.cos(angle);
                float nearZ = torusRingRZ * (float) Math.sin(angle);
                Vec3DFloat tube = Vec3DFloat.from(local.x() - nearX, local.y(), local.z() - nearZ);
                float tubeDist2 = tube.dot(tube);
                float tubeR2 = (float) torusTubeR * torusTubeR;
                if (!hollow) return tubeDist2 <= tubeR2;
                float ir = Math.max(0.5f, torusTubeR - 1f);
                return tubeDist2 <= tubeR2 && tubeDist2 > ir * ir;
            }
            case OCTAHEDRON: {
                Vec3DFloat n = offset.toFloat().minus(center).divide(radius).abs();
                float norm = n.sum();
                float vR = 0.5f * Vec3DFloat.ONE.divide(radius).sum();
                boolean outer = passL1(norm, vR, threshold);
                if (!hollow) return outer;
                Vec3DFloat innerR = radius.minus(Vec3DFloat.ONE).max(Vec3DFloat.from(0.5f));
                Vec3DFloat inner = offset.toFloat().minus(center).divide(innerR).abs();
                return outer && inner.sum() > 1f;
            }
            case DISK: {
                int dy = offset.y(), h = dims.y();
                int midY = (h - 1) / 2;
                if (dy != midY) return false;
                Vec2DFloat n =
                        Vec2DFloat.from((offset.x() - center.x()) / radius.x(), (offset.z() - center.z()) / radius.z());
                float dist = n.dot(n);
                Vec2DFloat invR = Vec2DFloat.from(1f / radius.x(), 1f / radius.z());
                float vR = 0.5f * (float) Math.sqrt(invR.dot(invR));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                Vec2DFloat innerR = Vec2DFloat.from(Math.max(0.5f, radius.x() - 1), Math.max(0.5f, radius.z() - 1));
                Vec2DFloat inner =
                        Vec2DFloat.from((offset.x() - center.x()) / innerR.x(), (offset.z() - center.z()) / innerR.y());
                return outer && inner.dot(inner) > 1f;
            }
            case PLANE: {
                int h = dims.y();
                return offset.y() == (h - 1) / 2;
            }
            case SUPERELLIPSE: {
                int dy = offset.y(), h = dims.y();
                int midY = (h - 1) / 2;
                if (dy != midY) return false;
                float nx = Math.abs((offset.x() - center.x()) / radius.x());
                float nz = Math.abs((offset.z() - center.z()) / radius.z());
                float dist = (float) (Math.pow(nx, exponent) + Math.pow(nz, exponent));
                Vec2DFloat invR = Vec2DFloat.from(1f / radius.x(), 1f / radius.z());
                float vR = 0.5f * (float) Math.sqrt(invR.dot(invR));
                float cutoffN = (float) Math.pow(1f - vR * (1f - threshold), exponent);
                boolean outer = dist <= cutoffN;
                if (!hollow) return outer;
                Vec2DFloat innerR = Vec2DFloat.from(Math.max(0.5f, radius.x() - 1), Math.max(0.5f, radius.z() - 1));
                float inx = Math.abs((offset.x() - center.x()) / innerR.x());
                float inz = Math.abs((offset.z() - center.z()) / innerR.y());
                return outer && Math.pow(inx, exponent) + Math.pow(inz, exponent) > 1f;
            }
            case SUPERSPHERE: {
                Vec3DFloat n = offset.toFloat().minus(center).divide(radius).abs();
                float dist = (float) (Math.pow(n.x(), supersphereExp)
                        + Math.pow(n.y(), supersphereExp)
                        + Math.pow(n.z(), supersphereExp));
                Vec3DFloat invR = Vec3DFloat.ONE.divide(radius);
                float vR = 0.5f * (float) Math.sqrt(invR.dot(invR));
                float cutoffN = (float) Math.pow(1f - vR * (1f - threshold), supersphereExp);
                boolean outer = dist <= cutoffN;
                if (!hollow) return outer;
                Vec3DFloat innerR = radius.minus(Vec3DFloat.ONE).max(Vec3DFloat.from(0.5f));
                Vec3DFloat inner = offset.toFloat().minus(center).divide(innerR).abs();
                return outer
                        && Math.pow(inner.x(), supersphereExp)
                                        + Math.pow(inner.y(), supersphereExp)
                                        + Math.pow(inner.z(), supersphereExp)
                                > 1f;
            }
            case TUBE: {
                Vec2DFloat n =
                        Vec2DFloat.from((offset.x() - center.x()) / radius.x(), (offset.z() - center.z()) / radius.z());
                if (n.dot(n) > 1f) return false;
                float irx = Math.max(0.5f, radius.x() - tubeWallThickness);
                float irz = Math.max(0.5f, radius.z() - tubeWallThickness);
                Vec2DFloat inner = Vec2DFloat.from((offset.x() - center.x()) / irx, (offset.z() - center.z()) / irz);
                return inner.dot(inner) >= 1f;
            }
            case DODECAHEDRON:
                return dodecahedronContains(offset.toFloat().minus(center), radius, hollow);
            case ICOSAHEDRON:
                return icosahedronContains(offset.toFloat().minus(center), radius, hollow);
            case REGULAR_POLYGON: {
                int dy = offset.y(), h = dims.y();
                int midY = (h - 1) / 2;
                if (dy != midY) return false;
                return regularPolygonContains(
                        offset.x() - center.x(), offset.z() - center.z(), radius.x(), radius.z(), polygonSides, hollow);
            }
            case ARCHIMEDEAN_SPIRAL: {
                int dy = offset.y(), h = dims.y();
                int midY = (h - 1) / 2;
                if (dy != midY) return false;
                return spiralHit(offset.x() - center.x(), offset.z() - center.z(), spiralSpacing, spiralTurns);
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

    private static boolean regularPolygonContains(
            float lpx, float lpz, float rx, float rz, int polygonSides, boolean hollow) {
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

    private static float dodecahedronMax(Vec3DFloat p, float phi, float invMag) {
        float fa = (p.y() + phi * p.z()) * invMag;
        float fb = (p.x() + phi * p.y()) * invMag;
        float fc = (phi * p.x() + p.z()) * invMag;
        return Math.max(fa, Math.max(fb, fc));
    }

    private static boolean dodecahedronContains(Vec3DFloat localPos, Vec3DFloat radius, boolean hollow) {
        float phi = 1.6180339887f;
        float invMag = 1f / (float) Math.sqrt(1f + phi * phi);
        float thresh = phi * phi / ((float) Math.sqrt(3f) * (float) Math.sqrt(1f + phi * phi));
        boolean in = dodecahedronMax(localPos.divide(radius).abs(), phi, invMag) <= thresh;
        if (!hollow) return in;
        Vec3DFloat innerR = radius.minus(Vec3DFloat.ONE).max(Vec3DFloat.from(0.5f));
        return in && dodecahedronMax(localPos.divide(innerR).abs(), phi, invMag) > thresh;
    }

    private static float icosaMax(Vec3DFloat p, float phi, float inv3) {
        float c1 = p.sum() * inv3;
        float c2 = (phi * p.y() + p.z() / phi) * inv3;
        float c3 = (p.x() / phi + phi * p.z()) * inv3;
        float c4 = (phi * p.x() + p.y() / phi) * inv3;
        return Math.max(c1, Math.max(c2, Math.max(c3, c4)));
    }

    private static boolean icosahedronContains(Vec3DFloat localPos, Vec3DFloat radius, boolean hollow) {
        float phi = 1.6180339887f;
        float inv3 = 1f / (float) Math.sqrt(3f);
        float thresh = phi * phi / ((float) Math.sqrt(3f) * (float) Math.sqrt(1f + phi * phi));
        boolean in = icosaMax(localPos.divide(radius).abs(), phi, inv3) <= thresh;
        if (!hollow) return in;
        Vec3DFloat innerR = radius.minus(Vec3DFloat.ONE).max(Vec3DFloat.from(0.5f));
        return in && icosaMax(localPos.divide(innerR).abs(), phi, inv3) > thresh;
    }

    /**
     * Build a rotation matrix R = Rz(rotZ) * Ry(rotY) * Rx(rotX), angles in degrees.
     * Delegates to {@link Mat3DFloat#fromEulerDeg}.
     */
    public static Mat3DFloat buildRotationMatrix(float rotXDeg, float rotYDeg, float rotZDeg) {
        return Mat3DFloat.fromEulerDeg(rotXDeg, rotYDeg, rotZDeg);
    }

    /**
     * Float-coord shape test. Accepts block-center local coordinates (may be non-integer
     * when inverse-transforming a rotated query point). Same formulas as inShapeGeom.
     */
    public static boolean inShapeGeomF(
            ShapeToolState.ShapeType type,
            Vec3DFloat offset,
            Vec3DInt dims,
            boolean hollow,
            float exponent,
            int torusRingR,
            int torusRingRZ,
            int torusTubeR,
            int tubeWallThickness,
            float supersphereExp,
            int polygonSides,
            float spiralSpacing,
            float spiralTurns,
            float threshold) {
        // In the float variant: center == radius == dims / 2
        Vec3DFloat center = dims.toFloat().times(0.5f);
        Vec3DFloat radius = center;
        switch (type) {
            case CUBOID: {
                int w = dims.x(), h = dims.y(), d = dims.z();
                boolean in = offset.x() >= 0
                        && offset.x() < w
                        && offset.y() >= 0
                        && offset.y() < h
                        && offset.z() >= 0
                        && offset.z() < d;
                if (!hollow) return in;
                return in
                        && (offset.x() < 1f
                                || offset.x() > w - 2f
                                || offset.y() < 1f
                                || offset.y() > h - 2f
                                || offset.z() < 1f
                                || offset.z() > d - 2f);
            }
            case SPHERE: {
                Vec3DFloat n = offset.minus(center).divide(radius);
                float dist = n.dot(n);
                Vec3DFloat invR = Vec3DFloat.ONE.divide(radius);
                float vR = 0.5f * (float) Math.sqrt(invR.dot(invR));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                Vec3DFloat innerR = radius.minus(Vec3DFloat.ONE).max(Vec3DFloat.from(0.5f));
                Vec3DFloat inner = offset.minus(center).divide(innerR);
                return outer && inner.dot(inner) > 1f;
            }
            case CYLINDER: {
                int h = dims.y();
                float dy = offset.y();
                Vec2DFloat n =
                        Vec2DFloat.from((offset.x() - center.x()) / radius.x(), (offset.z() - center.z()) / radius.z());
                float dist = n.dot(n);
                Vec2DFloat invR = Vec2DFloat.from(1f / radius.x(), 1f / radius.z());
                float vR = 0.5f * (float) Math.sqrt(invR.dot(invR));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer && dy >= 0 && dy < h;
                float irx = Math.max(0.5f, radius.x() - 1), irz = Math.max(0.5f, radius.z() - 1);
                boolean onCap = dy < 1f || dy > h - 2f;
                Vec2DFloat inner = Vec2DFloat.from((offset.x() - center.x()) / irx, (offset.z() - center.z()) / irz);
                return outer && dy >= 0 && dy < h && (inner.dot(inner) > 1f || onCap);
            }
            case PYRAMID: {
                int h = dims.y();
                float dy = offset.y();
                if (dy < 0 || dy >= h) return false;
                float level = dy / Math.max(1f, h - 1f);
                float hw = (1f - level) * radius.x(), hd = (1f - level) * radius.z();
                return Math.abs(offset.x() - center.x()) <= hw && Math.abs(offset.z() - center.z()) <= hd;
            }
            case CONE: {
                int h = dims.y();
                float dy = offset.y();
                if (dy < 0 || dy >= h) return false;
                float level = dy / Math.max(1f, h - 1f);
                float scale = 1f - level;
                float arx = radius.x() * scale, arz = radius.z() * scale;
                float lx = offset.x() - center.x(), lz = offset.z() - center.z();
                if (arx < 0.5f || arz < 0.5f) return Math.abs(lx) < 0.5f && Math.abs(lz) < 0.5f;
                Vec2DFloat n = Vec2DFloat.from(lx / arx, lz / arz);
                float dist = n.dot(n);
                Vec2DFloat invR = Vec2DFloat.from(1f / arx, 1f / arz);
                float vR = 0.5f * (float) Math.sqrt(invR.dot(invR));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                float irx2 = Math.max(0.5f, arx - 1), irz2 = Math.max(0.5f, arz - 1);
                Vec2DFloat inner = Vec2DFloat.from(lx / irx2, lz / irz2);
                return outer && (inner.dot(inner) > 1f || dy < 1f);
            }
            case TORUS: {
                Vec3DFloat local = offset.minus(center);
                float angle = (float) Math.atan2(
                        torusRingR > 0 ? local.z() / torusRingRZ : local.z(),
                        torusRingRZ > 0 ? local.x() / torusRingR : local.x());
                float nearX = torusRingR * (float) Math.cos(angle);
                float nearZ = torusRingRZ * (float) Math.sin(angle);
                Vec3DFloat tube = Vec3DFloat.from(local.x() - nearX, local.y(), local.z() - nearZ);
                float tubeDist2 = tube.dot(tube);
                float tubeR2 = (float) torusTubeR * torusTubeR;
                if (!hollow) return tubeDist2 <= tubeR2;
                float ir = Math.max(0.5f, torusTubeR - 1f);
                return tubeDist2 <= tubeR2 && tubeDist2 > ir * ir;
            }
            case OCTAHEDRON: {
                Vec3DFloat n = offset.minus(center).divide(radius).abs();
                float norm = n.sum();
                float vR = 0.5f * Vec3DFloat.ONE.divide(radius).sum();
                boolean outer = passL1(norm, vR, threshold);
                if (!hollow) return outer;
                Vec3DFloat innerR = radius.minus(Vec3DFloat.ONE).max(Vec3DFloat.from(0.5f));
                Vec3DFloat inner = offset.minus(center).divide(innerR).abs();
                return outer && inner.sum() > 1f;
            }
            case DISK: {
                if (Math.abs(offset.y() - center.y()) > 0.5f) return false;
                Vec2DFloat n =
                        Vec2DFloat.from((offset.x() - center.x()) / radius.x(), (offset.z() - center.z()) / radius.z());
                float dist = n.dot(n);
                Vec2DFloat invR = Vec2DFloat.from(1f / radius.x(), 1f / radius.z());
                float vR = 0.5f * (float) Math.sqrt(invR.dot(invR));
                boolean outer = passL2(dist, vR, threshold);
                if (!hollow) return outer;
                Vec2DFloat innerR = Vec2DFloat.from(Math.max(0.5f, radius.x() - 1), Math.max(0.5f, radius.z() - 1));
                Vec2DFloat inner =
                        Vec2DFloat.from((offset.x() - center.x()) / innerR.x(), (offset.z() - center.z()) / innerR.y());
                return outer && inner.dot(inner) > 1f;
            }
            case PLANE:
                return Math.abs(offset.y() - center.y()) <= 0.5f;
            case SUPERELLIPSE: {
                if (Math.abs(offset.y() - center.y()) > 0.5f) return false;
                float nx = Math.abs((offset.x() - center.x()) / radius.x());
                float nz = Math.abs((offset.z() - center.z()) / radius.z());
                float dist = (float) (Math.pow(nx, exponent) + Math.pow(nz, exponent));
                Vec2DFloat invR = Vec2DFloat.from(1f / radius.x(), 1f / radius.z());
                float vR = 0.5f * (float) Math.sqrt(invR.dot(invR));
                float cutoffN = (float) Math.pow(1f - vR * (1f - threshold), exponent);
                boolean outer = dist <= cutoffN;
                if (!hollow) return outer;
                Vec2DFloat innerR = Vec2DFloat.from(Math.max(0.5f, radius.x() - 1), Math.max(0.5f, radius.z() - 1));
                float inx = Math.abs((offset.x() - center.x()) / innerR.x());
                float inz = Math.abs((offset.z() - center.z()) / innerR.y());
                return outer && Math.pow(inx, exponent) + Math.pow(inz, exponent) > 1f;
            }
            case SUPERSPHERE: {
                Vec3DFloat n = offset.minus(center).divide(radius).abs();
                float dist = (float) (Math.pow(n.x(), supersphereExp)
                        + Math.pow(n.y(), supersphereExp)
                        + Math.pow(n.z(), supersphereExp));
                Vec3DFloat invR = Vec3DFloat.ONE.divide(radius);
                float vR = 0.5f * (float) Math.sqrt(invR.dot(invR));
                float cutoffN = (float) Math.pow(1f - vR * (1f - threshold), supersphereExp);
                boolean outer = dist <= cutoffN;
                if (!hollow) return outer;
                Vec3DFloat innerR = radius.minus(Vec3DFloat.ONE).max(Vec3DFloat.from(0.5f));
                Vec3DFloat inner = offset.minus(center).divide(innerR).abs();
                return outer
                        && Math.pow(inner.x(), supersphereExp)
                                        + Math.pow(inner.y(), supersphereExp)
                                        + Math.pow(inner.z(), supersphereExp)
                                > 1f;
            }
            case TUBE: {
                int h = dims.y();
                float dy = offset.y();
                Vec2DFloat n =
                        Vec2DFloat.from((offset.x() - center.x()) / radius.x(), (offset.z() - center.z()) / radius.z());
                if (n.dot(n) > 1f || dy < 0 || dy >= h) return false;
                float irx = Math.max(0.5f, radius.x() - tubeWallThickness);
                float irz = Math.max(0.5f, radius.z() - tubeWallThickness);
                Vec2DFloat inner = Vec2DFloat.from((offset.x() - center.x()) / irx, (offset.z() - center.z()) / irz);
                return inner.dot(inner) >= 1f;
            }
            case DODECAHEDRON:
                return dodecahedronContains(offset.minus(center), radius, hollow);
            case ICOSAHEDRON:
                return icosahedronContains(offset.minus(center), radius, hollow);
            case REGULAR_POLYGON: {
                if (Math.abs(offset.y() - center.y()) > 0.5f) return false;
                return regularPolygonContains(
                        offset.x() - center.x(), offset.z() - center.z(), radius.x(), radius.z(), polygonSides, hollow);
            }
            case ARCHIMEDEAN_SPIRAL: {
                if (Math.abs(offset.y() - center.y()) > 0.5f) return false;
                return spiralHit(offset.x() - center.x(), offset.z() - center.z(), spiralSpacing, spiralTurns);
            }
            default:
                return true;
        }
    }

    /**
     * Functional interface for iterateRotatedShape — receives the offset of each voxel
     * that passes the shape test. Return false to stop iteration early.
     */
    @FunctionalInterface
    public interface ShapeVoxelConsumer {

        boolean accept(Vec3DInt offset);
    }

    /**
     * Computes the integer AABB of a shape's base bounding box after rotation.
     * Returns Vec3DInt[2] = {min, max}.
     */
    public static Vec3DInt[] computeRotatedBounds(Mat3DFloat R, Vec3DInt dims) {
        Vec3DFloat center = dims.toFloat().times(0.5f);
        Vec3DFloat min = Vec3DFloat.from(Float.MAX_VALUE);
        Vec3DFloat max = Vec3DFloat.from(-Float.MAX_VALUE);
        for (int mask = 0; mask < 8; mask++) {
            Vec3DFloat corner = Vec3DFloat.from(
                    ((mask & 1) != 0 ? dims.x() : 0),
                    ((mask & 2) != 0 ? dims.y() : 0),
                    ((mask & 4) != 0 ? dims.z() : 0));
            Vec3DFloat rot = R.mul(corner.minus(center)).plus(center);
            min = min.min(rot);
            max = max.max(rot);
        }
        return new Vec3DInt[] {
            Vec3DInt.floor(min),
            Vec3DInt.from((int) Math.ceil(max.x()), (int) Math.ceil(max.y()), (int) Math.ceil(max.z()))
        };
    }

    /**
     * Iterates all voxel offsets inside a (possibly rotated) shape, calling consumer for each.
     * Consumer returns false to abort early. The inverse rotation (R^T) maps rotated coords
     * back to local shape space before testing inShapeGeomF.
     */
    public static void iterateRotatedShape(
            ShapeToolState.ShapeType type,
            Vec3DInt dims,
            boolean hollow,
            float exponent,
            int torusRingR,
            int torusRingRZ,
            int torusTubeR,
            int tubeWallThickness,
            float supersphereExp,
            int polygonSides,
            float spiralSpacing,
            float spiralTurns,
            float threshold,
            Mat3DFloat R,
            Vec3DInt boundsMin,
            Vec3DInt boundsMax,
            ShapeVoxelConsumer consumer) {
        Vec3DFloat center = dims.toFloat().times(0.5f);
        // anyInclusive stops (returns true) when predicate returns true.
        // Consumer returns false to abort, so we invert: predicate = !consumer.accept(voxel).
        Vec3DInt.anyInclusive(boundsMin, boundsMax, (ox, oy, oz) -> {
            Vec3DInt voxel = Vec3DInt.from(ox, oy, oz);
            // Map rotated voxel center back to local shape space via R^T
            Vec3DFloat localOffset =
                    R.mulTranspose(voxel.toFloat().plus(0.5f).minus(center)).plus(center);
            if (!inShapeGeomF(
                    type,
                    localOffset,
                    dims,
                    hollow,
                    exponent,
                    torusRingR,
                    torusRingRZ,
                    torusTubeR,
                    tubeWallThickness,
                    supersphereExp,
                    polygonSides,
                    spiralSpacing,
                    spiralTurns,
                    threshold)) return false;
            return !consumer.accept(voxel);
        });
    }
}
