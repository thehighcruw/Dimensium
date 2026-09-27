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

/**
 * Generates an arc — a partial curve through configurable angle sweep and orientation plane.
 * Useful for bridge arches, doorway frames, and partial ring shapes.
 */
public class ArcCurveNode implements PipelineNode {

    public static final String ID = "arc_curve";

    private static final int PLANE_XY = 1;
    private static final int PLANE_YZ = 2;

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("arc.radius", 8.0f, 1.0f, 64.0f, "dimensium.ui.pipeline.arc_radius")
            .floatParam("arc.startAngle", 0.0f, -180.0f, 180.0f, "dimensium.ui.pipeline.arc_start_angle")
            .floatParam("arc.sweepAngle", 180.0f, 1.0f, 360.0f, "dimensium.ui.pipeline.arc_sweep_angle")
            .enumParam(
                    "arc.plane",
                    PLANE_YZ,
                    "dimensium.ui.pipeline.arc_plane",
                    "Horizontal (XZ)",
                    "Vertical N–S (XY)",
                    "Vertical E–W (YZ)")
            .description("dimensium.ui.pipeline.node.arc_curve.desc")
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("curve", PortType.CURVE);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt center = inputs.get("origin", Vec3DInt.class);
        if (center == null) center = context.origin;

        float radius = params.getFloat("arc.radius", 8.0f);
        float startAngleDeg = params.getFloat("arc.startAngle", 0.0f);
        float sweepAngleDeg = params.getFloat("arc.sweepAngle", 180.0f);
        int plane = params.getInt("arc.plane", PLANE_YZ);

        double sweepRad = Math.toRadians(Math.abs(sweepAngleDeg));
        int segments = Math.max(4, (int) Math.ceil(radius * sweepRad));

        List<Vec3DFloat> points = new ArrayList<>(segments + 1);
        float cx = center.x();
        float cy = center.y();
        float cz = center.z();

        for (int index = 0; index <= segments; index++) {
            double angle = Math.toRadians(startAngleDeg) + sweepRad * index / segments;
            float cosA = (float) Math.cos(angle);
            float sinA = (float) Math.sin(angle);
            Vec3DFloat point;
            switch (plane) {
                case PLANE_XY:
                    point = Vec3DFloat.from(cx + radius * cosA, cy + radius * sinA, cz);
                    break;
                case PLANE_YZ:
                    point = Vec3DFloat.from(cx, cy + radius * sinA, cz + radius * cosA);
                    break;
                default: // PLANE_XZ
                    point = Vec3DFloat.from(cx + radius * cosA, cy, cz + radius * sinA);
                    break;
            }
            points.add(point);
        }

        boolean closed = Math.abs(sweepAngleDeg) >= 360.0f;
        outputs.set("curve", new Curve(points, closed));
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
