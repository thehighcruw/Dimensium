/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.math;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record Vec2DDouble(double x, double y) {

    public static final Vec2DDouble ZERO = new Vec2DDouble(0, 0);

    public static Vec2DDouble from(double x, double y) {
        return new Vec2DDouble(x, y);
    }

    public Vec2DDouble plus(Vec2DDouble other) {
        return new Vec2DDouble(x + other.x, y + other.y);
    }

    public Vec2DDouble minus(Vec2DDouble other) {
        return new Vec2DDouble(x - other.x, y - other.y);
    }

    public Vec2DDouble times(double scalar) {
        return new Vec2DDouble(x * scalar, y * scalar);
    }

    public Vec2DDouble divide(double scalar) {
        return new Vec2DDouble(x / scalar, y / scalar);
    }

    public Vec2DDouble negate() {
        return new Vec2DDouble(-x, -y);
    }

    public double dot(Vec2DDouble other) {
        return x * other.x + y * other.y;
    }

    public double lengthSq() {
        return x * x + y * y;
    }

    public double length() {
        return Math.sqrt(lengthSq());
    }

    public Vec2DDouble normalize() {
        double len = length();
        return len == 0 ? ZERO : divide(len);
    }

    /**
     * Computes a normalized screen-space direction from two projected points.
     * Returns (1,0) when the points are degenerate (behind camera or too close).
     */
    public static Vec2DDouble screenDir(double[] from, double[] to) {
        double dx = to[0] - from[0], dy = to[1] - from[1];
        double len = Math.sqrt(dx * dx + dy * dy);
        return len > 0.001 ? Vec2DDouble.from(dx / len, dy / len) : Vec2DDouble.from(1, 0);
    }

    /** Returns a point at the given polar coordinates: (cos(angle)*radius, sin(angle)*radius). */
    public static Vec2DDouble fromPolar(double angle, double radius) {
        return new Vec2DDouble(Math.cos(angle) * radius, Math.sin(angle) * radius);
    }

    /** Returns Math.max(1.0, distance) between two projected points — usable as pixels-per-unit scale. */
    public static double screenScale(double[] from, double[] to) {
        double dx = to[0] - from[0], dy = to[1] - from[1];
        return Math.max(1.0, Math.sqrt(dx * dx + dy * dy));
    }
}
