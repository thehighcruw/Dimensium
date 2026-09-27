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
import java.util.Random;

/**
 * Outputs a seeded uniform-random float in [min, max]. Deterministic: same pipeline seed produces
 * the same value every execution.
 */
public class RandomFloatNode implements PipelineNode {

    public static final String ID = "random_float";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("rf.min", 0.0f, -1000.0f, 1000.0f, "dimensium.ui.pipeline.rf_min")
            .floatParam("rf.max", 1.0f, -1000.0f, 1000.0f, "dimensium.ui.pipeline.rf_max")
            .description("dimensium.ui.pipeline.node.random_float.desc")
            .outputPort("value", PortType.FLOAT);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        float min = params.getFloat("rf.min", 0.0f);
        float max = params.getFloat("rf.max", 1.0f);
        if (min >= max) {
            outputs.set("value", min);
            return;
        }
        float t = new Random(context.nodeSeed(0)).nextFloat();
        outputs.set("value", min + t * (max - min));
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
