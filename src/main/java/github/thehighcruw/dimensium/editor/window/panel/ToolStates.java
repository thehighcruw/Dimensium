/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.panel;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.creating.fill.FloodfillToolState;
import github.thehighcruw.dimensium.editor.tool.creating.freehand.FreehandToolState;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingToolState;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState;
import github.thehighcruw.dimensium.editor.tool.creating.rock.RockToolState;
import github.thehighcruw.dimensium.editor.tool.creating.sculpt.SculptToolState;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeToolState;
import github.thehighcruw.dimensium.editor.tool.creating.stamp.StampToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.distort.DistortToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.ElevationToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.extrude.ExtrudeToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.melt.MeltToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.roughen.RoughenToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.shatter.ShatterToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.smooth.SmoothToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.weld.WeldToolState;
import github.thehighcruw.dimensium.editor.tool.painting.gradient.GradientToolState;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseToolState;
import github.thehighcruw.dimensium.editor.tool.painting.painter.PainterToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.box.BoxSelectToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.lasso.LassoSelectToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.magic.MagicSelectToolState;
import github.thehighcruw.dimensium.editor.tool.state.PaletteState;
import github.thehighcruw.dimensium.editor.tool.utility.ruler.RulerToolState;

/**
 * Production wiring container. Holds all tool state instances. Sections receive state via constructor, not static refs.
 */
public final class ToolStates {

    public static final ToolStates INSTANCE = new ToolStates();

    public final BrushState brush = BrushState.INSTANCE;
    public final LassoSelectToolState lassoSelect = LassoSelectToolState.INSTANCE;
    public final PaletteState palette = PaletteState.INSTANCE;
    public final BoxSelectToolState select = BoxSelectToolState.INSTANCE;
    public final MagicSelectToolState magicSelect = MagicSelectToolState.INSTANCE;
    public final FreehandToolState freehand = FreehandToolState.INSTANCE;
    public final PainterToolState painter = PainterToolState.INSTANCE;
    public final SculptToolState sculpt = SculptToolState.INSTANCE;
    public final WeldToolState weld = WeldToolState.INSTANCE;
    public final MeltToolState melt = MeltToolState.INSTANCE;
    public final RoughenToolState roughen = RoughenToolState.INSTANCE;
    public final ShatterToolState shatter = ShatterToolState.INSTANCE;
    public final DistortToolState distort = DistortToolState.INSTANCE;
    public final GradientToolState gradient = GradientToolState.INSTANCE;
    public final NoiseToolState noise = NoiseToolState.INSTANCE;
    public final RockToolState rock = RockToolState.INSTANCE;
    public final SmoothToolState smooth = SmoothToolState.INSTANCE;
    public final ShapeToolState shape = ShapeToolState.INSTANCE;
    public final FloodfillToolState floodfill = FloodfillToolState.INSTANCE;
    public final ExtrudeToolState extrude = ExtrudeToolState.INSTANCE;
    public final ElevationToolState elevation = ElevationToolState.INSTANCE;
    public final ModellingToolState modelling = ModellingToolState.INSTANCE;
    public final PathToolState path = PathToolState.INSTANCE;
    public final RulerToolState ruler = RulerToolState.INSTANCE;
    public final StampToolState stamp = StampToolState.INSTANCE;

    private ToolStates() {}
}
