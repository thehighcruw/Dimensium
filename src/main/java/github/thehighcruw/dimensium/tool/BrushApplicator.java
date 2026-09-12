/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.tool.brushes.DistortBrush;
import github.thehighcruw.dimensium.tool.brushes.ElevationBrush;
import github.thehighcruw.dimensium.tool.brushes.FillBrush;
import github.thehighcruw.dimensium.tool.brushes.FreehandBrush;
import github.thehighcruw.dimensium.tool.brushes.GradientBrush;
import github.thehighcruw.dimensium.tool.brushes.MeltBrush;
import github.thehighcruw.dimensium.tool.brushes.NoiseBrush;
import github.thehighcruw.dimensium.tool.brushes.PainterBrush;
import github.thehighcruw.dimensium.tool.brushes.RockBrush;
import github.thehighcruw.dimensium.tool.brushes.RoughenBrush;
import github.thehighcruw.dimensium.tool.brushes.SculptBrush;
import github.thehighcruw.dimensium.tool.brushes.ShapeBrush;
import github.thehighcruw.dimensium.tool.brushes.ShatterBrush;
import github.thehighcruw.dimensium.tool.brushes.SmoothBrush;
import github.thehighcruw.dimensium.tool.brushes.WeldBrush;

public class BrushApplicator {

    private static final Map<Tool, BrushStrategy> BRUSHES = new EnumMap<>(Tool.class);

    static {
        BRUSHES.put(Tool.PAINTER, new PainterBrush());
        BRUSHES.put(Tool.FREEHAND_DRAW, new FreehandBrush());
        BRUSHES.put(Tool.SCULPT_DRAW, new SculptBrush());
        BRUSHES.put(Tool.NOISE, new NoiseBrush());
        BRUSHES.put(Tool.ROCK, new RockBrush());
        BRUSHES.put(Tool.GRADIENT, new GradientBrush());
        BRUSHES.put(Tool.SMOOTH, new SmoothBrush());
        BRUSHES.put(Tool.WELD, new WeldBrush());
        BRUSHES.put(Tool.MELT, new MeltBrush());
        BRUSHES.put(Tool.SHAPE, new ShapeBrush());
        BRUSHES.put(Tool.FILL, new FillBrush());
        BRUSHES.put(Tool.ELEVATION, new ElevationBrush());
        BRUSHES.put(Tool.DISTORT, new DistortBrush());
        BRUSHES.put(Tool.ROUGHEN, new RoughenBrush());
        BRUSHES.put(Tool.SHATTER, new ShatterBrush());
    }

    public static void clearElevAccum() {
        ElevationBrush.clearAccum();
    }

    public static void applyTool(World world, int blockX, int blockY, int blockZ) {
        MovingObjectPosition mop = new MovingObjectPosition(
            blockX,
            blockY,
            blockZ,
            1,
            Vec3.createVectorHelper(blockX + 0.5, blockY + 0.5, blockZ + 0.5));
        BrushStrategy strategy = BRUSHES.get(DimensiumMode.INSTANCE.selectedTool);
        if (strategy != null) strategy.apply(world, mop);
    }
}
