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
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.Map;
import net.minecraft.block.Block;

/** Removes all positions present in B from A. */
public class SubtractBlocksNode implements PipelineNode {

    public static final String ID = "subtract_blocks";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .description("dimensium.ui.pipeline.node.subtract_blocks.desc")
            .inputPort("blocksA", PortType.BLOCK_MAP)
            .inputPort("blocksB", PortType.BLOCK_MAP)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap blockMapA = inputs.get("blocksA", BlockMap.class);
        BlockMap blockMapB = inputs.get("blocksB", BlockMap.class);

        if (blockMapA == null) {
            outputs.set("blocks", new BlockMap());
            return;
        }
        if (blockMapB == null) {
            BlockMap copy = new BlockMap();
            copy.merge(blockMapA);
            outputs.set("blocks", copy);
            return;
        }

        BlockMap result = new BlockMap();
        Map<Long, int[]> bEntries = blockMapB.entries();
        for (Map.Entry<Long, int[]> entry : blockMapA.entries().entrySet()) {
            if (!bEntries.containsKey(entry.getKey())) {
                Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
                result.put(pos, Block.getBlockById(entry.getValue()[0]), entry.getValue()[1]);
            }
        }
        outputs.set("blocks", result);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
