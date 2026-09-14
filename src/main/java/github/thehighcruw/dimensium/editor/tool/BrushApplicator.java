/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool;

import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.creating.fill.FillBrush;
import github.thehighcruw.dimensium.editor.tool.creating.freehand.FreehandBrush;
import github.thehighcruw.dimensium.editor.tool.creating.rock.RockBrush;
import github.thehighcruw.dimensium.editor.tool.creating.sculpt.SculptBrush;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeBrush;
import github.thehighcruw.dimensium.editor.tool.manipulating.distort.DistortBrush;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.ElevationBrush;
import github.thehighcruw.dimensium.editor.tool.manipulating.melt.MeltBrush;
import github.thehighcruw.dimensium.editor.tool.manipulating.roughen.RoughenBrush;
import github.thehighcruw.dimensium.editor.tool.manipulating.shatter.ShatterBrush;
import github.thehighcruw.dimensium.editor.tool.manipulating.smooth.SmoothBrush;
import github.thehighcruw.dimensium.editor.tool.manipulating.weld.WeldBrush;
import github.thehighcruw.dimensium.editor.tool.painting.gradient.GradientBrush;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseBrush;
import github.thehighcruw.dimensium.editor.tool.painting.painter.PainterBrush;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

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
                blockX, blockY, blockZ, 1, Vec3.createVectorHelper(blockX + 0.5, blockY + 0.5, blockZ + 0.5));
        BrushStrategy strategy = BRUSHES.get(DimensiumEditorMode.INSTANCE.selectedTool);
        if (strategy != null) strategy.apply(world, mop);
    }
}
