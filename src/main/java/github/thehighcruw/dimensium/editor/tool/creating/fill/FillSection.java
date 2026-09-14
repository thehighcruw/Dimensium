/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.fill;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.creating.fill.FloodfillToolState.FloodfillDir;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class FillSection implements ToolSection {

    private final FloodfillToolState state;
    private final int[] limit = new int[1];
    private final ImInt dirIdx = new ImInt();

    public FillSection(FloodfillToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.fill"));

        limit[0] = state.floodfillLimit;
        if (ImGui.sliderInt(I18n.format("dimensium.ui.fill.limit") + "##fill_limit", limit, 1, 100000)) {
            state.floodfillLimit = limit[0];
        }

        FloodfillDir[] dirs = FloodfillDir.values();
        String[] dirLabels = new String[dirs.length];
        for (int i = 0; i < dirs.length; i++) dirLabels[i] = I18n.format(dirs[i].label);
        dirIdx.set(state.floodfillDir.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.fill.direction") + "##fill_dir", dirIdx, dirLabels)) {
            state.floodfillDir = dirs[dirIdx.get()];
        }

        ImBoolean corners = new ImBoolean(state.floodfillCorners);
        if (ImGui.checkbox(I18n.format("dimensium.ui.fill.corners") + "##fill_corners", corners)) {
            state.floodfillCorners = corners.get();
        }
    }
}
