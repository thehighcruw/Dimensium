/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import github.thehighcruw.dimensium.editor.handler.ExtrudeHelper;
import org.junit.Test;

/**
 * Tests pure-logic methods of ExtrudeHelper that require no Minecraft or World.
 */
public class ExtrudeHelperTest {

    // ── sideToOutwardDir ─────────────────────────────────────────────────────

    @Test
    public void sideToOutwardDirAllSixSides() {
        assertArrayEquals(new int[] {0, -1, 0}, ExtrudeHelper.sideToOutwardDir(0));
        assertArrayEquals(new int[] {0, 1, 0}, ExtrudeHelper.sideToOutwardDir(1));
        assertArrayEquals(new int[] {0, 0, -1}, ExtrudeHelper.sideToOutwardDir(2));
        assertArrayEquals(new int[] {0, 0, 1}, ExtrudeHelper.sideToOutwardDir(3));
        assertArrayEquals(new int[] {-1, 0, 0}, ExtrudeHelper.sideToOutwardDir(4));
        assertArrayEquals(new int[] {1, 0, 0}, ExtrudeHelper.sideToOutwardDir(5));
    }

    @Test(expected = RuntimeException.class)
    public void sideToOutwardDirThrowsOnInvalidSide() {
        ExtrudeHelper.sideToOutwardDir(6);
    }

    @Test
    public void sideToOutwardDirDirectionsAreUnitVectors() {
        for (int side = 0; side < 6; side++) {
            int[] d = ExtrudeHelper.sideToOutwardDir(side);
            int manLen = Math.abs(d[0]) + Math.abs(d[1]) + Math.abs(d[2]);
            assertEquals("side " + side + " must be a unit vector", 1, manLen);
        }
    }

    @Test
    public void oppositeSidesAreNegations() {
        // sides 0/1 (down/up), 2/3 (north/south), 4/5 (west/east)
        for (int i = 0; i < 3; i++) {
            int[] a = ExtrudeHelper.sideToOutwardDir(i * 2);
            int[] b = ExtrudeHelper.sideToOutwardDir(i * 2 + 1);
            assertEquals(-a[0], b[0]);
            assertEquals(-a[1], b[1]);
            assertEquals(-a[2], b[2]);
        }
    }

    // ── perpAxes ─────────────────────────────────────────────────────────────

    @Test
    public void perpAxesForYAxisReturnsXZ() {
        // dir=(0,1,0) → perp axes should be X and Z
        int[][] perp = ExtrudeHelper.perpAxes(new int[] {0, 1, 0});
        assertArrayEquals(new int[] {1, 0, 0}, perp[0]);
        assertArrayEquals(new int[] {0, 0, 1}, perp[1]);
    }

    @Test
    public void perpAxesForNegYAxisReturnsXZ() {
        int[][] perp = ExtrudeHelper.perpAxes(new int[] {0, -1, 0});
        assertArrayEquals(new int[] {1, 0, 0}, perp[0]);
        assertArrayEquals(new int[] {0, 0, 1}, perp[1]);
    }

    @Test
    public void perpAxesForZAxisReturnsXY() {
        int[][] perp = ExtrudeHelper.perpAxes(new int[] {0, 0, 1});
        assertArrayEquals(new int[] {1, 0, 0}, perp[0]);
        assertArrayEquals(new int[] {0, 1, 0}, perp[1]);
    }

    @Test
    public void perpAxesForXAxisReturnsYZ() {
        int[][] perp = ExtrudeHelper.perpAxes(new int[] {1, 0, 0});
        assertArrayEquals(new int[] {0, 1, 0}, perp[0]);
        assertArrayEquals(new int[] {0, 0, 1}, perp[1]);
    }

    @Test
    public void perpAxesArePerpToDir() {
        for (int side = 0; side < 6; side++) {
            int[] dir = ExtrudeHelper.sideToOutwardDir(side);
            int[][] perp = ExtrudeHelper.perpAxes(dir);
            int dot0 = dir[0] * perp[0][0] + dir[1] * perp[0][1] + dir[2] * perp[0][2];
            int dot1 = dir[0] * perp[1][0] + dir[1] * perp[1][1] + dir[2] * perp[1][2];
            assertEquals("perp[0] must be perpendicular to dir for side " + side, 0, dot0);
            assertEquals("perp[1] must be perpendicular to dir for side " + side, 0, dot1);
        }
    }

    @Test
    public void perpAxesTwoAxesArePerpToEachOther() {
        for (int side = 0; side < 6; side++) {
            int[] dir = ExtrudeHelper.sideToOutwardDir(side);
            int[][] perp = ExtrudeHelper.perpAxes(dir);
            int dot = perp[0][0] * perp[1][0] + perp[0][1] * perp[1][1] + perp[0][2] * perp[1][2];
            assertEquals("the two perp axes must be orthogonal for side " + side, 0, dot);
        }
    }

    // ── extrudeKey ───────────────────────────────────────────────────────────

    @Test
    public void extrudeKeyIsConsistent() {
        long k1 = ExtrudeHelper.extrudeKey(10, 64, 20);
        long k2 = ExtrudeHelper.extrudeKey(10, 64, 20);
        assertEquals(k1, k2);
    }

    @Test
    public void extrudeKeyDifferentXProducesDifferentKey() {
        assertNotEquals(ExtrudeHelper.extrudeKey(0, 64, 0), ExtrudeHelper.extrudeKey(1, 64, 0));
    }

    @Test
    public void extrudeKeyDifferentYProducesDifferentKey() {
        assertNotEquals(ExtrudeHelper.extrudeKey(0, 64, 0), ExtrudeHelper.extrudeKey(0, 65, 0));
    }

    @Test
    public void extrudeKeyDifferentZProducesDifferentKey() {
        assertNotEquals(ExtrudeHelper.extrudeKey(0, 64, 0), ExtrudeHelper.extrudeKey(0, 64, 1));
    }

    @Test
    public void extrudeKeyNegativeCoordsDoNotCollide() {
        long a = ExtrudeHelper.extrudeKey(-1, 64, 0);
        long b = ExtrudeHelper.extrudeKey(0, 64, 0);
        assertNotEquals(a, b);
    }
}
