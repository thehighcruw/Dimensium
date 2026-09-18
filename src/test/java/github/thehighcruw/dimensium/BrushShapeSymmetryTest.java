/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushShape;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import org.junit.Test;

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

    private static void assertXSym(BrushShape shape, Vec3DInt brushSize) {
        String tag = shape + " " + brushSize;
        Vec3DInt.forEachInclusive(brushSize.negate(), brushSize, offset -> {
            boolean a = BrushUtil.inShape(shape, offset, brushSize);
            boolean b = BrushUtil.inShape(shape, Vec3DInt.from(-offset.x(), offset.y(), offset.z()), brushSize);
            if (a != b) fail(tag + " X-sym broken at " + offset);
        });
    }

    private static void assertYSym(BrushShape shape, Vec3DInt brushSize) {
        String tag = shape + " " + brushSize;
        Vec3DInt.forEachInclusive(brushSize.negate(), brushSize, offset -> {
            boolean a = BrushUtil.inShape(shape, offset, brushSize);
            boolean b = BrushUtil.inShape(shape, Vec3DInt.from(offset.x(), -offset.y(), offset.z()), brushSize);
            if (a != b) fail(tag + " Y-sym broken at " + offset);
        });
    }

    private static void assertZSym(BrushShape shape, Vec3DInt brushSize) {
        String tag = shape + " " + brushSize;
        Vec3DInt.forEachInclusive(brushSize.negate(), brushSize, offset -> {
            boolean a = BrushUtil.inShape(shape, offset, brushSize);
            boolean b = BrushUtil.inShape(shape, Vec3DInt.from(offset.x(), offset.y(), -offset.z()), brushSize);
            if (a != b) fail(tag + " Z-sym broken at " + offset);
        });
    }

    private static void assertXYZSym(BrushShape shape, Vec3DInt brushSize) {
        assertXSym(shape, brushSize);
        assertYSym(shape, brushSize);
        assertZSym(shape, brushSize);
    }

    private static void assertXZSym(BrushShape shape, Vec3DInt brushSize) {
        assertXSym(shape, brushSize);
        assertZSym(shape, brushSize);
    }

    // ── SPHERE ───────────────────────────────────────────────────────────────

    @Test
    public void sphereSymSmall() {
        assertXYZSym(BrushShape.SPHERE, Vec3DInt.from(3, 3, 3));
    }

    @Test
    public void sphereSymMedium() {
        assertXYZSym(BrushShape.SPHERE, Vec3DInt.from(5, 5, 5));
    }

    @Test
    public void sphereSymLarge() {
        assertXYZSym(BrushShape.SPHERE, Vec3DInt.from(8, 8, 8));
    }

    @Test
    public void sphereSymNonCubic() {
        assertXYZSym(BrushShape.SPHERE, Vec3DInt.from(3, 5, 7));
    }

    @Test
    public void sphereSymEven() {
        assertXYZSym(BrushShape.SPHERE, Vec3DInt.from(4, 4, 4));
    }

    // ── ELLIPSOID ────────────────────────────────────────────────────────────

    @Test
    public void ellipsoidSymCubic() {
        assertXYZSym(BrushShape.ELLIPSOID, Vec3DInt.from(4, 4, 4));
    }

    @Test
    public void ellipsoidSymNonCubic() {
        assertXYZSym(BrushShape.ELLIPSOID, Vec3DInt.from(3, 6, 4));
    }

    @Test
    public void ellipsoidSymLarge() {
        assertXYZSym(BrushShape.ELLIPSOID, Vec3DInt.from(8, 5, 7));
    }

    // ── CUBE / CUBOID ─────────────────────────────────────────────────────────

    @Test
    public void cubeSymCubic() {
        assertXYZSym(BrushShape.CUBE, Vec3DInt.from(4, 4, 4));
    }

    @Test
    public void cuboidSymNonCubic() {
        assertXYZSym(BrushShape.CUBOID, Vec3DInt.from(3, 5, 7));
    }

    // ── CYLINDER ─────────────────────────────────────────────────────────────

    @Test
    public void cylinderSymCubic() {
        assertXYZSym(BrushShape.CYLINDER, Vec3DInt.from(5, 5, 5));
    }

    @Test
    public void cylinderSymTallNarrow() {
        assertXYZSym(BrushShape.CYLINDER, Vec3DInt.from(3, 8, 3));
    }

    @Test
    public void cylinderSymWideShort() {
        assertXYZSym(BrushShape.CYLINDER, Vec3DInt.from(7, 2, 7));
    }

    @Test
    public void cylinderSymEven() {
        assertXYZSym(BrushShape.CYLINDER, Vec3DInt.from(4, 6, 4));
    }

    // ── CAPSULE ───────────────────────────────────────────────────────────────

    @Test
    public void capsuleSymCubic() {
        assertXYZSym(BrushShape.CAPSULE, Vec3DInt.from(4, 4, 4));
    }

    @Test
    public void capsuleSymTall() {
        assertXYZSym(BrushShape.CAPSULE, Vec3DInt.from(3, 7, 3));
    }

    @Test
    public void capsuleSymShort() {
        assertXYZSym(BrushShape.CAPSULE, Vec3DInt.from(5, 2, 5));
    }

    @Test
    public void capsuleSymNonCubic() {
        assertXYZSym(BrushShape.CAPSULE, Vec3DInt.from(3, 6, 3));
    }

    // ── CONE ──────────────────────────────────────────────────────────────────
    // Tapers from base (dy=-sy) to apex (dy=+sy): Y-symmetry intentionally breaks.

    @Test
    public void coneXZSymCubic() {
        assertXZSym(BrushShape.CONE, Vec3DInt.from(5, 5, 5));
    }

    @Test
    public void coneXZSymTall() {
        assertXZSym(BrushShape.CONE, Vec3DInt.from(3, 8, 3));
    }

    @Test
    public void coneXZSymWide() {
        assertXZSym(BrushShape.CONE, Vec3DInt.from(7, 4, 7));
    }

    @Test
    public void coneXZSymEven() {
        assertXZSym(BrushShape.CONE, Vec3DInt.from(4, 6, 4));
    }

    // ── OCTAHEDRON ────────────────────────────────────────────────────────────

    @Test
    public void octahedronSymCubic() {
        assertXYZSym(BrushShape.OCTAHEDRON, Vec3DInt.from(5, 5, 5));
    }

    @Test
    public void octahedronSymNonCubic() {
        assertXYZSym(BrushShape.OCTAHEDRON, Vec3DInt.from(3, 5, 7));
    }

    @Test
    public void octahedronSymLarge() {
        assertXYZSym(BrushShape.OCTAHEDRON, Vec3DInt.from(8, 6, 8));
    }

    @Test
    public void octahedronSymEven() {
        assertXYZSym(BrushShape.OCTAHEDRON, Vec3DInt.from(4, 4, 4));
    }
}
