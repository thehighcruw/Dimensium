/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.nodes;

import github.thehighcruw.dimensium.editor.pipeline.Curve;
import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.ArrayList;
import java.util.List;

/** Generates a circle curve lying in the XZ plane centred at the pipeline origin. */
public class CircleCurveNode implements PipelineNode {

    public static final String ID = "circle_curve";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("circle.radius", 8.0f, 1.0f, 64.0f, "dimensium.ui.pipeline.circle_radius")
            .description("dimensium.ui.pipeline.node.circle_curve.desc")
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("curve", PortType.CURVE);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt center = inputs.get("origin", Vec3DInt.class);
        if (center == null) center = context.origin;

        float radius = params.getFloat("circle.radius", 8.0f);
        int segments = Math.max(8, (int) Math.ceil(2.0 * Math.PI * radius));

        List<Vec3DFloat> points = new ArrayList<>(segments);
        float cx = center.x();
        float cy = center.y();
        float cz = center.z();
        for (int index = 0; index < segments; index++) {
            double angle = 2.0 * Math.PI * index / segments;
            points.add(
                    Vec3DFloat.from(cx + radius * (float) Math.cos(angle), cy, cz + radius * (float) Math.sin(angle)));
        }
        outputs.set("curve", new Curve(points, true));
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
