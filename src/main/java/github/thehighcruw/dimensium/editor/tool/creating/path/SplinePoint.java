/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.path;

import com.github.bsideup.jabel.Desugar;

import github.thehighcruw.dimensium.shared.Vec3DDouble;

@Desugar
record SplinePoint(double x, double y, double z, double t) {

    static SplinePoint of(double x, double y, double z, double t) {
        return new SplinePoint(x, y, z, t);
    }

    static SplinePoint of(Vec3DDouble pos, double t) {
        return new SplinePoint(pos.x(), pos.y(), pos.z(), t);
    }

    Vec3DDouble pos() {
        return Vec3DDouble.from(x, y, z);
    }
}
