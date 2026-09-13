/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.tool.Tool;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.SelectionTransforms;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class SelectionWindow extends ToggleableWindow {

    public static final SelectionWindow INSTANCE = new SelectionWindow();

    private static final String WINDOW_ID = "###selection_window";

    private SelectionWindow() {}

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowSelectionOpen(value);
    }

    public void open() {
        setOpen(true);
    }

    public void renderImGui() {
        if (!open) return;

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui
            .begin(I18n.format("dimensium.ui.window.selection") + WINDOW_ID, pOpen, ImGuiWindowFlags.None);
        captureBounds();

        if (visible && pOpen.get()) {
            SelectionState sel = SelectionState.INSTANCE;
            boolean hasSel = sel.hasSelection();
            float w = ImGui.getContentRegionAvailX();

            if (!sel.pendingPos1 && !hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
            } else {
                if (sel.pendingPos1) {
                    ImGui.text(String.format("P1  %d, %d, %d", sel.pendingX, sel.pendingY, sel.pendingZ));
                }
                if (hasSel) {
                    ImGui.text(String.format("Blocks  %d", sel.size()));
                    ImGui.text(String.format("Size  %d × %d × %d", sel.width(), sel.height(), sel.depth()));
                }
            }

            ImGui.separator();

            ImGui.beginDisabled(!hasSel);

            if (ImGui.button(I18n.format("dimensium.select.clear"), w, 0)) {
                sel.clearSelection();
            }

            ImGui.separator();

            if (ImGui.button(I18n.format("dimensium.select.move"), w, 0)) {
                DimensiumEditorMode.INSTANCE.selectedTool = Tool.MOVE;
            }
            if (ImGui.button(I18n.format("dimensium.select.filter"), w, 0)) {
                FilterSelectionWindow.INSTANCE.open();
            }

            ImGui.separator();

            if (ImGui.button(I18n.format("dimensium.select.expand"), w, 0)) {
                sel.applyOp(SelectionTransforms.expand(sel.getSelectedBlocks(), 1), BooleanOp.REPLACE);
            }
            if (ImGui.button(I18n.format("dimensium.select.shrink"), w, 0)) {
                sel.applyOp(SelectionTransforms.shrink(sel.getSelectedBlocks(), 1), BooleanOp.REPLACE);
            }

            ImGui.separator();

            if (ImGui.button(I18n.format("dimensium.select.distort"), w, 0)) {
                DistortSelectionWindow.INSTANCE.open();
            }
            if (ImGui.button(I18n.format("dimensium.select.smooth"), w, 0)) {
                SmoothSelectionWindow.INSTANCE.open();
            }

            ImGui.separator();

            if (ImGui.button(I18n.format("dimensium.select.bounding_box"), w, 0)) {
                sel.applyOp(
                    SelectionState.aabbBlocks(sel.minX(), sel.minY(), sel.minZ(), sel.maxX(), sel.maxY(), sel.maxZ()),
                    BooleanOp.REPLACE);
            }
            if (ImGui.button(I18n.format("dimensium.select.convex_hull"), w, 0)) {
                sel.applyOp(SelectionTransforms.convexHull(sel.getSelectedBlocks()), BooleanOp.REPLACE);
            }

            ImGui.endDisabled();
        }

        ImGui.end();
        if (!pOpen.get()) setOpen(false);
    }
}
