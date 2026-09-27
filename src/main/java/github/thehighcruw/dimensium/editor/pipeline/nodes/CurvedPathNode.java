/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline.nodes;

import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import github.thehighcruw.dimensium.editor.pipeline.Skeleton;
import github.thehighcruw.dimensium.editor.pipeline.SkeletonNode;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;

/** Generates a leaning skeleton chain in a parameterised direction. */
public class CurvedPathNode implements PipelineNode {

    public static final String ID = "curved_path";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("curved.height", 10, 2, 40, "dimensium.ui.pipeline.curved_height")
            .floatParam("curved.baseRadius", 1.5f, 0.2f, 6.0f, "dimensium.ui.pipeline.curved_base_radius")
            .floatParam("curved.taper", 0.6f, 0.0f, 1.0f, "dimensium.ui.pipeline.curved_taper")
            .floatParam("curved.leanAngle", 20.0f, 0.0f, 80.0f, "dimensium.ui.pipeline.curved_lean_angle")
            .floatParam("curved.leanDirection", 0.0f, 0.0f, 360.0f, "dimensium.ui.pipeline.curved_lean_direction")
            .description("dimensium.ui.pipeline.node.curved_path.desc")
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("skeleton", PortType.SKELETON);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt origin = inputs.get("origin", Vec3DInt.class);
        if (origin == null) origin = context.origin;

        int height = params.getInt("curved.height", 10);
        float baseRadius = params.getFloat("curved.baseRadius", 1.5f);
        float taper = params.getFloat("curved.taper", 0.6f);
        float leanAngle = params.getFloat("curved.leanAngle", 20.0f);
        float leanDirection = params.getFloat("curved.leanDirection", 0.0f);

        float leanRad = (float) Math.toRadians(leanAngle);
        float dirRad = (float) Math.toRadians(leanDirection);
        Vec3DFloat stepDir =
                Vec3DFloat.from((float) (Math.sin(leanRad) * Math.cos(dirRad)), (float) Math.cos(leanRad), (float)
                        (Math.sin(leanRad) * Math.sin(dirRad)));

        SkeletonNode root = new SkeletonNode(origin, baseRadius);
        SkeletonNode current = root;
        Vec3DFloat pos = origin.toFloat();
        for (int step = 1; step <= height; step++) {
            pos = pos.plus(stepDir);
            float radius = baseRadius * (taper + (1f - taper) * (1f - (float) step / height));
            SkeletonNode next = new SkeletonNode(pos.round(), radius);
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
