/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import org.junit.Test;

import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeToolState.ShapeType;
import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;

public class ShapeMathTest {

    private static final int TORUS_R = 6, TORUS_RZ = 6, TORUS_TUBE = 2;
    private static final float EXP = 2f;
    private static final int WALL = 2;
    private static final int SIDES = 6;
    private static final float SPIRAL_SPACING = 1.5f, SPIRAL_TURNS = 3f;

    private static boolean shape(ShapeType type, int dx, int dy, int dz, int w, int h, int d, boolean hollow) {
        return ShapeMath.inShapeGeom(
            type,
            dx,
            dy,
            dz,
            w,
            h,
            d,
            hollow,
            EXP,
            TORUS_R,
            TORUS_RZ,
            TORUS_TUBE,
            WALL,
            EXP,
            SIDES,
            SPIRAL_SPACING,
            SPIRAL_TURNS,
            1f);
    }

    private static int count(ShapeType type, int w, int h, int d, boolean hollow) {
        int n = 0;
        for (int dx = 0; dx < w; dx++) for (int dy = 0; dy < h; dy++)
            for (int dz = 0; dz < d; dz++) if (shape(type, dx, dy, dz, w, h, d, hollow)) n++;
        return n;
    }

    // ── CUBOID ───────────────────────────────────────────────────────────────

    @Test
    public void cuboidSolidFillsAll() {
        assertEquals(5 * 5 * 5, count(ShapeType.CUBOID, 5, 5, 5, false));
    }

    @Test
    public void cuboidHollowExcludesCenter() {
        assertFalse(shape(ShapeType.CUBOID, 2, 2, 2, 5, 5, 5, true));
    }

    @Test
    public void cuboidHollowIncludesFace() {
        assertTrue(shape(ShapeType.CUBOID, 0, 2, 2, 5, 5, 5, true));
    }

    @Test
    public void cuboidHollowCountIsShell() {
        // 5^3 - 3^3 = 125 - 27 = 98
        assertEquals(98, count(ShapeType.CUBOID, 5, 5, 5, true));
    }

    // ── SPHERE ───────────────────────────────────────────────────────────────

    @Test
    public void sphereCenterInside() {
        assertTrue(shape(ShapeType.SPHERE, 5, 5, 5, 11, 11, 11, false));
    }

    @Test
    public void sphereCornerOutside() {
        assertFalse(shape(ShapeType.SPHERE, 0, 0, 0, 11, 11, 11, false));
    }

    @Test
    public void sphereEquatorInside() {
        assertTrue(shape(ShapeType.SPHERE, 5, 5, 0, 11, 11, 11, false));
    }

    @Test
    public void sphereSmallerThanBoundingBox() {
        int solid = count(ShapeType.SPHERE, 11, 11, 11, false);
        assertTrue(solid > 0);
        assertTrue(solid < 11 * 11 * 11);
    }

    @Test
    public void sphereHollowSmallerThanSolid() {
        int solid = count(ShapeType.SPHERE, 11, 11, 11, false);
        int hollow = count(ShapeType.SPHERE, 11, 11, 11, true);
        assertTrue(hollow > 0);
        assertTrue(hollow < solid);
    }

    // ── CYLINDER ─────────────────────────────────────────────────────────────

    @Test
    public void cylinderCenterInside() {
        assertTrue(shape(ShapeType.CYLINDER, 5, 3, 5, 11, 7, 11, false));
    }

    @Test
    public void cylinderCornerOutside() {
        assertFalse(shape(ShapeType.CYLINDER, 0, 3, 0, 11, 7, 11, false));
    }

    @Test
    public void cylinderHollowCapsIncluded() {
        assertTrue(shape(ShapeType.CYLINDER, 5, 0, 5, 11, 7, 11, true));
        assertTrue(shape(ShapeType.CYLINDER, 5, 6, 5, 11, 7, 11, true));
    }

    @Test
    public void cylinderHollowMidCenterExcluded() {
        assertFalse(shape(ShapeType.CYLINDER, 5, 3, 5, 11, 7, 11, true));
    }

    @Test
    public void cylinderXSymmetric() {
        assertEquals(
            shape(ShapeType.CYLINDER, 3, 3, 5, 11, 7, 11, false),
            shape(ShapeType.CYLINDER, 7, 3, 5, 11, 7, 11, false));
    }

    // ── PYRAMID ──────────────────────────────────────────────────────────────

    @Test
    public void pyramidBaseFullWidth() {
        assertTrue(shape(ShapeType.PYRAMID, 4, 0, 4, 9, 9, 9, false));
        assertTrue(shape(ShapeType.PYRAMID, 0, 0, 0, 9, 9, 9, false));
    }

    @Test
    public void pyramidApexOneBlock() {
        assertTrue(shape(ShapeType.PYRAMID, 4, 8, 4, 9, 9, 9, false));
        assertFalse(shape(ShapeType.PYRAMID, 6, 8, 4, 9, 9, 9, false));
    }

    @Test
    public void pyramidMonotoneShrinks() {
        int w = 9, h = 9, d = 9;
        int prev = w * d;
        for (int dy = 1; dy < h; dy++) {
            int slice = 0;
            for (int dx = 0; dx < w; dx++)
                for (int dz = 0; dz < d; dz++) if (shape(ShapeType.PYRAMID, dx, dy, dz, w, h, d, false)) slice++;
            assertTrue("slice at dy=" + dy + " should be <= previous", slice <= prev);
            prev = slice;
        }
    }

    // ── CONE ─────────────────────────────────────────────────────────────────

    @Test
    public void coneBaseEdgeInside() {
        assertTrue(shape(ShapeType.CONE, 5, 0, 5, 11, 11, 11, false));
        assertTrue(shape(ShapeType.CONE, 0, 0, 5, 11, 11, 11, false));
    }

    @Test
    public void coneBaseCornerOutside() {
        assertFalse(shape(ShapeType.CONE, 0, 0, 0, 11, 11, 11, false));
    }

    @Test
    public void coneApexOneBlock() {
        int apexCount = 0;
        for (int dx = 0; dx < 11; dx++)
            for (int dz = 0; dz < 11; dz++) if (shape(ShapeType.CONE, dx, 10, dz, 11, 11, 11, false)) apexCount++;
        assertEquals(1, apexCount);
    }

    @Test
    public void coneMonotoneShrinks() {
        int w = 11, h = 11, d = 11;
        int prev = w * d;
        for (int dy = 1; dy < h; dy++) {
            int slice = 0;
            for (int dx = 0; dx < w; dx++)
                for (int dz = 0; dz < d; dz++) if (shape(ShapeType.CONE, dx, dy, dz, w, h, d, false)) slice++;
            assertTrue("cone slice at dy=" + dy + " should shrink", slice <= prev);
            prev = slice;
        }
    }

    @Test
    public void coneHollowSmallerThanSolid() {
        int solid = count(ShapeType.CONE, 11, 11, 11, false);
        int hollow = count(ShapeType.CONE, 11, 11, 11, true);
        assertTrue(hollow < solid);
    }

    // ── TORUS ─────────────────────────────────────────────────────────────────

    @Test
    public void torusRingEquatorInsideSolid() {
        int R = TORUS_R, r = TORUS_TUBE, outer = R + r;
        int w = outer * 2 + 1, h = r * 2 + 1;
        float cx = (w - 1) / 2f, cy = (h - 1) / 2f;
        int dx = Math.round(cx + R), dy = Math.round(cy), dz = Math.round(cx);
        assertTrue(shape(ShapeType.TORUS, dx, dy, dz, w, h, w, false));
    }

    @Test
    public void torusRingEquatorOutsideHollow() {
        int R = TORUS_R, r = TORUS_TUBE, outer = R + r;
        int w = outer * 2 + 1, h = r * 2 + 1;
        float cx = (w - 1) / 2f, cy = (h - 1) / 2f;
        int dx = Math.round(cx + R), dy = Math.round(cy), dz = Math.round(cx);
        assertFalse(shape(ShapeType.TORUS, dx, dy, dz, w, h, w, true));
    }

    @Test
    public void torusOuterSurfaceInBoth() {
        int R = TORUS_R, r = TORUS_TUBE, outer = R + r;
        int w = outer * 2 + 1, h = r * 2 + 1;
        float cx = (w - 1) / 2f, cy = (h - 1) / 2f;
        int dxOuter = Math.round(cx + R + r), dy = Math.round(cy), dz = Math.round(cx);
        assertTrue(shape(ShapeType.TORUS, dxOuter, dy, dz, w, h, w, false));
        assertTrue(shape(ShapeType.TORUS, dxOuter, dy, dz, w, h, w, true));
    }

    @Test
    public void torusCenterIsHole() {
        int r = TORUS_TUBE;
        int outer = TORUS_R + r;
        int w = outer * 2 + 1, h = r * 2 + 1;
        float cx = (w - 1) / 2f, cy = (h - 1) / 2f;
        assertFalse(shape(ShapeType.TORUS, Math.round(cx), Math.round(cy), Math.round(cx), w, h, w, false));
    }

    @Test
    public void torusHollowSmallerThanSolid() {
        int r = TORUS_TUBE;
        int outer = TORUS_R + r;
        int w = outer * 2 + 1, h = r * 2 + 1;
        int solid = count(ShapeType.TORUS, w, h, w, false);
        int hollow = count(ShapeType.TORUS, w, h, w, true);
        assertTrue(solid > 0);
        assertTrue(hollow > 0);
        assertTrue(hollow < solid);
    }

    // ── OCTAHEDRON ────────────────────────────────────────────────────────────

    @Test
    public void octahedronCenterInside() {
        assertTrue(shape(ShapeType.OCTAHEDRON, 5, 5, 5, 11, 11, 11, false));
    }

    @Test
    public void octahedronCornerOutside() {
        assertFalse(shape(ShapeType.OCTAHEDRON, 0, 0, 0, 11, 11, 11, false));
    }

    @Test
    public void octahedronAxialApicesInside() {
        assertTrue(shape(ShapeType.OCTAHEDRON, 10, 5, 5, 11, 11, 11, false));
        assertTrue(shape(ShapeType.OCTAHEDRON, 5, 10, 5, 11, 11, 11, false));
        assertTrue(shape(ShapeType.OCTAHEDRON, 5, 5, 10, 11, 11, 11, false));
    }

    @Test
    public void octahedronXSymmetric() {
        assertEquals(
            shape(ShapeType.OCTAHEDRON, 3, 5, 5, 11, 11, 11, false),
            shape(ShapeType.OCTAHEDRON, 7, 5, 5, 11, 11, 11, false));
    }

    @Test
    public void octahedronHollowSmallerThanSolid() {
        int solid = count(ShapeType.OCTAHEDRON, 11, 11, 11, false);
        int hollow = count(ShapeType.OCTAHEDRON, 11, 11, 11, true);
        assertTrue(solid < 11 * 11 * 11);
        assertTrue(hollow > 0);
        assertTrue(hollow < solid);
    }

    // ── DISK ─────────────────────────────────────────────────────────────────

    @Test
    public void diskCenterMidYInside() {
        assertTrue(shape(ShapeType.DISK, 5, 2, 5, 11, 5, 11, false));
    }

    @Test
    public void diskOffYExcluded() {
        assertFalse(shape(ShapeType.DISK, 5, 3, 5, 11, 5, 11, false));
    }

    @Test
    public void diskCornerOnMidYOutside() {
        assertFalse(shape(ShapeType.DISK, 0, 2, 0, 11, 5, 11, false));
    }

    @Test
    public void diskOnlyOneMidYLayer() {
        // Solid disk count == cylinder of height 1 count
        int diskCount = count(ShapeType.DISK, 11, 5, 11, false);
        int cylOne = count(ShapeType.CYLINDER, 11, 1, 11, false);
        assertEquals(cylOne, diskCount);
    }

    @Test
    public void diskHollowSmallerThanSolid() {
        int solid = count(ShapeType.DISK, 11, 5, 11, false);
        int hollow = count(ShapeType.DISK, 11, 5, 11, true);
        assertTrue(hollow > 0);
        assertTrue(hollow < solid);
    }

    // ── PLANE ─────────────────────────────────────────────────────────────────

    @Test
    public void planeExactlyOneMidYLayer() {
        assertEquals(9 * 9, count(ShapeType.PLANE, 9, 5, 9, false));
    }

    @Test
    public void planeWrongYExcluded() {
        assertFalse(shape(ShapeType.PLANE, 4, 3, 4, 9, 5, 9, false));
    }

    @Test
    public void planeHollowSameAsSolid() {
        assertEquals(count(ShapeType.PLANE, 9, 5, 9, false), count(ShapeType.PLANE, 9, 5, 9, true));
    }

    // ── SUPERELLIPSE ──────────────────────────────────────────────────────────

    @Test
    public void superellipseN2MatchesCylinderMidSlice() {
        int w = 11, h = 5, d = 11;
        int midY = (h - 1) / 2;
        int seCount = count(ShapeType.SUPERELLIPSE, w, h, d, false);
        int cylMid = 0;
        for (int dx = 0; dx < w; dx++)
            for (int dz = 0; dz < d; dz++) if (shape(ShapeType.CYLINDER, dx, midY, dz, w, h, d, false)) cylMid++;
        assertEquals(cylMid, seCount);
    }

    @Test
    public void superellipseOffLayerExcluded() {
        assertFalse(shape(ShapeType.SUPERELLIPSE, 5, 3, 5, 11, 5, 11, false));
    }

    @Test
    public void superellipseHollowSmallerThanSolid() {
        int solid = count(ShapeType.SUPERELLIPSE, 11, 5, 11, false);
        int hollow = count(ShapeType.SUPERELLIPSE, 11, 5, 11, true);
        assertTrue(hollow > 0);
        assertTrue(hollow < solid);
    }

    // ── SUPERSPHERE ───────────────────────────────────────────────────────────

    @Test
    public void supersphereN2EqualsSphereCounts() {
        assertEquals(count(ShapeType.SPHERE, 11, 11, 11, false), count(ShapeType.SUPERSPHERE, 11, 11, 11, false));
    }

    @Test
    public void supersphereCenterInside() {
        assertTrue(shape(ShapeType.SUPERSPHERE, 5, 5, 5, 11, 11, 11, false));
    }

    @Test
    public void supersphereCornerOutside() {
        assertFalse(shape(ShapeType.SUPERSPHERE, 0, 0, 0, 11, 11, 11, false));
    }

    @Test
    public void supersphereHollowSmallerThanSolid() {
        int solid = count(ShapeType.SUPERSPHERE, 11, 11, 11, false);
        int hollow = count(ShapeType.SUPERSPHERE, 11, 11, 11, true);
        assertTrue(hollow > 0);
        assertTrue(hollow < solid);
    }

    // ── TUBE ─────────────────────────────────────────────────────────────────

    @Test
    public void tubeCenterEmpty() {
        assertFalse(shape(ShapeType.TUBE, 5, 3, 5, 11, 7, 11, false));
    }

    @Test
    public void tubeRingBandIncluded() {
        // rx=5.5, wall=2 → inner rx=3.5: dx=cx+4=9 is in band
        assertTrue(shape(ShapeType.TUBE, 9, 3, 5, 11, 7, 11, false));
    }

    @Test
    public void tubeOutsideExcluded() {
        assertFalse(shape(ShapeType.TUBE, 0, 3, 0, 11, 7, 11, false));
    }

    @Test
    public void tubeSmallerThanCylinder() {
        assertTrue(count(ShapeType.TUBE, 11, 7, 11, false) < count(ShapeType.CYLINDER, 11, 7, 11, false));
    }

    // ── DODECAHEDRON ──────────────────────────────────────────────────────────

    @Test
    public void dodecahedronCenterInside() {
        assertTrue(shape(ShapeType.DODECAHEDRON, 6, 6, 6, 13, 13, 13, false));
    }

    @Test
    public void dodecahedronCornerOutside() {
        assertFalse(shape(ShapeType.DODECAHEDRON, 0, 0, 0, 13, 13, 13, false));
    }

    @Test
    public void dodecahedronHollowSmallerThanSolid() {
        int solid = count(ShapeType.DODECAHEDRON, 13, 13, 13, false);
        int hollow = count(ShapeType.DODECAHEDRON, 13, 13, 13, true);
        assertTrue(solid > 0);
        assertTrue(solid < 13 * 13 * 13);
        assertTrue(hollow > 0);
        assertTrue(hollow < solid);
    }

    // ── ICOSAHEDRON ───────────────────────────────────────────────────────────

    @Test
    public void icosahedronCenterInside() {
        assertTrue(shape(ShapeType.ICOSAHEDRON, 6, 6, 6, 13, 13, 13, false));
    }

    @Test
    public void icosahedronCornerOutside() {
        assertFalse(shape(ShapeType.ICOSAHEDRON, 0, 0, 0, 13, 13, 13, false));
    }

    @Test
    public void icosahedronHollowSmallerThanSolid() {
        int solid = count(ShapeType.ICOSAHEDRON, 13, 13, 13, false);
        int hollow = count(ShapeType.ICOSAHEDRON, 13, 13, 13, true);
        assertTrue(solid > 0);
        assertTrue(solid < 13 * 13 * 13);
        assertTrue(hollow > 0);
        assertTrue(hollow < solid);
    }

    // ── REGULAR_POLYGON ───────────────────────────────────────────────────────

    @Test
    public void regularPolygonCenterInside() {
        assertTrue(shape(ShapeType.REGULAR_POLYGON, 6, 2, 6, 13, 5, 13, false));
    }

    @Test
    public void regularPolygonOffLayerExcluded() {
        assertFalse(shape(ShapeType.REGULAR_POLYGON, 6, 3, 6, 13, 5, 13, false));
    }

    @Test
    public void regularPolygonSmallerThanBoundingSquare() {
        int solid = count(ShapeType.REGULAR_POLYGON, 13, 5, 13, false);
        assertTrue(solid > 0);
        assertTrue(solid < 13 * 13);
    }

    @Test
    public void regularPolygonHollowSmallerThanSolid() {
        int solid = count(ShapeType.REGULAR_POLYGON, 13, 5, 13, false);
        int hollow = count(ShapeType.REGULAR_POLYGON, 13, 5, 13, true);
        assertTrue(hollow > 0);
        assertTrue(hollow < solid);
    }

    // ── ARCHIMEDEAN_SPIRAL ────────────────────────────────────────────────────

    @Test
    public void spiralOffLayerExcluded() {
        assertFalse(shape(ShapeType.ARCHIMEDEAN_SPIRAL, 10, 3, 10, 21, 5, 21, false));
    }

    @Test
    public void spiralHasBlocks() {
        assertTrue(count(ShapeType.ARCHIMEDEAN_SPIRAL, 21, 5, 21, false) > 0);
    }

    @Test
    public void spiralSparserThanDisk() {
        int spiral = count(ShapeType.ARCHIMEDEAN_SPIRAL, 21, 5, 21, false);
        int disk = count(ShapeType.DISK, 21, 5, 21, false);
        assertTrue(spiral < disk);
    }

    // ── ROTATION MATRIX ───────────────────────────────────────────────────────

    @Test
    public void rotationMatrixIdentity() {
        Mat3DFloat I = ShapeMath.buildRotationMatrix(0, 0, 0);
        assertEquals(1f, I.r00(), 1e-6f);
        assertEquals(1f, I.r11(), 1e-6f);
        assertEquals(1f, I.r22(), 1e-6f);
        assertEquals(0f, I.r01(), 1e-6f);
        assertEquals(0f, I.r10(), 1e-6f);
    }

    @Test
    public void rotationMatrixRoundtrip() {
        float[][] cases = { { 30, 45, 60 }, { -15, 70, 0 }, { 45, 0, -30 } };
        for (float[] c : cases) {
            Mat3DFloat R = ShapeMath.buildRotationMatrix(c[0], c[1], c[2]);
            Vec3DFloat back = R.toEulerDeg();
            Mat3DFloat R2 = ShapeMath.buildRotationMatrix(back.x(), back.y(), back.z());
            assertEquals("r00 for (" + c[0] + "," + c[1] + "," + c[2] + ")", R.r00(), R2.r00(), 1e-4f);
            assertEquals("r01", R.r01(), R2.r01(), 1e-4f);
            assertEquals("r02", R.r02(), R2.r02(), 1e-4f);
            assertEquals("r10", R.r10(), R2.r10(), 1e-4f);
            assertEquals("r11", R.r11(), R2.r11(), 1e-4f);
            assertEquals("r12", R.r12(), R2.r12(), 1e-4f);
            assertEquals("r20", R.r20(), R2.r20(), 1e-4f);
            assertEquals("r21", R.r21(), R2.r21(), 1e-4f);
            assertEquals("r22", R.r22(), R2.r22(), 1e-4f);
        }
    }

    @Test
    public void rotationMatrixGimbalLockY90() {
        Mat3DFloat R = ShapeMath.buildRotationMatrix(0, 90, 0);
        Vec3DFloat back = R.toEulerDeg();
        Mat3DFloat R2 = ShapeMath.buildRotationMatrix(back.x(), back.y(), back.z());
        assertEquals("r00", R.r00(), R2.r00(), 1e-4f);
        assertEquals("r01", R.r01(), R2.r01(), 1e-4f);
        assertEquals("r02", R.r02(), R2.r02(), 1e-4f);
        assertEquals("r10", R.r10(), R2.r10(), 1e-4f);
        assertEquals("r11", R.r11(), R2.r11(), 1e-4f);
        assertEquals("r12", R.r12(), R2.r12(), 1e-4f);
        assertEquals("r20", R.r20(), R2.r20(), 1e-4f);
        assertEquals("r21", R.r21(), R2.r21(), 1e-4f);
        assertEquals("r22", R.r22(), R2.r22(), 1e-4f);
    }
}
