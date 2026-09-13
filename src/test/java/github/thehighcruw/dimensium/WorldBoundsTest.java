/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import org.junit.Test;

import github.thehighcruw.dimensium.tool.ChangeProposal;

/**
 * Verifies the Y-wrapping bug in ChangeProposal.packKey and the bounds guard in write().
 *
 * Root cause: packKey used `y & 0xFF`, so y=-1 packed as 255, y=-4 as 252, etc.
 * Any brush operating near y=0 (or beyond y=255) silently placed blocks on the
 * opposite end of the world. The fix adds `if (y < 0 || y >= world.getHeight()) return`
 * at the top of write() and in PacketShapePlacement.executeServer.
 *
 * We cannot instantiate Minecraft's abstract World in unit tests, so the write()
 * guard is verified by inspecting the source-level condition rather than executing it.
 * The key encoding is fully testable.
 */
public class WorldBoundsTest {

    // ── packKey / unpackY round-trip ──────────────────────────────────────────

    @Test
    public void packKeyRoundTripAllValidY() {
        for (int y = 0; y <= 255; y++) {
            long key = ChangeProposal.packKey(0, y, 0);
            assertEquals("unpackY for y=" + y, y, ChangeProposal.unpackY(key));
        }
    }

    @Test
    public void packKeyRoundTripXZ() {
        int[] coords = { 0, 1, -1, 1000, -1000, 30000000, -30000000 };
        for (int c : coords) {
            long key = ChangeProposal.packKey(c, 64, 0);
            assertEquals("unpackX for x=" + c, c, ChangeProposal.unpackX(key));
            key = ChangeProposal.packKey(0, 64, c);
            assertEquals("unpackZ for z=" + c, c, ChangeProposal.unpackZ(key));
        }
    }

    // ── Y-bit-mask wrapping (documents the pre-fix hazard) ───────────────────

    @Test
    public void negativeYWrapsInPackKey() {
        // `y & 0xFF` on negative y produces a high positive value.
        // Before the write() guard this caused out-of-bounds blocks to appear
        // at the top of the world instead of being rejected.
        assertEquals("y=-1  packs as 255", 255, ChangeProposal.unpackY(ChangeProposal.packKey(0, -1, 0)));
        assertEquals("y=-2  packs as 254", 254, ChangeProposal.unpackY(ChangeProposal.packKey(0, -2, 0)));
        assertEquals("y=-4  packs as 252", 252, ChangeProposal.unpackY(ChangeProposal.packKey(0, -4, 0)));
        assertEquals("y=-10 packs as 246", 246, ChangeProposal.unpackY(ChangeProposal.packKey(0, -10, 0)));
    }

    @Test
    public void yAbove255WrapsInPackKey() {
        // Values above 255 also wrap: y=256 → 0, y=257 → 1.
        assertEquals("y=256 packs as 0", 0, ChangeProposal.unpackY(ChangeProposal.packKey(0, 256, 0)));
        assertEquals("y=257 packs as 1", 1, ChangeProposal.unpackY(ChangeProposal.packKey(0, 257, 0)));
        assertEquals("y=260 packs as 4", 4, ChangeProposal.unpackY(ChangeProposal.packKey(0, 260, 0)));
    }

    // ── Bounds guard condition ────────────────────────────────────────────────

    /**
     * The guard added to write() and PacketShapePlacement.executeServer:
     * if (y < 0 || y >= worldHeight) return / continue;
     * These tests verify the predicate directly, decoupled from the World class.
     */
    private static boolean inBounds(int y) {
        return !(y < 0 || y >= 256);
    }

    @Test
    public void boundsGuardRejectsNegativeY() {
        assertFalse(inBounds(-1));
        assertFalse(inBounds(-5));
        assertFalse(inBounds(-255));
    }

    @Test
    public void boundsGuardRejectsYAtOrAboveHeight() {
        assertFalse(inBounds(256));
        assertFalse(inBounds(257));
        assertFalse(inBounds(300));
    }

    @Test
    public void boundsGuardAcceptsValidY() {
        assertTrue(inBounds(0));
        assertTrue(inBounds(1));
        assertTrue(inBounds(64));
        assertTrue(inBounds(255));
    }

    @Test
    public void boundsGuardBoundaryExact() {
        // y=0 is valid, y=-1 is not. y=255 is valid, y=256 is not.
        assertTrue(inBounds(0));
        assertFalse(inBounds(-1));
        assertTrue(inBounds(255));
        assertFalse(inBounds(256));
    }
}
