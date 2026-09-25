/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.shape;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;

public class ShapeToolState {

    public static final ShapeToolState INSTANCE = new ShapeToolState();

    public enum ShapeType {
        CUBOID("dimensium.shape_type.cuboid"),
        SPHERE("dimensium.shape_type.sphere"),
        CYLINDER("dimensium.shape_type.cylinder"),
        PYRAMID("dimensium.shape_type.pyramid"),
        CONE("dimensium.shape_type.cone"),
        TORUS("dimensium.shape_type.torus"),
        OCTAHEDRON("dimensium.shape_type.octahedron"),
        SUPERSPHERE("dimensium.shape_type.supersphere"),
        TUBE("dimensium.shape_type.tube"),
        DODECAHEDRON("dimensium.shape_type.dodecahedron"),
        ICOSAHEDRON("dimensium.shape_type.icosahedron"),
        DISK("dimensium.shape_type.disk"),
        PLANE("dimensium.shape_type.plane"),
        SUPERELLIPSE("dimensium.shape_type.superellipse"),
        REGULAR_POLYGON("dimensium.shape_type.regular_polygon"),
        ARCHIMEDEAN_SPIRAL("dimensium.shape_type.archimedean_spiral");

        public final String label;

        ShapeType(String labelKey) {
            label = labelKey;
        }
    }

    public static final int DIM_MIN = 1;
    public static final int DIM_MAX = 64;
    public static final int TORUS_RING_MIN = 1;
    public static final int TORUS_RING_MAX = 32;
    public static final int TORUS_TUBE_MIN = 1;
    public static final int TORUS_TUBE_MAX = 16;
    public static final int WALL_MIN = 1;
    public static final int WALL_MAX = 16;
    public static final float EXPONENT_MIN = 0.5f;
    public static final float EXPONENT_MAX = 10f;
    public static final int POLYGON_SIDES_MIN = 3;
    public static final int POLYGON_SIDES_MAX = 32;
    public static final float SPIRAL_SPACING_MIN = 0.5f;
    public static final float SPIRAL_SPACING_MAX = 10f;
    public static final float SPIRAL_TURNS_MIN = 1f;
    public static final float SPIRAL_TURNS_MAX = 20f;

    public ShapeType shapeType = ShapeType.CUBOID;
    public int shapeWidth = 5;
    public int shapeHeight = 5;
    public int shapeDepth = 5;
    public boolean shapeHollow = false;
    public float shapeExponent = 2.0f;
    public boolean shapeKeepExisting = false;
    public boolean useStairsAndSlabs = false;
    public int torusRingRadius = 6;
    public int torusRingRadiusZ = 6;
    public int torusTubeRadius = 2;
    public boolean torusSeparateAxes = false;
    public int tubeWallThickness = 2;
    public boolean shapeSeparateAxes = false;
    public int shapePolygonSides = 6;
    public float shapeSpiralSpacing = 1.0f;
    public float shapeSpiralTurns = 3.0f;
    public final float shapeSupersphereExp = 2.0f;

    public Vec3DInt effectiveDimensions(int w, int h, int d) {
        if (shapeType == ShapeType.TORUS) {
            int outerX = torusRingRadius + torusTubeRadius;
            int outerZ = torusRingRadiusZ + torusTubeRadius;
            w = outerX * 2 + 1;
            h = torusTubeRadius * 2 + 1;
            d = outerZ * 2 + 1;
        } else if (shapeType == ShapeType.ARCHIMEDEAN_SPIRAL) {
            int r = (int) Math.ceil(shapeSpiralSpacing * shapeSpiralTurns);
            w = r * 2 + 1;
            h = 1;
            d = r * 2 + 1;
        } else if (!shapeSeparateAxes
                && (shapeType == ShapeType.CYLINDER || shapeType == ShapeType.CONE || shapeType == ShapeType.TUBE)) {
            d = w;
        }
        return Vec3DInt.from(w, h, d);
    }
}
