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

/** Generates a helical curve rising from the pipeline origin — useful for spiral staircases and vines. */
public class HelixCurveNode implements PipelineNode {

    public static final String ID = "helix_curve";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("helix.radius", 4.0f, 0.5f, 32.0f, "dimensium.ui.pipeline.helix_radius")
            .intParam("helix.height", 16, 1, 128, "dimensium.ui.pipeline.helix_height")
            .floatParam("helix.turns", 2.0f, 0.25f, 16.0f, "dimensium.ui.pipeline.helix_turns")
            .description("dimensium.ui.pipeline.node.helix_curve.desc")
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("curve", PortType.CURVE);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt base = inputs.get("origin", Vec3DInt.class);
        if (base == null) base = context.origin;

        float radius = params.getFloat("helix.radius", 4.0f);
        int height = params.getInt("helix.height", 16);
        float turns = params.getFloat("helix.turns", 2.0f);

        double arcPerTurn = Math.sqrt(Math.pow(2.0 * Math.PI * radius, 2.0) + Math.pow((double) height / turns, 2.0));
        int totalSamples = Math.max(8, (int) Math.ceil(turns * arcPerTurn));
        List<Vec3DFloat> points = new ArrayList<>(totalSamples + 1);
        float bx = base.x();
        float by = base.y();
        float bz = base.z();
        for (int sample = 0; sample <= totalSamples; sample++) {
            float fraction = (float) sample / totalSamples;
            double angle = 2.0 * Math.PI * turns * fraction;
            float px = bx + radius * (float) Math.cos(angle);
            float py = by + height * fraction;
            float pz = bz + radius * (float) Math.sin(angle);
            points.add(Vec3DFloat.from(px, py, pz));
        }
        outputs.set("curve", new Curve(points, false));
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
