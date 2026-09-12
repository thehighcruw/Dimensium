package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import org.junit.Test;

import github.thehighcruw.dimensium.tool.state.SelectionState;

/**
 * Tests SelectionState coordinate packing in isolation (no Minecraft deps).
 * Also tests clipboardKey encoding used by SelectionOps.clipboardToPlacements.
 */
public class SelectionStatePackingTest {

    // ── pack / unpack round-trips ─────────────────────────────────────────────

    @Test
    public void roundTripOrigin() {
        long key = SelectionState.pack(0, 0, 0);
        assertEquals(0, SelectionState.unpackX(key));
        assertEquals(0, SelectionState.unpackY(key));
        assertEquals(0, SelectionState.unpackZ(key));
    }

    @Test
    public void roundTripArbitrary() {
        long key = SelectionState.pack(1024, 128, 2048);
        assertEquals(1024, SelectionState.unpackX(key));
        assertEquals(128, SelectionState.unpackY(key));
        assertEquals(2048, SelectionState.unpackZ(key));
    }

    @Test
    public void roundTripNegativeCoords() {
        long key = SelectionState.pack(-8000000, 64, -15000000);
        assertEquals(-8000000, SelectionState.unpackX(key));
        assertEquals(64, SelectionState.unpackY(key));
        assertEquals(-15000000, SelectionState.unpackZ(key));
    }

    @Test
    public void roundTripMaxY() {
        long key = SelectionState.pack(0, 255, 0);
        assertEquals(255, SelectionState.unpackY(key));
    }

    @Test
    public void roundTripMaxCoords() {
        long key = SelectionState.pack(29_999_999, 255, 29_999_999);
        assertEquals(29_999_999, SelectionState.unpackX(key));
        assertEquals(255, SelectionState.unpackY(key));
        assertEquals(29_999_999, SelectionState.unpackZ(key));
    }

    @Test
    public void roundTripNegativeExtreme() {
        long key = SelectionState.pack(-29_999_999, 0, -29_999_999);
        assertEquals(-29_999_999, SelectionState.unpackX(key));
        assertEquals(0, SelectionState.unpackY(key));
        assertEquals(-29_999_999, SelectionState.unpackZ(key));
    }

    // ── distinctness: different positions produce different keys ──────────────

    @Test
    public void distinctXProducesDifferentKeys() {
        assertNotEquals(SelectionState.pack(0, 64, 0), SelectionState.pack(1, 64, 0));
    }

    @Test
    public void distinctYProducesDifferentKeys() {
        assertNotEquals(SelectionState.pack(0, 64, 0), SelectionState.pack(0, 65, 0));
    }

    @Test
    public void distinctZProducesDifferentKeys() {
        assertNotEquals(SelectionState.pack(0, 64, 0), SelectionState.pack(0, 64, 1));
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
