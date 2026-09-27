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

/** Applies a binary math operation to two float inputs. Operation is selected by an integer param. */
public class MathNode implements PipelineNode {

    public static final String ID = "math";

    /** Operation indices — kept as constants so presets can reference them by name. */
    public static final int OP_ADD = 0;

    public static final int OP_SUBTRACT = 1;
    public static final int OP_MULTIPLY = 2;
    public static final int OP_DIVIDE = 3;
    public static final int OP_POW = 4;
    public static final int OP_MIN = 5;
    public static final int OP_MAX = 6;

    private static final NodeSchema SCHEMA = new NodeSchema()
            .enumParam(
                    "math.op",
                    OP_MULTIPLY,
                    "dimensium.ui.pipeline.math_op",
                    "Add",
                    "Subtract",
                    "Multiply",
                    "Divide",
                    "Power",
                    "Min",
                    "Max")
            .floatParamNoPort("math.valueB", 1.0f, -100.0f, 100.0f, "dimensium.ui.pipeline.math_value_b")
            .description("dimensium.ui.pipeline.node.math.desc")
            .inputPort("valueA", PortType.FLOAT)
            .optionalInputPort("valueB", PortType.FLOAT)
            .outputPort("value", PortType.FLOAT);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Float inputA = inputs.get("valueA", Float.class);
        if (inputA == null) return;

        Float inputB = inputs.get("valueB", Float.class);
        float valueB = inputB != null ? inputB : params.getFloat("math.valueB", 1.0f);
        int op = params.getInt("math.op", OP_MULTIPLY);

        float result;
        switch (op) {
            case OP_ADD:
                result = inputA + valueB;
                break;
            case OP_SUBTRACT:
                result = inputA - valueB;
                break;
            case OP_MULTIPLY:
                result = inputA * valueB;
                break;
            case OP_DIVIDE:
                result = valueB == 0f ? 0f : inputA / valueB;
                break;
            case OP_POW:
                result = (float) Math.pow(inputA, valueB);
                break;
            case OP_MIN:
                result = Math.min(inputA, valueB);
                break;
            case OP_MAX:
                result = Math.max(inputA, valueB);
                break;
            default:
                result = inputA;
        }
        outputs.set("value", result);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
