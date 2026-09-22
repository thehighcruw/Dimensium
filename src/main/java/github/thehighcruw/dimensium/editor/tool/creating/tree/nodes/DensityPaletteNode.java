/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.nodes;

import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.PipelineNode;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;

public class DensityPaletteNode implements PipelineNode<BlockMap, BlockMap> {

    public static final String ID = "density_palette";

    private static final List<int[]> DEFAULT_PALETTE = Arrays.asList(new int[] {18, 4}, new int[] {18, 0});

    private static final Vec3DInt[] OFFSETS_6 = {
        Vec3DInt.from(1, 0, 0),
        Vec3DInt.from(-1, 0, 0),
        Vec3DInt.from(0, 1, 0),
        Vec3DInt.from(0, -1, 0),
        Vec3DInt.from(0, 0, 1),
        Vec3DInt.from(0, 0, -1)
    };

    private static final NodeSchema SCHEMA = new NodeSchema()
            .paletteParam("dp2.palette", DEFAULT_PALETTE, "dimensium.ui.tree.density_palette")
            .intParam("dp2.sampleRadius", 2, 1, 4, "dimensium.ui.tree.density_sample_radius")
            .boolParam("dp2.onlyLeaves", true, "dimensium.ui.tree.density_only_leaves");

    @Override
    public BlockMap apply(BlockMap blockMap, NodeParams params, PipelineContext context) {
        List<int[]> palette = params.getPalette("dp2.palette", DEFAULT_PALETTE);
        if (palette.isEmpty()) return blockMap;

        int sampleRadius = params.getInt("dp2.sampleRadius", 2);
        boolean onlyLeaves = params.getBool("dp2.onlyLeaves", true);

        int maxNeighbors = (int) Math.pow(2 * sampleRadius + 1, 3) - 1;

        for (Map.Entry<Long, int[]> entry : new ArrayList<>(blockMap.entries().entrySet())) {
            int blockId = entry.getValue()[0];
            if (onlyLeaves && !isLeaf(blockId)) continue;

            Vec3DInt pos = ChangeProposal.unpackKey(entry.getKey());
            int filled = countFilledNeighbors(blockMap, pos, sampleRadius);
            float density = maxNeighbors > 0 ? (float) filled / maxNeighbors : 0f;

            int index = Math.min(palette.size() - 1, (int) (density * palette.size()));
            int[] chosen = palette.get(index);
            blockMap.put(pos, Block.getBlockById(chosen[0]), chosen[1]);
        }

        return blockMap;
    }

    private static int countFilledNeighbors(BlockMap map, Vec3DInt center, int radius) {
        int count = 0;
        Vec3DInt rVec = Vec3DInt.from(radius, radius, radius);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (map.contains(center.plus(dx, dy, dz))) count++;
                }
            }
        }
        return count;
    }

    private static boolean isLeaf(int blockId) {
        return blockId == 18 || blockId == 161;
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
