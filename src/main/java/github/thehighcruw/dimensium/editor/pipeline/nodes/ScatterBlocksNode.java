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
import java.util.Random;

/**
 * Places copies of the input block map at random offsets within a volume.
 * Each copy is shifted by a random amount within the spread extents.
 */
public class ScatterBlocksNode implements PipelineNode {

    public static final String ID = "scatter_blocks";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("scatter.count", 5, 1, 64, "dimensium.ui.pipeline.scatter_count")
            .intParam("scatter.spreadX", 16, 0, 128, "dimensium.ui.pipeline.scatter_spread_x")
            .intParam("scatter.spreadY", 0, 0, 128, "dimensium.ui.pipeline.scatter_spread_y")
            .intParam("scatter.spreadZ", 16, 0, 128, "dimensium.ui.pipeline.scatter_spread_z")
            .description("dimensium.ui.pipeline.node.scatter_blocks.desc")
            .inputPort("blocks", PortType.BLOCK_MAP)
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap input = inputs.get("blocks", BlockMap.class);
        if (input == null) return;

        Vec3DInt scatterOrigin = inputs.get("origin", Vec3DInt.class);
        if (scatterOrigin == null) scatterOrigin = context.origin;

        int count = params.getInt("scatter.count", 5);
        int spreadX = params.getInt("scatter.spreadX", 16);
        int spreadY = params.getInt("scatter.spreadY", 0);
        int spreadZ = params.getInt("scatter.spreadZ", 16);
        Random rand = new Random(context.nodeSeed(0));

        Vec3DInt inputOrigin = context.origin;
        BlockMap result = new BlockMap();

        for (int placement = 0; placement < count; placement++) {
            int offsetX = spreadX > 0 ? rand.nextInt(spreadX * 2 + 1) - spreadX : 0;
            int offsetY = spreadY > 0 ? rand.nextInt(spreadY * 2 + 1) - spreadY : 0;
            int offsetZ = spreadZ > 0 ? rand.nextInt(spreadZ * 2 + 1) - spreadZ : 0;

            int shiftX = scatterOrigin.x() - inputOrigin.x() + offsetX;
            int shiftY = scatterOrigin.y() - inputOrigin.y() + offsetY;
            int shiftZ = scatterOrigin.z() - inputOrigin.z() + offsetZ;

            for (Map.Entry<Long, int[]> entry : input.entries().entrySet()) {
                Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
                Vec3DInt shifted = Vec3DInt.from(pos.x() + shiftX, pos.y() + shiftY, pos.z() + shiftZ);
                result.put(shifted, entry.getValue()[0], entry.getValue()[1]);
            }
        }
        outputs.set("blocks", result);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
