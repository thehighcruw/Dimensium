/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.stamp;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.blueprint.Blueprint;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.window.popup.BlueprintBrowserPopup;
import github.thehighcruw.dimensium.shared.SelectionState;
import imgui.ImGui;
import imgui.type.ImBoolean;
import java.util.ArrayList;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class StampSection implements ToolSection {

    private final StampToolState state;
    private final BrushSection brushSection;
    private final float[] baseChance = new float[1];
    private final float[] minSpacing = new float[1];
    private final float[] entryChanBuf = new float[1];
    private final int[] entryOffY = new int[1];

    public StampSection(StampToolState state) {
        this.state = state;
        this.brushSection = new BrushSection(BrushState.INSTANCE);
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.text(I18n.format("dimensium.ui.section.stamp.blueprints"));
        ImGui.separator();

        int removeIdx = -1;
        for (int i = 0; i < state.blueprints.size(); i++) {
            StampEntry entry = state.blueprints.get(i);
            ImGui.pushID(i);

            String label =
                    entry.blueprint.name().isEmpty() ? I18n.format("dimensium.stamp.unnamed") : entry.blueprint.name();
            ImGui.text(label);
            ImGui.sameLine();
            if (ImGui.smallButton(I18n.format("dimensium.stamp.remove") + "##rm")) removeIdx = i;

            entryChanBuf[0] = entry.chance;
            if (ImGui.sliderFloat(
                    I18n.format("dimensium.stamp.entry.chance") + "##ec",
                    entryChanBuf,
                    StampToolState.ENTRY_CHANCE_MIN,
                    StampToolState.ENTRY_CHANCE_MAX)) {
                entry.chance = entryChanBuf[0];
            }

            entryOffY[0] = entry.offsetY;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.stamp.entry.offset_y") + "##ey",
                    entryOffY,
                    StampToolState.OFFSET_Y_MIN,
                    StampToolState.OFFSET_Y_MAX)) {
                entry.offsetY = entryOffY[0];
            }

            ImGui.popID();
            ImGui.spacing();
        }

        if (removeIdx >= 0) state.blueprints.remove(removeIdx);

        if (ImGui.button(I18n.format("dimensium.stamp.add_blueprint") + "##ab")) {
            BlueprintBrowserPopup.INSTANCE.open(bp -> state.blueprints.add(new StampEntry(bp)));
        }
        ImGui.sameLine();
        if (ImGui.button(I18n.format("dimensium.stamp.add_clipboard") + "##ac")) {
            addFromClipboard();
        }

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.stamp.options"));
        ImGui.separator();

        baseChance[0] = state.baseChance;
        if (ImGui.sliderFloat(
                I18n.format("dimensium.stamp.base_chance") + "##bc",
                baseChance,
                StampToolState.BASE_CHANCE_MIN,
                StampToolState.BASE_CHANCE_MAX)) {
            state.baseChance = baseChance[0];
        }

        minSpacing[0] = state.minSpacingPct;
        if (ImGui.sliderFloat(
                I18n.format("dimensium.stamp.min_spacing") + "##ms",
                minSpacing,
                StampToolState.MIN_SPACING_MIN,
                StampToolState.MIN_SPACING_MAX)) {
            state.minSpacingPct = minSpacing[0];
        }

        ImGui.spacing();

        ImBoolean cbYaw = new ImBoolean(state.randomYaw);
        if (ImGui.checkbox(I18n.format("dimensium.stamp.random_yaw") + "##ry", cbYaw)) state.randomYaw = cbYaw.get();

        ImBoolean cbFlipX = new ImBoolean(state.randomXFlip);
        if (ImGui.checkbox(I18n.format("dimensium.stamp.random_x_flip") + "##rx", cbFlipX))
            state.randomXFlip = cbFlipX.get();

        ImBoolean cbFlipZ = new ImBoolean(state.randomZFlip);
        if (ImGui.checkbox(I18n.format("dimensium.stamp.random_z_flip") + "##rz", cbFlipZ))
            state.randomZFlip = cbFlipZ.get();

        ImBoolean cbKeep = new ImBoolean(state.keepExisting);
        if (ImGui.checkbox(I18n.format("dimensium.stamp.keep_existing") + "##ke", cbKeep))
            state.keepExisting = cbKeep.get();
    }

    private void addFromClipboard() {
        SelectionState sel = SelectionState.INSTANCE;
        if (sel.clipboard == null || sel.clipboard.isEmpty()) return;

        Blueprint bp = Blueprint.fromClipboard(
                I18n.format("dimensium.stamp.clipboard_name"), new ArrayList<>(), sel.clipboard, sel.clipDim, null);
        state.blueprints.add(new StampEntry(bp));
    }
}
