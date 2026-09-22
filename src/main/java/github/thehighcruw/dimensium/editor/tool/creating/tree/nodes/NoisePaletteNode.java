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
import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;

public class NoisePaletteNode implements PipelineNode<BlockMap, BlockMap> {

    public static final String ID = "noise_palette";

    private static final List<int[]> DEFAULT_PALETTE = Arrays.asList(new int[] {18, 4}, new int[] {18, 2});

    private static final NodeSchema SCHEMA = new NodeSchema()
            .paletteParam("np.palette", DEFAULT_PALETTE, "dimensium.ui.tree.noise_palette")
            .floatParam("np.noiseScale", 0.1f, 0.01f, 0.5f, "dimensium.ui.tree.noise_scale")
            .boolParam("np.onlyLeaves", true, "dimensium.ui.tree.noise_only_leaves");

    @Override
    public BlockMap apply(BlockMap blockMap, NodeParams params, PipelineContext context) {
        List<int[]> palette = params.getPalette("np.palette", DEFAULT_PALETTE);
        if (palette.isEmpty()) return blockMap;

        float noiseScale = Math.max(0.01f, params.getFloat("np.noiseScale", 0.1f));
        boolean onlyLeaves = params.getBool("np.onlyLeaves", true);
        long seed = context.nodeSeed(5);

        for (Map.Entry<Long, int[]> entry : new ArrayList<>(blockMap.entries().entrySet())) {
            int blockId = entry.getValue()[0];
            if (onlyLeaves && !isLeaf(blockId)) continue;

            Vec3DInt pos = ChangeProposal.unpackKey(entry.getKey());
            float noise =
                    (NoiseSampler.rawSimplex3(pos.x() * noiseScale, pos.y() * noiseScale, pos.z() * noiseScale, seed)
                                    + 1f)
                            * 0.5f;

            int index = Math.min(palette.size() - 1, (int) (noise * palette.size()));
            int[] chosen = palette.get(index);
            blockMap.put(pos, Block.getBlockById(chosen[0]), chosen[1]);
        }

        return blockMap;
    }

    private static boolean isLeaf(int blockId) {
        return blockId == 18 || blockId == 161;
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
