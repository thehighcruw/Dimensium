/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.panel.sections;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.panel.ToolSection;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.PainterToolState;
import imgui.ImGui;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class PainterSection implements ToolSection {

    private final PainterToolState state;
    private final BrushSection brushSection;
    private final ImBoolean maskSurface = new ImBoolean();

    public PainterSection(PainterToolState state, BrushState bs) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.options"));

        maskSurface.set(state.painterMaskSurface);
        if (ImGui.checkbox(I18n.format("dimensium.ui.painter.mask_surface") + "##paint_mask", maskSurface)) {
            state.painterMaskSurface = maskSurface.get();
        }
    }
}
