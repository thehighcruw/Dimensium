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

/** Generates an ellipse curve lying in the XZ plane centred at the pipeline origin. */
public class EllipseCurveNode implements PipelineNode {

    public static final String ID = "ellipse_curve";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("ellipse.radiusX", 10.0f, 1.0f, 64.0f, "dimensium.ui.pipeline.ellipse_radius_x")
            .floatParam("ellipse.radiusZ", 6.0f, 1.0f, 64.0f, "dimensium.ui.pipeline.ellipse_radius_z")
            .description("dimensium.ui.pipeline.node.ellipse_curve.desc")
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("curve", PortType.CURVE);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt center = inputs.get("origin", Vec3DInt.class);
        if (center == null) center = context.origin;

        float radiusX = params.getFloat("ellipse.radiusX", 10.0f);
        float radiusZ = params.getFloat("ellipse.radiusZ", 6.0f);
        int segments = Math.max(8, (int) Math.ceil(2.0 * Math.PI * Math.max(radiusX, radiusZ)));

        List<Vec3DFloat> points = new ArrayList<>(segments);
        float cx = center.x();
        float cy = center.y();
        float cz = center.z();
        for (int index = 0; index < segments; index++) {
            double angle = 2.0 * Math.PI * index / segments;
            points.add(Vec3DFloat.from(
                    cx + radiusX * (float) Math.cos(angle), cy, cz + radiusZ * (float) Math.sin(angle)));
        }
        outputs.set("curve", new Curve(points, true));
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
