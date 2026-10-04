/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.painting.gradient;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.painting.MultiPaletteSection;
import github.thehighcruw.dimensium.editor.tool.painting.gradient.GradientToolState.GradientInterp;
import github.thehighcruw.dimensium.editor.tool.painting.gradient.GradientToolState.GradientShape;
import github.thehighcruw.dimensium.editor.tool.state.PaletteState;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class GradientSection implements ToolSection {

    private final GradientToolState state;
    private final MultiPaletteSection paletteSection;
    private final BrushSection brushSection;
    private final ImInt shapeIdx = new ImInt();
    private final ImInt interpIdx = new ImInt();
    private final ImBoolean maskSurface = new ImBoolean();
    private final ImBoolean clampToEdge = new ImBoolean();
    private final GradientShape[] shapes = GradientShape.values();
    private final GradientInterp[] interps = GradientInterp.values();
    private final String[] shapeLabels;
    private final String[] interpLabels;

    public GradientSection(GradientToolState state, BrushState bs, PaletteState ps) {
        this.state = state;
        this.paletteSection = new MultiPaletteSection(ps);
        this.brushSection = new BrushSection(bs);
        this.shapeLabels = new String[shapes.length];
        for (int i = 0; i < shapes.length; i++) shapeLabels[i] = I18n.format(shapes[i].label);
        this.interpLabels = new String[interps.length];
        for (int i = 0; i < interps.length; i++) interpLabels[i] = I18n.format(interps[i].label);
    }

    @Override
    public void render() {
        paletteSection.render();
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.gradient"));

        shapeIdx.set(state.gradientShape.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.gradient.shape") + "##grad_shape", shapeIdx, shapeLabels)) {
            state.gradientShape = shapes[shapeIdx.get()];
        }

        interpIdx.set(state.gradientInterp.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.gradient.interp") + "##grad_interp", interpIdx, interpLabels)) {
            state.gradientInterp = interps[interpIdx.get()];
        }

        maskSurface.set(state.gradientMaskSurface);
        if (ImGui.checkbox(I18n.format("dimensium.ui.gradient.mask_surface") + "##grad_mask", maskSurface)) {
            state.gradientMaskSurface = maskSurface.get();
        }

        clampToEdge.set(state.gradientClampToEdge);
        if (ImGui.checkbox(I18n.format("dimensium.ui.gradient.clamp_edge") + "##grad_clamp", clampToEdge)) {
            state.gradientClampToEdge = clampToEdge.get();
        }

        if (ImGui.button(I18n.format("dimensium.ui.gradient.randomize_seed") + "##grad_seed")) {
            state.gradientSeed = ThreadLocalRandom.current().nextLong();
        }

        if (state.gradientHasPos1) {
            if (ImGui.button(I18n.format("dimensium.ui.gradient.clear_pos1") + "##grad_clear_pos1")) {
                state.gradientHasPos1 = false;
            }
        }
    }
}
