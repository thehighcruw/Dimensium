/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.slope;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;

public class SlopeToolState {

    public static final SlopeToolState INSTANCE = new SlopeToolState();

    public enum SlopeShape {
        PLANE("dimensium.slope_shape.plane"),
        CONE("dimensium.slope_shape.cone");

        public final String label;

        SlopeShape(String l) {
            label = l;
        }
    }

    public enum SlopeApplyMode {
        RAISE_AND_LOWER("dimensium.slope_apply.raise_and_lower"),
        RAISE("dimensium.slope_apply.raise"),
        LOWER("dimensium.slope_apply.lower");

        public final String label;

        SlopeApplyMode(String l) {
            label = l;
        }
    }

    public static final int RADIUS_MIN = 1;
    public static final int RADIUS_MAX = 64;
    public static final float SMOOTHING_MIN = 0.0f;
    public static final float SMOOTHING_MAX = 1.0f;

    public SlopeShape slopeShape = SlopeShape.PLANE;
    public SlopeApplyMode slopeApplyMode = SlopeApplyMode.RAISE_AND_LOWER;
    public float slopeSmoothing = 0.0f;
    public int slopeRadius = 8;
    /** When true, clamp t to [0,1] so slope does not extend past pos1/pos2. */
    public boolean slopeClamp = true;

    public boolean hasPos1 = false;
    public Vec3DInt pos1 = Vec3DInt.ZERO;

    /** Set once at drag start and cleared on release; defines the slope axis together with pos1. */
    public boolean hasPos2 = false;

    public Vec3DInt pos2 = Vec3DInt.ZERO;
}
