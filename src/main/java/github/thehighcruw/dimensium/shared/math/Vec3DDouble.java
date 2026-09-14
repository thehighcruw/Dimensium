/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.math;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.util.Vec3;

@Desugar
public record Vec3DDouble(double x, double y, double z) {

    public static final Vec3DDouble ZERO = new Vec3DDouble(0, 0, 0);
    public static final Vec3DDouble ONE = new Vec3DDouble(1, 1, 1);

    public static Vec3DDouble from(double xyz) {
        return new Vec3DDouble(xyz, xyz, xyz);
    }

    public static Vec3DDouble from(double x, double y, double z) {
        return new Vec3DDouble(x, y, z);
    }

    public static Vec3DDouble fromVec3(Vec3 v) {
        return new Vec3DDouble(v.xCoord, v.yCoord, v.zCoord);
    }

    // --- arithmetic ---

    public Vec3DDouble plus(Vec3DDouble other) {
        return new Vec3DDouble(x + other.x, y + other.y, z + other.z);
    }

    public Vec3DDouble plus(double scalar) {
        return new Vec3DDouble(x + scalar, y + scalar, z + scalar);
    }

    public Vec3DDouble minus(Vec3DDouble other) {
        return new Vec3DDouble(x - other.x, y - other.y, z - other.z);
    }

    public Vec3DDouble times(Vec3DDouble other) {
        return new Vec3DDouble(x * other.x, y * other.y, z * other.z);
    }

    public Vec3DDouble times(double scalar) {
        return new Vec3DDouble(x * scalar, y * scalar, z * scalar);
    }

    public Vec3DDouble divide(Vec3DDouble other) {
        return new Vec3DDouble(x / other.x, y / other.y, z / other.z);
    }

    public Vec3DDouble divide(double scalar) {
        return new Vec3DDouble(x / scalar, y / scalar, z / scalar);
    }

    public Vec3DDouble negate() {
        return new Vec3DDouble(-x, -y, -z);
    }

    public Vec3DDouble abs() {
        return new Vec3DDouble(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    // --- component reductions ---

    public Vec3DDouble min(Vec3DDouble other) {
        return new Vec3DDouble(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
    }

    public Vec3DDouble max(Vec3DDouble other) {
        return new Vec3DDouble(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
    }

    public double sum() {
        return x + y + z;
    }

    // --- geometry ---

    public double lengthSq() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSq());
    }

    public Vec3DDouble normalize() {
        double len = length();
        return len == 0 ? ZERO : divide(len);
    }

    public double dot(Vec3DDouble other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vec3DDouble cross(Vec3DDouble other) {
        return new Vec3DDouble(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x);
    }

    public Vec3DDouble lerp(Vec3DDouble other, double t) {
        return new Vec3DDouble(x + (other.x - x) * t, y + (other.y - y) * t, z + (other.z - z) * t);
    }

    // --- conversions ---

    public Vec3DInt floor() {
        return Vec3DInt.floor(this);
    }

    public Vec3DInt round() {
        return Vec3DInt.round(this);
    }

    public Vec3DFloat toFloat() {
        return new Vec3DFloat((float) x, (float) y, (float) z);
    }

    public Vec3 toVec3() {
        return Vec3.createVectorHelper(x, y, z);
    }
}
