/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import github.thehighcruw.dimensium.editor.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import org.junit.Test;

/**
 * Tests pure-logic methods of ExtrudeHelper that require no Minecraft or World.
 */
public class ExtrudeHelperTest {

    // ── sideToOutwardDir ─────────────────────────────────────────────────────

    @Test
    public void sideToOutwardDirAllSixSides() {
        assertEquals(Vec3DInt.from(0, -1, 0), ExtrudeHelper.sideToOutwardDir(0));
        assertEquals(Vec3DInt.from(0, 1, 0), ExtrudeHelper.sideToOutwardDir(1));
        assertEquals(Vec3DInt.from(0, 0, -1), ExtrudeHelper.sideToOutwardDir(2));
        assertEquals(Vec3DInt.from(0, 0, 1), ExtrudeHelper.sideToOutwardDir(3));
        assertEquals(Vec3DInt.from(-1, 0, 0), ExtrudeHelper.sideToOutwardDir(4));
        assertEquals(Vec3DInt.from(1, 0, 0), ExtrudeHelper.sideToOutwardDir(5));
    }

    @Test(expected = RuntimeException.class)
    public void sideToOutwardDirThrowsOnInvalidSide() {
        ExtrudeHelper.sideToOutwardDir(6);
    }

    @Test
    public void sideToOutwardDirDirectionsAreUnitVectors() {
        for (int side = 0; side < 6; side++) {
            Vec3DInt d = ExtrudeHelper.sideToOutwardDir(side);
            int manLen = Math.abs(d.x()) + Math.abs(d.y()) + Math.abs(d.z());
            assertEquals("side " + side + " must be a unit vector", 1, manLen);
        }
    }

    @Test
    public void oppositeSidesAreNegations() {
        for (int i = 0; i < 3; i++) {
            Vec3DInt a = ExtrudeHelper.sideToOutwardDir(i * 2);
            Vec3DInt b = ExtrudeHelper.sideToOutwardDir(i * 2 + 1);
            assertEquals(-a.x(), b.x());
            assertEquals(-a.y(), b.y());
            assertEquals(-a.z(), b.z());
        }
    }

    // ── perpAxes ─────────────────────────────────────────────────────────────

    @Test
    public void perpAxesForYAxisReturnsXZ() {
        Vec3DInt[] perp = ExtrudeHelper.perpAxes(Vec3DInt.from(0, 1, 0));
        assertEquals(Vec3DInt.from(1, 0, 0), perp[0]);
        assertEquals(Vec3DInt.from(0, 0, 1), perp[1]);
    }

    @Test
    public void perpAxesForNegYAxisReturnsXZ() {
        Vec3DInt[] perp = ExtrudeHelper.perpAxes(Vec3DInt.from(0, -1, 0));
        assertEquals(Vec3DInt.from(1, 0, 0), perp[0]);
        assertEquals(Vec3DInt.from(0, 0, 1), perp[1]);
    }

    @Test
    public void perpAxesForZAxisReturnsXY() {
        Vec3DInt[] perp = ExtrudeHelper.perpAxes(Vec3DInt.from(0, 0, 1));
        assertEquals(Vec3DInt.from(1, 0, 0), perp[0]);
        assertEquals(Vec3DInt.from(0, 1, 0), perp[1]);
    }

    @Test
    public void perpAxesForXAxisReturnsYZ() {
        Vec3DInt[] perp = ExtrudeHelper.perpAxes(Vec3DInt.from(1, 0, 0));
        assertEquals(Vec3DInt.from(0, 1, 0), perp[0]);
        assertEquals(Vec3DInt.from(0, 0, 1), perp[1]);
    }

    @Test
    public void perpAxesArePerpToDir() {
        for (int side = 0; side < 6; side++) {
            Vec3DInt dir = ExtrudeHelper.sideToOutwardDir(side);
            Vec3DInt[] perp = ExtrudeHelper.perpAxes(dir);
            int dot0 = dir.x() * perp[0].x() + dir.y() * perp[0].y() + dir.z() * perp[0].z();
            int dot1 = dir.x() * perp[1].x() + dir.y() * perp[1].y() + dir.z() * perp[1].z();
            assertEquals("perp[0] must be perpendicular to dir for side " + side, 0, dot0);
            assertEquals("perp[1] must be perpendicular to dir for side " + side, 0, dot1);
        }
    }

    @Test
    public void perpAxesTwoAxesArePerpToEachOther() {
        for (int side = 0; side < 6; side++) {
            Vec3DInt dir = ExtrudeHelper.sideToOutwardDir(side);
            Vec3DInt[] perp = ExtrudeHelper.perpAxes(dir);
            int dot = perp[0].x() * perp[1].x() + perp[0].y() * perp[1].y() + perp[0].z() * perp[1].z();
            assertEquals("the two perp axes must be orthogonal for side " + side, 0, dot);
        }
    }

    // ── extrudeKey ───────────────────────────────────────────────────────────

    @Test
    public void extrudeKeyIsConsistent() {
        long k1 = ExtrudeHelper.extrudeKey(Vec3DInt.from(10, 64, 20));
        long k2 = ExtrudeHelper.extrudeKey(Vec3DInt.from(10, 64, 20));
        assertEquals(k1, k2);
    }

    @Test
    public void extrudeKeyDifferentXProducesDifferentKey() {
        assertNotEquals(
                ExtrudeHelper.extrudeKey(Vec3DInt.from(0, 64, 0)), ExtrudeHelper.extrudeKey(Vec3DInt.from(1, 64, 0)));
    }

    @Test
    public void extrudeKeyDifferentYProducesDifferentKey() {
        assertNotEquals(
                ExtrudeHelper.extrudeKey(Vec3DInt.from(0, 64, 0)), ExtrudeHelper.extrudeKey(Vec3DInt.from(0, 65, 0)));
    }

    @Test
    public void extrudeKeyDifferentZProducesDifferentKey() {
        assertNotEquals(
                ExtrudeHelper.extrudeKey(Vec3DInt.from(0, 64, 0)), ExtrudeHelper.extrudeKey(Vec3DInt.from(0, 64, 1)));
    }

    @Test
    public void extrudeKeyNegativeCoordsDoNotCollide() {
        long a = ExtrudeHelper.extrudeKey(Vec3DInt.from(-1, 64, 0));
        long b = ExtrudeHelper.extrudeKey(Vec3DInt.from(0, 64, 0));
        assertNotEquals(a, b);
    }
}
