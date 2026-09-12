/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.state;

public enum BooleanOp {

    ADD("dimensium.boolean_op.add"),
    SUBTRACT("dimensium.boolean_op.subtract"),
    REPLACE("dimensium.boolean_op.replace"),
    INTERSECT("dimensium.boolean_op.intersect");

    public final String label;

    BooleanOp(String label) {
        this.label = label;
    }

}
