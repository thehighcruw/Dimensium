/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.panel.sections;

import java.util.List;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.panel.ToolSection;
import github.thehighcruw.dimensium.tool.state.RulerToolState;
import github.thehighcruw.dimensium.tool.state.RulerToolState.Mode;
import imgui.ImGui;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class RulerSection implements ToolSection {

    private final RulerToolState state;
    private final ImInt modeIdx = new ImInt();

    public RulerSection(RulerToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.ruler"));

        Mode[] modes = Mode.values();
        String[] modeLabels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) modeLabels[i] = I18n.format(modes[i].label);
        modeIdx.set(state.mode.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.ruler.mode") + "##ruler_mode", modeIdx, modeLabels)) {
            state.mode = modes[modeIdx.get()];
        }

        List<int[]> pts = state.points;
        if (pts.size() >= 2) {
            int[] a = pts.get(0);
            int[] b = pts.get(pts.size() - 1);
            double dx = b[0] - a[0];
            double dy = b[1] - a[1];
            double dz = b[2] - a[2];
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            ImGui.text(I18n.format("dimensium.ui.ruler.distance") + ": " + String.format("%.2f", dist));
            ImGui.textDisabled("(" + (int) Math.abs(dx) + ", " + (int) Math.abs(dy) + ", " + (int) Math.abs(dz) + ")");
        } else {
            ImGui.textDisabled(I18n.format("dimensium.ui.ruler.hint"));
        }
    }
}
