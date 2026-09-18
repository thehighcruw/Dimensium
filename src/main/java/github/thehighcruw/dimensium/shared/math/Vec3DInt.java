/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.math;

import com.github.bsideup.jabel.Desugar;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.util.Vec3;

@Desugar
public record Vec3DInt(int x, int y, int z) {

    public static final Vec3DInt ZERO = Vec3DInt.from(0);
    public static final Vec3DInt ONE = Vec3DInt.from(1);

    public static final Vec3DInt MIN_VALUE = Vec3DInt.from(Integer.MIN_VALUE);
    public static final Vec3DInt MAX_VALUE = Vec3DInt.from(Integer.MAX_VALUE);

    public static Vec3DInt from(int xyz) {
        return from(xyz, xyz, xyz);
    }

    public static Vec3DInt from(int x, int y, int z) {
        return new Vec3DInt(x, y, z);
    }

    public static Vec3DInt floor(Vec3DDouble v) {
        return from((int) Math.floor(v.x()), (int) Math.floor(v.y()), (int) Math.floor(v.z()));
    }

    public static Vec3DInt floor(Vec3DFloat v) {
        return from((int) Math.floor(v.x()), (int) Math.floor(v.y()), (int) Math.floor(v.z()));
    }

    public static Vec3DInt round(Vec3DDouble v) {
        return from((int) Math.round(v.x()), (int) Math.round(v.y()), (int) Math.round(v.z()));
    }

    public static Vec3DInt round(Vec3DFloat v) {
        return from(Math.round(v.x()), Math.round(v.y()), Math.round(v.z()));
    }

    public boolean inBounds(Vec3DInt lower, Vec3DInt upper) {
        return lower.x <= x && x <= upper.x && lower.y <= y && y <= upper.y && lower.z <= z && z <= upper.z;
    }

    // --- arithmetic ---

    public Vec3DInt plus(Vec3DInt other) {
        return from(x + other.x, y + other.y, z + other.z);
    }

    public Vec3DInt plus(int otherX, int otherY, int otherZ) {
        return from(x + otherX, y + otherY, z + otherZ);
    }

    public Vec3DInt plus(int scalar) {
        return from(x + scalar, y + scalar, z + scalar);
    }

    public Vec3DInt minus(Vec3DInt other) {
        return from(x - other.x, y - other.y, z - other.z);
    }

    public Vec3DInt minus(int scalar) {
        return from(x - scalar, y - scalar, z - scalar);
    }

    public Vec3DInt times(Vec3DInt other) {
        return from(x * other.x, y * other.y, z * other.z);
    }

    public Vec3DInt times(int scalar) {
        return from(x * scalar, y * scalar, z * scalar);
    }

    public Vec3DInt negate() {
        return from(-x, -y, -z);
    }

    public Vec3DInt abs() {
        return from(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    // --- component reductions ---

    public int min() {
        return Math.min(x, Math.min(y, z));
    }

    public Vec3DInt min(Vec3DInt other) {
        return from(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
    }

    public Vec3DInt max(Vec3DInt other) {
        return from(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
    }

    public int sum() {
        return x + y + z;
    }

    public int product() {
        return x * y * z;
    }

    public long longProduct() {
        return (long) x * y * z;
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

    /**
     * Iterates (0,0,0) inclusive to (x,y,z) exclusive — treats this vec as dimensions.
     */
    public void forEach(TriIntConsumer fn) {
        forEachInclusive(ZERO, this.minus(1), fn);
    }

    /** Same as {@link #forEach(TriIntConsumer)} but passes each position as a Vec3DInt. */
    public void forEach(Consumer<Vec3DInt> fn) {
        forEachInclusive(ZERO, this.minus(1), fn);
    }

    /**
     * Iterates min to max inclusive on all axes.
     */
    public static void forEachInclusive(Vec3DInt min, Vec3DInt max, TriIntConsumer fn) {
        for (int ix = min.x; ix <= max.x; ix++)
            for (int iy = min.y; iy <= max.y; iy++) for (int iz = min.z; iz <= max.z; iz++) fn.accept(ix, iy, iz);
    }

    /**
     * Iterates min to max inclusive on all axes, passing each position as a Vec3DInt.
     */
    public static void forEachInclusive(Vec3DInt min, Vec3DInt max, Consumer<Vec3DInt> fn) {
        forEachInclusive(min, max, (x, y, z) -> fn.accept(Vec3DInt.from(x, y, z)));
    }

    /**
     * Iterates min to max inclusive on all axes.
     */
    public static boolean anyInclusive(Vec3DInt min, Vec3DInt max, TriIntPredicate fn) {
        for (int ix = min.x; ix <= max.x; ix++)
            for (int iy = min.y; iy <= max.y; iy++)
                for (int iz = min.z; iz <= max.z; iz++) if (fn.test(ix, iy, iz)) return true;
        return false;
    }

    /** Same as {@link #anyInclusive(Vec3DInt, Vec3DInt, TriIntPredicate)} but passes each position as a Vec3DInt. */
    public static boolean anyInclusive(Vec3DInt min, Vec3DInt max, Predicate<Vec3DInt> fn) {
        return anyInclusive(min, max, (x, y, z) -> fn.test(Vec3DInt.from(x, y, z)));
    }

    // --- indexed component access ---

    /** Returns the component at axis 0=x, 1=y, 2=z. */
    public int get(int axis) {
        return switch (axis) {
            case 0 -> x;
            case 1 -> y;
            case 2 -> z;
            default -> throw new IllegalArgumentException("axis must be 0, 1, or 2");
        };
    }

    /** Returns a new Vec3DInt with one component replaced. axis: 0=x, 1=y, 2=z. */
    public Vec3DInt withAxis(int axis, int value) {
        return switch (axis) {
            case 0 -> from(value, y, z);
            case 1 -> from(x, value, z);
            case 2 -> from(x, y, value);
            default -> throw new IllegalArgumentException("axis must be 0, 1, or 2");
        };
    }

    /** Component-wise Integer.signum. */
    public Vec3DInt signum() {
        return from(Integer.signum(x), Integer.signum(y), Integer.signum(z));
    }

    // --- indexing ---

    /** Converts this coord to a flat array index: x*strideX + y*strideY + z. */
    public int toIndex(int strideX, int strideY) {
        return x * strideX + y * strideY + z;
    }

    /** Converts this coord to a flat array index given dimensions (stride = dims.y()*dims.z(), dims.z()). */
    public int toIndex(Vec3DInt dims) {
        return x * dims.y() * dims.z() + y * dims.z() + z;
    }

    /** Decomposes a flat array index back to a Vec3DInt given dimensions. */
    public static Vec3DInt fromIndex(int idx, Vec3DInt dims) {
        int lz = idx % dims.z();
        int ly = (idx / dims.z()) % dims.y();
        int lx = idx / (dims.z() * dims.y());
        return from(lx, ly, lz);
    }

    /** Creates a block-op array {x, y, z, blockId, meta} for use with PacketBlockList. */
    public int[] toBlockOp(int blockId, int meta) {
        return new int[] {x, y, z, blockId, meta};
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

    public String asCommaString() {
        return x + "," + y + "," + z;
    }
}
