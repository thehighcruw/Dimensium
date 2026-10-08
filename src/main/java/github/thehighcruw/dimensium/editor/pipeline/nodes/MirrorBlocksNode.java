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

/**
 * Reflects a block map across an axis plane through the pipeline origin.
 * Optionally merges the reflection with the original, producing a symmetric result.
 */
public class MirrorBlocksNode implements PipelineNode {

    public static final String ID = "mirror_blocks";

    public static final int AXIS_X = 0;
    public static final int AXIS_Y = 1;
    public static final int AXIS_Z = 2;

    private static final NodeSchema SCHEMA = new NodeSchema()
            .enumParam(
                    "mirror.axis",
                    AXIS_X,
                    "dimensium.ui.pipeline.mirror_axis",
                    "X (left/right)",
                    "Y (up/down)",
                    "Z (forward/back)")
            .boolParam("mirror.keepOriginal", true, "dimensium.ui.pipeline.mirror_keep_original")
            .description("dimensium.ui.pipeline.node.mirror_blocks.desc")
            .inputPort("blocks", PortType.BLOCK_MAP)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap input = inputs.get("blocks", BlockMap.class);
        if (input == null) return;

        int axis = params.getInt("mirror.axis", AXIS_X);
        boolean keepOriginal = params.getBool("mirror.keepOriginal", true);

        Vec3DInt origin = context.origin;
        BlockMap result = new BlockMap();
        if (keepOriginal) result.merge(input);

        for (Map.Entry<Long, int[]> entry : input.entries().entrySet()) {
            Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
            int dx = pos.x() - origin.x();
            int dy = pos.y() - origin.y();
            int dz = pos.z() - origin.z();

            int mdx = axis == AXIS_X ? -dx : dx;
            int mdy = axis == AXIS_Y ? -dy : dy;
            int mdz = axis == AXIS_Z ? -dz : dz;

            Vec3DInt mirrored = Vec3DInt.from(origin.x() + mdx, origin.y() + mdy, origin.z() + mdz);
            result.put(mirrored, entry.getValue()[0], entry.getValue()[1]);
        }
        outputs.set("blocks", result);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
