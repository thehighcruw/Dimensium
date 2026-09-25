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
import java.util.TreeMap;
import net.minecraft.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Post-processes a block map to replace surface blocks with stairs and slabs using a sub-voxel SDF
 * approach.
 *
 * <p>
 * Each integer block position is subdivided into a 2×2×2 grid of half-voxels (sub-voxels). The
 * path-tube signed distance function is evaluated at the center of each sub-voxel; the resulting
 * 8-bit mask is matched against precomputed masks for all representable block shapes using minimum
 * Hamming distance. The closest shape is placed.
 *
 * <h3>Sub-voxel bit encoding</h3>
 *
 * <pre>
 * bit 0 — x: 0 = west (−0.25),   1 = east (+0.25)
 * bit 1 — y: 0 = bottom (−0.25), 1 = top  (+0.25)
 * bit 2 — z: 0 = north (−0.25),  1 = south (+0.25)
 *
 * Derived masks:
 *   bottom layer (bit1=0): sub-voxels 0,1,4,5 → 0x33
 *   top layer    (bit1=1): sub-voxels 2,3,6,7 → 0xCC
 *   top quadrants:    NE=0x08  NW=0x04  SE=0x80  SW=0x40
 *   bottom quadrants: NE=0x02  NW=0x01  SE=0x20  SW=0x10
 * </pre>
 *
 * <h3>Representable shapes</h3>
 *
 * <p>
 * Slabs and straight stairs cover horizontal and cardinal-diagonal surfaces. Corner shapes
 * (inner/outer) cover 45° diagonal surfaces. Corner shapes are not directly placeable in Minecraft
 * 1.7.10 — the game renders them automatically when two perpendicular stairs are adjacent. For
 * corner shape hits, {@link BlockShape#stairMeta()} holds the nearest straight-stair meta that best
 * approximates the corner as a standalone block.
 */
public final class StairSlabSmoother {

    private static final Logger LOG = LogManager.getLogger("dimensium.smoother");

    /**
     * Set to {@code true} to enable per-block debug logging in {@link #smooth}.
     * Logs mask, Hamming distance to chosen shape, and shape name for every processed block.
     * Also prints a shape-frequency summary after each smooth() call.
     * Disable before shipping — very chatty.
     */
    public static boolean debugLogging = false;

    /** A sphere sample recorded during path generation. */
    @Desugar
    public record SphereSample(Vec3DFloat center, float effectiveRadius) {}

    enum PlacementKind {
        AIR,
        FULL,
        SLAB_BOTTOM,
        SLAB_TOP,
        STAIR
    }

    /**
     * A representable block shape. {@code mask} is the 8-bit sub-voxel fill pattern.
     * {@code stairMeta} is the Minecraft stair metadata (0–7); only meaningful when
     * {@code kind == STAIR}.
     */
    @Desugar
    record BlockShape(String name, int mask, PlacementKind kind, int stairMeta) {

        static BlockShape air() {
            return new BlockShape("air", 0x00, PlacementKind.AIR, -1);
        }

        static BlockShape full() {
            return new BlockShape("full", 0xFF, PlacementKind.FULL, -1);
        }

        static BlockShape slabBottom() {
            return new BlockShape("slab bottom", 0x33, PlacementKind.SLAB_BOTTOM, -1);
        }

        static BlockShape slabTop() {
            return new BlockShape("slab top", 0xCC, PlacementKind.SLAB_TOP, -1);
        }

        static BlockShape stair(String name, int mask, int meta) {
            return new BlockShape(name, mask, PlacementKind.STAIR, meta);
        }
    }

    // @formatter:off
    private static final BlockShape[] SHAPES = {
        // ── special ──────────────────────────────────────────────────────────────────────────────
        BlockShape.air(),
        BlockShape.slabBottom(),
        BlockShape.slabTop(),
        // ── straight stairs ──────────────────────────────────── meta  mask   geometry ──────────
        BlockShape.stair("stair east", 0xBB, 0), // bottom + top-east
        BlockShape.stair("stair west", 0x77, 1), // bottom + top-west
        BlockShape.stair("stair south", 0xF3, 2), // bottom + top-south
        BlockShape.stair("stair north", 0x3F, 3), // bottom + top-north
        // ── upside-down straight stairs ──────────────────────────────────────────────────────────
        BlockShape.stair("stair ud east", 0xEE, 4), // top + bottom-east
        BlockShape.stair("stair ud west", 0xDD, 5), // top + bottom-west
        BlockShape.stair("stair ud south", 0xFC, 6), // top + bottom-south
        BlockShape.stair("stair ud north", 0xCF, 7), // top + bottom-north
        // ── bottom inner corners (bottom + 1 top quadrant, 5 sub-voxels) ─────────────────────────
        BlockShape.stair("corner bottom inner NE", 0x3B, 0), // bottom + top-NE
        BlockShape.stair("corner bottom inner NW", 0x37, 1), // bottom + top-NW
        BlockShape.stair("corner bottom inner SE", 0xB3, 0), // bottom + top-SE
        BlockShape.stair("corner bottom inner SW", 0x73, 1), // bottom + top-SW
        // ── bottom outer corners (bottom + 3 top quadrants, 7 sub-voxels) ────────────────────────
        BlockShape.stair("corner bottom outer NE", 0xF7, 1), // bottom + top-W/S/SW  (missing top-NE)
        BlockShape.stair("corner bottom outer NW", 0xFB, 0), // bottom + top-E/S/SE  (missing top-NW)
        BlockShape.stair("corner bottom outer SE", 0x7F, 1), // bottom + top-W/N/NW  (missing top-SE)
        BlockShape.stair("corner bottom outer SW", 0xBF, 0), // bottom + top-E/N/NE  (missing top-SW)
        // ── upside-down inner corners (top + 1 bottom quadrant, 5 sub-voxels) ────────────────────
        BlockShape.stair("corner ud inner NE", 0xCE, 4), // top + bottom-NE
        BlockShape.stair("corner ud inner NW", 0xCD, 5), // top + bottom-NW
        BlockShape.stair("corner ud inner SE", 0xEC, 4), // top + bottom-SE
        BlockShape.stair("corner ud inner SW", 0xDC, 5), // top + bottom-SW
        // ── upside-down outer corners (top + 3 bottom quadrants, 7 sub-voxels) ───────────────────
        BlockShape.stair("corner ud outer NE", 0xFD, 5), // top + bottom-W/S/SW  (missing bottom-NE)
        BlockShape.stair("corner ud outer NW", 0xFE, 4), // top + bottom-E/S/SE  (missing bottom-NW)
        BlockShape.stair("corner ud outer SE", 0xDF, 5), // top + bottom-W/N/NW  (missing bottom-SE)
        BlockShape.stair("corner ud outer SW", 0xEF, 4), // top + bottom-E/N/NE  (missing bottom-SW)
        // ── full block (fallback — only wins for mask 0xFF) ───────────────────────────────────────
        BlockShape.full(),
    };
    // @formatter:on

    private static final int SHAPE_IDX_AIR = 0;
    private static final int SHAPE_IDX_FULL = SHAPES.length - 1;

    static {
        assert SHAPES[SHAPE_IDX_AIR].kind() == PlacementKind.AIR : "SHAPE_IDX_AIR must point to the AIR shape";
        assert SHAPES[SHAPE_IDX_FULL].kind() == PlacementKind.FULL : "SHAPE_IDX_FULL must point to the FULL shape";
    }

    /** Sub-voxel bits belonging to the top half (bit1 set). */
    private static final int TOP_BITS = 0xCC;

    /** Sub-voxel bits belonging to the bottom half (bit1 clear). */
    private static final int BOTTOM_BITS = 0x33;

    /**
     * Lookup table: for each 8-bit sub-voxel mask (0–255), the index into {@link #SHAPES} of the
     * best-fitting shape. Air (index 0) is only chosen for mask 0x00; all other masks map to the
     * closest non-air shape.
     */
    private static final int[] SHAPE_LOOKUP = buildLookup();

    /**
     * Returns a y-orientation match score (higher = better) comparing a shape's top/bottom bias
     * against the mask's top/bottom bias. Used as the first Hamming-distance tiebreaker so that
     * top-heavy masks prefer top-heavy shapes (upside-down stairs / top slab) and vice-versa.
     */
    private static int yOrientationMatch(int maskTop, int maskBottom, int shapeTop, int shapeBottom) {
        if (maskTop > maskBottom && shapeTop > shapeBottom) return 2; // both top-heavy
        if (maskBottom > maskTop && shapeBottom > shapeTop) return 2; // both bottom-heavy
        if (maskTop == maskBottom) return 1; // mask neutral — no preference
        return 0; // orientation mismatch
    }

    private static int[] buildLookup() {
        int[] lookup = new int[256];
        lookup[0] = SHAPE_IDX_AIR;
        for (int mask = 1; mask < 256; mask++) {
            int maskTopCount = Integer.bitCount(mask & TOP_BITS);
            int maskBottomCount = Integer.bitCount(mask & BOTTOM_BITS);
            int best = SHAPE_IDX_FULL;
            int bestDist = Integer.MAX_VALUE;
            // Skip index 0 (air) — non-zero masks never map to air.
            for (int s = 1; s < SHAPES.length; s++) {
                int shapeMask = SHAPES[s].mask();
                int dist = Integer.bitCount(mask ^ shapeMask);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = s;
                } else if (dist == bestDist) {
                    // Tiebreak 1: y-orientation match.
                    int sTopCount = Integer.bitCount(shapeMask & TOP_BITS);
                    int sBottomCount = Integer.bitCount(shapeMask & BOTTOM_BITS);
                    int bestTopCount = Integer.bitCount(SHAPES[best].mask() & TOP_BITS);
                    int bestBottomCount = Integer.bitCount(SHAPES[best].mask() & BOTTOM_BITS);
                    int sScore = yOrientationMatch(maskTopCount, maskBottomCount, sTopCount, sBottomCount);
                    int bestScore = yOrientationMatch(maskTopCount, maskBottomCount, bestTopCount, bestBottomCount);
                    if (sScore > bestScore) {
                        best = s;
                    } else if (sScore == bestScore) {
                        // Tiebreak 2: popcount proximity.
                        if (Math.abs(Integer.bitCount(shapeMask) - Integer.bitCount(mask))
                                < Math.abs(Integer.bitCount(SHAPES[best].mask()) - Integer.bitCount(mask))) {
                            best = s;
                        }
                    }
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
     * Each block's 8 half-voxels are tested against the union of all path spheres. A half-voxel
     * is inside if any sphere contains it. The resulting 8-bit mask is looked up in
     * {@link #SHAPE_LOOKUP} to select the closest representable shape.
     *
     * @param blocks        generated block map (full blocks)
     * @param sphereSamples all sphere samples populated during generation
     */
    public static Map<Long, int[]> smooth(Map<Long, int[]> blocks, List<SphereSample> sphereSamples) {
        Map<Long, int[]> result = new HashMap<>(blocks);
        // shape-name → count; TreeMap for stable alphabetical output in summary.
        Map<String, Integer> shapeFrequency = debugLogging ? new TreeMap<>() : null;

        for (Map.Entry<Long, int[]> entry : blocks.entrySet()) {
            long key = entry.getKey();
            int[] bm = entry.getValue();

            Vec3DInt blockPos = ChangeProposal.unpackKey(key);
            BlockFamilyRegistry.BlockFamily family = BlockFamilyRegistry.lookup(bm[0], bm[1]);
            if (family == null) continue;

            int subMask = subVoxelMask(blockPos.toFloat(), sphereSamples);
            int subVoxelsInside = Integer.bitCount(subMask);

            // Barely-inside blocks (<3 sub-voxels): remove unless no neighbor is closer to
            // the spine (which would mean this block itself is the path center — removing it
            // would create a gap).
            if (subVoxelsInside < 3) {
                if (hasNeighborCloserToSpine(blockPos, blocks, sphereSamples)) {
                    result.remove(key);
                }
                if (!debugLogging) continue;
            }

            // Blocks deep inside the path (≥7 sub-voxels inside, ≤1 exposed) stay full.
            // The exposed corner is too small to benefit from discretization and creates notches.
            if (subVoxelsInside >= 7 && !debugLogging) continue;

            BlockShape shape = SHAPES[SHAPE_LOOKUP[subMask]];
            int hamming = Integer.bitCount(subMask ^ shape.mask());

            if (debugLogging) {
                String label = shape.kind() == PlacementKind.STAIR
                        ? shape.name() + " [meta " + shape.stairMeta() + "]"
                        : shape.name();
                LOG.info(
                        "({},{},{})  mask=0x{} hamming={}  {}{}",
                        blockPos.x(),
                        blockPos.y(),
                        blockPos.z(),
                        Integer.toHexString(subMask).toUpperCase(),
                        hamming,
                        label,
                        hamming > 0 ? "  (fallback)" : "");
                shapeFrequency.merge(label, 1, Integer::sum);
            }

            if (hamming > 0) {
                // No exact shape match: stairs/slabs with wrong geometry look worse than a
                // slightly over/under-sized solid block. Fall back to full or air by majority.
                // Exception: spine blocks (no neighbor is closer to the sphere center) must
                // always retain solid coverage to avoid gaps in thin paths.
                if (subVoxelsInside <= 4 && hasNeighborCloserToSpine(blockPos, blocks, sphereSamples)) {
                    result.remove(key);
                }
                // else: keep as full (no change needed)
                continue;
            }

            switch (shape.kind()) {
                case AIR:
                    result.remove(key);
                    break;
                case FULL:
                    break;
                case SLAB_BOTTOM:
                    result.put(key, new int[] {Block.getIdFromBlock(family.slab()), family.slabMetaBottom()});
                    break;
                case SLAB_TOP:
                    result.put(key, new int[] {Block.getIdFromBlock(family.slab()), family.slabMetaTop()});
                    break;
                case STAIR:
                    result.put(key, new int[] {Block.getIdFromBlock(family.stairs()), shape.stairMeta()});
                    break;
            }
        }

        if (debugLogging) {
            LOG.info("=== smooth() summary: {} blocks in, {} out ===", blocks.size(), result.size());
            shapeFrequency.forEach((name, count) -> LOG.info("  {}x  {}", count, name));
        }

        return result;
    }

    private static final Vec3DFloat[] FACE_NEIGHBORS = {
        Vec3DFloat.from(1, 0, 0),
        Vec3DFloat.from(-1, 0, 0),
        Vec3DFloat.from(0, 1, 0),
        Vec3DFloat.from(0, -1, 0),
        Vec3DFloat.from(0, 0, 1),
        Vec3DFloat.from(0, 0, -1),
    };

    /**
     * Returns true if any 6-connected neighbor of {@code blockPos} that exists in {@code blocks}
     * is strictly closer to the nearest sphere center than {@code blockPos} itself.
     * Used to decide whether a barely-inside block is safe to remove.
     */
    private static boolean hasNeighborCloserToSpine(
            Vec3DInt blockPos, Map<Long, int[]> blocks, List<SphereSample> sphereSamples) {
        Vec3DFloat posF = blockPos.toFloat();
        float myDistSq = Float.MAX_VALUE;
        Vec3DFloat nearestCenter = null;
        for (SphereSample sphere : sphereSamples) {
            float dSq = posF.minus(sphere.center()).lengthSq();
            if (dSq < myDistSq) {
                myDistSq = dSq;
                nearestCenter = sphere.center();
            }
        }
        if (nearestCenter == null) return false;
        for (Vec3DFloat offset : FACE_NEIGHBORS) {
            Vec3DFloat neighborF = posF.plus(offset);
            long neighborKey = ChangeProposal.packKey((int) neighborF.x(), (int) neighborF.y(), (int) neighborF.z());
            if (!blocks.containsKey(neighborKey)) continue;
            if (neighborF.minus(nearestCenter).lengthSq() < myDistSq) return true;
        }
        return false;
    }

    /**
     * Evaluates the sub-voxel SDF for {@code blockPos} against {@code spheres} and returns the
     * 8-bit mask of sub-voxels that lie inside any sphere. Package-private for testing.
     */
    static int subVoxelMask(Vec3DFloat blockPos, List<SphereSample> spheres) {
        int mask = 0;
        for (int i = 0; i < 8; i++) {
            Vec3DFloat subVoxel = blockPos.plus(Vec3DFloat.from(
                    (i & 1) != 0 ? 0.25f : -0.25f, (i & 2) != 0 ? 0.25f : -0.25f, (i & 4) != 0 ? 0.25f : -0.25f));
            for (SphereSample sphere : spheres) {
                if (subVoxel.minus(sphere.center()).lengthSq() <= sphere.effectiveRadius() * sphere.effectiveRadius()) {
                    mask |= (1 << i);
                    break;
                }
            }
        }
        return mask;
    }

    /** Returns the shape selected for {@code subMask}. Package-private for testing. */
    static BlockShape shapeForMask(int subMask) {
        return SHAPES[SHAPE_LOOKUP[subMask]];
    }

    private StairSlabSmoother() {}
}
