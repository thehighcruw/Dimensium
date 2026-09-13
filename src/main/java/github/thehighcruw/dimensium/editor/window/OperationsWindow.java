/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.handler.EditorActions;
import github.thehighcruw.dimensium.editor.handler.SelectionOps;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.SelectionState;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class OperationsWindow extends ToggleableWindow {

    public static final OperationsWindow INSTANCE = new OperationsWindow();

    private static final String WINDOW_ID = "###operations_window";

    private OperationsWindow() {}

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowOperationsOpen(value);
    }

    public void open() {
        setOpen(true);
    }

    public void renderImGui() {
        if (!open) return;

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui
            .begin(I18n.format("dimensium.ui.window.operations") + WINDOW_ID, pOpen, ImGuiWindowFlags.None);
        captureBounds();

        if (visible && pOpen.get()) {
            boolean hasSel = SelectionState.INSTANCE.hasSelection();
            float w = ImGui.getContentRegionAvailX();

            if (!hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.separator();
            }

            ImGui.beginDisabled(!hasSel);

            if (ImGui.button(I18n.format("dimensium.ui.op.delete"), w, 0)) {
                SelectionState _sel = SelectionState.INSTANCE;
                BlockSender.sendChunked(SelectionOps.selectionToAirOps(_sel), I18n.format("dimensium.action.erase"));
            }
            if (ImGui.button(I18n.format("dimensium.ui.op.fill"), w, 0)) {
                SelectionState _sel = SelectionState.INSTANCE;
                SelectedBlockState sbs = SelectedBlockState.INSTANCE;
                int bid = net.minecraft.block.Block.getIdFromBlock(sbs.getPaintBlock());
                int meta = sbs.getPaintMeta();
                java.util.List<int[]> ops = new java.util.ArrayList<>(_sel.size());
                for (long key : _sel.getSelectedBlocks()) {
                    ops.add(
                        new int[] { SelectionState.unpackX(key), SelectionState.unpackY(key),
                            SelectionState.unpackZ(key), bid, meta });
                }
                BlockSender.sendChunked(ops, I18n.format("dimensium.action.fill"));
            }

            ImGui.separator();

            if (ImGui.button(I18n.format("dimensium.op.fill"), w, 0)) {
                FillSelectionWindow.INSTANCE.open();
            }
            if (ImGui.button(I18n.format("dimensium.op.fill_nearest"), w, 0)) {
                EditorActions.fillNearest();
            }
            if (ImGui.button(I18n.format("dimensium.op.replace"), w, 0)) {
                ReplaceSelectionWindow.INSTANCE.open();
            }
            if (ImGui.button(I18n.format("dimensium.op.type_replace"), w, 0)) {
                TypeReplaceSelectionWindow.INSTANCE.open();
            }

            ImGui.separator();

            if (ImGui.button(I18n.format("dimensium.op.autoshade"), w, 0)) {
                AutoshadeWindow.INSTANCE.open();
            }

            ImGui.separator();

            if (ImGui.button(I18n.format("dimensium.op.drain"), w, 0)) {
                EditorActions.drain();
            }
            if (ImGui.button(I18n.format("dimensium.op.simulate_gravity"), w, 0)) {
                EditorActions.simulateGravity();
            }
            if (ImGui.button(I18n.format("dimensium.op.trigger_updates"), w, 0)) {
                EditorActions.triggerUpdates();
            }

            ImGui.separator();

            if (ImGui.button(I18n.format("dimensium.op.hollow"), w, 0)) {
                EditorActions.hollow();
            }
            if (ImGui.button(I18n.format("dimensium.op.fill_gaps"), w, 0)) {
                EditorActions.fillGaps();
            }

            ImGui.separator();

            if (ImGui.button(I18n.format("dimensium.op.generate_colour_field"), w, 0)) {
                ColourFieldWindow.INSTANCE.open();
            }
            if (ImGui.button(I18n.format("dimensium.op.analyze"), w, 0)) {
                AnalyzeWindow.INSTANCE.open();
            }

            ImGui.endDisabled();
        }

        ImGui.end();
        if (!pOpen.get()) setOpen(false);
    }
}
