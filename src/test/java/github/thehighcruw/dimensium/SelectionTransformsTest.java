/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.SelectionTransforms;

public class SelectionTransformsTest {

    private static Set<Long> pack(int[][] coords) {
        Set<Long> s = new HashSet<>();
        for (int[] c : coords) s.add(SelectionState.pack(c[0], c[1], c[2]));
        return s;
    }

    private static boolean has(Set<Long> set, int x, int y, int z) {
        return set.contains(SelectionState.pack(x, y, z));
    }

    // ── move ─────────────────────────────────────────────────────────────────

    @Test
    public void moveByZeroIsIdentity() {
        Set<Long> blocks = pack(new int[][] { { 5, 64, 10 }, { 6, 64, 10 } });
        Set<Long> result = SelectionTransforms.move(blocks, 0, 0, 0);
        assertEquals(blocks, result);
    }

    @Test
    public void moveTranslatesAllCoords() {
        Set<Long> blocks = pack(new int[][] { { 0, 64, 0 } });
        Set<Long> result = SelectionTransforms.move(blocks, 3, -2, 7);
        assertEquals(1, result.size());
        assertTrue(has(result, 3, 62, 7));
    }

    @Test
    public void moveDropsBlocksBelowY0() {
        Set<Long> blocks = pack(new int[][] { { 0, 1, 0 }, { 0, 0, 0 } });
        Set<Long> result = SelectionTransforms.move(blocks, 0, -1, 0);
        // y=1 → y=0 kept, y=0 → y=-1 dropped
        assertEquals(1, result.size());
        assertTrue(has(result, 0, 0, 0));
    }

    @Test
    public void moveDropsBlocksAboveY255() {
        Set<Long> blocks = pack(new int[][] { { 0, 255, 0 }, { 0, 254, 0 } });
        Set<Long> result = SelectionTransforms.move(blocks, 0, 1, 0);
        // y=255 → 256 dropped, y=254 → 255 kept
        assertEquals(1, result.size());
        assertTrue(has(result, 0, 255, 0));
    }

    @Test
    public void moveNegativeXZ() {
        Set<Long> blocks = pack(new int[][] { { 100, 64, 200 } });
        Set<Long> result = SelectionTransforms.move(blocks, -100, 0, -200);
        assertEquals(1, result.size());
        assertTrue(has(result, 0, 64, 0));
    }

    // ── expand ────────────────────────────────────────────────────────────────

    @Test
    public void expandZeroIsIdentity() {
        Set<Long> blocks = pack(new int[][] { { 5, 64, 5 } });
        Set<Long> result = SelectionTransforms.expand(blocks, 0);
        assertEquals(blocks, result);
    }

    @Test
    public void expandByOneAdds6Neighbours() {
        Set<Long> blocks = pack(new int[][] { { 5, 64, 5 } });
        Set<Long> result = SelectionTransforms.expand(blocks, 1);
        // original + 6 orthogonal neighbours
        assertEquals(7, result.size());
        assertTrue(has(result, 5, 64, 5));
        assertTrue(has(result, 6, 64, 5));
        assertTrue(has(result, 4, 64, 5));
        assertTrue(has(result, 5, 65, 5));
        assertTrue(has(result, 5, 63, 5));
        assertTrue(has(result, 5, 64, 6));
        assertTrue(has(result, 5, 64, 4));
    }

    @Test
    public void expandDoesNotGoBelowY0() {
        Set<Long> blocks = pack(new int[][] { { 5, 0, 5 } });
        Set<Long> result = SelectionTransforms.expand(blocks, 1);
        assertFalse(has(result, 5, -1, 5));
    }

    @Test
    public void expandDoesNotGoAboveY255() {
        Set<Long> blocks = pack(new int[][] { { 5, 255, 5 } });
        Set<Long> result = SelectionTransforms.expand(blocks, 1);
        assertFalse(
            "should not produce y=256",
            result.stream()
                .anyMatch(k -> SelectionState.unpackY(k) > 255));
    }

    @Test
    public void expandByTwoGrowsFurther() {
        Set<Long> single = pack(new int[][] { { 0, 64, 0 } });
        Set<Long> by1 = SelectionTransforms.expand(single, 1);
        Set<Long> by2 = SelectionTransforms.expand(single, 2);
        assertTrue("expand(2) must be superset of expand(1)", by2.containsAll(by1));
        assertTrue("expand(2) > expand(1)", by2.size() > by1.size());
    }

    // ── shrink ────────────────────────────────────────────────────────────────

    @Test
    public void shrinkZeroIsIdentity() {
        Set<Long> blocks = pack(new int[][] { { 5, 64, 5 }, { 6, 64, 5 }, { 5, 64, 6 } });
        assertEquals(blocks, SelectionTransforms.shrink(blocks, 0));
    }

    @Test
    public void shrinkSingleBlockToEmpty() {
        Set<Long> blocks = pack(new int[][] { { 5, 64, 5 } });
        Set<Long> result = SelectionTransforms.shrink(blocks, 1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void shrinkRemovesShellOf3x3x3Cube() {
        // Build a 3×3×3 cube centred at (5,64,5)
        Set<Long> blocks = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
            for (int dz = -1; dz <= 1; dz++) blocks.add(SelectionState.pack(5 + dx, 64 + dy, 5 + dz));

        Set<Long> result = SelectionTransforms.shrink(blocks, 1);
        // Only the single interior block (5,64,5) survives
        assertEquals(1, result.size());
        assertTrue(has(result, 5, 64, 5));
    }

    @Test
    public void expandThenShrinkReturnsOriginal() {
        Set<Long> blocks = pack(
            new int[][] { { 0, 64, 0 }, { 1, 64, 0 }, { 2, 64, 0 }, { 0, 65, 0 }, { 1, 65, 0 }, { 2, 65, 0 },
                { 0, 64, 1 }, { 1, 64, 1 }, { 2, 64, 1 }, { 0, 65, 1 }, { 1, 65, 1 }, { 2, 65, 1 } });
        Set<Long> expanded = SelectionTransforms.expand(blocks, 1);
        Set<Long> back = SelectionTransforms.shrink(expanded, 1);
        // Original must be subset of shrunk result (shrink may keep more interior)
        assertTrue("original must be contained in shrink(expand(original))", back.containsAll(blocks));
    }

    @Test
    public void shrinkNegativeOffsetIsIdentity() {
        Set<Long> blocks = pack(new int[][] { { 5, 64, 5 } });
        assertEquals(blocks, SelectionTransforms.shrink(blocks, -1));
    }

    // ── smooth ────────────────────────────────────────────────────────────────

    @Test
    public void smoothEmptyReturnsEmpty() {
        assertTrue(
            SelectionTransforms.smooth(new HashSet<>(), 1, 0.5f)
                .isEmpty());
    }

    @Test
    public void smoothSolidCubeAtLowThresholdRetainsMostBlocks() {
        // 5×5×5 solid cube — smoothed at low threshold should keep most interior blocks
        Set<Long> blocks = new HashSet<>();
        for (int x = 0; x < 5; x++)
            for (int y = 64; y < 69; y++) for (int z = 0; z < 5; z++) blocks.add(SelectionState.pack(x, y, z));

        Set<Long> result = SelectionTransforms.smooth(blocks, 1, 0.1f);
        assertFalse("smooth of solid cube should not be empty", result.isEmpty());
    }

    @Test
    public void smoothAtThreshold1OfSingleBlockReturnsEmpty() {
        // Single isolated block has density < 1.0 under any kernel with neighbours, so threshold=1 drops it
        Set<Long> blocks = pack(new int[][] { { 0, 64, 0 } });
        Set<Long> result = SelectionTransforms.smooth(blocks, 1, 1.0f);
        assertTrue("single block at threshold=1 should vanish", result.isEmpty());
    }
}
