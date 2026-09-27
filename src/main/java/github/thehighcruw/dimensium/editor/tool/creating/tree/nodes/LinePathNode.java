/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.nodes;

import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import github.thehighcruw.dimensium.editor.pipeline.Skeleton;
import github.thehighcruw.dimensium.editor.pipeline.SkeletonNode;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;

/** Generates a straight vertical skeleton chain — the simplest path generator. */
public class LinePathNode implements PipelineNode {

    public static final String ID = "line_path";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("line.height", 10, 2, 40, "dimensium.ui.pipeline.line_height")
            .floatParam("line.baseRadius", 1.5f, 0.2f, 6.0f, "dimensium.ui.pipeline.line_base_radius")
            .floatParam("line.taper", 0.6f, 0.0f, 1.0f, "dimensium.ui.pipeline.line_taper")
            .description("dimensium.ui.pipeline.node.line_path.desc")
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("skeleton", PortType.SKELETON);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt origin = inputs.get("origin", Vec3DInt.class);
        if (origin == null) origin = context.origin;

        int height = params.getInt("line.height", 10);
        float baseRadius = params.getFloat("line.baseRadius", 1.5f);
        float taper = params.getFloat("line.taper", 0.6f);

        SkeletonNode root = new SkeletonNode(origin, baseRadius);
        SkeletonNode current = root;
        for (int step = 1; step <= height; step++) {
            float radius = baseRadius * (taper + (1f - taper) * (1f - (float) step / height));
            SkeletonNode next = new SkeletonNode(origin.plus(0, step, 0), radius);
            current.children.add(next);
            current = next;
        }

        outputs.set("skeleton", new Skeleton(root));
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
