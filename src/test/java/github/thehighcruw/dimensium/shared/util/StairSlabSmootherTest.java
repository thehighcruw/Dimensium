/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import static org.junit.Assert.*;

import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.util.StairSlabSmoother.BlockShape;
import github.thehighcruw.dimensium.shared.util.StairSlabSmoother.PlacementKind;
import github.thehighcruw.dimensium.shared.util.StairSlabSmoother.SphereSample;
import java.util.Arrays;
import org.junit.Test;

/**
 * Verifies the stair/slab shape lookup in two independent layers:
 *
 * <ol>
 * <li><b>Geometry specification</b>: for each stair meta, the expected sub-voxel mask is
 * computed from named sub-voxel constants (not from {@code SHAPES}). If {@code SHAPES}
 * assigns the wrong mask to a meta, these tests catch it.
 * <li><b>Lookup behaviour</b>: surface-block-like sub-voxel patterns map to the expected shape
 * kind and orientation.
 * </ol>
 *
 * <h3>Sub-voxel bit encoding</h3>
 *
 * <pre>
 *   bit 0 = east  (+x, +0.25)    bit 1 = top   (+y, +0.25)    bit 2 = south (+z, +0.25)
 *
 *   index  x       y       z
 *     0    west    bottom  north   (0b000)
 *     1    east    bottom  north   (0b001)
 *     2    west    top     north   (0b010)
 *     3    east    top     north   (0b011)
 *     4    west    bottom  south   (0b100)
 *     5    east    bottom  south   (0b101)
 *     6    west    top     south   (0b110)
 *     7    east    top     south   (0b111)
 * </pre>
 */
public class StairSlabSmootherTest {

    // ── named sub-voxel positions ─────────────────────────────────────────────

    private static final int WBN = 0; // west-bottom-north
    private static final int EBN = 1; // east-bottom-north
    private static final int WTN = 2; // west-top-north
    private static final int ETN = 3; // east-top-north
    private static final int WBS = 4; // west-bottom-south
    private static final int EBS = 5; // east-bottom-south
    private static final int WTS = 6; // west-top-south
    private static final int ETS = 7; // east-top-south

    /** Builds a mask from the given filled sub-voxel indices. */
    private static int filled(int... indices) {
        int m = 0;
        for (int i : indices) m |= (1 << i);
        return m;
    }

    // ── layer / half constants derived from sub-voxel positions ──────────────

    /** Bottom layer: all sub-voxels where y = bottom. */
    private static final int BOTTOM_LAYER = filled(WBN, EBN, WBS, EBS); // 0x33

    /** Top layer: all sub-voxels where y = top. */
    private static final int TOP_LAYER = filled(WTN, ETN, WTS, ETS); // 0xCC

    /** Top half on the east side (east-top-N and east-top-S). */
    private static final int TOP_EAST = filled(ETN, ETS); // 0x88

    /** Top half on the west side. */
    private static final int TOP_WEST = filled(WTN, WTS); // 0x44

    /** Top half on the south side. */
    private static final int TOP_SOUTH = filled(WTS, ETS); // 0xC0

    /** Top half on the north side. */
    private static final int TOP_NORTH = filled(WTN, ETN); // 0x0C

    /** Bottom half on the east side. */
    private static final int BOTTOM_EAST = filled(EBN, EBS); // 0x22

    /** Bottom half on the west side. */
    private static final int BOTTOM_WEST = filled(WBN, WBS); // 0x11

    /** Bottom half on the south side. */
    private static final int BOTTOM_SOUTH = filled(WBS, EBS); // 0x30

    /** Bottom half on the north side. */
    private static final int BOTTOM_NORTH = filled(WBN, EBN); // 0x03

    // ── lookup helpers ────────────────────────────────────────────────────────

    private static BlockShape shape(int mask) {
        return StairSlabSmoother.shapeForMask(mask);
    }

    private static void assertStairMeta(String label, int expectedMask, int expectedMeta) {
        BlockShape s = shape(expectedMask);
        assertEquals(label + ": kind", PlacementKind.STAIR, s.kind());
        assertEquals(label + ": meta", expectedMeta, s.stairMeta());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER 1 — Geometry specification
    //
    // Each test independently constructs the expected sub-voxel mask from named
    // constants and verifies it against the shape registered for that meta.
    // These tests will fail if SHAPES has the wrong mask for any meta.
    // ═══════════════════════════════════════════════════════════════════════════

    // ── slabs ─────────────────────────────────────────────────────────────────

    @Test
    public void spec_slabBottom_fillsBottomLayer() {
        // Bottom slab: the four sub-voxels in the lower half are solid.
        int expected = BOTTOM_LAYER; // 0x33
        assertEquals(0x33, expected);
        assertEquals(PlacementKind.SLAB_BOTTOM, shape(expected).kind());
    }

    @Test
    public void spec_slabTop_fillsTopLayer() {
        int expected = TOP_LAYER; // 0xCC
        assertEquals(0xCC, expected);
        assertEquals(PlacementKind.SLAB_TOP, shape(expected).kind());
    }

    // ── straight stairs: bottom-half full + one top quadrant ─────────────────

    @Test
    public void spec_meta0_ascendingEast() {
        // Ascending east: walking east takes you up. Solid = bottom + top-east half.
        // The step faces west; the raised part is on the east.
        int expected = BOTTOM_LAYER | TOP_EAST; // 0xBB
        assertEquals(0xBB, expected);
        assertStairMeta("meta 0 (ascending east)", expected, 0);
    }

    @Test
    public void spec_meta1_ascendingWest() {
        int expected = BOTTOM_LAYER | TOP_WEST; // 0x77
        assertEquals(0x77, expected);
        assertStairMeta("meta 1 (ascending west)", expected, 1);
    }

    @Test
    public void spec_meta2_ascendingSouth() {
        int expected = BOTTOM_LAYER | TOP_SOUTH; // 0xF3
        assertEquals(0xF3, expected);
        assertStairMeta("meta 2 (ascending south)", expected, 2);
    }

    @Test
    public void spec_meta3_ascendingNorth() {
        int expected = BOTTOM_LAYER | TOP_NORTH; // 0x3F
        assertEquals(0x3F, expected);
        assertStairMeta("meta 3 (ascending north)", expected, 3);
    }

    // ── upside-down stairs: top-half full + one bottom quadrant ──────────────

    @Test
    public void spec_meta4_upsideDownAscendingEast() {
        // Upside-down: top half full + the bottom-east quadrant.
        int expected = TOP_LAYER | BOTTOM_EAST; // 0xEE
        assertEquals(0xEE, expected);
        assertStairMeta("meta 4 (ud ascending east)", expected, 4);
    }

    @Test
    public void spec_meta5_upsideDownAscendingWest() {
        int expected = TOP_LAYER | BOTTOM_WEST; // 0xDD
        assertEquals(0xDD, expected);
        assertStairMeta("meta 5 (ud ascending west)", expected, 5);
    }

    @Test
    public void spec_meta6_upsideDownAscendingSouth() {
        int expected = TOP_LAYER | BOTTOM_SOUTH; // 0xFC
        assertEquals(0xFC, expected);
        assertStairMeta("meta 6 (ud ascending south)", expected, 6);
    }

    @Test
    public void spec_meta7_upsideDownAscendingNorth() {
        int expected = TOP_LAYER | BOTTOM_NORTH; // 0xCF
        assertEquals(0xCF, expected);
        assertStairMeta("meta 7 (ud ascending north)", expected, 7);
    }

    // ── bottom inner corners: bottom full + one top quadrant ─────────────────

    @Test
    public void spec_cornerBottomInnerNE() {
        int expected = BOTTOM_LAYER | filled(ETN); // 0x3B — bottom + top-NE only
        assertEquals(0x3B, expected);
        assertStairMeta("corner bottom inner NE", expected, 0);
    }

    @Test
    public void spec_cornerBottomInnerNW() {
        int expected = BOTTOM_LAYER | filled(WTN); // 0x37 — bottom + top-NW only
        assertEquals(0x37, expected);
        assertStairMeta("corner bottom inner NW", expected, 1);
    }

    @Test
    public void spec_cornerBottomInnerSE() {
        int expected = BOTTOM_LAYER | filled(ETS); // 0xB3 — bottom + top-SE only
        assertEquals(0xB3, expected);
        assertStairMeta("corner bottom inner SE", expected, 0);
    }

    @Test
    public void spec_cornerBottomInnerSW() {
        int expected = BOTTOM_LAYER | filled(WTS); // 0x73 — bottom + top-SW only
        assertEquals(0x73, expected);
        assertStairMeta("corner bottom inner SW", expected, 1);
    }

    // ── bottom outer corners: bottom full + three top quadrants ──────────────

    @Test
    public void spec_cornerBottomOuterNE() {
        // Missing the NE top quadrant; all other top quadrants filled.
        int expected = BOTTOM_LAYER | (TOP_LAYER & ~filled(ETN)); // 0xF7
        assertEquals(0xF7, expected);
        assertStairMeta("corner bottom outer NE", expected, 1);
    }

    @Test
    public void spec_cornerBottomOuterNW() {
        int expected = BOTTOM_LAYER | (TOP_LAYER & ~filled(WTN)); // 0xFB
        assertEquals(0xFB, expected);
        assertStairMeta("corner bottom outer NW", expected, 0);
    }

    @Test
    public void spec_cornerBottomOuterSE() {
        int expected = BOTTOM_LAYER | (TOP_LAYER & ~filled(ETS)); // 0x7F
        assertEquals(0x7F, expected);
        assertStairMeta("corner bottom outer SE", expected, 1);
    }

    @Test
    public void spec_cornerBottomOuterSW() {
        int expected = BOTTOM_LAYER | (TOP_LAYER & ~filled(WTS)); // 0xBF
        assertEquals(0xBF, expected);
        assertStairMeta("corner bottom outer SW", expected, 0);
    }

    // ── upside-down inner corners: top full + one bottom quadrant ────────────

    @Test
    public void spec_cornerUdInnerNE() {
        int expected = TOP_LAYER | filled(EBN); // 0xCE — top + bottom-NE only
        assertEquals(0xCE, expected);
        assertStairMeta("corner ud inner NE", expected, 4);
    }

    @Test
    public void spec_cornerUdInnerNW() {
        int expected = TOP_LAYER | filled(WBN); // 0xCD — top + bottom-NW only
        assertEquals(0xCD, expected);
        assertStairMeta("corner ud inner NW", expected, 5);
    }

    @Test
    public void spec_cornerUdInnerSE() {
        int expected = TOP_LAYER | filled(EBS); // 0xEC — top + bottom-SE only
        assertEquals(0xEC, expected);
        assertStairMeta("corner ud inner SE", expected, 4);
    }

    @Test
    public void spec_cornerUdInnerSW() {
        int expected = TOP_LAYER | filled(WBS); // 0xDC — top + bottom-SW only
        assertEquals(0xDC, expected);
        assertStairMeta("corner ud inner SW", expected, 5);
    }

    // ── upside-down outer corners: top full + three bottom quadrants ──────────

    @Test
    public void spec_cornerUdOuterNE() {
        int expected = TOP_LAYER | (BOTTOM_LAYER & ~filled(EBN)); // 0xFD
        assertEquals(0xFD, expected);
        assertStairMeta("corner ud outer NE", expected, 5);
    }

    @Test
    public void spec_cornerUdOuterNW() {
        int expected = TOP_LAYER | (BOTTOM_LAYER & ~filled(WBN)); // 0xFE
        assertEquals(0xFE, expected);
        assertStairMeta("corner ud outer NW", expected, 4);
    }

    @Test
    public void spec_cornerUdOuterSE() {
        int expected = TOP_LAYER | (BOTTOM_LAYER & ~filled(EBS)); // 0xDF
        assertEquals(0xDF, expected);
        assertStairMeta("corner ud outer SE", expected, 5);
    }

    @Test
    public void spec_cornerUdOuterSW() {
        int expected = TOP_LAYER | (BOTTOM_LAYER & ~filled(WBS)); // 0xEF
        assertEquals(0xEF, expected);
        assertStairMeta("corner ud outer SW", expected, 4);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER 2 — Lookup behaviour
    //
    // Surface blocks produce masks where only SOME sub-voxels are inside the
    // sphere. These tests verify the lookup selects the right shape kind and
    // orientation for representative surface patterns.
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    public void lookup_air_and_full() {
        assertEquals(PlacementKind.AIR, shape(0x00).kind());
        assertEquals(PlacementKind.FULL, shape(0xFF).kind());
    }

    @Test
    public void lookup_topSurface_bottomSubvoxelsInside() {
        // Sphere below: bottom layer all inside, top layer all outside → bottom slab.
        assertEquals(PlacementKind.SLAB_BOTTOM, shape(BOTTOM_LAYER).kind());
    }

    @Test
    public void lookup_bottomSurface_topSubvoxelsInside() {
        // Sphere above: top layer all inside, bottom layer all outside → top slab.
        assertEquals(PlacementKind.SLAB_TOP, shape(TOP_LAYER).kind());
    }

    @Test
    public void lookup_topSurface_eastDiagonal_regularStair() {
        // Sphere below-east: bottom full + top-east → ascending east stair (meta 0).
        BlockShape s = shape(BOTTOM_LAYER | TOP_EAST);
        assertEquals(PlacementKind.STAIR, s.kind());
        assertEquals(0, s.stairMeta());
    }

    @Test
    public void lookup_bottomSurface_eastDiagonal_upsideDownStair() {
        // Sphere above-east: top full + bottom-east → upside-down east stair (meta 4).
        BlockShape s = shape(TOP_LAYER | BOTTOM_EAST);
        assertEquals(PlacementKind.STAIR, s.kind());
        assertEquals(4, s.stairMeta());
    }

    @Test
    public void lookup_topHeavyMask_prefersUpsideDownShape() {
        // 5 top bits (0xEC = top full + bottom-SE): clearly top-heavy.
        BlockShape s = shape(TOP_LAYER | filled(EBS));
        assertEquals(PlacementKind.STAIR, s.kind());
        assertTrue("expected ud stair (meta ≥4), got " + s.stairMeta(), s.stairMeta() >= 4);
    }

    @Test
    public void lookup_bottomHeavyMask_prefersRegularShape() {
        // 5 bottom bits (0x3B = bottom full + top-NE): clearly bottom-heavy.
        BlockShape s = shape(BOTTOM_LAYER | filled(ETN));
        assertEquals(PlacementKind.STAIR, s.kind());
        assertTrue("expected regular stair (meta <4), got " + s.stairMeta(), s.stairMeta() < 4);
    }

    @Test
    public void lookup_allNonZeroMasksAreNonAir() {
        for (int mask = 1; mask < 256; mask++) {
            assertNotEquals(
                    String.format("mask 0x%02X should not map to AIR", mask),
                    PlacementKind.AIR,
                    shape(mask).kind());
        }
    }

    @Test
    public void lookup_allStairMetasInRange() {
        for (int mask = 0; mask < 256; mask++) {
            BlockShape s = shape(mask);
            if (s.kind() == PlacementKind.STAIR) {
                assertTrue(
                        String.format("mask 0x%02X (%s): meta %d out of [0,7]", mask, s.name(), s.stairMeta()),
                        s.stairMeta() >= 0 && s.stairMeta() <= 7);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER 3 — Sub-voxel discretization
    //
    // Tests that subVoxelMask() correctly classifies sub-voxels as inside/outside
    // given sphere center + radius. Block is fixed at origin (0,0,0); spheres are
    // positioned so that the geometry is unambiguous.
    //
    // Positioning principle: a sphere centered at (block ± 0.5) along one axis with
    // radius 0.6 always includes the four near-side sub-voxels (max dist ≈ 0.433)
    // and excludes the four far-side sub-voxels (min dist ≈ 0.829).
    // ═══════════════════════════════════════════════════════════════════════════

    private static final Vec3DFloat BLOCK = Vec3DFloat.from(0, 0, 0);
    private static final float HALF_SIDE_RADIUS = 0.6f;

    private static int maskFrom(Vec3DFloat sphereCenter, float radius) {
        return StairSlabSmoother.subVoxelMask(BLOCK, Arrays.asList(new SphereSample(sphereCenter, radius)));
    }

    private static int maskFrom(SphereSample... samples) {
        return StairSlabSmoother.subVoxelMask(BLOCK, Arrays.asList(samples));
    }

    private static SphereSample sphere(float x, float y, float z) {
        return new SphereSample(Vec3DFloat.from(x, y, z), HALF_SIDE_RADIUS);
    }

    @Test
    public void discretization_sphereFullyCovering_allSubvoxelsInside() {
        // Sphere at block center, radius > diagonal half (sqrt(3)*0.25 ≈ 0.433).
        int mask = maskFrom(BLOCK, 0.5f);
        assertEquals("all sub-voxels inside", 0xFF, mask);
    }

    @Test
    public void discretization_sphereFarAway_noSubvoxelsInside() {
        // Sphere 2 units away — nothing inside.
        int mask = maskFrom(Vec3DFloat.from(2, 0, 0), 0.5f);
        assertEquals("no sub-voxels inside", 0x00, mask);
    }

    @Test
    public void discretization_sphereBelow_bottomLayerOnly() {
        // Sphere below block: near side = bottom (y=−0.25), far side = top (y=+0.25).
        int mask = maskFrom(sphere(0, -0.5f, 0));
        assertEquals("bottom layer", BOTTOM_LAYER, mask);
    }

    @Test
    public void discretization_sphereAbove_topLayerOnly() {
        int mask = maskFrom(sphere(0, 0.5f, 0));
        assertEquals("top layer", TOP_LAYER, mask);
    }

    @Test
    public void discretization_sphereToEast_eastHalfOnly() {
        // Sphere to east: near side = east (x=+0.25), but those are the EAST sub-voxels
        // of the block, which is between the block and the sphere.
        // Actually: sphere at +0.5 on x → near side of block is east face → EAST sub-voxels inside.
        int mask = maskFrom(sphere(0.5f, 0, 0));
        int eastHalf = filled(EBN, ETN, EBS, ETS); // 0xAA
        assertEquals("east half", eastHalf, mask);
    }

    @Test
    public void discretization_sphereToWest_westHalfOnly() {
        int mask = maskFrom(sphere(-0.5f, 0, 0));
        int westHalf = filled(WBN, WTN, WBS, WTS); // 0x55
        assertEquals("west half", westHalf, mask);
    }

    @Test
    public void discretization_sphereToSouth_southHalfOnly() {
        int mask = maskFrom(sphere(0, 0, 0.5f));
        int southHalf = filled(WBS, EBS, WTS, ETS); // 0xF0
        assertEquals("south half", southHalf, mask);
    }

    @Test
    public void discretization_sphereToNorth_northHalfOnly() {
        int mask = maskFrom(sphere(0, 0, -0.5f));
        int northHalf = filled(WBN, EBN, WTN, ETN); // 0x0F
        assertEquals("north half", northHalf, mask);
    }

    @Test
    public void discretization_belowAndTopEast_ascendingEastStairMask() {
        // Two spheres compose bottom layer + top-east: expected mask = 0xBB (ascending east).
        int mask = maskFrom(sphere(0, -0.5f, 0), sphere(0.5f, 0.5f, 0));
        assertEquals("ascending east stair mask", BOTTOM_LAYER | TOP_EAST, mask);
    }

    @Test
    public void discretization_aboveAndBottomEast_upsideDownEastStairMask() {
        int mask = maskFrom(sphere(0, 0.5f, 0), sphere(0.5f, -0.5f, 0));
        assertEquals("ud east stair mask", TOP_LAYER | BOTTOM_EAST, mask);
    }

    @Test
    public void discretization_belowAndTopSouth_ascendingSouthStairMask() {
        int mask = maskFrom(sphere(0, -0.5f, 0), sphere(0, 0.5f, 0.5f));
        assertEquals("ascending south stair mask", BOTTOM_LAYER | TOP_SOUTH, mask);
    }

    @Test
    public void discretization_belowAndTopNorthEast_innerCornerMask() {
        // Single top-NE quadrant: inner corner NE (mask 0x3B).
        int mask = maskFrom(sphere(0, -0.5f, 0), sphere(0.5f, 0.5f, -0.5f));
        assertEquals("inner corner bottom NE mask", BOTTOM_LAYER | filled(ETN), mask);
    }

    @Test
    public void discretization_shape_fromMask_ascendingEast() {
        int mask = maskFrom(sphere(0, -0.5f, 0), sphere(0.5f, 0.5f, 0));
        BlockShape shape = StairSlabSmoother.shapeForMask(mask);
        assertEquals(PlacementKind.STAIR, shape.kind());
        assertEquals("ascending east meta", 0, shape.stairMeta());
    }

    @Test
    public void discretization_shape_fromMask_bottomSlab() {
        int mask = maskFrom(sphere(0, -0.5f, 0));
        BlockShape shape = StairSlabSmoother.shapeForMask(mask);
        assertEquals(PlacementKind.SLAB_BOTTOM, shape.kind());
    }

    @Test
    public void discretization_shape_fromMask_topSlab() {
        int mask = maskFrom(sphere(0, 0.5f, 0));
        BlockShape shape = StairSlabSmoother.shapeForMask(mask);
        assertEquals(PlacementKind.SLAB_TOP, shape.kind());
    }

    // ── sanity: derived constants match their hex values ─────────────────────

    @Test
    public void constants_matchExpectedHex() {
        assertEquals(0x33, BOTTOM_LAYER);
        assertEquals(0xCC, TOP_LAYER);
        assertEquals(0x88, TOP_EAST);
        assertEquals(0x44, TOP_WEST);
        assertEquals(0xC0, TOP_SOUTH);
        assertEquals(0x0C, TOP_NORTH);
        assertEquals(0x22, BOTTOM_EAST);
        assertEquals(0x11, BOTTOM_WEST);
        assertEquals(0x30, BOTTOM_SOUTH);
        assertEquals(0x03, BOTTOM_NORTH);
    }
}
