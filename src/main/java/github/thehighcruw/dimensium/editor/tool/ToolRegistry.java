/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.creating.fill.FillBrushInput;
import github.thehighcruw.dimensium.editor.tool.creating.fill.FillSection;
import github.thehighcruw.dimensium.editor.tool.creating.freehand.FreehandSection;
import github.thehighcruw.dimensium.editor.tool.creating.freehand.FreehandToolState;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingBrushInput;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingSection;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingToolRenderer;
import github.thehighcruw.dimensium.editor.tool.creating.path.PathBrushInput;
import github.thehighcruw.dimensium.editor.tool.creating.path.PathSection;
import github.thehighcruw.dimensium.editor.tool.creating.path.PathToolRenderer;
import github.thehighcruw.dimensium.editor.tool.creating.rock.RockSection;
import github.thehighcruw.dimensium.editor.tool.creating.sculpt.SculptSection;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeBrushInput;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeSection;
import github.thehighcruw.dimensium.editor.tool.creating.stamp.StampBrushInput;
import github.thehighcruw.dimensium.editor.tool.creating.stamp.StampSection;
import github.thehighcruw.dimensium.editor.tool.manipulating.distort.DistortSection;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.ElevationBrushInput;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.ElevationSection;
import github.thehighcruw.dimensium.editor.tool.manipulating.extrude.ExtrudeBrushInput;
import github.thehighcruw.dimensium.editor.tool.manipulating.extrude.ExtrudeSection;
import github.thehighcruw.dimensium.editor.tool.manipulating.melt.MeltSection;
import github.thehighcruw.dimensium.editor.tool.manipulating.move.MoveBrushInput;
import github.thehighcruw.dimensium.editor.tool.manipulating.move.MoveSection;
import github.thehighcruw.dimensium.editor.tool.manipulating.roughen.RoughenSection;
import github.thehighcruw.dimensium.editor.tool.manipulating.shatter.ShatterSection;
import github.thehighcruw.dimensium.editor.tool.manipulating.smooth.SmoothBrushInput;
import github.thehighcruw.dimensium.editor.tool.manipulating.smooth.SmoothSection;
import github.thehighcruw.dimensium.editor.tool.manipulating.weld.WeldSection;
import github.thehighcruw.dimensium.editor.tool.painting.PaintBrushInput;
import github.thehighcruw.dimensium.editor.tool.painting.gradient.GradientBrushInput;
import github.thehighcruw.dimensium.editor.tool.painting.gradient.GradientSection;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseSection;
import github.thehighcruw.dimensium.editor.tool.painting.painter.PainterSection;
import github.thehighcruw.dimensium.editor.tool.painting.painter.PainterToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.box.BoxSelectBrushInput;
import github.thehighcruw.dimensium.editor.tool.selecting.box.BoxSelectSection;
import github.thehighcruw.dimensium.editor.tool.selecting.box.BoxSelectToolRenderer;
import github.thehighcruw.dimensium.editor.tool.selecting.freehand.FreehandSelectBrushInput;
import github.thehighcruw.dimensium.editor.tool.selecting.freehand.FreehandSelectSection;
import github.thehighcruw.dimensium.editor.tool.selecting.lasso.LassoBrushInput;
import github.thehighcruw.dimensium.editor.tool.selecting.lasso.LassoSelectSection;
import github.thehighcruw.dimensium.editor.tool.selecting.lasso.LassoSelectToolRenderer;
import github.thehighcruw.dimensium.editor.tool.selecting.magic.MagicSelectBrushInput;
import github.thehighcruw.dimensium.editor.tool.selecting.magic.MagicSelectSection;
import github.thehighcruw.dimensium.editor.tool.utility.ruler.RulerBrushInput;
import github.thehighcruw.dimensium.editor.tool.utility.ruler.RulerSection;
import github.thehighcruw.dimensium.editor.tool.utility.ruler.RulerToolRenderer;
import github.thehighcruw.dimensium.editor.window.panel.ToolSection;
import github.thehighcruw.dimensium.editor.window.panel.ToolStates;
import github.thehighcruw.dimensium.editor.window.viewport.world.BrushPreviewRenderer;

/**
 * Central registry mapping each {@link Tool} to its {@link ToolDescriptor}.
 * Single source of truth for brush input, brush view, panel section, and accent color per tool.
 */
@SideOnly(Side.CLIENT)
public final class ToolRegistry {

    private static final Map<Tool, ToolDescriptor> REGISTRY = new EnumMap<>(Tool.class);

    private static final int[][] FACE_DIRS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    static {
        register(Tool.POINTER, null, 0f, 0f, 0f, null, ToolRenderer.NONE);

        register(
            Tool.SELECT,
            s -> new BoxSelectSection(s.select),
            0.20f,
            0.85f,
            0.75f,
            new BoxSelectBrushInput(),
            BoxSelectToolRenderer.INSTANCE);

        register(
            Tool.MAGIC_SELECT,
            s -> new MagicSelectSection(s.magicSelect, s.select),
            0.75f,
            0.35f,
            1.00f,
            new MagicSelectBrushInput(),
            ToolRenderer.NONE);

        register(
            Tool.FREEHAND_SELECT,
            s -> new FreehandSelectSection(s.brush),
            0.20f,
            0.85f,
            0.75f,
            new FreehandSelectBrushInput(),
            ToolRenderer.DEFAULT_BRUSH);

        register(
            Tool.LASSO_SELECT,
            s -> new LassoSelectSection(s.lassoSelect),
            0.20f,
            0.85f,
            0.75f,
            LassoBrushInput.INSTANCE,
            LassoSelectToolRenderer.INSTANCE);

        register(
            Tool.FREEHAND_DRAW,
            s -> new FreehandSection(s.freehand, s.brush),
            0.24f,
            0.50f,
            1.00f,
            PaintBrushInput.INSTANCE,
            new ToolRenderer() {

                @Override
                public boolean isBlockAffected(Minecraft mc, int wx, int wy, int wz) {
                    if (!FreehandToolState.INSTANCE.freehandReplaceSolid
                        && mc.theWorld.getBlock(wx, wy, wz) != Blocks.air) return false;
                    if (FreehandToolState.INSTANCE.freehandMaskSurface) return hasAirNeighbor(mc, wx, wy, wz);
                    return true;
                }

                @Override
                public void renderWorldPreview(Minecraft mc, double rx, double ry, double rz) {
                    BrushPreviewRenderer.INSTANCE.render(this, mc, rx, ry, rz);
                }
            });

        register(
            Tool.SCULPT_DRAW,
            s -> new SculptSection(s.sculpt, s.brush),
            0.08f,
            0.65f,
            0.80f,
            PaintBrushInput.INSTANCE,
            ToolRenderer.DEFAULT_BRUSH);

        register(
            Tool.SHAPE,
            s -> new ShapeSection(s.shape),
            0.28f,
            0.78f,
            0.30f,
            ShapeBrushInput.INSTANCE,
            ToolRenderer.NONE);

        register(
            Tool.STAMP,
            s -> new StampSection(s.stamp),
            0.90f,
            0.65f,
            0.20f,
            StampBrushInput.INSTANCE,
            ToolRenderer.DEFAULT_BRUSH);

        register(
            Tool.FILL,
            s -> new FillSection(s.floodfill),
            1.00f,
            0.78f,
            0.10f,
            FillBrushInput.INSTANCE,
            ToolRenderer.NONE);

        register(
            Tool.PAINTER,
            s -> new PainterSection(s.painter, s.brush),
            0.24f,
            0.50f,
            1.00f,
            PaintBrushInput.INSTANCE,
            new ToolRenderer() {

                @Override
                public boolean isBlockAffected(Minecraft mc, int wx, int wy, int wz) {
                    if (mc.theWorld.getBlock(wx, wy, wz) == Blocks.air) return false;
                    if (PainterToolState.INSTANCE.painterMaskSurface) return hasAirNeighbor(mc, wx, wy, wz);
                    return true;
                }

                @Override
                public void renderWorldPreview(Minecraft mc, double rx, double ry, double rz) {
                    BrushPreviewRenderer.INSTANCE.render(this, mc, rx, ry, rz);
                }
            });

        register(
            Tool.NOISE,
            s -> new NoiseSection(s.noise, s.brush, s.palette),
            1.00f,
            0.58f,
            0.20f,
            PaintBrushInput.INSTANCE,
            ToolRenderer.DEFAULT_BRUSH);

        register(
            Tool.ROCK,
            s -> new RockSection(s.rock, s.brush),
            0.55f,
            0.42f,
            0.28f,
            PaintBrushInput.INSTANCE,
            ToolRenderer.DEFAULT_BRUSH);

        register(
            Tool.GRADIENT,
            s -> new GradientSection(s.gradient, s.brush, s.palette),
            0.62f,
            0.28f,
            1.00f,
            GradientBrushInput.INSTANCE,
            ToolRenderer.DEFAULT_BRUSH);

        register(
            Tool.SMOOTH,
            s -> new SmoothSection(s.smooth, s.brush),
            0.20f,
            0.78f,
            0.72f,
            SmoothBrushInput.INSTANCE,
            ToolRenderer.DEFAULT_BRUSH);

        register(
            Tool.EXTRUDE,
            s -> new ExtrudeSection(s.extrude),
            1.00f,
            0.28f,
            0.68f,
            ExtrudeBrushInput.INSTANCE,
            ToolRenderer.NONE);

        register(Tool.MOVE, s -> new MoveSection(), 0.95f, 0.70f, 0.15f, MoveBrushInput.INSTANCE, ToolRenderer.NONE);

        register(
            Tool.PATH,
            s -> new PathSection(s.path),
            0.55f,
            0.85f,
            1.00f,
            PathBrushInput.INSTANCE,
            PathToolRenderer.INSTANCE);

        register(
            Tool.ELEVATION,
            s -> new ElevationSection(s.elevation),
            0.40f,
            0.72f,
            0.30f,
            ElevationBrushInput.INSTANCE,
            ToolRenderer.NONE);

        register(
            Tool.DISTORT,
            s -> new DistortSection(s.distort, s.brush),
            0.85f,
            0.45f,
            0.90f,
            PaintBrushInput.INSTANCE,
            ToolRenderer.DEFAULT_BRUSH);

        register(
            Tool.WELD,
            s -> new WeldSection(s.weld, s.brush),
            0.60f,
            0.80f,
            0.70f,
            PaintBrushInput.INSTANCE,
            ToolRenderer.DEFAULT_BRUSH);

        register(
            Tool.MELT,
            s -> new MeltSection(s.melt, s.brush),
            0.05f,
            0.90f,
            0.65f,
            PaintBrushInput.INSTANCE,
            ToolRenderer.DEFAULT_BRUSH);

        register(
            Tool.ROUGHEN,
            s -> new RoughenSection(s.roughen, s.brush),
            0.75f,
            0.55f,
            0.30f,
            PaintBrushInput.INSTANCE,
            ToolRenderer.DEFAULT_BRUSH);

        register(
            Tool.SHATTER,
            s -> new ShatterSection(s.shatter, s.brush),
            0.60f,
            0.65f,
            0.75f,
            PaintBrushInput.INSTANCE,
            ToolRenderer.DEFAULT_BRUSH);

        register(
            Tool.RULER,
            s -> new RulerSection(s.ruler),
            0.40f,
            0.85f,
            0.55f,
            new RulerBrushInput(),
            new RulerToolRenderer());

        register(
            Tool.MODELLING,
            s -> new ModellingSection(s.modelling),
            0.80f,
            0.55f,
            0.90f,
            ModellingBrushInput.INSTANCE,
            ModellingToolRenderer.INSTANCE);
    }

    private static void register(Tool tool, java.util.function.Function<ToolStates, ToolSection> sectionFactory,
        float r, float g, float b, BrushInput brushInput, ToolRenderer toolRenderer) {
        REGISTRY.put(tool, new Desc(sectionFactory, r, g, b, brushInput, toolRenderer));
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

    public static ToolRenderer toolRenderer(Tool tool) {
        ToolDescriptor d = REGISTRY.get(tool);
        return d != null ? d.toolRenderer() : ToolRenderer.NONE;
    }

    public static boolean hasBrushPreview(Tool tool) {
        return toolRenderer(tool) != ToolRenderer.NONE;
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
        private final ToolRenderer toolRenderer;

        Desc(java.util.function.Function<ToolStates, ToolSection> sectionFactory, float r, float g, float b,
            BrushInput brushInput, ToolRenderer toolRenderer) {
            this.sectionFactory = sectionFactory;
            this.r = r;
            this.g = g;
            this.b = b;
            this.brushInput = brushInput;
            this.toolRenderer = toolRenderer;
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
        public ToolRenderer toolRenderer() {
            return toolRenderer;
        }
    }
}
