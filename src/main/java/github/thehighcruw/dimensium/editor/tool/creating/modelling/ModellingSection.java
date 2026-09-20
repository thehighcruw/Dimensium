/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.modelling;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingToolState.Mode;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingToolState.PasteMode;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class ModellingSection implements ToolSection {

    private final ModellingToolState state;
    private final ImInt modeIdx = new ImInt();
    private final ImInt pasteModeIdx = new ImInt();
    private final ImBoolean offsetTarget = new ImBoolean();

    public ModellingSection(ModellingToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        ImGui.dummy(0f, 3f);
        ImGui.separator();
        ImGui.dummy(0f, 2f);
        ImGui.text(I18n.format("dimensium.ui.section.modelling"));

        Mode[] modes = Mode.values();
        String[] modeLabels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) modeLabels[i] = I18n.format(modes[i].label);
        modeIdx.set(state.mode.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.modelling.mode") + "##mod_mode", modeIdx, modeLabels)) {
            state.mode = modes[modeIdx.get()];
        }

        PasteMode[] pasteModes = PasteMode.values();
        String[] pasteModeLabels = new String[pasteModes.length];
        for (int i = 0; i < pasteModes.length; i++) pasteModeLabels[i] = I18n.format(pasteModes[i].label);
        pasteModeIdx.set(state.pasteMode.ordinal());
        if (ImGui.combo(
                I18n.format("dimensium.ui.modelling.paste_mode") + "##mod_paste", pasteModeIdx, pasteModeLabels)) {
            state.pasteMode = pasteModes[pasteModeIdx.get()];
        }

        offsetTarget.set(state.offsetTargetPoint);
        if (ImGui.checkbox(I18n.format("dimensium.ui.modelling.offset_target") + "##mod_offset", offsetTarget)) {
            state.offsetTargetPoint = offsetTarget.get();
        }

        if (state.mode.usesRows()) {
            ImGui.dummy(0f, 3f);
            ImGui.separator();
            ImGui.dummy(0f, 2f);
            ImGui.text(I18n.format("dimensium.ui.modelling.rows"));
            ImGui.textDisabled(I18n.format("dimensium.ui.modelling.rows_hint"));
            for (int rowIndex = 0; rowIndex < state.rows.size(); rowIndex++) {
                boolean isActive = rowIndex == state.currentRowIndex;
                if (isActive) ImGui.pushStyleColor(ImGuiCol.Button, 0.20f, 0.50f, 0.20f, 1.0f);
                String rowLabel = I18n.format(
                        "dimensium.ui.modelling.row",
                        rowIndex + 1,
                        state.rows.get(rowIndex).size());
                if (ImGui.button(rowLabel + "##mod_row_" + rowIndex)) {
                    state.currentRowIndex = rowIndex;
                    state.selectedRow = rowIndex;
                    state.selectedPoint = -1;
                }
                if (isActive) ImGui.popStyleColor();
            }
            if (ImGui.button(I18n.format("dimensium.ui.modelling.add_row") + "##mod_add_row")) {
                state.addRow();
            }
        }

        ModellingToolState.ModelPoint selPt = state.selectedPointObj();
        if (selPt != null) {
            ImGui.dummy(0f, 3f);
            ImGui.separator();
            ImGui.dummy(0f, 2f);
            ImGui.text(I18n.format("dimensium.ui.modelling.selected_point"));
            ImGui.textDisabled(selPt.pos().x() + ", "
                    + selPt.pos().y()
                    + ", "
                    + selPt.pos().z());
            ImGui.pushStyleColor(ImGuiCol.Button, 0.65f, 0.10f, 0.10f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.80f, 0.20f, 0.20f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.50f, 0.05f, 0.05f, 1.0f);
            if (ImGui.button(I18n.format("dimensium.ui.modelling.remove_selected") + "##mod_rm_sel")) {
                state.removeSelectedPoint();
            }
            ImGui.popStyleColor(3);
        }
    }
}
