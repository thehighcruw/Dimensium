/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.math;

import net.minecraft.util.Vec3;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record Vec3DFloat(float x, float y, float z) {

    public static final Vec3DFloat ZERO = new Vec3DFloat(0, 0, 0);
    public static final Vec3DFloat ONE = new Vec3DFloat(1, 1, 1);

    public static Vec3DFloat from(float xyz) {
        return new Vec3DFloat(xyz, xyz, xyz);
    }

    public static Vec3DFloat from(float x, float y, float z) {
        return new Vec3DFloat(x, y, z);
    }

    // --- arithmetic ---

    public Vec3DFloat plus(Vec3DFloat other) {
        return new Vec3DFloat(x + other.x, y + other.y, z + other.z);
    }

    public Vec3DFloat plus(float scalar) {
        return new Vec3DFloat(x + scalar, y + scalar, z + scalar);
    }

    public Vec3DFloat minus(Vec3DFloat other) {
        return new Vec3DFloat(x - other.x, y - other.y, z - other.z);
    }

    public Vec3DFloat times(Vec3DFloat other) {
        return new Vec3DFloat(x * other.x, y * other.y, z * other.z);
    }

    public Vec3DFloat times(float scalar) {
        return new Vec3DFloat(x * scalar, y * scalar, z * scalar);
    }

    public Vec3DFloat divide(Vec3DFloat other) {
        return new Vec3DFloat(x / other.x, y / other.y, z / other.z);
    }

    public Vec3DFloat divide(float scalar) {
        return new Vec3DFloat(x / scalar, y / scalar, z / scalar);
    }

    public Vec3DFloat negate() {
        return new Vec3DFloat(-x, -y, -z);
    }

    public Vec3DFloat abs() {
        return new Vec3DFloat(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    // --- component reductions ---

    public Vec3DFloat min(Vec3DFloat other) {
        return new Vec3DFloat(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
    }

    public Vec3DFloat max(Vec3DFloat other) {
        return new Vec3DFloat(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
    }

    public float sum() {
        return x + y + z;
    }

    // --- geometry ---

    public float lengthSq() {
        return x * x + y * y + z * z;
    }

    public float length() {
        return (float) Math.sqrt(lengthSq());
    }

    public Vec3DFloat normalize() {
        float len = length();
        return len == 0 ? ZERO : divide(len);
    }

    public float dot(Vec3DFloat other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vec3DFloat cross(Vec3DFloat other) {
        return new Vec3DFloat(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x);
    }

    public Vec3DFloat lerp(Vec3DFloat other, float t) {
        return new Vec3DFloat(x + (other.x - x) * t, y + (other.y - y) * t, z + (other.z - z) * t);
    }

    // --- conversions ---

    public Vec3DInt floor() {
        return Vec3DInt.floor(this);
    }

    public Vec3DInt round() {
        return Vec3DInt.round(this);
    }

    public Vec3DDouble toDouble() {
        return new Vec3DDouble(x, y, z);
    }

    public Vec3 toVec3() {
        return Vec3.createVectorHelper(x, y, z);
    }
}
