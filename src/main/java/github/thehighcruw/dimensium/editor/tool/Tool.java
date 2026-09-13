/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool;

public enum Tool {

    POINTER("dimensium.tool.pointer"),
    SELECT("dimensium.tool.select"),
    MAGIC_SELECT("dimensium.tool.magic_select"),
    FREEHAND_SELECT("dimensium.tool.freehand_select"),
    LASSO_SELECT("dimensium.tool.lasso_select"),
    FREEHAND_DRAW("dimensium.tool.freehand_draw"),
    SCULPT_DRAW("dimensium.tool.sculpt_draw"),
    SHAPE("dimensium.tool.shape"),
    STAMP("dimensium.tool.stamp"),
    FILL("dimensium.tool.fill"),
    PAINTER("dimensium.tool.painter"),
    NOISE("dimensium.tool.noise"),
    ROCK("dimensium.tool.rock"),
    GRADIENT("dimensium.tool.gradient"),
    SMOOTH("dimensium.tool.smooth"),
    EXTRUDE("dimensium.tool.extrude"),
    MOVE("dimensium.tool.move"),
    PATH("dimensium.tool.path"),
    ELEVATION("dimensium.tool.elevation"),
    DISTORT("dimensium.tool.distort"),
    WELD("dimensium.tool.weld"),
    MELT("dimensium.tool.melt"),
    ROUGHEN("dimensium.tool.roughen"),
    SHATTER("dimensium.tool.shatter"),
    RULER("dimensium.tool.ruler"),
    MODELLING("dimensium.tool.modelling");

    public final String label;

    Tool(String label) {
        this.label = label;
    }
}
