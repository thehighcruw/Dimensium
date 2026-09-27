/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.nodes;

import github.thehighcruw.dimensium.editor.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.pipeline.Curve;
import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.Map;
import net.minecraft.block.Block;

/**
 * Places copies of a block map centred at each Nth point along a curve.
 * Useful for colonnades, fence posts, and other evenly-spaced repeated elements.
 */
public class CurveScatterNode implements PipelineNode {

    public static final String ID = "curve_scatter";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("curveScatter.stride", 1, 1, 64, "dimensium.ui.pipeline.curve_scatter_stride")
            .description("dimensium.ui.pipeline.node.curve_scatter.desc")
            .inputPort("curve", PortType.CURVE)
            .inputPort("blocks", PortType.BLOCK_MAP)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Curve curve = inputs.get("curve", Curve.class);
        BlockMap input = inputs.get("blocks", BlockMap.class);
        if (curve == null || input == null || curve.points.isEmpty()) return;

        int stride = params.getInt("curveScatter.stride", 1);
        Vec3DInt inputOrigin = context.origin;
        BlockMap result = new BlockMap();

        for (int index = 0; index < curve.points.size(); index += stride) {
            Vec3DFloat point = curve.points.get(index);
            int shiftX = Math.round(point.x()) - inputOrigin.x();
            int shiftY = Math.round(point.y()) - inputOrigin.y();
            int shiftZ = Math.round(point.z()) - inputOrigin.z();

            for (Map.Entry<Long, int[]> entry : input.entries().entrySet()) {
                Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
                Vec3DInt shifted = Vec3DInt.from(pos.x() + shiftX, pos.y() + shiftY, pos.z() + shiftZ);
                result.put(shifted, Block.getBlockById(entry.getValue()[0]), entry.getValue()[1]);
            }
        }
        outputs.set("blocks", result);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
