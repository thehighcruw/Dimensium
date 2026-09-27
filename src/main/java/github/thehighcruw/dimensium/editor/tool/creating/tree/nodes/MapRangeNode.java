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

/** Remaps a float from [inMin, inMax] to [outMin, outMax] with optional clamping. */
public class MapRangeNode implements PipelineNode {

    public static final String ID = "map_range";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("mr.inMin", 0.0f, -10.0f, 10.0f, "dimensium.ui.pipeline.mr_in_min")
            .floatParam("mr.inMax", 1.0f, -10.0f, 10.0f, "dimensium.ui.pipeline.mr_in_max")
            .floatParam("mr.outMin", 0.0f, -10.0f, 10.0f, "dimensium.ui.pipeline.mr_out_min")
            .floatParam("mr.outMax", 1.0f, -10.0f, 10.0f, "dimensium.ui.pipeline.mr_out_max")
            .boolParam("mr.clamp", true, "dimensium.ui.pipeline.mr_clamp")
            .description("dimensium.ui.pipeline.node.map_range.desc")
            .inputPort("value", PortType.FLOAT)
            .outputPort("value", PortType.FLOAT);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Float inputValue = inputs.get("value", Float.class);
        if (inputValue == null) return;

        float inMin = params.getFloat("mr.inMin", 0.0f);
        float inMax = params.getFloat("mr.inMax", 1.0f);
        float outMin = params.getFloat("mr.outMin", 0.0f);
        float outMax = params.getFloat("mr.outMax", 1.0f);
        boolean clamp = params.getBool("mr.clamp", true);

        float range = inMax - inMin;
        float normalized = range == 0f ? 0f : (inputValue - inMin) / range;
        if (clamp) normalized = Math.max(0f, Math.min(1f, normalized));
        outputs.set("value", outMin + normalized * (outMax - outMin));
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
