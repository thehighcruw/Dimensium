/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import org.junit.Test;

import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;

/**
 * Tests SelectionState coordinate packing in isolation (no Minecraft deps).
 * Also tests clipboardKey encoding used by SelectionOps.clipboardToPlacements.
 */
public class SelectionStatePackingTest {

    // ── pack / unpack round-trips ─────────────────────────────────────────────

    @Test
    public void roundTripOrigin() {
        long key = SelectionState.pack(Vec3DInt.from(0, 0, 0));
        Vec3DInt v = SelectionState.unpack(key);
        assertEquals(0, v.x());
        assertEquals(0, v.y());
        assertEquals(0, v.z());
    }

    @Test
    public void roundTripArbitrary() {
        long key = SelectionState.pack(Vec3DInt.from(1024, 128, 2048));
        Vec3DInt v = SelectionState.unpack(key);
        assertEquals(1024, v.x());
        assertEquals(128, v.y());
        assertEquals(2048, v.z());
    }

    @Test
    public void roundTripNegativeCoords() {
        long key = SelectionState.pack(Vec3DInt.from(-8000000, 64, -15000000));
        Vec3DInt v = SelectionState.unpack(key);
        assertEquals(-8000000, v.x());
        assertEquals(64, v.y());
        assertEquals(-15000000, v.z());
    }

    @Test
    public void roundTripMaxY() {
        long key = SelectionState.pack(Vec3DInt.from(0, 255, 0));
        assertEquals(
            255,
            SelectionState.unpack(key)
                .y());
    }

    @Test
    public void roundTripMaxCoords() {
        long key = SelectionState.pack(Vec3DInt.from(29_999_999, 255, 29_999_999));
        Vec3DInt v = SelectionState.unpack(key);
        assertEquals(29_999_999, v.x());
        assertEquals(255, v.y());
        assertEquals(29_999_999, v.z());
    }

    @Test
    public void roundTripNegativeExtreme() {
        long key = SelectionState.pack(Vec3DInt.from(-29_999_999, 0, -29_999_999));
        Vec3DInt v = SelectionState.unpack(key);
        assertEquals(-29_999_999, v.x());
        assertEquals(0, v.y());
        assertEquals(-29_999_999, v.z());
    }

    // ── distinctness: different positions produce different keys ──────────────

    @Test
    public void distinctXProducesDifferentKeys() {
        assertNotEquals(SelectionState.pack(Vec3DInt.from(0, 64, 0)), SelectionState.pack(Vec3DInt.from(1, 64, 0)));
    }

    @Test
    public void distinctYProducesDifferentKeys() {
        assertNotEquals(SelectionState.pack(Vec3DInt.from(0, 64, 0)), SelectionState.pack(Vec3DInt.from(0, 65, 0)));
    }

    @Test
    public void distinctZProducesDifferentKeys() {
        assertNotEquals(SelectionState.pack(Vec3DInt.from(0, 64, 0)), SelectionState.pack(Vec3DInt.from(0, 64, 1)));
    }

    // ── clipboardKey encoding ─────────────────────────────────────────────────

    @Test
    public void clipboardKeyRoundTripOrigin() {
        long key = SelectionState.clipboardKey(0, 0, 0);
        // local coords packed as: x<<20 | y<<10 | z
        int lx = (int) (key >> 20) & 0xFFFFF;
        int ly = (int) (key >> 10) & 0x3FF;
        int lz = (int) key & 0x3FF;
        assertEquals(0, lx);
        assertEquals(0, ly);
        assertEquals(0, lz);
    }

    @Test
    public void clipboardKeyRoundTripMid() {
        long key = SelectionState.clipboardKey(100, 63, 200);
        int lx = (int) (key >> 20) & 0xFFFFF;
        int ly = (int) (key >> 10) & 0x3FF;
        int lz = (int) key & 0x3FF;
        assertEquals(100, lx);
        assertEquals(63, ly);
        assertEquals(200, lz);
    }

    @Test
    public void clipboardKeyDistinctForDifferentPositions() {
        assertNotEquals(SelectionState.clipboardKey(1, 0, 0), SelectionState.clipboardKey(0, 1, 0));
        assertNotEquals(SelectionState.clipboardKey(0, 1, 0), SelectionState.clipboardKey(0, 0, 1));
    }
}
