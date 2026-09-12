/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.handler.brushes.BrushInput;
import github.thehighcruw.dimensium.handler.brushes.ElevationBrushInput;
import github.thehighcruw.dimensium.handler.brushes.ExtrudeBrushInput;
import github.thehighcruw.dimensium.handler.brushes.FillBrushInput;
import github.thehighcruw.dimensium.handler.brushes.FreehandSelectBrushInput;
import github.thehighcruw.dimensium.handler.brushes.GradientBrushInput;
import github.thehighcruw.dimensium.handler.brushes.LassoBrushInput;
import github.thehighcruw.dimensium.handler.brushes.MagicSelectBrushInput;
import github.thehighcruw.dimensium.handler.brushes.ModellingBrushInput;
import github.thehighcruw.dimensium.handler.brushes.MoveBrushInput;
import github.thehighcruw.dimensium.handler.brushes.PaintBrushInput;
import github.thehighcruw.dimensium.handler.brushes.PathBrushInput;
import github.thehighcruw.dimensium.handler.brushes.RulerBrushInput;
import github.thehighcruw.dimensium.handler.brushes.SelectBrushInput;
import github.thehighcruw.dimensium.handler.brushes.ShapeBrushInput;
import github.thehighcruw.dimensium.handler.brushes.SmoothBrushInput;
import github.thehighcruw.dimensium.handler.brushes.StampBrushInput;
import github.thehighcruw.dimensium.render.brushes.BrushView;
import github.thehighcruw.dimensium.render.brushes.RulerBrushView;
import github.thehighcruw.dimensium.render.panel.ToolSection;
import github.thehighcruw.dimensium.render.panel.ToolStates;
import github.thehighcruw.dimensium.render.panel.sections.DistortSection;
import github.thehighcruw.dimensium.render.panel.sections.ElevationSection;
import github.thehighcruw.dimensium.render.panel.sections.ExtrudeSection;
import github.thehighcruw.dimensium.render.panel.sections.FillSection;
import github.thehighcruw.dimensium.render.panel.sections.FreehandSection;
import github.thehighcruw.dimensium.render.panel.sections.FreehandSelectSection;
import github.thehighcruw.dimensium.render.panel.sections.GradientSection;
import github.thehighcruw.dimensium.render.panel.sections.LassoSelectSection;
import github.thehighcruw.dimensium.render.panel.sections.MagicSelectSection;
import github.thehighcruw.dimensium.render.panel.sections.MeltSection;
import github.thehighcruw.dimensium.render.panel.sections.ModellingSection;
import github.thehighcruw.dimensium.render.panel.sections.MoveSection;
import github.thehighcruw.dimensium.render.panel.sections.NoiseSection;
import github.thehighcruw.dimensium.render.panel.sections.PainterSection;
import github.thehighcruw.dimensium.render.panel.sections.PathSection;
import github.thehighcruw.dimensium.render.panel.sections.RockSection;
import github.thehighcruw.dimensium.render.panel.sections.RoughenSection;
import github.thehighcruw.dimensium.render.panel.sections.RulerSection;
import github.thehighcruw.dimensium.render.panel.sections.SculptSection;
import github.thehighcruw.dimensium.render.panel.sections.SelectSection;
import github.thehighcruw.dimensium.render.panel.sections.ShapeSection;
import github.thehighcruw.dimensium.render.panel.sections.ShatterSection;
import github.thehighcruw.dimensium.render.panel.sections.SmoothSection;
import github.thehighcruw.dimensium.render.panel.sections.StampSection;
import github.thehighcruw.dimensium.render.panel.sections.WeldSection;
import github.thehighcruw.dimensium.tool.state.FreehandToolState;
import github.thehighcruw.dimensium.tool.state.PainterToolState;

/**
 * Central registry mapping each {@link Tool} to its {@link ToolDescriptor}.
 * Single source of truth for brush input, brush view, panel section, and accent color per tool.
 */
@SideOnly(Side.CLIENT)
public final class ToolRegistry {

    private static final Map<Tool, ToolDescriptor> REGISTRY = new EnumMap<>(Tool.class);

    private static final BrushView DEFAULT_VIEW = (mc, wx, wy, wz) -> mc.theWorld.getBlock(wx, wy, wz) != Blocks.air;

    private static final int[][] FACE_DIRS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    static {
        register(Tool.POINTER, null, 0f, 0f, 0f, null, null);

        register(Tool.SELECT, s -> new SelectSection(s.select), 0.20f, 0.85f, 0.75f, new SelectBrushInput(), null);

        register(
            Tool.MAGIC_SELECT,
            s -> new MagicSelectSection(s.magicSelect, s.select),
            0.75f,
            0.35f,
            1.00f,
            new MagicSelectBrushInput(),
            null);

        register(
            Tool.FREEHAND_SELECT,
            s -> new FreehandSelectSection(s.brush),
            0.20f,
            0.85f,
            0.75f,
            new FreehandSelectBrushInput(),
            DEFAULT_VIEW);

        register(
            Tool.LASSO_SELECT,
            s -> new LassoSelectSection(s.lassoSelect),
            0.20f,
            0.85f,
            0.75f,
            LassoBrushInput.INSTANCE,
            null);

        register(
            Tool.FREEHAND_DRAW,
            s -> new FreehandSection(s.freehand, s.brush),
            0.24f,
            0.50f,
            1.00f,
            PaintBrushInput.INSTANCE,
            (mc, wx, wy, wz) -> {
                if (!FreehandToolState.INSTANCE.freehandReplaceSolid && mc.theWorld.getBlock(wx, wy, wz) != Blocks.air)
                    return false;
                if (FreehandToolState.INSTANCE.freehandMaskSurface) return hasAirNeighbor(mc, wx, wy, wz);
                return true;
            });

        register(
            Tool.SCULPT_DRAW,
            s -> new SculptSection(s.sculpt, s.brush),
            0.08f,
            0.65f,
            0.80f,
            PaintBrushInput.INSTANCE,
            DEFAULT_VIEW);

        register(Tool.SHAPE, s -> new ShapeSection(s.shape), 0.28f, 0.78f, 0.30f, ShapeBrushInput.INSTANCE, null);

        register(
            Tool.STAMP,
            s -> new StampSection(s.stamp),
            0.90f,
            0.65f,
            0.20f,
            StampBrushInput.INSTANCE,
            DEFAULT_VIEW);

        register(Tool.FILL, s -> new FillSection(s.floodfill), 1.00f, 0.78f, 0.10f, FillBrushInput.INSTANCE, null);

        register(
            Tool.PAINTER,
            s -> new PainterSection(s.painter, s.brush),
            0.24f,
            0.50f,
            1.00f,
            PaintBrushInput.INSTANCE,
            (mc, wx, wy, wz) -> {
                if (mc.theWorld.getBlock(wx, wy, wz) == Blocks.air) return false;
                if (PainterToolState.INSTANCE.painterMaskSurface) return hasAirNeighbor(mc, wx, wy, wz);
                return true;
            });

        register(
            Tool.NOISE,
            s -> new NoiseSection(s.noise, s.brush, s.palette),
            1.00f,
            0.58f,
            0.20f,
            PaintBrushInput.INSTANCE,
            DEFAULT_VIEW);

        register(
            Tool.ROCK,
            s -> new RockSection(s.rock, s.brush),
            0.55f,
            0.42f,
            0.28f,
            PaintBrushInput.INSTANCE,
            DEFAULT_VIEW);

        register(
            Tool.GRADIENT,
            s -> new GradientSection(s.gradient, s.brush, s.palette),
            0.62f,
            0.28f,
            1.00f,
            GradientBrushInput.INSTANCE,
            DEFAULT_VIEW);

        register(
            Tool.SMOOTH,
            s -> new SmoothSection(s.smooth, s.brush),
            0.20f,
            0.78f,
            0.72f,
            SmoothBrushInput.INSTANCE,
            DEFAULT_VIEW);

        register(
            Tool.EXTRUDE,
            s -> new ExtrudeSection(s.extrude),
            1.00f,
            0.28f,
            0.68f,
            ExtrudeBrushInput.INSTANCE,
            null);

        register(Tool.MOVE, s -> new MoveSection(), 0.95f, 0.70f, 0.15f, MoveBrushInput.INSTANCE, null);

        register(Tool.PATH, s -> new PathSection(s.path), 0.55f, 0.85f, 1.00f, PathBrushInput.INSTANCE, null);

        register(
            Tool.ELEVATION,
            s -> new ElevationSection(s.elevation),
            0.40f,
            0.72f,
            0.30f,
            ElevationBrushInput.INSTANCE,
            null);

        register(
            Tool.DISTORT,
            s -> new DistortSection(s.distort, s.brush),
            0.85f,
            0.45f,
            0.90f,
            PaintBrushInput.INSTANCE,
            DEFAULT_VIEW);

        register(
            Tool.WELD,
            s -> new WeldSection(s.weld, s.brush),
            0.60f,
            0.80f,
            0.70f,
            PaintBrushInput.INSTANCE,
            DEFAULT_VIEW);

        register(
            Tool.MELT,
            s -> new MeltSection(s.melt, s.brush),
            0.05f,
            0.90f,
            0.65f,
            PaintBrushInput.INSTANCE,
            DEFAULT_VIEW);

        register(
            Tool.ROUGHEN,
            s -> new RoughenSection(s.roughen, s.brush),
            0.75f,
            0.55f,
            0.30f,
            PaintBrushInput.INSTANCE,
            DEFAULT_VIEW);

        register(
            Tool.SHATTER,
            s -> new ShatterSection(s.shatter, s.brush),
            0.60f,
            0.65f,
            0.75f,
            PaintBrushInput.INSTANCE,
            DEFAULT_VIEW);

        register(
            Tool.RULER,
            s -> new RulerSection(s.ruler),
            0.40f,
            0.85f,
            0.55f,
            new RulerBrushInput(),
            new RulerBrushView());

        register(
            Tool.MODELLING,
            s -> new ModellingSection(s.modelling),
            0.80f,
            0.55f,
            0.90f,
            ModellingBrushInput.INSTANCE,
            null);
    }

    private static void register(Tool tool, java.util.function.Function<ToolStates, ToolSection> sectionFactory,
        float r, float g, float b, BrushInput brushInput, BrushView brushView) {
        REGISTRY.put(tool, new Desc(sectionFactory, r, g, b, brushInput, brushView));
    }

    private static boolean hasAirNeighbor(Minecraft mc, int wx, int wy, int wz) {
        for (int[] n : FACE_DIRS) {
            if (mc.theWorld.getBlock(wx + n[0], wy + n[1], wz + n[2]) == Blocks.air) return true;
        }
        return false;
    }

    public static ToolDescriptor get(Tool tool) {
        return REGISTRY.get(tool);
    }

    public static BrushInput brushInput(Tool tool) {
        ToolDescriptor d = REGISTRY.get(tool);
        return d != null ? d.brushInput() : null;
    }

    public static boolean usesDragLoop(Tool tool) {
        BrushInput input = brushInput(tool);
        return input != null && input.usesDragLoop();
    }

    public static BrushView brushView(Tool tool) {
        ToolDescriptor d = REGISTRY.get(tool);
        return d != null ? d.brushView() : null;
    }

    public static boolean hasBrushView(Tool tool) {
        ToolDescriptor d = REGISTRY.get(tool);
        return d != null && d.brushView() != null;
    }

    public static ToolSection createSection(Tool tool, ToolStates states) {
        ToolDescriptor d = REGISTRY.get(tool);
        return d != null ? d.createSection(states) : null;
    }

    private ToolRegistry() {}

    private static final class Desc implements ToolDescriptor {

        private final java.util.function.Function<ToolStates, ToolSection> sectionFactory;
        private final float r, g, b;
        private final BrushInput brushInput;
        private final BrushView brushView;

        Desc(java.util.function.Function<ToolStates, ToolSection> sectionFactory, float r, float g, float b,
            BrushInput brushInput, BrushView brushView) {
            this.sectionFactory = sectionFactory;
            this.r = r;
            this.g = g;
            this.b = b;
            this.brushInput = brushInput;
            this.brushView = brushView;
        }

        @Override
        public ToolSection createSection(ToolStates states) {
            return sectionFactory != null ? sectionFactory.apply(states) : null;
        }

        @Override
        public float r() {
            return r;
        }

        @Override
        public float g() {
            return g;
        }

        @Override
        public float b() {
            return b;
        }

        @Override
        public BrushInput brushInput() {
            return brushInput;
        }

        @Override
        public BrushView brushView() {
            return brushView;
        }
    }
}
