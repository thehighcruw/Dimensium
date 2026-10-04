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
import github.thehighcruw.dimensium.shared.util.BlockFamilyRegistry;
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
    private final ImBoolean typeReplace = new ImBoolean();
    private final PaletteState paletteState;
    private final GradientShape[] shapes = GradientShape.values();
    private final GradientInterp[] interps = GradientInterp.values();
    private final String[] shapeLabels;
    private final String[] interpLabels;
    private final String sectionLabel;
    private final String shapeComboLabel;
    private final String interpComboLabel;
    private final String maskSurfaceCheckLabel;
    private final String clampEdgeCheckLabel;
    private final String typeReplaceCheckLabel;
    private final String randomizeSeedButtonLabel;
    private final String clearPos1ButtonLabel;

    public GradientSection(GradientToolState state, BrushState bs, PaletteState ps) {
        this.state = state;
        this.paletteState = ps;
        this.paletteSection = new MultiPaletteSection(ps);
        this.brushSection = new BrushSection(bs);
        this.shapeLabels = new String[shapes.length];
        for (int i = 0; i < shapes.length; i++) shapeLabels[i] = I18n.format(shapes[i].label);
        this.interpLabels = new String[interps.length];
        for (int i = 0; i < interps.length; i++) interpLabels[i] = I18n.format(interps[i].label);
        this.sectionLabel = I18n.format("dimensium.ui.section.gradient");
        this.shapeComboLabel = I18n.format("dimensium.ui.gradient.shape") + "##grad_shape";
        this.interpComboLabel = I18n.format("dimensium.ui.gradient.interp") + "##grad_interp";
        this.maskSurfaceCheckLabel = I18n.format("dimensium.ui.gradient.mask_surface") + "##grad_mask";
        this.clampEdgeCheckLabel = I18n.format("dimensium.ui.gradient.clamp_edge") + "##grad_clamp";
        this.typeReplaceCheckLabel = I18n.format("dimensium.ui.paint.type_replace") + "##grad_type_replace";
        this.randomizeSeedButtonLabel = I18n.format("dimensium.ui.gradient.randomize_seed") + "##grad_seed";
        this.clearPos1ButtonLabel = I18n.format("dimensium.ui.gradient.clear_pos1") + "##grad_clear_pos1";
    }

    @Override
    public void render() {
        paletteSection.render();
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(sectionLabel);

        shapeIdx.set(state.gradientShape.ordinal());
        if (ImGui.combo(shapeComboLabel, shapeIdx, shapeLabels)) {
            state.gradientShape = shapes[shapeIdx.get()];
        }

        interpIdx.set(state.gradientInterp.ordinal());
        if (ImGui.combo(interpComboLabel, interpIdx, interpLabels)) {
            state.gradientInterp = interps[interpIdx.get()];
        }

        maskSurface.set(state.gradientMaskSurface);
        if (ImGui.checkbox(maskSurfaceCheckLabel, maskSurface)) {
            state.gradientMaskSurface = maskSurface.get();
        }

        clampToEdge.set(state.gradientClampToEdge);
        if (ImGui.checkbox(clampEdgeCheckLabel, clampToEdge)) {
            state.gradientClampToEdge = clampToEdge.get();
        }

        boolean canTypeReplace = BlockFamilyRegistry.allHaveFamily(paletteState.palette);
        if (canTypeReplace) {
            typeReplace.set(state.gradientTypeReplace);
            if (ImGui.checkbox(typeReplaceCheckLabel, typeReplace)) {
                state.gradientTypeReplace = typeReplace.get();
            }
        } else {
            state.gradientTypeReplace = false;
        }

        if (ImGui.button(randomizeSeedButtonLabel)) {
            state.gradientSeed = ThreadLocalRandom.current().nextLong();
        }

        if (state.gradientHasPos1) {
            if (ImGui.button(clearPos1ButtonLabel)) {
                state.gradientHasPos1 = false;
            }
        }
    }
}
