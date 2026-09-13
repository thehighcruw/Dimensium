/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import github.thehighcruw.dimensium.editor.tool.selecting.lasso.LassoComputer;

/**
 * Tests LassoComputer.pointInPolygon via reflection (private static).
 * Ray-casting PIP: odd crossing count = inside.
 */
public class LassoPointInPolygonTest {

    private static Method pip;

    @BeforeClass
    public static void setup() throws Exception {
        pip = LassoComputer.class.getDeclaredMethod("pointInPolygon", double.class, double.class, List.class);
        pip.setAccessible(true);
    }

    private static boolean inside(double px, double py, float[]... pts) throws Exception {
        return (Boolean) pip.invoke(null, px, py, Arrays.asList(pts));
    }

    private static float[] p(float x, float y) {
        return new float[] { x, y };
    }

    // ── unit square (0,0)-(1,0)-(1,1)-(0,1) ─────────────────────────────────

    @Test
    public void squareCenterInside() throws Exception {
        assertTrue(inside(0.5, 0.5, p(0, 0), p(1, 0), p(1, 1), p(0, 1)));
    }

    @Test
    public void squareOutsideRight() throws Exception {
        assertFalse(inside(1.5, 0.5, p(0, 0), p(1, 0), p(1, 1), p(0, 1)));
    }

    @Test
    public void squareOutsideLeft() throws Exception {
        assertFalse(inside(-0.5, 0.5, p(0, 0), p(1, 0), p(1, 1), p(0, 1)));
    }

    @Test
    public void squareOutsideAbove() throws Exception {
        assertFalse(inside(0.5, 1.5, p(0, 0), p(1, 0), p(1, 1), p(0, 1)));
    }

    @Test
    public void squareOutsideBelow() throws Exception {
        assertFalse(inside(0.5, -0.5, p(0, 0), p(1, 0), p(1, 1), p(0, 1)));
    }

    // ── triangle (0,0)-(10,0)-(5,10) ──────────────────────────────────────────

    @Test
    public void triangleCentroidInside() throws Exception {
        // centroid = (5, 10/3 ≈ 3.33)
        assertTrue(inside(5, 3.3, p(0, 0), p(10, 0), p(5, 10)));
    }

    @Test
    public void triangleFarOutside() throws Exception {
        assertFalse(inside(20, 20, p(0, 0), p(10, 0), p(5, 10)));
    }

    @Test
    public void triangleNearApexInside() throws Exception {
        assertTrue(inside(5, 9, p(0, 0), p(10, 0), p(5, 10)));
    }

    // ── L-shaped concave polygon ───────────────────────────────────────────────

    @Test
    public void lShapeInnerConcavityOutside() throws Exception {
        // L-shape: outer 3×3 minus top-right 2×2
        // vertices: (0,0) (3,0) (3,1) (1,1) (1,3) (0,3)
        assertFalse(inside(2, 2, p(0, 0), p(3, 0), p(3, 1), p(1, 1), p(1, 3), p(0, 3)));
    }

    @Test
    public void lShapeLeftArmInside() throws Exception {
        assertTrue(inside(0.5, 2, p(0, 0), p(3, 0), p(3, 1), p(1, 1), p(1, 3), p(0, 3)));
    }

    @Test
    public void lShapeBottomBarInside() throws Exception {
        assertTrue(inside(2, 0.5, p(0, 0), p(3, 0), p(3, 1), p(1, 1), p(1, 3), p(0, 3)));
    }

    // ── degenerate inputs ─────────────────────────────────────────────────────

    @Test
    public void emptyPolygonAlwaysOutside() throws Exception {
        assertFalse(inside(0, 0));
    }

    @Test
    public void singlePointPolygonAlwaysOutside() throws Exception {
        assertFalse(inside(0, 0, p(0, 0)));
    }

    @Test
    public void twoPointPolygonAlwaysOutside() throws Exception {
        assertFalse(inside(0.5, 0, p(0, 0), p(1, 0)));
    }
}
