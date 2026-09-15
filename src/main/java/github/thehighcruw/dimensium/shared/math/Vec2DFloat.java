/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.math;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record Vec2DFloat(float x, float y) {

    public static final Vec2DFloat ZERO = new Vec2DFloat(0, 0);
    public static final Vec2DFloat ONE = new Vec2DFloat(1, 1);

    public static Vec2DFloat from(float xy) {
        return new Vec2DFloat(xy, xy);
    }

    public static Vec2DFloat from(float x, float y) {
        return new Vec2DFloat(x, y);
    }

    // --- arithmetic ---

    public Vec2DFloat plus(Vec2DFloat other) {
        return new Vec2DFloat(x + other.x, y + other.y);
    }

    public Vec2DFloat plus(float scalar) {
        return new Vec2DFloat(x + scalar, y + scalar);
    }

    public Vec2DFloat minus(Vec2DFloat other) {
        return new Vec2DFloat(x - other.x, y - other.y);
    }

    public Vec2DFloat times(Vec2DFloat other) {
        return new Vec2DFloat(x * other.x, y * other.y);
    }

    public Vec2DFloat times(float scalar) {
        return new Vec2DFloat(x * scalar, y * scalar);
    }

    public Vec2DFloat divide(Vec2DFloat other) {
        return new Vec2DFloat(x / other.x, y / other.y);
    }

    public Vec2DFloat divide(float scalar) {
        return new Vec2DFloat(x / scalar, y / scalar);
    }

    public Vec2DFloat negate() {
        return new Vec2DFloat(-x, -y);
    }

    public Vec2DFloat abs() {
        return new Vec2DFloat(Math.abs(x), Math.abs(y));
    }

    // --- component reductions ---

    public float sum() {
        return x + y;
    }

    // --- geometry ---

    public float lengthSq() {
        return x * x + y * y;
    }

    public float length() {
        return (float) Math.sqrt(lengthSq());
    }

    public Vec2DFloat normalize() {
        float len = length();
        return len == 0 ? ZERO : divide(len);
    }

    public float dot(Vec2DFloat other) {
        return x * other.x + y * other.y;
    }
}
