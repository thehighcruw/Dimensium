/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler.brushes;

import java.util.EnumMap;
import java.util.Map;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.tool.Tool;

@SideOnly(Side.CLIENT)
public final class BrushInputRegistry {

    private static final Map<Tool, BrushInput> INPUTS = new EnumMap<>(Tool.class);

    static {
        INPUTS.put(Tool.STAMP, StampBrushInput.INSTANCE);
        INPUTS.put(Tool.SELECT, new SelectBrushInput());
        INPUTS.put(Tool.MAGIC_SELECT, new MagicSelectBrushInput());
        INPUTS.put(Tool.FREEHAND_SELECT, new FreehandSelectBrushInput());
        INPUTS.put(Tool.LASSO_SELECT, LassoBrushInput.INSTANCE);
        INPUTS.put(Tool.RULER, new RulerBrushInput());
        INPUTS.put(Tool.SHAPE, ShapeBrushInput.INSTANCE);
        INPUTS.put(Tool.FILL, FillBrushInput.INSTANCE);
        INPUTS.put(Tool.EXTRUDE, ExtrudeBrushInput.INSTANCE);
        INPUTS.put(Tool.MOVE, MoveBrushInput.INSTANCE);
        INPUTS.put(Tool.PATH, PathBrushInput.INSTANCE);
        INPUTS.put(Tool.MODELLING, ModellingBrushInput.INSTANCE);
        INPUTS.put(Tool.GRADIENT, GradientBrushInput.INSTANCE);
        INPUTS.put(Tool.SMOOTH, SmoothBrushInput.INSTANCE);
        INPUTS.put(Tool.ELEVATION, ElevationBrushInput.INSTANCE);

        for (Tool t : new Tool[] { Tool.FREEHAND_DRAW, Tool.PAINTER, Tool.NOISE, Tool.SCULPT_DRAW, Tool.ROCK, Tool.WELD,
            Tool.MELT, Tool.ROUGHEN, Tool.SHATTER, Tool.DISTORT }) {
            INPUTS.put(t, PaintBrushInput.INSTANCE);
        }
    }

    /** Returns the input handler for a tool, or null if the tool has no registered input. */
    public static BrushInput get(Tool tool) {
        return INPUTS.get(tool);
    }

    public static boolean usesDragLoop(Tool tool) {
        BrushInput input = INPUTS.get(tool);
        return input != null && input.usesDragLoop();
    }

    private BrushInputRegistry() {}
}
