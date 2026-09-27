/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A sampled parametric curve — an ordered list of 3D points in world space. */
public class Curve {

    public final List<Vec3DFloat> points;
    /** True when the last point implicitly connects back to the first (circle, ellipse, etc.). */
    public final boolean closed;

    public Curve(List<Vec3DFloat> points, boolean closed) {
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
        this.closed = closed;
    }
}
