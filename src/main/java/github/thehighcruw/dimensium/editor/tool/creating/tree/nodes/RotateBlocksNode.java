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
import java.util.Map;
import net.minecraft.block.Block;

/**
 * Rotates a block map in 90-degree increments around the pipeline origin.
 * Rotation is clockwise when viewed from the positive end of the chosen axis.
 */
public class RotateBlocksNode implements PipelineNode {

    public static final String ID = "rotate_blocks";

    public static final int AXIS_Y = 0;
    public static final int AXIS_X = 1;
    public static final int AXIS_Z = 2;

    private static final NodeSchema SCHEMA = new NodeSchema()
            .enumParam("rot.axis", AXIS_Y, "dimensium.ui.pipeline.rotate_axis", "Y (yaw)", "X (pitch)", "Z (roll)")
            .enumParam("rot.turns", 0, "dimensium.ui.pipeline.rotate_turns", "90°", "180°", "270°")
            .description("dimensium.ui.pipeline.node.rotate_blocks.desc")
            .inputPort("blocks", PortType.BLOCK_MAP)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap input = inputs.get("blocks", BlockMap.class);
        if (input == null) return;

        int axis = params.getInt("rot.axis", AXIS_Y);
        int turns = params.getInt("rot.turns", 0) + 1; // enum index 0 = 1 turn = 90°

        Vec3DInt origin = context.origin;
        BlockMap result = new BlockMap();
        for (Map.Entry<Long, int[]> entry : input.entries().entrySet()) {
            Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
            int dx = pos.x() - origin.x();
            int dy = pos.y() - origin.y();
            int dz = pos.z() - origin.z();

            for (int turn = 0; turn < turns; turn++) {
                int ndx, ndy, ndz;
                if (axis == AXIS_Y) {
                    // Clockwise 90° looking down: (dx, dy, dz) -> (dz, dy, -dx)
                    ndx = dz;
                    ndy = dy;
                    ndz = -dx;
                } else if (axis == AXIS_X) {
                    // Clockwise 90° looking from +X: (dx, dy, dz) -> (dx, dz, -dy)
                    ndx = dx;
                    ndy = dz;
                    ndz = -dy;
                } else {
                    // Clockwise 90° looking from +Z: (dx, dy, dz) -> (dy, -dx, dz)
                    ndx = dy;
                    ndy = -dx;
                    ndz = dz;
                }
                dx = ndx;
                dy = ndy;
                dz = ndz;
            }

            Vec3DInt rotated = Vec3DInt.from(origin.x() + dx, origin.y() + dy, origin.z() + dz);
            result.put(rotated, Block.getBlockById(entry.getValue()[0]), entry.getValue()[1]);
        }
        outputs.set("blocks", result);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
