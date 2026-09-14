/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.sculpt;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import imgui.ImGui;
import imgui.type.ImBoolean;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class SculptSection implements ToolSection {

    private final SculptToolState state;
    private final BrushSection brushSection;
    private final float[] strength = new float[1];
    private final ImBoolean invert = new ImBoolean();
    private final ImBoolean maskY = new ImBoolean();
    private final ImBoolean denoise = new ImBoolean();

    public SculptSection(SculptToolState state, BrushState bs) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.sculpt"));

        strength[0] = state.sculptStrength;
        if (ImGui.sliderFloat(I18n.format("dimensium.ui.sculpt.strength") + "##sculpt_str", strength, 0.1f, 5.0f)) {
            state.sculptStrength = strength[0];
        }

        invert.set(state.sculptInvert);
        if (ImGui.checkbox(I18n.format("dimensium.ui.sculpt.invert") + "##sculpt_inv", invert)) {
            state.sculptInvert = invert.get();
        }

        maskY.set(state.sculptMaskY);
        if (ImGui.checkbox(I18n.format("dimensium.ui.sculpt.mask_y") + "##sculpt_masky", maskY)) {
            state.sculptMaskY = maskY.get();
        }

        denoise.set(state.sculptDenoise);
        if (ImGui.checkbox(I18n.format("dimensium.ui.sculpt.denoise") + "##sculpt_denoise", denoise)) {
            state.sculptDenoise = denoise.get();
        }
    }
}
