/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.List;
import org.junit.After;
import org.junit.Test;

public class ChangeProposalPackingTest {

    @After
    public void tearDown() {
        ChangeProposal.cancel();
    }

    // ── pack / unpack round-trips ─────────────────────────────────────────────

    @Test
    public void roundTripOrigin() {
        long key = ChangeProposal.packKey(0, 0, 0);
        assertEquals(0, ChangeProposal.unpackX(key));
        assertEquals(0, ChangeProposal.unpackY(key));
        assertEquals(0, ChangeProposal.unpackZ(key));
    }

    @Test
    public void roundTripPositive() {
        long key = ChangeProposal.packKey(12345, 200, 67890);
        assertEquals(12345, ChangeProposal.unpackX(key));
        assertEquals(200, ChangeProposal.unpackY(key));
        assertEquals(67890, ChangeProposal.unpackZ(key));
    }

    @Test
    public void roundTripNegative() {
        long key = ChangeProposal.packKey(-500000, 127, -999999);
        assertEquals(-500000, ChangeProposal.unpackX(key));
        assertEquals(127, ChangeProposal.unpackY(key));
        assertEquals(-999999, ChangeProposal.unpackZ(key));
    }

    @Test
    public void roundTripMaxCoords() {
        // MC world limit ±30 000 000, y=255
        long key = ChangeProposal.packKey(29999999, 255, 29999999);
        assertEquals(29999999, ChangeProposal.unpackX(key));
        assertEquals(255, ChangeProposal.unpackY(key));
        assertEquals(29999999, ChangeProposal.unpackZ(key));
    }

    @Test
    public void roundTripNegativeExtreme() {
        long key = ChangeProposal.packKey(-29999999, 1, -29999999);
        assertEquals(-29999999, ChangeProposal.unpackX(key));
        assertEquals(1, ChangeProposal.unpackY(key));
        assertEquals(-29999999, ChangeProposal.unpackZ(key));
    }

    // ── ChangeProposal lifecycle ──────────────────────────────────────────────

    @Test
    public void flushOnNullActiveReturnsEmpty() {
        assertNull(ChangeProposal.getActiveDrag());
        List<int[]> result = ChangeProposal.flush();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void startDragCreatesInstance() {
        ChangeProposal.startDrag(null);
        assertNotNull(ChangeProposal.getActiveDrag());
    }

    @Test
    public void cancelClearsActive() {
        ChangeProposal.startDrag(null);
        ChangeProposal.cancel();
        assertNull(ChangeProposal.getActiveDrag());
    }

    @Test
    public void flushClearsActiveAndReturnsPending() {
        ChangeProposal.startDrag(null);
        // Manually insert an entry to bypass World dependency
        ChangeProposal.getActiveDrag().proposed.put(ChangeProposal.packKey(10, 64, 20), new int[] {1, 0});

        List<int[]> ops = ChangeProposal.flush();

        assertNull(ChangeProposal.getActiveDrag());
        assertEquals(1, ops.size());
        int[] op = ops.get(0);
        assertEquals(10, op[0]);
        assertEquals(64, op[1]);
        assertEquals(20, op[2]);
        assertEquals(1, op[3]);
        assertEquals(0, op[4]);
    }

    @Test
    public void laterWriteToSamePositionOverwritesEarlier() {
        ChangeProposal.startDrag(null);
        long key = ChangeProposal.packKey(5, 70, 5);
        ChangeProposal.getActiveDrag().proposed.put(key, new int[] {1, 0});
        ChangeProposal.getActiveDrag().proposed.put(key, new int[] {4, 2});

        assertEquals(1, ChangeProposal.getActiveDrag().proposed.size());
        assertArrayEquals(
                new int[] {4, 2}, ChangeProposal.getActiveDrag().proposed.get(key));
    }
}
