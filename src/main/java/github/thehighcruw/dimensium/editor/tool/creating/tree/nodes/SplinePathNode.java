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
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;

/**
 * Generates a skeleton chain that follows a cubic Bezier curve through four control points.
 * Control points p1, p2, p3 are specified as offsets from the origin; p0 = origin.
 */
public class SplinePathNode implements PipelineNode {

    public static final String ID = "spline_path";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("spline.p1x", 2.0f, -20.0f, 20.0f, "dimensium.ui.pipeline.spline_p1x")
            .floatParam("spline.p1y", 5.0f, -20.0f, 40.0f, "dimensium.ui.pipeline.spline_p1y")
            .floatParam("spline.p1z", 0.0f, -20.0f, 20.0f, "dimensium.ui.pipeline.spline_p1z")
            .floatParam("spline.p2x", -2.0f, -20.0f, 20.0f, "dimensium.ui.pipeline.spline_p2x")
            .floatParam("spline.p2y", 10.0f, -20.0f, 40.0f, "dimensium.ui.pipeline.spline_p2y")
            .floatParam("spline.p2z", 0.0f, -20.0f, 20.0f, "dimensium.ui.pipeline.spline_p2z")
            .floatParam("spline.p3x", 0.0f, -20.0f, 20.0f, "dimensium.ui.pipeline.spline_p3x")
            .floatParam("spline.p3y", 14.0f, -20.0f, 40.0f, "dimensium.ui.pipeline.spline_p3y")
            .floatParam("spline.p3z", 0.0f, -20.0f, 20.0f, "dimensium.ui.pipeline.spline_p3z")
            .intParam("spline.segments", 14, 3, 60, "dimensium.ui.pipeline.spline_segments")
            .floatParam("spline.baseRadius", 1.5f, 0.2f, 6.0f, "dimensium.ui.pipeline.spline_base_radius")
            .floatParam("spline.taper", 0.5f, 0.0f, 1.0f, "dimensium.ui.pipeline.spline_taper")
            .description("dimensium.ui.pipeline.node.spline_path.desc")
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("skeleton", PortType.SKELETON);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt originInt = inputs.get("origin", Vec3DInt.class);
        if (originInt == null) originInt = context.origin;

        Vec3DFloat origin = originInt.toFloat();
        Vec3DFloat p1 = origin.plus(Vec3DFloat.from(
                params.getFloat("spline.p1x", 2.0f),
                params.getFloat("spline.p1y", 5.0f),
                params.getFloat("spline.p1z", 0.0f)));
        Vec3DFloat p2 = origin.plus(Vec3DFloat.from(
                params.getFloat("spline.p2x", -2.0f),
                params.getFloat("spline.p2y", 10.0f),
                params.getFloat("spline.p2z", 0.0f)));
        Vec3DFloat p3 = origin.plus(Vec3DFloat.from(
                params.getFloat("spline.p3x", 0.0f),
                params.getFloat("spline.p3y", 14.0f),
                params.getFloat("spline.p3z", 0.0f)));

        int segments = params.getInt("spline.segments", 14);
        float baseRadius = params.getFloat("spline.baseRadius", 1.5f);
        float taper = params.getFloat("spline.taper", 0.5f);

        SkeletonNode root = new SkeletonNode(originInt, baseRadius);
        SkeletonNode current = root;

        for (int step = 1; step <= segments; step++) {
            float t = (float) step / segments;
            Vec3DFloat pos = cubicBezier(origin, p1, p2, p3, t);
            float radius = baseRadius * (taper + (1f - taper) * (1f - t));
            SkeletonNode next = new SkeletonNode(pos.round(), radius);
            current.children.add(next);
            current = next;
        }

        outputs.set("skeleton", new Skeleton(root));
    }

    private static Vec3DFloat cubicBezier(Vec3DFloat p0, Vec3DFloat p1, Vec3DFloat p2, Vec3DFloat p3, float t) {
        float u = 1f - t;
        return p0.times(u * u * u)
                .plus(p1.times(3f * u * u * t))
                .plus(p2.times(3f * u * t * t))
                .plus(p3.times(t * t * t));
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
