/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.math;

import java.util.function.Predicate;

import net.minecraft.util.Vec3;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record Vec3DInt(int x, int y, int z) {

    public static final Vec3DInt ZERO = Vec3DInt.from(0);
    public static final Vec3DInt ONE = Vec3DInt.from(1);

    public static final Vec3DInt MIN_VALUE = Vec3DInt.from(Integer.MIN_VALUE);
    public static final Vec3DInt MAX_VALUE = Vec3DInt.from(Integer.MAX_VALUE);

    public static Vec3DInt from(int xyz) {
        return new Vec3DInt(xyz, xyz, xyz);
    }

    public static Vec3DInt from(int x, int y, int z) {
        return new Vec3DInt(x, y, z);
    }

    public static Vec3DInt floor(Vec3DDouble v) {
        return new Vec3DInt((int) Math.floor(v.x()), (int) Math.floor(v.y()), (int) Math.floor(v.z()));
    }

    public static Vec3DInt floor(Vec3DFloat v) {
        return new Vec3DInt((int) Math.floor(v.x()), (int) Math.floor(v.y()), (int) Math.floor(v.z()));
    }

    public static Vec3DInt round(Vec3DDouble v) {
        return new Vec3DInt((int) Math.round(v.x()), (int) Math.round(v.y()), (int) Math.round(v.z()));
    }

    public static Vec3DInt round(Vec3DFloat v) {
        return new Vec3DInt(Math.round(v.x()), Math.round(v.y()), Math.round(v.z()));
    }

    // --- arithmetic ---

    public Vec3DInt plus(Vec3DInt other) {
        return new Vec3DInt(x + other.x, y + other.y, z + other.z);
    }

    public Vec3DInt plus(int scalar) {
        return new Vec3DInt(x + scalar, y + scalar, z + scalar);
    }

    public Vec3DInt minus(Vec3DInt other) {
        return new Vec3DInt(x - other.x, y - other.y, z - other.z);
    }

    public Vec3DInt minus(int scalar) {
        return new Vec3DInt(x - scalar, y - scalar, z - scalar);
    }

    public Vec3DInt times(Vec3DInt other) {
        return new Vec3DInt(x * other.x, y * other.y, z * other.z);
    }

    public Vec3DInt times(int scalar) {
        return new Vec3DInt(x * scalar, y * scalar, z * scalar);
    }

    public Vec3DInt negate() {
        return new Vec3DInt(-x, -y, -z);
    }

    public Vec3DInt abs() {
        return new Vec3DInt(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    // --- component reductions ---

    public Vec3DInt min(Vec3DInt other) {
        return new Vec3DInt(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
    }

    public Vec3DInt max(Vec3DInt other) {
        return new Vec3DInt(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
    }

    public int sum() {
        return x + y + z;
    }

    public int product() {
        return x * y * z;
    }

    // --- geometry ---

    public long lengthSq() {
        return (long) x * x + (long) y * y + (long) z * z;
    }

    public double length() {
        return Math.sqrt(lengthSq());
    }

    public long dot(Vec3DInt other) {
        return (long) x * other.x + (long) y * other.y + (long) z * other.z;
    }

    // --- predicates ---

    public boolean any(Predicate<Integer> tester) {
        return tester.test(x) || tester.test(y) || tester.test(z);
    }

    public boolean all(Predicate<Integer> tester) {
        return tester.test(x) && tester.test(y) && tester.test(z);
    }

    // --- iteration ---

    /** Iterates (0,0,0) inclusive to (x,y,z) exclusive — treats this vec as dimensions. */
    public void forEach(TriIntConsumer fn) {
        for (int ix = 0; ix < x; ix++)
            for (int iy = 0; iy < y; iy++) for (int iz = 0; iz < z; iz++) fn.accept(ix, iy, iz);
    }

    /** Iterates min to max inclusive on all axes. */
    public static void forEachInclusive(Vec3DInt min, Vec3DInt max, TriIntConsumer fn) {
        for (int ix = min.x; ix <= max.x; ix++)
            for (int iy = min.y; iy <= max.y; iy++) for (int iz = min.z; iz <= max.z; iz++) fn.accept(ix, iy, iz);
    }

    // --- conversions ---

    public Vec3DDouble toDouble() {
        return new Vec3DDouble(x, y, z);
    }

    public Vec3DFloat toFloat() {
        return new Vec3DFloat(x, y, z);
    }

    public Vec3 toVec3() {
        return Vec3.createVectorHelper(x, y, z);
    }
}
