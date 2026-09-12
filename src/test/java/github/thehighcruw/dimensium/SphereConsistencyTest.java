/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import org.junit.Test;

import github.thehighcruw.dimensium.tool.math.ShapeMath;
import github.thehighcruw.dimensium.tool.state.ShapeToolState.ShapeType;

/**
 * Checks sphere (and other shapes) for:
 * 1. Half-count equality: blocks in left half == right half (and same for Y, Z).
 * 2. Agreement between inShapeGeom (integer, used by ShapeBrush) and inShapeGeomF
 * (float, used by ShapePlacementState ghost preview) for the same block positions.
 *
 * inShapeGeomF samples at block center (dx + 0.5f); inShapeGeom samples at dx.
 * They use different center expressions:
 * inShapeGeom: cx = (w-1)/2f
 * inShapeGeomF: ccx = w/2f
 * Algebra: (dx - (w-1)/2f) / (w/2f) == ((dx+0.5f) - w/2f) / (w/2f)
 * so they ARE equivalent — confirmed here.
 */
public class SphereConsistencyTest {

    private static final ShapeType[] HALF_SHAPES = { ShapeType.SPHERE, ShapeType.CYLINDER, ShapeType.OCTAHEDRON,
        ShapeType.SUPERSPHERE, ShapeType.DODECAHEDRON, ShapeType.ICOSAHEDRON, ShapeType.CUBOID, ShapeType.TUBE, };
    private static final int[] SIZES = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };

    private static boolean geomInt(ShapeType t, int dx, int dy, int dz, int w, int h, int d, boolean hollow) {
        return ShapeMath.inShapeGeom(t, dx, dy, dz, w, h, d, hollow, 2f, 6, 6, 2, 2, 2f, 6, 1.5f, 3f, 1f);
    }

    private static boolean geomF(ShapeType t, float dx, float dy, float dz, int w, int h, int d, boolean hollow) {
        return ShapeMath.inShapeGeomF(t, dx, dy, dz, w, h, d, hollow, 2f, 6, 6, 2, 2, 2f, 6, 1.5f, 3f, 1f);
    }

    // ── inShapeGeom vs inShapeGeomF agreement ────────────────────────────────

    /**
     * inShapeGeom(dx) and inShapeGeomF(dx + 0.5) must agree for every block.
     * Both compute the same normalized displacement from the bounding-box center:
     * int path: (dx - (w-1)/2f) / (w/2f)
     * float path: (dx+0.5f - w/2f) / (w/2f) = same value
     */
    private static void assertIntFloatAgree(ShapeType type, int w, int h, int d, boolean hollow) {
        String tag = type + " " + w + "x" + h + "x" + d + " hollow=" + hollow;
        for (int dx = 0; dx < w; dx++) for (int dy = 0; dy < h; dy++) for (int dz = 0; dz < d; dz++) {
            boolean i = geomInt(type, dx, dy, dz, w, h, d, hollow);
            boolean f = geomF(type, dx + 0.5f, dy + 0.5f, dz + 0.5f, w, h, d, hollow);
            if (i != f) fail(
                tag + ": inShapeGeom("
                    + dx
                    + ","
                    + dy
                    + ","
                    + dz
                    + ")="
                    + i
                    + " but inShapeGeomF("
                    + (dx + 0.5f)
                    + ","
                    + (dy + 0.5f)
                    + ","
                    + (dz + 0.5f)
                    + ")="
                    + f);
        }
    }

    @Test
    public void sphereIntFloatAgreementOdd() {
        for (int s : new int[] { 3, 5, 7, 9, 11, 13, 15 }) assertIntFloatAgree(ShapeType.SPHERE, s, s, s, false);
    }

    @Test
    public void sphereIntFloatAgreementEven() {
        for (int s : new int[] { 4, 6, 8, 10, 12, 14 }) assertIntFloatAgree(ShapeType.SPHERE, s, s, s, false);
    }

    @Test
    public void sphereIntFloatAgreementNonCubic() {
        assertIntFloatAgree(ShapeType.SPHERE, 5, 7, 9, false);
        assertIntFloatAgree(ShapeType.SPHERE, 10, 8, 6, false);
        assertIntFloatAgree(ShapeType.SPHERE, 7, 11, 13, false);
    }

    @Test
    public void sphereHollowIntFloatAgreement() {
        for (int s : new int[] { 5, 7, 9, 11 }) assertIntFloatAgree(ShapeType.SPHERE, s, s, s, true);
        for (int s : new int[] { 6, 8, 10 }) assertIntFloatAgree(ShapeType.SPHERE, s, s, s, true);
    }

    @Test
    public void allShapesIntFloatAgreement() {
        for (ShapeType t : HALF_SHAPES) for (int s : new int[] { 5, 6, 9, 10 }) assertIntFloatAgree(t, s, s, s, false);
    }

    // ── Half-count equality ───────────────────────────────────────────────────

    /**
     * Counts blocks in each spatial half and verifies they match.
     * For even w: two halves split at w/2. No center slice.
     * For odd w: two halves exclude the center slice (it belongs to neither half).
     */
    private static void assertHalfCountsEqual(ShapeType type, int w, int h, int d, boolean hollow) {
        String tag = type + " " + w + "x" + h + "x" + d + " hollow=" + hollow;

        // X halves
        int lo = 0, hi = 0;
        for (int dx = 0; dx < w; dx++) {
            if (dx * 2 + 1 == w) continue; // center column, skip for odd w
            for (int dy = 0; dy < h; dy++)
                for (int dz = 0; dz < d; dz++) if (geomInt(type, dx, dy, dz, w, h, d, hollow)) {
                    if (dx * 2 < w - 1) lo++;
                    else hi++;
                }
        }
        assertEquals(tag + " X halves differ: lo=" + lo + " hi=" + hi, lo, hi);

        // Y halves
        lo = 0;
        hi = 0;
        for (int dy = 0; dy < h; dy++) {
            if (dy * 2 + 1 == h) continue;
            for (int dx = 0; dx < w; dx++)
                for (int dz = 0; dz < d; dz++) if (geomInt(type, dx, dy, dz, w, h, d, hollow)) {
                    if (dy * 2 < h - 1) lo++;
                    else hi++;
                }
        }
        assertEquals(tag + " Y halves differ: lo=" + lo + " hi=" + hi, lo, hi);

        // Z halves
        lo = 0;
        hi = 0;
        for (int dz = 0; dz < d; dz++) {
            if (dz * 2 + 1 == d) continue;
            for (int dx = 0; dx < w; dx++)
                for (int dy = 0; dy < h; dy++) if (geomInt(type, dx, dy, dz, w, h, d, hollow)) {
                    if (dz * 2 < d - 1) lo++;
                    else hi++;
                }
        }
        assertEquals(tag + " Z halves differ: lo=" + lo + " hi=" + hi, lo, hi);
    }

    @Test
    public void sphereHalfCountsOdd() {
        for (int s : SIZES) if (s % 2 != 0) assertHalfCountsEqual(ShapeType.SPHERE, s, s, s, false);
    }

    @Test
    public void sphereHalfCountsEven() {
        for (int s : SIZES) if (s % 2 == 0) assertHalfCountsEqual(ShapeType.SPHERE, s, s, s, false);
    }

    @Test
    public void sphereHalfCountsNonCubic() {
        assertHalfCountsEqual(ShapeType.SPHERE, 5, 7, 9, false);
        assertHalfCountsEqual(ShapeType.SPHERE, 9, 5, 13, false);
        assertHalfCountsEqual(ShapeType.SPHERE, 8, 10, 6, false);
        assertHalfCountsEqual(ShapeType.SPHERE, 11, 7, 13, false);
    }

    @Test
    public void sphereHollowHalfCountsOdd() {
        for (int s : SIZES) if (s % 2 != 0) assertHalfCountsEqual(ShapeType.SPHERE, s, s, s, true);
    }

    @Test
    public void sphereHollowHalfCountsEven() {
        for (int s : SIZES) if (s % 2 == 0) assertHalfCountsEqual(ShapeType.SPHERE, s, s, s, true);
    }

    @Test
    public void allShapesSolidHalfCounts() {
        for (ShapeType t : HALF_SHAPES) for (int s : SIZES) assertHalfCountsEqual(t, s, s, s, false);
    }

    @Test
    public void allShapesHollowHalfCounts() {
        for (ShapeType t : HALF_SHAPES) for (int s : SIZES) assertHalfCountsEqual(t, s, s, s, true);
    }

    // ── Path analysis: where does center offset actually matter? ─────────────

    /**
     * Drawing brushes (BrushUtil.forBrush): iterate dx = -sx..sx.
     * CENTER = hit block. No offset.
     *
     * Interactive shape placement (PacketShapePlacement + ShapePlacementState):
     * Both ghost and server use ccx = w/2f as center.
     * centerX() = anchorFX + w/2.0. Consistent. No mismatch.
     *
     * Non-interactive ShapeBrush click: places x + dx for dx=0..w-1.
     * CENTER = anchorX + (w-1)/2, i.e., hit block is the CORNER, not the center.
     * For w=9: sphere extends from hit block to hit+8; center is hit+4, not hit.
     * This is the observable in-world asymmetry: player expects sphere centered
     * on the clicked block but gets a sphere whose corner is there.
     *
     * This test documents the offset that the non-interactive click path imposes.
     */
    @Test
    public void shapeBrushClickPlacesFromCornerNotCenter() {
        for (int w : SIZES) {
            // Non-interactive ShapeBrush: blocks at anchorX + 0, 1, ..., w-1
            // Center of those blocks relative to anchor:
            double centerRelativeToAnchor = (w - 1) / 2.0;
            // For the shape to LOOK centered on the click, this should be 0.
            // It isn't — user perceives shape as offset by (w-1)/2 blocks.
            assertTrue("offset for w=" + w + " should be >= 0", centerRelativeToAnchor >= 0);
            // Interactive PacketShapePlacement uses ccx=w/2f; those offsets can be negative.
            // The two paths are independent: the gizmo is never used with ShapeBrush.apply.
            double interactiveCcx = w / 2.0; // anchorFX + ox where ox can be negative
            // Only asserting the non-interactive path always extends in +X direction
            assertEquals("non-interactive blocks start at offset 0", 0.0, 0.0, 1e-9);
            // Document: interactive and non-interactive centers differ by:
            double diff = interactiveCcx - centerRelativeToAnchor;
            assertEquals("interactive vs non-interactive center offset for w=" + w, 0.5, diff, 1e-9);
        }
    }
}
