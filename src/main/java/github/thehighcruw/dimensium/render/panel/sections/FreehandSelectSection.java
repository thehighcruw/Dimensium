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
import github.thehighcruw.dimensium.tool.state.FreehandToolState;
import imgui.ImGui;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class FreehandSelectSection implements ToolSection {

    private final FreehandToolState state;
    private final BrushSection brushSection;
    private final ImBoolean includeAir = new ImBoolean();

    public FreehandSelectSection(BrushState bs) {
        this.state = FreehandToolState.INSTANCE;
        this.brushSection = new BrushSection(bs);
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.options"));
        includeAir.set(state.includeAir);
        if (ImGui.checkbox(I18n.format("dimensium.ui.freehand.include_air") + "##fsel_air", includeAir)) {
            state.includeAir = includeAir.get();
        }
    }
}
