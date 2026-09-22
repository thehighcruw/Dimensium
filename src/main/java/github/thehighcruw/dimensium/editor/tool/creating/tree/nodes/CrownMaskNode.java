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
import java.util.List;

public class CrownMaskNode implements PipelineNode<BlockMap, BlockMap> {

    public static final String ID = "crown_mask";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("mask.radiusX", 8.0f, 1.0f, 30.0f, "dimensium.ui.tree.mask_radius_x")
            .floatParam("mask.radiusY", 6.0f, 1.0f, 30.0f, "dimensium.ui.tree.mask_radius_y")
            .floatParam("mask.radiusZ", 8.0f, 1.0f, 30.0f, "dimensium.ui.tree.mask_radius_z")
            .floatParam("mask.centerOffsetY", 12.0f, 0.0f, 50.0f, "dimensium.ui.tree.mask_center_offset_y")
            .boolParam("mask.onlyLeaves", true, "dimensium.ui.tree.mask_only_leaves");

    @Override
    public BlockMap apply(BlockMap blockMap, NodeParams params, PipelineContext context) {
        float radiusX = Math.max(0.01f, params.getFloat("mask.radiusX", 8.0f));
        float radiusY = Math.max(0.01f, params.getFloat("mask.radiusY", 6.0f));
        float radiusZ = Math.max(0.01f, params.getFloat("mask.radiusZ", 8.0f));
        float centerOffsetY = params.getFloat("mask.centerOffsetY", 12.0f);
        boolean onlyLeaves = params.getBool("mask.onlyLeaves", true);

        float centerX = context.origin.x();
        float centerY = context.origin.y() + centerOffsetY;
        float centerZ = context.origin.z();

        List<Long> toRemove = new ArrayList<>();
        for (Long key : blockMap.entries().keySet()) {
            int[] entry = blockMap.entries().get(key);
            if (onlyLeaves && !isLeaf(entry[0])) continue;

            Vec3DInt pos = ChangeProposal.unpackKey(key);
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

        return blockMap;
    }

    private static boolean isLeaf(int blockId) {
        return blockId == 18 || blockId == 161;
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
