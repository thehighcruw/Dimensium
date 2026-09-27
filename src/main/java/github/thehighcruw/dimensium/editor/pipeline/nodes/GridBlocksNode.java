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

/**
 * Places copies of the input block map at regular grid positions, centred at the pipeline origin.
 * Useful for columns, fences, window arrays, and other repeating structural elements.
 */
public class GridBlocksNode implements PipelineNode {

    public static final String ID = "grid_blocks";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("grid.columns", 3, 1, 16, "dimensium.ui.pipeline.grid_columns")
            .intParam("grid.rows", 3, 1, 16, "dimensium.ui.pipeline.grid_rows")
            .intParam("grid.spacingX", 8, 1, 64, "dimensium.ui.pipeline.grid_spacing_x")
            .intParam("grid.spacingZ", 8, 1, 64, "dimensium.ui.pipeline.grid_spacing_z")
            .description("dimensium.ui.pipeline.node.grid_blocks.desc")
            .inputPort("blocks", PortType.BLOCK_MAP)
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap input = inputs.get("blocks", BlockMap.class);
        if (input == null) return;

        Vec3DInt gridOrigin = inputs.get("origin", Vec3DInt.class);
        if (gridOrigin == null) gridOrigin = context.origin;

        int columns = params.getInt("grid.columns", 3);
        int rows = params.getInt("grid.rows", 3);
        int spacingX = params.getInt("grid.spacingX", 8);
        int spacingZ = params.getInt("grid.spacingZ", 8);

        Vec3DInt inputOrigin = context.origin;
        int baseShiftX = gridOrigin.x() - inputOrigin.x();
        int baseShiftZ = gridOrigin.z() - inputOrigin.z();
        int shiftY = gridOrigin.y() - inputOrigin.y();

        // Centre the grid on the origin
        float halfGridX = (columns - 1) * spacingX * 0.5f;
        float halfGridZ = (rows - 1) * spacingZ * 0.5f;

        BlockMap result = new BlockMap();
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                int shiftX = baseShiftX + Math.round(column * spacingX - halfGridX);
                int shiftZ = baseShiftZ + Math.round(row * spacingZ - halfGridZ);

                for (Map.Entry<Long, int[]> entry : input.entries().entrySet()) {
                    Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
                    Vec3DInt shifted = Vec3DInt.from(pos.x() + shiftX, pos.y() + shiftY, pos.z() + shiftZ);
                    result.put(shifted, Block.getBlockById(entry.getValue()[0]), entry.getValue()[1]);
                }
            }
        }
        outputs.set("blocks", result);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
