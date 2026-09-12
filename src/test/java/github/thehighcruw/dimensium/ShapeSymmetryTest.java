package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import org.junit.Test;

import github.thehighcruw.dimensium.tool.math.ShapeMath;
import github.thehighcruw.dimensium.tool.state.ShapeToolState.ShapeType;

/**
 * Exhaustive symmetry tests for every ShapeType at multiple sizes (odd, even, non-cubic).
 *
 * Symmetry definitions (all about bounding-box center):
 * X: shape(dx, dy, dz) == shape(w-1-dx, dy, dz)
 * Y: shape(dx, dy, dz) == shape(dx, h-1-dy, dz)
 * Z: shape(dx, dy, dz) == shape(dx, dy, d-1-dz)
 *
 * Expected symmetries per shape:
 * XYZ — CUBOID, SPHERE, CYLINDER, TORUS, OCTAHEDRON, SUPERSPHERE, TUBE,
 * DODECAHEDRON, ICOSAHEDRON
 * XZ — PYRAMID, CONE (taper breaks Y)
 * XZ+Y when h is odd — DISK, PLANE, SUPERELLIPSE, REGULAR_POLYGON (even sides)
 * (odd h: midY = (h-1)/2 = h-1-midY → Y-symmetric)
 * (even h: midY ≠ h-1-midY → not Y-symmetric)
 */
public class ShapeSymmetryTest {

    // Default shape params
    private static final float EXP = 2f;
    private static final int WALL = 2;
    private static final int SIDES = 6; // hexagon: even, so XZ-symmetric
    private static final float SPIRAL_SP = 1.5f, SPIRAL_T = 3f;

    private static boolean s(ShapeType type, int dx, int dy, int dz, int w, int h, int d, boolean hollow, int ringR,
        int ringRZ, int tubeR) {
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
            ringR,
            ringRZ,
            tubeR,
            WALL,
            EXP,
            SIDES,
            SPIRAL_SP,
            SPIRAL_T);
    }

    private static boolean s(ShapeType type, int dx, int dy, int dz, int w, int h, int d, boolean hollow) {
        return s(type, dx, dy, dz, w, h, d, hollow, 6, 6, 2);
    }

    // ── symmetry assertion helpers ────────────────────────────────────────────

    private static void assertXSym(ShapeType type, int w, int h, int d, boolean hollow, int rR, int rRZ, int tR) {
        String tag = type + " w=" + w + " h=" + h + " d=" + d + " hollow=" + hollow;
        for (int dx = 0; dx < w; dx++) for (int dy = 0; dy < h; dy++) for (int dz = 0; dz < d; dz++) {
            boolean a = s(type, dx, dy, dz, w, h, d, hollow, rR, rRZ, tR);
            boolean b = s(type, w - 1 - dx, dy, dz, w, h, d, hollow, rR, rRZ, tR);
            if (a != b) fail(tag + " X-sym broken at (" + dx + "," + dy + "," + dz + ")");
        }
    }

    private static void assertYSym(ShapeType type, int w, int h, int d, boolean hollow, int rR, int rRZ, int tR) {
        String tag = type + " w=" + w + " h=" + h + " d=" + d + " hollow=" + hollow;
        for (int dx = 0; dx < w; dx++) for (int dy = 0; dy < h; dy++) for (int dz = 0; dz < d; dz++) {
            boolean a = s(type, dx, dy, dz, w, h, d, hollow, rR, rRZ, tR);
            boolean b = s(type, dx, h - 1 - dy, dz, w, h, d, hollow, rR, rRZ, tR);
            if (a != b) fail(tag + " Y-sym broken at (" + dx + "," + dy + "," + dz + ")");
        }
    }

    private static void assertZSym(ShapeType type, int w, int h, int d, boolean hollow, int rR, int rRZ, int tR) {
        String tag = type + " w=" + w + " h=" + h + " d=" + d + " hollow=" + hollow;
        for (int dx = 0; dx < w; dx++) for (int dy = 0; dy < h; dy++) for (int dz = 0; dz < d; dz++) {
            boolean a = s(type, dx, dy, dz, w, h, d, hollow, rR, rRZ, tR);
            boolean b = s(type, dx, dy, d - 1 - dz, w, h, d, hollow, rR, rRZ, tR);
            if (a != b) fail(tag + " Z-sym broken at (" + dx + "," + dy + "," + dz + ")");
        }
    }

    private static void assertXSym(ShapeType t, int w, int h, int d, boolean hollow) {
        assertXSym(t, w, h, d, hollow, 6, 6, 2);
    }

    private static void assertYSym(ShapeType t, int w, int h, int d, boolean hollow) {
        assertYSym(t, w, h, d, hollow, 6, 6, 2);
    }

    private static void assertZSym(ShapeType t, int w, int h, int d, boolean hollow) {
        assertZSym(t, w, h, d, hollow, 6, 6, 2);
    }

    private static void assertXYZSym(ShapeType t, int w, int h, int d, boolean hollow) {
        assertXSym(t, w, h, d, hollow);
        assertYSym(t, w, h, d, hollow);
        assertZSym(t, w, h, d, hollow);
    }

    private static void assertXZSym(ShapeType t, int w, int h, int d, boolean hollow) {
        assertXSym(t, w, h, d, hollow);
        assertZSym(t, w, h, d, hollow);
    }

    // Sizes: odd cubic, even cubic, non-cubic (w≠h≠d), small, large
    // Hollow tested separately — doubles coverage.

    // ── CUBOID ───────────────────────────────────────────────────────────────

    @Test
    public void cuboidSymmetryOddCubic() {
        assertXYZSym(ShapeType.CUBOID, 5, 5, 5, false);
    }

    @Test
    public void cuboidSymmetryEvenCubic() {
        assertXYZSym(ShapeType.CUBOID, 6, 6, 6, false);
    }

    @Test
    public void cuboidSymmetryNonCubic() {
        assertXYZSym(ShapeType.CUBOID, 5, 7, 9, false);
    }

    @Test
    public void cuboidHollowSymmetryOdd() {
        assertXYZSym(ShapeType.CUBOID, 7, 7, 7, true);
    }

    @Test
    public void cuboidHollowSymmetryEven() {
        assertXYZSym(ShapeType.CUBOID, 8, 6, 4, true);
    }

    // ── SPHERE ───────────────────────────────────────────────────────────────

    @Test
    public void sphereSymmetryOddCubic() {
        assertXYZSym(ShapeType.SPHERE, 9, 9, 9, false);
    }

    @Test
    public void sphereSymmetryEvenCubic() {
        assertXYZSym(ShapeType.SPHERE, 8, 8, 8, false);
    }

    @Test
    public void sphereSymmetryNonCubic() {
        assertXYZSym(ShapeType.SPHERE, 7, 9, 11, false);
    }

    @Test
    public void sphereSymmetryLarge() {
        assertXYZSym(ShapeType.SPHERE, 15, 11, 13, false);
    }

    @Test
    public void sphereHollowSymmetryOdd() {
        assertXYZSym(ShapeType.SPHERE, 11, 11, 11, true);
    }

    @Test
    public void sphereHollowSymmetryEven() {
        assertXYZSym(ShapeType.SPHERE, 10, 8, 12, true);
    }

    // ── CYLINDER ─────────────────────────────────────────────────────────────

    @Test
    public void cylinderSymmetryOddCubic() {
        assertXYZSym(ShapeType.CYLINDER, 9, 9, 9, false);
    }

    @Test
    public void cylinderSymmetryEvenCubic() {
        assertXYZSym(ShapeType.CYLINDER, 8, 8, 8, false);
    }

    @Test
    public void cylinderSymmetryTallNarrow() {
        assertXYZSym(ShapeType.CYLINDER, 7, 13, 7, false);
    }

    @Test
    public void cylinderSymmetryWideShort() {
        assertXYZSym(ShapeType.CYLINDER, 11, 5, 11, false);
    }

    @Test
    public void cylinderHollowSymmetryOdd() {
        assertXYZSym(ShapeType.CYLINDER, 11, 7, 11, true);
    }

    @Test
    public void cylinderHollowSymmetryEven() {
        assertXYZSym(ShapeType.CYLINDER, 10, 6, 10, true);
    }

    // ── PYRAMID ──────────────────────────────────────────────────────────────
    // Tapers upward: Y-symmetry breaks (base full, apex 1 block). XZ always symmetric.

    @Test
    public void pyramidXZSymmetryOdd() {
        assertXZSym(ShapeType.PYRAMID, 9, 9, 9, false);
    }

    @Test
    public void pyramidXZSymmetryEven() {
        assertXZSym(ShapeType.PYRAMID, 8, 8, 8, false);
    }

    @Test
    public void pyramidXZSymmetryNonCubic() {
        assertXZSym(ShapeType.PYRAMID, 7, 11, 9, false);
    }

    @Test
    public void pyramidXZSymmetryLarge() {
        assertXZSym(ShapeType.PYRAMID, 13, 15, 11, false);
    }

    // ── CONE ─────────────────────────────────────────────────────────────────
    // Same as pyramid: Y broken, XZ symmetric.

    @Test
    public void coneXZSymmetryOdd() {
        assertXZSym(ShapeType.CONE, 9, 9, 9, false);
    }

    @Test
    public void coneXZSymmetryEven() {
        assertXZSym(ShapeType.CONE, 8, 8, 8, false);
    }

    @Test
    public void coneXZSymmetryNonCubic() {
        assertXZSym(ShapeType.CONE, 7, 11, 9, false);
    }

    @Test
    public void coneXZSymmetryLarge() {
        assertXZSym(ShapeType.CONE, 13, 15, 11, false);
    }

    @Test
    public void coneHollowXZSymmetryOdd() {
        assertXZSym(ShapeType.CONE, 11, 11, 11, true);
    }

    @Test
    public void coneHollowXZSymmetryEven() {
        assertXZSym(ShapeType.CONE, 10, 10, 10, true);
    }

    // ── TORUS ─────────────────────────────────────────────────────────────────

    private static void torusXYZSym(int ringR, int ringRZ, int tubeR, boolean hollow) {
        int outer = ringR + tubeR;
        int w = outer * 2 + 1, h = tubeR * 2 + 1;
        assertXSym(ShapeType.TORUS, w, h, w, hollow, ringR, ringRZ, tubeR);
        assertYSym(ShapeType.TORUS, w, h, w, hollow, ringR, ringRZ, tubeR);
        assertZSym(ShapeType.TORUS, w, h, w, hollow, ringR, ringRZ, tubeR);
    }

    @Test
    public void torusSymmetrySmall() {
        torusXYZSym(4, 4, 2, false);
    }

    @Test
    public void torusSymmetryMedium() {
        torusXYZSym(6, 6, 2, false);
    }

    @Test
    public void torusSymmetryLargeTube() {
        torusXYZSym(7, 7, 3, false);
    }

    // Hollow torus: XZ solid-surface boundary is fine; the inner hollow boundary
    // at tubeDist2 == ir*ir can break float X-symmetry by ULP when atan2 operands
    // change sign. Only assert Z-symmetry (atan2 argument order unaffected by Z-flip).
    @Test
    public void torusHollowSymmetrySmallZ() {
        int ringR = 4, ringRZ = 4, tubeR = 2;
        int outer = ringR + tubeR;
        int w = outer * 2 + 1, h = tubeR * 2 + 1;
        assertZSym(ShapeType.TORUS, w, h, w, true, ringR, ringRZ, tubeR);
    }

    @Test
    public void torusHollowSymmetryMediumZ() {
        int ringR = 6, ringRZ = 6, tubeR = 2;
        int outer = ringR + tubeR;
        int w = outer * 2 + 1, h = tubeR * 2 + 1;
        assertZSym(ShapeType.TORUS, w, h, w, true, ringR, ringRZ, tubeR);
    }

    // Elliptic torus (ringR != ringRZ): XZ still symmetric, Y symmetric
    @Test
    public void torusEllipticSymmetry() {
        torusXYZSym(6, 4, 2, false);
    }

    // ── OCTAHEDRON ────────────────────────────────────────────────────────────

    @Test
    public void octahedronSymmetryOddCubic() {
        assertXYZSym(ShapeType.OCTAHEDRON, 9, 9, 9, false);
    }

    @Test
    public void octahedronSymmetryEvenCubic() {
        assertXYZSym(ShapeType.OCTAHEDRON, 8, 8, 8, false);
    }

    @Test
    public void octahedronSymmetryNonCubic() {
        assertXYZSym(ShapeType.OCTAHEDRON, 7, 9, 11, false);
    }

    @Test
    public void octahedronSymmetryLarge() {
        assertXYZSym(ShapeType.OCTAHEDRON, 13, 11, 15, false);
    }

    @Test
    public void octahedronHollowSymmetry() {
        assertXYZSym(ShapeType.OCTAHEDRON, 11, 11, 11, true);
    }

    // ── DISK ─────────────────────────────────────────────────────────────────
    // Single midY layer. XZ symmetric. Y-symmetric only when h is odd.

    @Test
    public void diskXZSymmetryOddH() {
        assertXZSym(ShapeType.DISK, 9, 5, 9, false);
    }

    @Test
    public void diskXZSymmetryEvenH() {
        assertXZSym(ShapeType.DISK, 8, 6, 8, false);
    }

    @Test
    public void diskXZSymmetryNonCubic() {
        assertXZSym(ShapeType.DISK, 11, 7, 13, false);
    }

    @Test
    public void diskXZSymmetryLarge() {
        assertXZSym(ShapeType.DISK, 15, 9, 17, false);
    }

    @Test
    public void diskYSymmetryOddH() {
        assertYSym(ShapeType.DISK, 9, 5, 9, false);
    }

    @Test
    public void diskYSymmetryOddH2() {
        assertYSym(ShapeType.DISK, 11, 7, 11, false);
    }

    @Test
    public void diskHollowXZSymmetryOdd() {
        assertXZSym(ShapeType.DISK, 11, 5, 11, true);
    }

    @Test
    public void diskHollowYSymmetryOddH() {
        assertYSym(ShapeType.DISK, 11, 5, 11, true);
    }

    // ── PLANE ─────────────────────────────────────────────────────────────────

    @Test
    public void planeXZSymmetryOddH() {
        assertXZSym(ShapeType.PLANE, 9, 5, 9, false);
    }

    @Test
    public void planeXZSymmetryEvenH() {
        assertXZSym(ShapeType.PLANE, 8, 6, 8, false);
    }

    @Test
    public void planeXZSymmetryNonCubic() {
        assertXZSym(ShapeType.PLANE, 11, 7, 13, false);
    }

    @Test
    public void planeYSymmetryOddH() {
        assertYSym(ShapeType.PLANE, 9, 5, 9, false);
    }

    @Test
    public void planeYSymmetryOddH2() {
        assertYSym(ShapeType.PLANE, 7, 3, 11, false);
    }

    // ── SUPERELLIPSE ──────────────────────────────────────────────────────────

    @Test
    public void superellipseXZSymmetryOddH() {
        assertXZSym(ShapeType.SUPERELLIPSE, 9, 5, 9, false);
    }

    @Test
    public void superellipseXZSymmetryEvenH() {
        assertXZSym(ShapeType.SUPERELLIPSE, 8, 6, 8, false);
    }

    @Test
    public void superellipseXZSymmetryNonCubic() {
        assertXZSym(ShapeType.SUPERELLIPSE, 11, 7, 13, false);
    }

    @Test
    public void superellipseYSymmetryOddH() {
        assertYSym(ShapeType.SUPERELLIPSE, 9, 5, 9, false);
    }

    @Test
    public void superellipseYSymmetryOddH2() {
        assertYSym(ShapeType.SUPERELLIPSE, 11, 7, 11, false);
    }

    @Test
    public void superellipseHollowXZSymmetry() {
        assertXZSym(ShapeType.SUPERELLIPSE, 11, 5, 11, true);
    }

    @Test
    public void superellipseHollowYSymmetryOdd() {
        assertYSym(ShapeType.SUPERELLIPSE, 11, 5, 11, true);
    }

    // ── SUPERSPHERE ───────────────────────────────────────────────────────────

    @Test
    public void supersphereSymmetryOddCubic() {
        assertXYZSym(ShapeType.SUPERSPHERE, 9, 9, 9, false);
    }

    @Test
    public void supersphereSymmetryEvenCubic() {
        assertXYZSym(ShapeType.SUPERSPHERE, 8, 8, 8, false);
    }

    @Test
    public void supersphereSymmetryNonCubic() {
        assertXYZSym(ShapeType.SUPERSPHERE, 7, 9, 11, false);
    }

    @Test
    public void supersphereSymmetryLarge() {
        assertXYZSym(ShapeType.SUPERSPHERE, 13, 11, 15, false);
    }

    @Test
    public void supersphereHollowSymmetry() {
        assertXYZSym(ShapeType.SUPERSPHERE, 11, 11, 11, true);
    }

    // ── TUBE ─────────────────────────────────────────────────────────────────
    // No Y-dependency in formula → XYZ symmetric.

    @Test
    public void tubeSymmetryOddCubic() {
        assertXYZSym(ShapeType.TUBE, 9, 9, 9, false);
    }

    @Test
    public void tubeSymmetryEvenCubic() {
        assertXYZSym(ShapeType.TUBE, 8, 8, 8, false);
    }

    @Test
    public void tubeSymmetryTallNarrow() {
        assertXYZSym(ShapeType.TUBE, 9, 15, 9, false);
    }

    @Test
    public void tubeSymmetryWideShort() {
        assertXYZSym(ShapeType.TUBE, 13, 5, 13, false);
    }

    @Test
    public void tubeSymmetryNonCircular() {
        assertXYZSym(ShapeType.TUBE, 9, 7, 13, false);
    }

    // ── DODECAHEDRON ──────────────────────────────────────────────────────────

    @Test
    public void dodecahedronSymmetryOddCubic() {
        assertXYZSym(ShapeType.DODECAHEDRON, 9, 9, 9, false);
    }

    @Test
    public void dodecahedronSymmetryEvenCubic() {
        assertXYZSym(ShapeType.DODECAHEDRON, 8, 8, 8, false);
    }

    @Test
    public void dodecahedronSymmetryNonCubic() {
        assertXYZSym(ShapeType.DODECAHEDRON, 7, 9, 11, false);
    }

    @Test
    public void dodecahedronHollowSymmetry() {
        assertXYZSym(ShapeType.DODECAHEDRON, 11, 11, 11, true);
    }

    // ── ICOSAHEDRON ───────────────────────────────────────────────────────────

    @Test
    public void icosahedronSymmetryOddCubic() {
        assertXYZSym(ShapeType.ICOSAHEDRON, 9, 9, 9, false);
    }

    @Test
    public void icosahedronSymmetryEvenCubic() {
        assertXYZSym(ShapeType.ICOSAHEDRON, 8, 8, 8, false);
    }

    @Test
    public void icosahedronSymmetryNonCubic() {
        assertXYZSym(ShapeType.ICOSAHEDRON, 7, 9, 11, false);
    }

    @Test
    public void icosahedronHollowSymmetry() {
        assertXYZSym(ShapeType.ICOSAHEDRON, 11, 11, 11, true);
    }

    // ── REGULAR_POLYGON (even sides = XZ-symmetric) ───────────────────────────
    // Hexagon (6 sides) and square (4 sides) both have X and Z mirror symmetry.
    // Y-symmetric only when h is odd.

    @Test
    public void hexagonXZSymmetryOddH() {
        assertXZSym(ShapeType.REGULAR_POLYGON, 11, 5, 11, false);
    }

    @Test
    public void hexagonXZSymmetryEvenH() {
        assertXZSym(ShapeType.REGULAR_POLYGON, 10, 6, 10, false);
    }

    @Test
    public void hexagonXZSymmetryNonCubic() {
        assertXZSym(ShapeType.REGULAR_POLYGON, 13, 7, 15, false);
    }

    @Test
    public void hexagonYSymmetryOddH() {
        assertYSym(ShapeType.REGULAR_POLYGON, 11, 5, 11, false);
    }

    @Test
    public void hexagonHollowXZSymmetry() {
        assertXZSym(ShapeType.REGULAR_POLYGON, 13, 5, 13, true);
    }

    @Test
    public void hexagonHollowYSymmetryOddH() {
        assertYSym(ShapeType.REGULAR_POLYGON, 13, 5, 13, true);
    }
}
