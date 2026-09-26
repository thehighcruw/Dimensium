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
import java.util.List;

/** Removes all blocks outside an axis-aligned ellipsoid centred above the pipeline origin. */
public class EllipsoidMaskNode implements PipelineNode {

    public static final String ID = "ellipsoid_mask";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("mask.radiusX", 8.0f, 1.0f, 30.0f, "dimensium.ui.pipeline.mask_radius_x")
            .floatParam("mask.radiusY", 6.0f, 1.0f, 30.0f, "dimensium.ui.pipeline.mask_radius_y")
            .floatParam("mask.radiusZ", 8.0f, 1.0f, 30.0f, "dimensium.ui.pipeline.mask_radius_z")
            .floatParam("mask.centerOffsetY", 12.0f, 0.0f, 50.0f, "dimensium.ui.pipeline.mask_center_offset_y")
            .description("dimensium.ui.pipeline.node.ellipsoid_mask.desc")
            .inputPort("blocks", PortType.BLOCK_MAP)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap blockMap = inputs.get("blocks", BlockMap.class);
        if (blockMap == null) return;

        float radiusX = Math.max(0.01f, params.getFloat("mask.radiusX", 8.0f));
        float radiusY = Math.max(0.01f, params.getFloat("mask.radiusY", 6.0f));
        float radiusZ = Math.max(0.01f, params.getFloat("mask.radiusZ", 8.0f));
        float centerOffsetY = params.getFloat("mask.centerOffsetY", 12.0f);

        float centerX = context.origin.x();
        float centerY = context.origin.y() + centerOffsetY;
        float centerZ = context.origin.z();

        List<Long> toRemove = new ArrayList<>();
        for (Long key : blockMap.entries().keySet()) {
            Vec3DInt pos = BlockMap.unpackKey(key);
            float dx = (pos.x() - centerX) / radiusX;
            float dy = (pos.y() - centerY) / radiusY;
            float dz = (pos.z() - centerZ) / radiusZ;
            if (dx * dx + dy * dy + dz * dz > 1.0f) {
                toRemove.add(key);
            }
        }

        for (Long key : toRemove) {
            blockMap.entries().remove(key);
        }

        outputs.set("blocks", blockMap);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
