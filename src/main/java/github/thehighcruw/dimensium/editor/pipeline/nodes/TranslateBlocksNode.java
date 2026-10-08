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

/** Shifts all blocks in a block map by a fixed integer offset. */
public class TranslateBlocksNode implements PipelineNode {

    public static final String ID = "translate_blocks";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("translate.x", 0, -128, 128, "dimensium.ui.pipeline.translate_offset_x")
            .intParam("translate.y", 0, -128, 128, "dimensium.ui.pipeline.translate_offset_y")
            .intParam("translate.z", 0, -128, 128, "dimensium.ui.pipeline.translate_offset_z")
            .description("dimensium.ui.pipeline.node.translate_blocks.desc")
            .inputPort("blocks", PortType.BLOCK_MAP)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap input = inputs.get("blocks", BlockMap.class);
        if (input == null) return;

        int offsetX = params.getInt("translate.x", 0);
        int offsetY = params.getInt("translate.y", 0);
        int offsetZ = params.getInt("translate.z", 0);

        BlockMap result = new BlockMap();
        for (Map.Entry<Long, int[]> entry : input.entries().entrySet()) {
            Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
            Vec3DInt shifted = pos.plus(new Vec3DInt(offsetX, offsetY, offsetZ));
            result.put(shifted, entry.getValue()[0], entry.getValue()[1]);
        }
        outputs.set("blocks", result);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
