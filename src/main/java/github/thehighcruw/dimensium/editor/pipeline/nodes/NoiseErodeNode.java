/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline.nodes;

import github.thehighcruw.dimensium.editor.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.ArrayList;
import java.util.List;

public class NoiseErodeNode implements PipelineNode {

    public static final String ID = "noise_erode";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("erode.strength", 0.4f, 0.0f, 1.0f, "dimensium.ui.pipeline.erode_strength")
            .floatParam("erode.noiseScale", 0.15f, 0.01f, 0.5f, "dimensium.ui.pipeline.erode_noise_scale")
            .description("dimensium.ui.pipeline.node.noise_erode.desc")
            .inputPort("blocks", PortType.BLOCK_MAP)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap blockMap = inputs.get("blocks", BlockMap.class);
        if (blockMap == null) return;

        float strength = params.getFloat("erode.strength", 0.4f);
        float noiseScale = Math.max(0.01f, params.getFloat("erode.noiseScale", 0.15f));
        long seed = context.nodeSeed(4);

        float threshold = 1f - strength;

        List<Long> toRemove = new ArrayList<>();
        for (Long key : blockMap.entries().keySet()) {
            Vec3DInt pos = BlockMap.unpackKey(key);
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

        outputs.set("blocks", blockMap);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
