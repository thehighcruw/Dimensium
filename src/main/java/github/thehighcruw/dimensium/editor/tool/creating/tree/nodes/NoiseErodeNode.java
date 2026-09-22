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
import java.util.List;

public class NoiseErodeNode implements PipelineNode<BlockMap, BlockMap> {

    public static final String ID = "noise_erode";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("erode.strength", 0.4f, 0.0f, 1.0f, "dimensium.ui.tree.erode_strength")
            .floatParam("erode.noiseScale", 0.15f, 0.01f, 0.5f, "dimensium.ui.tree.erode_noise_scale")
            .boolParam("erode.onlyLeaves", true, "dimensium.ui.tree.erode_only_leaves");

    @Override
    public BlockMap apply(BlockMap blockMap, NodeParams params, PipelineContext context) {
        float strength = params.getFloat("erode.strength", 0.4f);
        float noiseScale = Math.max(0.01f, params.getFloat("erode.noiseScale", 0.15f));
        boolean onlyLeaves = params.getBool("erode.onlyLeaves", true);
        long seed = context.nodeSeed(4);

        float threshold = 1f - strength;

        List<Long> toRemove = new ArrayList<>();
        for (Long key : blockMap.entries().keySet()) {
            int[] blockEntry = blockMap.entries().get(key);
            int blockId = blockEntry[0];

            if (onlyLeaves && !isLeaf(blockId)) continue;

            Vec3DInt pos = ChangeProposal.unpackKey(key);
            float noise =
                    (NoiseSampler.rawSimplex3(pos.x() * noiseScale, pos.y() * noiseScale, pos.z() * noiseScale, seed)
                                    + 1f)
                            * 0.5f;

            if (noise > threshold) {
                toRemove.add(key);
            }
        }

        for (Long key : toRemove) {
            blockMap.entries().remove(key);
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
