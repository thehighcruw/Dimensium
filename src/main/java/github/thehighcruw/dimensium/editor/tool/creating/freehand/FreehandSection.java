/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.freehand;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import imgui.ImGui;
import imgui.type.ImBoolean;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class FreehandSection implements ToolSection {

    private final FreehandToolState state;
    private final BrushSection brushSection;
    private final ImBoolean maskSurface = new ImBoolean();
    private final ImBoolean replaceSolid = new ImBoolean();

    public FreehandSection(FreehandToolState state, BrushState bs) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.options"));

        maskSurface.set(state.freehandMaskSurface);
        if (ImGui.checkbox(I18n.format("dimensium.ui.freehand.mask_surface") + "##fh_mask", maskSurface)) {
            state.freehandMaskSurface = maskSurface.get();
        }

        replaceSolid.set(state.freehandReplaceSolid);
        if (ImGui.checkbox(I18n.format("dimensium.ui.freehand.replace_solid") + "##fh_replace", replaceSolid)) {
            state.freehandReplaceSolid = replaceSolid.get();
        }
    }
}
