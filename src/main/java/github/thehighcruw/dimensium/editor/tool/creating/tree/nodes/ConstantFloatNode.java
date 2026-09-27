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

/** Outputs a single constant float value. */
public class ConstantFloatNode implements PipelineNode {

    public static final String ID = "constant_float";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("cf.value", 1.0f, -1000.0f, 1000.0f, "dimensium.ui.pipeline.cf_value")
            .description("dimensium.ui.pipeline.node.constant_float.desc")
            .outputPort("value", PortType.FLOAT);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        outputs.set("value", params.getFloat("cf.value", 1.0f));
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
