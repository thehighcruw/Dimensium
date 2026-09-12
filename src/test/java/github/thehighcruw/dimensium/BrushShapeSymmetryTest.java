package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import org.junit.Test;

import github.thehighcruw.dimensium.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.tool.state.BrushShape;

/**
 * Symmetry tests for BrushUtil.inShape (used by all brush-based tools).
 *
 * Symmetry definitions (about origin, since brush iterates -s..s):
 * X: inShape(dx, dy, dz) == inShape(-dx, dy, dz)
 * Y: inShape(dx, dy, dz) == inShape(dx, -dy, dz)
 * Z: inShape(dx, dy, dz) == inShape(dx, dy, -dz)
 *
 * Expected per shape:
 * XYZ — SPHERE, ELLIPSOID, CUBE, CUBOID, CYLINDER, CAPSULE, OCTAHEDRON
 * XZ — CONE (tapers along Y)
 */
public class BrushShapeSymmetryTest {

    private static boolean s(BrushShape shape, int dx, int dy, int dz, int sx, int sy, int sz) {
        return BrushUtil.inShape(shape, dx, dy, dz, sx, sy, sz);
    }

    private static void assertXSym(BrushShape shape, int sx, int sy, int sz) {
        String tag = shape + " sx=" + sx + " sy=" + sy + " sz=" + sz;
        for (int dx = -sx; dx <= sx; dx++) for (int dy = -sy; dy <= sy; dy++) for (int dz = -sz; dz <= sz; dz++) {
            boolean a = s(shape, dx, dy, dz, sx, sy, sz);
            boolean b = s(shape, -dx, dy, dz, sx, sy, sz);
            if (a != b) fail(tag + " X-sym broken at (" + dx + "," + dy + "," + dz + ")");
        }
    }

    private static void assertYSym(BrushShape shape, int sx, int sy, int sz) {
        String tag = shape + " sx=" + sx + " sy=" + sy + " sz=" + sz;
        for (int dx = -sx; dx <= sx; dx++) for (int dy = -sy; dy <= sy; dy++) for (int dz = -sz; dz <= sz; dz++) {
            boolean a = s(shape, dx, dy, dz, sx, sy, sz);
            boolean b = s(shape, dx, -dy, dz, sx, sy, sz);
            if (a != b) fail(tag + " Y-sym broken at (" + dx + "," + dy + "," + dz + ")");
        }
    }

    private static void assertZSym(BrushShape shape, int sx, int sy, int sz) {
        String tag = shape + " sx=" + sx + " sy=" + sy + " sz=" + sz;
        for (int dx = -sx; dx <= sx; dx++) for (int dy = -sy; dy <= sy; dy++) for (int dz = -sz; dz <= sz; dz++) {
            boolean a = s(shape, dx, dy, dz, sx, sy, sz);
            boolean b = s(shape, dx, dy, -dz, sx, sy, sz);
            if (a != b) fail(tag + " Z-sym broken at (" + dx + "," + dy + "," + dz + ")");
        }
    }

    private static void assertXYZSym(BrushShape shape, int sx, int sy, int sz) {
        assertXSym(shape, sx, sy, sz);
        assertYSym(shape, sx, sy, sz);
        assertZSym(shape, sx, sy, sz);
    }

    private static void assertXZSym(BrushShape shape, int sx, int sy, int sz) {
        assertXSym(shape, sx, sy, sz);
        assertZSym(shape, sx, sy, sz);
    }

    // ── SPHERE ───────────────────────────────────────────────────────────────

    @Test
    public void sphereSymSmall() {
        assertXYZSym(BrushShape.SPHERE, 3, 3, 3);
    }

    @Test
    public void sphereSymMedium() {
        assertXYZSym(BrushShape.SPHERE, 5, 5, 5);
    }

    @Test
    public void sphereSymLarge() {
        assertXYZSym(BrushShape.SPHERE, 8, 8, 8);
    }

    @Test
    public void sphereSymNonCubic() {
        assertXYZSym(BrushShape.SPHERE, 3, 5, 7);
    }

    @Test
    public void sphereSymEven() {
        assertXYZSym(BrushShape.SPHERE, 4, 4, 4);
    }

    // ── ELLIPSOID ────────────────────────────────────────────────────────────

    @Test
    public void ellipsoidSymCubic() {
        assertXYZSym(BrushShape.ELLIPSOID, 4, 4, 4);
    }

    @Test
    public void ellipsoidSymNonCubic() {
        assertXYZSym(BrushShape.ELLIPSOID, 3, 6, 4);
    }

    @Test
    public void ellipsoidSymLarge() {
        assertXYZSym(BrushShape.ELLIPSOID, 8, 5, 7);
    }

    // ── CUBE / CUBOID ─────────────────────────────────────────────────────────

    @Test
    public void cubeSymCubic() {
        assertXYZSym(BrushShape.CUBE, 4, 4, 4);
    }

    @Test
    public void cuboidSymNonCubic() {
        assertXYZSym(BrushShape.CUBOID, 3, 5, 7);
    }

    // ── CYLINDER ─────────────────────────────────────────────────────────────

    @Test
    public void cylinderSymCubic() {
        assertXYZSym(BrushShape.CYLINDER, 5, 5, 5);
    }

    @Test
    public void cylinderSymTallNarrow() {
        assertXYZSym(BrushShape.CYLINDER, 3, 8, 3);
    }

    @Test
    public void cylinderSymWideShort() {
        assertXYZSym(BrushShape.CYLINDER, 7, 2, 7);
    }

    @Test
    public void cylinderSymEven() {
        assertXYZSym(BrushShape.CYLINDER, 4, 6, 4);
    }

    // ── CAPSULE ───────────────────────────────────────────────────────────────

    @Test
    public void capsuleSymCubic() {
        assertXYZSym(BrushShape.CAPSULE, 4, 4, 4);
    }

    @Test
    public void capsuleSymTall() {
        assertXYZSym(BrushShape.CAPSULE, 3, 7, 3);
    }

    @Test
    public void capsuleSymShort() {
        assertXYZSym(BrushShape.CAPSULE, 5, 2, 5);
    }

    @Test
    public void capsuleSymNonCubic() {
        assertXYZSym(BrushShape.CAPSULE, 3, 6, 3);
    }

    // ── CONE ──────────────────────────────────────────────────────────────────
    // Tapers from base (dy=-sy) to apex (dy=+sy): Y-symmetry intentionally breaks.

    @Test
    public void coneXZSymCubic() {
        assertXZSym(BrushShape.CONE, 5, 5, 5);
    }

    @Test
    public void coneXZSymTall() {
        assertXZSym(BrushShape.CONE, 3, 8, 3);
    }

    @Test
    public void coneXZSymWide() {
        assertXZSym(BrushShape.CONE, 7, 4, 7);
    }

    @Test
    public void coneXZSymEven() {
        assertXZSym(BrushShape.CONE, 4, 6, 4);
    }

    // ── OCTAHEDRON ────────────────────────────────────────────────────────────

    @Test
    public void octahedronSymCubic() {
        assertXYZSym(BrushShape.OCTAHEDRON, 5, 5, 5);
    }

    @Test
    public void octahedronSymNonCubic() {
        assertXYZSym(BrushShape.OCTAHEDRON, 3, 5, 7);
    }

    @Test
    public void octahedronSymLarge() {
        assertXYZSym(BrushShape.OCTAHEDRON, 8, 6, 8);
    }

    @Test
    public void octahedronSymEven() {
        assertXYZSym(BrushShape.OCTAHEDRON, 4, 4, 4);
    }
}
