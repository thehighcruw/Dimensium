/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.panel;

import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.DistortToolState;
import github.thehighcruw.dimensium.tool.state.ElevationToolState;
import github.thehighcruw.dimensium.tool.state.ExtrudeToolState;
import github.thehighcruw.dimensium.tool.state.FloodfillToolState;
import github.thehighcruw.dimensium.tool.state.FreehandToolState;
import github.thehighcruw.dimensium.tool.state.GradientToolState;
import github.thehighcruw.dimensium.tool.state.LassoSelectToolState;
import github.thehighcruw.dimensium.tool.state.MagicSelectToolState;
import github.thehighcruw.dimensium.tool.state.MeltToolState;
import github.thehighcruw.dimensium.tool.state.ModellingToolState;
import github.thehighcruw.dimensium.tool.state.NoiseToolState;
import github.thehighcruw.dimensium.tool.state.PainterToolState;
import github.thehighcruw.dimensium.tool.state.PaletteState;
import github.thehighcruw.dimensium.tool.state.PathToolState;
import github.thehighcruw.dimensium.tool.state.RockToolState;
import github.thehighcruw.dimensium.tool.state.RoughenToolState;
import github.thehighcruw.dimensium.tool.state.RulerToolState;
import github.thehighcruw.dimensium.tool.state.SculptToolState;
import github.thehighcruw.dimensium.tool.state.SelectToolState;
import github.thehighcruw.dimensium.tool.state.ShapeToolState;
import github.thehighcruw.dimensium.tool.state.ShatterToolState;
import github.thehighcruw.dimensium.tool.state.SmoothToolState;
import github.thehighcruw.dimensium.tool.state.StampToolState;
import github.thehighcruw.dimensium.tool.state.WeldToolState;

/**
 * Production wiring container. Holds all tool state instances. Sections receive state via constructor, not static refs.
 */
public final class ToolStates {

    public static final ToolStates INSTANCE = new ToolStates();

    public final BrushState brush = BrushState.INSTANCE;
    public final LassoSelectToolState lassoSelect = LassoSelectToolState.INSTANCE;
    public final PaletteState palette = PaletteState.INSTANCE;
    public final SelectToolState select = SelectToolState.INSTANCE;
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
