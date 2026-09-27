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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;

public class NoisePaletteNode implements PipelineNode {

    public static final String ID = "noise_palette";

    private static final List<int[]> DEFAULT_PALETTE = Arrays.asList(new int[] {18, 4}, new int[] {18, 2});

    private static final NodeSchema SCHEMA = new NodeSchema()
            .paletteParam("np.palette", DEFAULT_PALETTE, "dimensium.ui.pipeline.noise_palette")
            .floatParam("np.noiseScale", 0.1f, 0.01f, 0.5f, "dimensium.ui.pipeline.noise_scale")
            .description("dimensium.ui.pipeline.node.noise_palette.desc")
            .inputPort("blocks", PortType.BLOCK_MAP)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap blockMap = inputs.get("blocks", BlockMap.class);
        if (blockMap == null) return;

        List<int[]> palette = params.getPalette("np.palette", DEFAULT_PALETTE);
        if (palette.isEmpty()) {
            outputs.set("blocks", blockMap);
            return;
        }

        float noiseScale = Math.max(0.01f, params.getFloat("np.noiseScale", 0.1f));
        long seed = context.nodeSeed(5);

        for (Map.Entry<Long, int[]> entry : new ArrayList<>(blockMap.entries().entrySet())) {
            Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
            float noise =
                    (NoiseSampler.rawSimplex3(pos.x() * noiseScale, pos.y() * noiseScale, pos.z() * noiseScale, seed)
                                    + 1f)
                            * 0.5f;

            int index = Math.min(palette.size() - 1, (int) (noise * palette.size()));
            int[] chosen = palette.get(index);
            blockMap.put(pos, Block.getBlockById(chosen[0]), chosen[1]);
        }

        outputs.set("blocks", blockMap);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
