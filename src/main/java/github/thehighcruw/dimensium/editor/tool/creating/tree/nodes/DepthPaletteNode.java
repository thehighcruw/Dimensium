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

public class DepthPaletteNode implements PipelineNode<BlockMap, BlockMap> {

    public static final String ID = "depth_palette";

    private static final List<int[]> DEFAULT_PALETTE =
            Arrays.asList(new int[] {17, 0}, new int[] {17, 0}, new int[] {18, 4});

    private static final NodeSchema SCHEMA = new NodeSchema()
            .paletteParam("dp.palette", DEFAULT_PALETTE, "dimensium.ui.tree.depth_palette")
            .floatParam("dp.gradientStart", 0.0f, 0.0f, 1.0f, "dimensium.ui.tree.depth_gradient_start")
            .floatParam("dp.gradientEnd", 1.0f, 0.0f, 1.0f, "dimensium.ui.tree.depth_gradient_end");

    @Override
    public BlockMap apply(BlockMap blockMap, NodeParams params, PipelineContext context) {
        List<int[]> palette = params.getPalette("dp.palette", DEFAULT_PALETTE);
        if (palette.isEmpty()) return blockMap;

        float gradientStart = params.getFloat("dp.gradientStart", 0.0f);
        float gradientEnd = params.getFloat("dp.gradientEnd", 1.0f);

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (long key : blockMap.entries().keySet()) {
            Vec3DInt pos = ChangeProposal.unpackKey(key);
            if (pos.y() < minY) minY = pos.y();
            if (pos.y() > maxY) maxY = pos.y();
        }
        if (minY == maxY) return blockMap;

        int range = maxY - minY;

        for (Map.Entry<Long, int[]> entry : new ArrayList<>(blockMap.entries().entrySet())) {
            Vec3DInt pos = ChangeProposal.unpackKey(entry.getKey());
            float t = (float) (pos.y() - minY) / range;
            t = gradientStart + t * (gradientEnd - gradientStart);
            t = Math.max(0f, Math.min(1f, t));

            int paletteIndex = Math.min(palette.size() - 1, (int) (t * palette.size()));
            int[] blockEntry = palette.get(paletteIndex);
            Block block = Block.getBlockById(blockEntry[0]);
            blockMap.put(pos, block, blockEntry[1]);
        }

        return blockMap;
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
