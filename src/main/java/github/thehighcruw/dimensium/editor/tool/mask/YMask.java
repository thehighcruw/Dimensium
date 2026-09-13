/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import net.minecraft.world.World;

public class YMask extends MaskNode {

    public enum Op {
        EQUAL,
        LESS,
        LESS_EQ,
        GREATER,
        GREATER_EQ
    }

    public Op op;
    public int value;

    public YMask(Op op, int value) {
        this.op = op;
        this.value = value;
    }

    public Op nextOp() {
        Op[] vals = Op.values();
        return vals[(op.ordinal() + 1) % vals.length];
    }

    @Override
    public boolean test(World world, int x, int y, int z) {
        return switch (op) {
            case EQUAL -> y == value;
            case LESS -> y < value;
            case LESS_EQ -> y <= value;
            case GREATER -> y > value;
            case GREATER_EQ -> y >= value;
        };
    }

    @Override
    public String displayName() {
        String sym = switch (op) {
            case EQUAL -> "=";
            case LESS -> "<";
            case LESS_EQ -> "<=";
            case GREATER -> ">";
            case GREATER_EQ -> ">=";
        };
        return "Y " + sym + " " + value;
    }
}
