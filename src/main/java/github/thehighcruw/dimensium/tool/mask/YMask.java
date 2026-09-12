/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.mask;

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
        switch (op) {
            case EQUAL:
                return y == value;
            case LESS:
                return y < value;
            case LESS_EQ:
                return y <= value;
            case GREATER:
                return y > value;
            case GREATER_EQ:
                return y >= value;
            default:
                return false;
        }
    }

    @Override
    public String displayName() {
        String sym;
        switch (op) {
            case EQUAL:
                sym = "=";
                break;
            case LESS:
                sym = "<";
                break;
            case LESS_EQ:
                sym = "<=";
                break;
            case GREATER:
                sym = ">";
                break;
            case GREATER_EQ:
                sym = ">=";
                break;
            default:
                sym = "?";
                break;
        }
        return "Y " + sym + " " + value;
    }
}
