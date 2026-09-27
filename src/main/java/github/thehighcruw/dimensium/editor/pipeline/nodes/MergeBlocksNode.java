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

/** Unions two BlockMaps; blocks from input B overwrite A at conflicting positions. */
public class MergeBlocksNode implements PipelineNode {

    public static final String ID = "merge_blocks";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .description("dimensium.ui.pipeline.node.merge_blocks.desc")
            .inputPort("blocksA", PortType.BLOCK_MAP)
            .inputPort("blocksB", PortType.BLOCK_MAP)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap blockMapA = inputs.get("blocksA", BlockMap.class);
        BlockMap blockMapB = inputs.get("blocksB", BlockMap.class);

        BlockMap result = new BlockMap();
        if (blockMapA != null) result.merge(blockMapA);
        if (blockMapB != null) result.merge(blockMapB);
        outputs.set("blocks", result);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
