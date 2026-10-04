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
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import imgui.ImGui;
import imgui.type.ImInt;
import java.util.List;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class RulerSection implements ToolSection {

    private final RulerToolState state;
    private final ImInt modeIdx = new ImInt();
    private final Mode[] modes = Mode.values();
    private final String[] modeLabels;
    private final String sectionLabel;
    private final String modeComboLabel;
    private final String clearButtonLabel;
    private final String distanceLabel;
    private final String hintLabel;

    public RulerSection(RulerToolState state) {
        this.state = state;
        this.modeLabels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) modeLabels[i] = I18n.format(modes[i].label);
        this.sectionLabel = I18n.format("dimensium.ui.section.ruler");
        this.modeComboLabel = I18n.format("dimensium.ui.ruler.mode") + "##ruler_mode";
        this.clearButtonLabel = I18n.format("dimensium.ui.ruler.clear_points") + "##ruler_clear";
        this.distanceLabel = I18n.format("dimensium.ui.ruler.distance");
        this.hintLabel = I18n.format("dimensium.ui.ruler.hint");
    }

    @Override
    public void render() {
        ImGui.separator();
        ImGui.text(sectionLabel);

        modeIdx.set(state.mode.ordinal());
        if (ImGui.combo(modeComboLabel, modeIdx, modeLabels)) {
            state.mode = modes[modeIdx.get()];
        }

        List<Vec3DInt> pts = state.points;
        if (!pts.isEmpty()) {
            if (ImGui.button(clearButtonLabel)) {
                pts.clear();
            }
        }
        if (pts.size() >= 2) {
            Vec3DInt a = pts.get(0);
            Vec3DInt b = pts.get(pts.size() - 1);
            Vec3DDouble delta = b.minus(a).toDouble();
            ImGui.text(distanceLabel + ": " + String.format("%.2f", delta.length()));
            Vec3DDouble abs = delta.abs();
            ImGui.textDisabled("(" + (int) abs.x() + ", " + (int) abs.y() + ", " + (int) abs.z() + ")");
        } else {
            ImGui.textDisabled(hintLabel);
        }
    }
}
