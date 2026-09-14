/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.math;

import com.github.bsideup.jabel.Desugar;

/**
 * Immutable row-major 3×3 float matrix.
 * Layout: r[row][col], stored as r00,r01,r02, r10,r11,r12, r20,r21,r22.
 * Rotation matrices built here are orthogonal, so inverse = transpose.
 */
@Desugar
public record Mat3DFloat(float r00, float r01, float r02, float r10, float r11, float r12, float r20, float r21,
    float r22) {

    public static final Mat3DFloat IDENTITY = new Mat3DFloat(1, 0, 0, 0, 1, 0, 0, 0, 1);

    // --- factory ---

    /**
     * Euler-angle rotation matrix R = Rz(rotZ) * Ry(rotY) * Rx(rotX), angles in degrees.
     * The inverse is {@link #transpose()} (orthogonal matrix).
     */
    public static Mat3DFloat fromEulerDeg(float rotXDeg, float rotYDeg, float rotZDeg) {
        double rx = Math.toRadians(rotXDeg);
        double ry = Math.toRadians(rotYDeg);
        double rz = Math.toRadians(rotZDeg);
        float cx = (float) Math.cos(rx), sx = (float) Math.sin(rx);
        float cy = (float) Math.cos(ry), sy = (float) Math.sin(ry);
        float cz = (float) Math.cos(rz), sz = (float) Math.sin(rz);
        return new Mat3DFloat(
            cy * cz,
            cz * sx * sy - cx * sz,
            cx * cz * sy + sx * sz,
            cy * sz,
            cx * cz + sx * sy * sz,
            cx * sy * sz - cz * sx,
            -sy,
            cy * sx,
            cx * cy);
    }

    // --- matrix operations ---

    /** R * M */
    public Mat3DFloat mul(Mat3DFloat m) {
        return new Mat3DFloat(
            r00 * m.r00 + r01 * m.r10 + r02 * m.r20,
            r00 * m.r01 + r01 * m.r11 + r02 * m.r21,
            r00 * m.r02 + r01 * m.r12 + r02 * m.r22,
            r10 * m.r00 + r11 * m.r10 + r12 * m.r20,
            r10 * m.r01 + r11 * m.r11 + r12 * m.r21,
            r10 * m.r02 + r11 * m.r12 + r12 * m.r22,
            r20 * m.r00 + r21 * m.r10 + r22 * m.r20,
            r20 * m.r01 + r21 * m.r11 + r22 * m.r21,
            r20 * m.r02 + r21 * m.r12 + r22 * m.r22);
    }

    /** R^T */
    public Mat3DFloat transpose() {
        return new Mat3DFloat(r00, r10, r20, r01, r11, r21, r02, r12, r22);
    }

    // --- matrix-vector operations ---

    /** R * v */
    public Vec3DFloat mul(Vec3DFloat v) {
        return Vec3DFloat.from(
            r00 * v.x() + r01 * v.y() + r02 * v.z(),
            r10 * v.x() + r11 * v.y() + r12 * v.z(),
            r20 * v.x() + r21 * v.y() + r22 * v.z());
    }

    /** R^T * v (inverse rotation for orthogonal matrices) */
    public Vec3DFloat mulTranspose(Vec3DFloat v) {
        return Vec3DFloat.from(
            r00 * v.x() + r10 * v.y() + r20 * v.z(),
            r01 * v.x() + r11 * v.y() + r21 * v.z(),
            r02 * v.x() + r12 * v.y() + r22 * v.z());
    }

    /** R^T * (ox, oy, oz) returning double precision — used when caller holds double coordinates. */
    public Vec3DDouble mulTransposeD(double ox, double oy, double oz) {
        return Vec3DDouble
            .from(r00 * ox + r10 * oy + r20 * oz, r01 * ox + r11 * oy + r21 * oz, r02 * ox + r12 * oy + r22 * oz);
    }

    // --- decomposition ---

    /**
     * Decomposes this rotation matrix back to Euler angles (degrees) as Vec3DFloat(rotX, rotY, rotZ).
     * Assumes the matrix was built with R = Rz * Ry * Rx ordering.
     */
    public Vec3DFloat toEulerDeg() {
        float sinRy = -r20;
        float ry = (float) Math.asin(Math.max(-1f, Math.min(1f, sinRy)));
        float cosRy = (float) Math.cos(ry);
        float rx, rz;
        if (cosRy > 0.001f) {
            rx = (float) Math.atan2(r21, r22);
            rz = (float) Math.atan2(r10, r00);
        } else {
            // Gimbal lock: freeze rz, solve rx from remaining
            rx = (float) Math.atan2(-r12, r11);
            rz = 0f;
        }
        return Vec3DFloat.from((float) Math.toDegrees(rx), (float) Math.toDegrees(ry), (float) Math.toDegrees(rz));
    }
}
