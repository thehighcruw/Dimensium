/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.magic;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOpSection;
import github.thehighcruw.dimensium.editor.tool.selecting.box.BoxSelectToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.magic.MagicSelectToolState.MagicCompareType;
import github.thehighcruw.dimensium.editor.tool.selecting.magic.MagicSelectToolState.MagicDirection;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class MagicSelectSection implements ToolSection {

    private final MagicSelectToolState state;
    private final BoxSelectToolState selectState;
    private final BooleanOpSection booleanOpSection = new BooleanOpSection();
    private final ImInt compareIdx = new ImInt();
    private final ImInt dirIdx = new ImInt();
    private final int[] limit = new int[1];
    private final int[] range = new int[1];
    private final ImBoolean surface = new ImBoolean();
    private final ImBoolean corners = new ImBoolean();

    public MagicSelectSection(MagicSelectToolState state, BoxSelectToolState selectState) {
        this.state = state;
        this.selectState = selectState;
    }

    @Override
    public void render() {
        booleanOpSection.render(selectState);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.magic_select"));

        MagicCompareType[] compareTypes = MagicCompareType.values();
        String[] compareLabels = new String[compareTypes.length];
        for (int i = 0; i < compareTypes.length; i++) compareLabels[i] = I18n.format(compareTypes[i].label);
        compareIdx.set(state.magicCompareType.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.magic.compare") + "##magic_cmp", compareIdx, compareLabels)) {
            state.magicCompareType = compareTypes[compareIdx.get()];
        }

        MagicDirection[] dirs = MagicDirection.values();
        String[] dirLabels = new String[dirs.length];
        for (int i = 0; i < dirs.length; i++) dirLabels[i] = I18n.format(dirs[i].label);
        dirIdx.set(state.magicDirection.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.magic.direction") + "##magic_dir", dirIdx, dirLabels)) {
            state.magicDirection = dirs[dirIdx.get()];
        }

        limit[0] = state.magicSelectLimit;
        if (ImGui.sliderInt(I18n.format("dimensium.ui.magic.limit") + "##magic_limit", limit, 1, 100000)) {
            state.magicSelectLimit = limit[0];
        }

        range[0] = state.magicSelectRange;
        if (ImGui.sliderInt(I18n.format("dimensium.ui.magic.range") + "##magic_range", range, 1, 16)) {
            state.magicSelectRange = range[0];
        }

        surface.set(state.magicSelectSurface);
        if (ImGui.checkbox(I18n.format("dimensium.ui.magic.surface") + "##magic_surf", surface)) {
            state.magicSelectSurface = surface.get();
        }

        corners.set(state.magicSelectCorners);
        if (ImGui.checkbox(I18n.format("dimensium.ui.magic.corners") + "##magic_corners", corners)) {
            state.magicSelectCorners = corners.get();
        }
    }
}
