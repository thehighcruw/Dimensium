/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.utility.ruler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.utility.ruler.RulerToolState.Mode;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import imgui.ImGui;
import imgui.type.ImInt;
import java.util.List;
import net.minecraft.client.resources.I18n;

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
            Vec3DDouble delta = Vec3DDouble.from(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
            ImGui.text(I18n.format("dimensium.ui.ruler.distance") + ": " + String.format("%.2f", delta.length()));
            Vec3DDouble abs = delta.abs();
            ImGui.textDisabled("(" + (int) abs.x() + ", " + (int) abs.y() + ", " + (int) abs.z() + ")");
        } else {
            ImGui.textDisabled(I18n.format("dimensium.ui.ruler.hint"));
        }
    }
}
