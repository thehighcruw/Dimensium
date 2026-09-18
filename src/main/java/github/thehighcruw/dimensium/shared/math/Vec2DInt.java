/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.math;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record Vec2DInt(int x, int y) {

    public static final Vec2DInt ZERO = new Vec2DInt(0, 0);
    public static final Vec2DInt ONE = new Vec2DInt(1, 1);

    public static Vec2DInt from(int xy) {
        return new Vec2DInt(xy, xy);
    }

    public static Vec2DInt from(int x, int y) {
        return new Vec2DInt(x, y);
    }

    // --- arithmetic ---

    public Vec2DInt plus(Vec2DInt other) {
        return new Vec2DInt(x + other.x, y + other.y);
    }

    public Vec2DInt plus(int scalar) {
        return new Vec2DInt(x + scalar, y + scalar);
    }

    public Vec2DInt plus(int dx, int dy) {
        return new Vec2DInt(x + dx, y + dy);
    }

    public Vec2DInt minus(Vec2DInt other) {
        return new Vec2DInt(x - other.x, y - other.y);
    }

    public Vec2DInt minus(int scalar) {
        return new Vec2DInt(x - scalar, y - scalar);
    }

    public Vec2DInt times(int scalar) {
        return new Vec2DInt(x * scalar, y * scalar);
    }

    public Vec2DInt times(Vec2DInt other) {
        return new Vec2DInt(x * other.x, y * other.y);
    }

    public Vec2DInt negate() {
        return new Vec2DInt(-x, -y);
    }

    public Vec2DInt abs() {
        return new Vec2DInt(Math.abs(x), Math.abs(y));
    }

    // --- component reductions ---

    public int sum() {
        return x + y;
    }

    // --- geometry ---

    public int lengthSq() {
        return x * x + y * y;
    }

    public float length() {
        return (float) Math.sqrt(lengthSq());
    }

    public int dot(Vec2DInt other) {
        return x * other.x + y * other.y;
    }

    // --- conversion ---

    public Vec2DFloat toFloat() {
        return Vec2DFloat.from(x, y);
    }
}
