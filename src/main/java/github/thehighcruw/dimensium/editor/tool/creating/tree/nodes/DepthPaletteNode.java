/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.nodes;

import github.thehighcruw.dimensium.editor.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;

public class DepthPaletteNode implements PipelineNode {

    public static final String ID = "depth_palette";

    private static final List<int[]> DEFAULT_PALETTE =
            Arrays.asList(new int[] {17, 0}, new int[] {17, 0}, new int[] {18, 4});

    private static final NodeSchema SCHEMA = new NodeSchema()
            .paletteParam("dp.palette", DEFAULT_PALETTE, "dimensium.ui.pipeline.depth_palette")
            .floatParam("dp.gradientStart", 0.0f, 0.0f, 1.0f, "dimensium.ui.pipeline.depth_gradient_start")
            .floatParam("dp.gradientEnd", 1.0f, 0.0f, 1.0f, "dimensium.ui.pipeline.depth_gradient_end")
            .description("dimensium.ui.pipeline.node.depth_palette.desc")
            .inputPort("blocks", PortType.BLOCK_MAP)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap blockMap = inputs.get("blocks", BlockMap.class);
        if (blockMap == null) return;

        List<int[]> palette = params.getPalette("dp.palette", DEFAULT_PALETTE);
        if (palette.isEmpty()) {
            outputs.set("blocks", blockMap);
            return;
        }

        float gradientStart = params.getFloat("dp.gradientStart", 0.0f);
        float gradientEnd = params.getFloat("dp.gradientEnd", 1.0f);

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (long key : blockMap.entries().keySet()) {
            Vec3DInt pos = BlockMap.unpackKey(key);
            if (pos.y() < minY) minY = pos.y();
            if (pos.y() > maxY) maxY = pos.y();
        }
        if (minY == maxY) {
            outputs.set("blocks", blockMap);
            return;
        }

        int range = maxY - minY;

        for (Map.Entry<Long, int[]> entry : new ArrayList<>(blockMap.entries().entrySet())) {
            Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
            float t = (float) (pos.y() - minY) / range;
            t = gradientStart + t * (gradientEnd - gradientStart);
            t = Math.max(0f, Math.min(1f, t));

            int paletteIndex = Math.min(palette.size() - 1, (int) (t * palette.size()));
            int[] blockEntry = palette.get(paletteIndex);
            Block block = Block.getBlockById(blockEntry[0]);
            blockMap.put(pos, block, blockEntry[1]);
        }

        outputs.set("blocks", blockMap);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
