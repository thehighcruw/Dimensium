/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import github.thehighcruw.dimensium.editor.handler.SelectionOps;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests SelectionOps methods that do not require a live World.
 * hollowOps and selectionToAirOps are pure geometry over SelectionState.
 */
public class SelectionOpsTest {

    private SelectionState sel;

    @Before
    public void setUp() {
        sel = new SelectionState();
    }

    private void addBlocks(int[][] coords) {
        Set<Long> set = new HashSet<>();
        for (int[] c : coords) set.add(SelectionState.pack(Vec3DInt.from(c[0], c[1], c[2])));
        sel.applyOp(set, BooleanOp.REPLACE);
    }

    // ── selectionToAirOps ────────────────────────────────────────────────────

    @Test
    public void selectionToAirOpsEmptySelectionReturnsEmpty() {
        assertTrue(SelectionOps.selectionToAirOps(sel).isEmpty());
    }

    @Test
    public void selectionToAirOpsProducesOneOpPerBlock() {
        addBlocks(new int[][] {{1, 64, 1}, {2, 64, 2}, {3, 64, 3}});
        List<int[]> ops = SelectionOps.selectionToAirOps(sel);
        assertEquals(3, ops.size());
    }

    @Test
    public void selectionToAirOpsSetsBlockIdAndMetaToZero() {
        addBlocks(new int[][] {{5, 64, 5}});
        List<int[]> ops = SelectionOps.selectionToAirOps(sel);
        assertEquals(1, ops.size());
        int[] op = ops.get(0);
        assertEquals(5, op[0]);
        assertEquals(64, op[1]);
        assertEquals(5, op[2]);
        assertEquals("block id must be 0 (air)", 0, op[3]);
        assertEquals("meta must be 0", 0, op[4]);
    }

    // ── hollowOps ────────────────────────────────────────────────────────────

    @Test
    public void hollowOpsSingleBlockHasNoInterior() {
        addBlocks(new int[][] {{5, 64, 5}});
        // Single block is always a shell — hollowOps should produce no ops
        assertTrue(SelectionOps.hollowOps(sel).isEmpty());
    }

    @Test
    public void hollowOpsLineOfBlocksHasNoInterior() {
        // A 1×1×N line: every block touches air on at least one face
        addBlocks(new int[][] {{0, 64, 0}, {1, 64, 0}, {2, 64, 0}, {3, 64, 0}});
        assertTrue(
                "a 1-block-thick line has no interior",
                SelectionOps.hollowOps(sel).isEmpty());
    }

    @Test
    public void hollowOps3x3x3CubeHasOneInteriorBlock() {
        // 3×3×3 cube: only the centre block (1,65,1) is fully surrounded
        Set<Long> blocks = new HashSet<>();
        for (int x = 0; x < 3; x++)
            for (int y = 64; y < 67; y++)
                for (int z = 0; z < 3; z++) blocks.add(SelectionState.pack(Vec3DInt.from(x, y, z)));
        sel.applyOp(blocks, BooleanOp.REPLACE);

        List<int[]> ops = SelectionOps.hollowOps(sel);
        assertEquals(1, ops.size());
        int[] op = ops.get(0);
        assertEquals(1, op[0]);
        assertEquals(65, op[1]);
        assertEquals(1, op[2]);
        assertEquals(0, op[3]);
        assertEquals(0, op[4]);
    }

    @Test
    public void hollowOps5x5x5CubeCorrectInteriorCount() {
        // 5×5×5 = 125 total, interior 3×3×3 = 27 blocks
        Set<Long> blocks = new HashSet<>();
        for (int x = 0; x < 5; x++)
            for (int y = 64; y < 69; y++)
                for (int z = 0; z < 5; z++) blocks.add(SelectionState.pack(Vec3DInt.from(x, y, z)));
        sel.applyOp(blocks, BooleanOp.REPLACE);

        List<int[]> ops = SelectionOps.hollowOps(sel);
        assertEquals("interior of 5x5x5 cube is 3x3x3 = 27 blocks", 27, ops.size());
    }

    @Test
    public void hollowOpsOnlyAirsInterior() {
        // All hollow ops must have blockId=0 meta=0
        Set<Long> blocks = new HashSet<>();
        for (int x = 0; x < 5; x++)
            for (int y = 64; y < 69; y++)
                for (int z = 0; z < 5; z++) blocks.add(SelectionState.pack(Vec3DInt.from(x, y, z)));
        sel.applyOp(blocks, BooleanOp.REPLACE);

        for (int[] op : SelectionOps.hollowOps(sel)) {
            assertEquals("hollow op must set block to air", 0, op[3]);
            assertEquals("hollow op meta must be 0", 0, op[4]);
        }
    }

    @Test
    public void hollowOpsInteriorBlocksMustBeInsideSelection() {
        Set<Long> blocks = new HashSet<>();
        for (int x = 0; x < 5; x++)
            for (int y = 64; y < 69; y++)
                for (int z = 0; z < 5; z++) blocks.add(SelectionState.pack(Vec3DInt.from(x, y, z)));
        sel.applyOp(blocks, BooleanOp.REPLACE);

        for (int[] op : SelectionOps.hollowOps(sel)) {
            long key = SelectionState.pack(Vec3DInt.from(op[0], op[1], op[2]));
            assertTrue("hollow op position must be in selection", blocks.contains(key));
        }
    }
}
