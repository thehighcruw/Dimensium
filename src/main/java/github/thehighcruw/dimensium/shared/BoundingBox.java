/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record BoundingBox(Vec3DInt minimum, Vec3DInt maximum) {

    public static BoundingBox from(Vec3DInt minimum, Vec3DInt maximum) {
        return new BoundingBox(minimum, maximum);
    }

    /** Inclusive size on each axis: max - min + 1. */
    public Vec3DInt size() {
        return maximum.minus(minimum)
            .plus(1);
    }

    public boolean contains(Vec3DInt p) {
        return p.x() >= minimum.x() && p.x() <= maximum.x()
            && p.y() >= minimum.y()
            && p.y() <= maximum.y()
            && p.z() >= minimum.z()
            && p.z() <= maximum.z();
    }

    public BoundingBox expand(int amount) {
        return new BoundingBox(minimum.minus(Vec3DInt.from(amount)), maximum.plus(amount));
    }

    public BoundingBox union(BoundingBox other) {
        return new BoundingBox(minimum.min(other.minimum), maximum.max(other.maximum));
    }
}
