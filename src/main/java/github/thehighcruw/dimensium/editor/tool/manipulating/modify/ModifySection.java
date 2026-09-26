/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.modify;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.manipulating.modify.ModifyToolState.ModifyMode;
import github.thehighcruw.dimensium.editor.tool.manipulating.modify.ModifyToolState.OffsetType;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import imgui.ImGui;
import imgui.type.ImInt;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class ModifySection implements ToolSection {

    private final ModifyToolState state;
    private final ImInt modeIdx = new ImInt();
    private final ImInt offsetTypeIdx = new ImInt();
    private final float[] offsetX = new float[1];
    private final float[] offsetY = new float[1];
    private final float[] offsetZ = new float[1];
    private final int[] count = new int[1];

    public ModifySection(ModifyToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.modify"));

        ModifyMode[] modes = ModifyMode.values();
        String[] modeLabels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) modeLabels[i] = I18n.format(modes[i].label);
        modeIdx.set(state.mode.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.modify.mode") + "##modify_mode", modeIdx, modeLabels)) {
            state.mode = modes[modeIdx.get()];
        }

        ImGui.spacing();

        switch (state.mode) {
            case TRANSLATE_COPIES:
                renderTranslateCopies();
                break;
        }
    }

    private void renderTranslateCopies() {
        SelectionState sel = SelectionState.INSTANCE;

        ImGui.text(I18n.format("dimensium.ui.section.translate_copies"));
        ImGui.separator();

        OffsetType[] offsetTypes = OffsetType.values();
        String[] offsetTypeLabels = new String[offsetTypes.length];
        for (int i = 0; i < offsetTypes.length; i++) offsetTypeLabels[i] = I18n.format(offsetTypes[i].label);
        offsetTypeIdx.set(state.translateCopiesOffsetType.ordinal());
        if (ImGui.combo(
                I18n.format("dimensium.ui.modify.offset_type") + "##modify_offset_type",
                offsetTypeIdx,
                offsetTypeLabels)) {
            state.translateCopiesOffsetType = offsetTypes[offsetTypeIdx.get()];
        }

        ImGui.spacing();

        offsetX[0] = state.translateCopiesOffset.x();
        if (ImGui.dragFloat(
                I18n.format("dimensium.ui.modify.offset_x") + "##modify_offset_x", offsetX, 0.1f, -256f, 256f)) {
            state.translateCopiesOffset =
                    Vec3DFloat.from(offsetX[0], state.translateCopiesOffset.y(), state.translateCopiesOffset.z());
        }

        offsetY[0] = state.translateCopiesOffset.y();
        if (ImGui.dragFloat(
                I18n.format("dimensium.ui.modify.offset_y") + "##modify_offset_y", offsetY, 0.1f, -256f, 256f)) {
            state.translateCopiesOffset =
                    Vec3DFloat.from(state.translateCopiesOffset.x(), offsetY[0], state.translateCopiesOffset.z());
        }

        offsetZ[0] = state.translateCopiesOffset.z();
        if (ImGui.dragFloat(
                I18n.format("dimensium.ui.modify.offset_z") + "##modify_offset_z", offsetZ, 0.1f, -256f, 256f)) {
            state.translateCopiesOffset =
                    Vec3DFloat.from(state.translateCopiesOffset.x(), state.translateCopiesOffset.y(), offsetZ[0]);
        }

        ImGui.spacing();

        count[0] = state.translateCopiesCount;
        if (ImGui.sliderInt(
                I18n.format("dimensium.ui.modify.count") + "##modify_count",
                count,
                ModifyToolState.COUNT_MIN,
                ModifyToolState.COUNT_MAX)) {
            state.translateCopiesCount = count[0];
        }

        ImGui.spacing();

        if (!sel.hasSelection()) {
            ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
        } else {
            ImGui.textDisabled(I18n.format("dimensium.ui.modify.hint"));
        }
    }
}
