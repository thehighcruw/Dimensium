/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import com.github.bsideup.jabel.Desugar;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;

/**
 * Post-processes a block map to replace surface blocks with stairs and slabs using a sub-voxel SDF
 * approach.
 *
 * <p>
 * Each integer block position is subdivided into a 2×2×2 grid of half-voxels (sub-voxels). The
 * path-tube signed distance function is evaluated at the center of each sub-voxel; the resulting
 * 8-bit mask is matched against precomputed masks for all representable block shapes (full block,
 * bottom/top slab, and all 8 stair orientations) using minimum Hamming distance. The closest shape
 * is placed.
 *
 * <h3>Sub-voxel bit encoding</h3>
 *
 * <pre>
 * bit 0 — x offset: 0 = west (−0.25), 1 = east (+0.25)
 * bit 1 — y offset: 0 = bottom (−0.25), 1 = top (+0.25)
 * bit 2 — z offset: 0 = north (−0.25), 1 = south (+0.25)
 * </pre>
 *
 * <h3>Shape masks</h3>
 *
 * <pre>
 * 0x00 → air          (removed)
 * 0x33 → bottom slab  (y=bottom layer: bits 0,1,4,5)
 * 0xCC → top slab     (y=top layer:    bits 2,3,6,7)
 * 0xBB → stair meta 0 (ascending east:  bottom + top-east)
 * 0x77 → stair meta 1 (ascending west:  bottom + top-west)
 * 0xF3 → stair meta 2 (ascending south: bottom + top-south)
 * 0x3F → stair meta 3 (ascending north: bottom + top-north)
 * 0xEE → stair meta 4 (ud ascending east:  top + bottom-east)
 * 0xDD → stair meta 5 (ud ascending west:  top + bottom-west)
 * 0xFC → stair meta 6 (ud ascending south: top + bottom-south)
 * 0xCF → stair meta 7 (ud ascending north: top + bottom-north)
 * 0xFF → full block
 * </pre>
 */
public final class StairSlabSmoother {

    /** A sphere sample as recorded during path generation. */
    @Desugar
    public record SphereSample(Vec3DFloat center, float effectiveRadius) {}

    private static final int[] SHAPE_MASKS = {
        0x00, // 0: air
        0x33, // 1: bottom slab
        0xCC, // 2: top slab
        0xBB, // 3: stair meta 0 — ascending east
        0x77, // 4: stair meta 1 — ascending west
        0xF3, // 5: stair meta 2 — ascending south
        0x3F, // 6: stair meta 3 — ascending north
        0xEE, // 7: stair meta 4 — upside-down ascending east
        0xDD, // 8: stair meta 5 — upside-down ascending west
        0xFC, // 9: stair meta 6 — upside-down ascending south
        0xCF, // 10: stair meta 7 — upside-down ascending north
        0xFF, // 11: full block
    };

    private static final int SHAPE_AIR = 0;
    private static final int SHAPE_FULL = 11;

    /**
     * Lookup table: for each 8-bit sub-voxel mask (0–255), the index into {@link #SHAPE_MASKS} of
     * the shape with minimum Hamming distance. Air (index 0) is only chosen for mask 0x00; all
     * other masks map to the closest non-air shape.
     */
    private static final int[] SHAPE_LOOKUP = buildLookup();

    private static int[] buildLookup() {
        int[] lookup = new int[256];
        lookup[0] = SHAPE_AIR;
        for (int mask = 1; mask < 256; mask++) {
            int best = SHAPE_FULL;
            int bestDist = Integer.MAX_VALUE;
            // Skip index 0 (air) so non-zero masks never map to air.
            for (int s = 1; s < SHAPE_MASKS.length; s++) {
                int dist = Integer.bitCount(mask ^ SHAPE_MASKS[s]);
                if (dist < bestDist
                        || (dist == bestDist
                                && Math.abs(Integer.bitCount(SHAPE_MASKS[s]) - Integer.bitCount(mask))
                                        < Math.abs(Integer.bitCount(SHAPE_MASKS[best]) - Integer.bitCount(mask)))) {
                    bestDist = dist;
                    best = s;
                }
            }
            lookup[mask] = best;
        }
        return lookup;
    }

    /**
     * Returns a new block map with eligible surface blocks replaced by the best-fitting stair, slab,
     * or full block determined by sub-voxel union-SDF evaluation.
     *
     * <p>
     * Each surface block's 8 half-voxels are tested against the union of all path spheres. A
     * half-voxel is inside if any sphere contains it. The resulting 8-bit mask is looked up in
     * {@link #SHAPE_LOOKUP} to select the closest representable block shape.
     *
     * @param blocks        generated block map (full blocks)
     * @param sphereSamples all sphere samples populated during generation
     */
    public static Map<Long, int[]> smooth(Map<Long, int[]> blocks, List<SphereSample> sphereSamples) {
        Map<Long, int[]> result = new HashMap<>(blocks);

        for (Map.Entry<Long, int[]> entry : blocks.entrySet()) {
            long key = entry.getKey();
            int[] bm = entry.getValue();

            // Interior blocks (block above present) stay full — all sub-voxels would be inside.
            Vec3DInt blockPos = ChangeProposal.unpackKey(key);
            if (blocks.containsKey(ChangeProposal.packKey(blockPos.plus(Vec3DInt.from(0, 1, 0))))) continue;

            BlockFamilyRegistry.BlockFamily family = BlockFamilyRegistry.lookup(bm[0], bm[1]);
            if (family == null) continue;

            // Evaluate union SDF at 8 half-voxel centers. A half-voxel is inside when any sphere
            // contains it (squared-distance test avoids sqrt in the inner loop).
            Vec3DFloat blockPosF = blockPos.toFloat();
            int subMask = 0;
            for (int i = 0; i < 8; i++) {
                Vec3DFloat subVoxel = blockPosF.plus(Vec3DFloat.from(
                        (i & 1) != 0 ? 0.25f : -0.25f, (i & 2) != 0 ? 0.25f : -0.25f, (i & 4) != 0 ? 0.25f : -0.25f));
                for (SphereSample sphere : sphereSamples) {
                    if (subVoxel.minus(sphere.center()).lengthSq()
                            <= sphere.effectiveRadius() * sphere.effectiveRadius()) {
                        subMask |= (1 << i);
                        break;
                    }
                }
            }

            int shapeIndex = SHAPE_LOOKUP[subMask];
            if (shapeIndex == SHAPE_FULL) continue;

            if (shapeIndex == SHAPE_AIR) {
                result.remove(key);
                continue;
            }

            if (shapeIndex == 1) {
                result.put(key, new int[] {Block.getIdFromBlock(family.slab()), family.slabMetaBottom()});
            } else if (shapeIndex == 2) {
                result.put(key, new int[] {Block.getIdFromBlock(family.slab()), family.slabMetaTop()});
            } else {
                result.put(key, new int[] {Block.getIdFromBlock(family.stairs()), shapeIndex - 3});
            }
        }

        return result;
    }

    private StairSlabSmoother() {}
}
