/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.move;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.shared.SelectionState;
import imgui.ImGui;

@SideOnly(Side.CLIENT)
public class MoveSection implements ToolSection {

    public MoveSection() {}

    @Override
    public void render() {
        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.move"));
        if (!SelectionState.INSTANCE.hasSelection()) {
            ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
        } else {
            ImGui.textDisabled(I18n.format("dimensium.ui.move.hint"));
        }
    }
}
