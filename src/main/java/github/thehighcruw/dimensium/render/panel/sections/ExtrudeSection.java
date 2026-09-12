/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.panel.sections;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.panel.ToolSection;
import github.thehighcruw.dimensium.tool.state.ExtrudeToolState;
import github.thehighcruw.dimensium.tool.state.ExtrudeToolState.ExtrudeMode;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class ExtrudeSection implements ToolSection {

    private final ExtrudeToolState state;
    private final ImInt modeIdx = new ImInt();
    private final int[] limit = new int[1];
    private final int[] count = new int[1];

    public ExtrudeSection(ExtrudeToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.extrude"));

        ExtrudeMode[] modes = ExtrudeMode.values();
        String[] modeLabels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) modeLabels[i] = I18n.format(modes[i].label);
        modeIdx.set(state.extrudeMode.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.extrude.mode") + "##ext_mode", modeIdx, modeLabels)) {
            state.extrudeMode = modes[modeIdx.get()];
        }

        count[0] = state.extrudeCount;
        if (ImGui.sliderInt(I18n.format("dimensium.ui.extrude.count") + "##ext_count", count, 1, 16)) {
            state.extrudeCount = count[0];
        }

        limit[0] = state.extrudeLimit;
        if (ImGui.sliderInt(I18n.format("dimensium.ui.extrude.limit") + "##ext_limit", limit, 1, 100000)) {
            state.extrudeLimit = limit[0];
        }

        ImBoolean displace = new ImBoolean(state.extrudeDisplace);
        if (ImGui.checkbox(I18n.format("dimensium.ui.extrude.displace") + "##ext_disp", displace)) {
            state.extrudeDisplace = displace.get();
        }

        ImBoolean corners = new ImBoolean(state.extrudeCorners);
        if (ImGui.checkbox(I18n.format("dimensium.ui.extrude.corners") + "##ext_corners", corners)) {
            state.extrudeCorners = corners.get();
        }
    }
}
