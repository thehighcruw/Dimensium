/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.painting.gradient;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.concurrent.ThreadLocalRandom;

public class GradientToolState {

    public static final GradientToolState INSTANCE = new GradientToolState();

    public enum GradientShape {
        PLANE("dimensium.gradient_shape.plane"),
        SPHERE("dimensium.gradient_shape.sphere");

        public final String label;

        GradientShape(String l) {
            label = l;
        }
    }

    public enum GradientInterp {
        NEAREST("dimensium.gradient_interp.nearest"),
        LINEAR("dimensium.gradient_interp.linear"),
        BEZIER("dimensium.gradient_interp.bezier");

        public final String label;

        GradientInterp(String l) {
            label = l;
        }
    }

    public GradientShape gradientShape = GradientShape.PLANE;
    public GradientInterp gradientInterp = GradientInterp.LINEAR;
    public boolean gradientMaskSurface = false;
    public boolean gradientClampToEdge = false;
    public long gradientSeed = ThreadLocalRandom.current().nextLong();
    public boolean gradientHasPos1 = false;
    public Vec3DInt gradientPos1 = Vec3DInt.ZERO;
    public boolean gradientHasPos2 = false;
    public Vec3DInt gradientPos2 = Vec3DInt.ZERO;
}
