/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.manipulating.smooth.SmoothToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiWindow;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.SelectionTransforms;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class SmoothSelectionWindow extends ImGuiWindow {

    public static final SmoothSelectionWindow INSTANCE = new SmoothSelectionWindow();

    private boolean open = false;

    private final int[] strength = { 2 };
    private final float[] threshold = { 0.5f };

    private static final String WINDOW_ID = "###smooth_selection_window";

    private SmoothSelectionWindow() {}

    public void open() {
        strength[0] = SmoothToolState.INSTANCE.smoothStrength;
        threshold[0] = 0.5f;
        open = true;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
    }

    public void renderImGui() {
        if (!open) return;

        float uiScale = ImGuiManager.INSTANCE.getUIScale();
        float w = 380f * uiScale;
        float vpW = ImGui.getIO()
            .getDisplaySizeX(),
            vpH = ImGui.getIO()
                .getDisplaySizeY();
        ImGui.setNextWindowPos((vpW - w) * 0.5f, vpH * 0.35f, ImGuiCond.Appearing);
        ImGui.setNextWindowSize(w, 200f * uiScale, ImGuiCond.Appearing);

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui.begin(I18n.format("dimensium.select.smooth.title") + WINDOW_ID, pOpen);
        captureBounds();

        if (visible) {
            boolean hasSel = SelectionState.INSTANCE.hasSelection();

            float windowW = ImGui.getWindowWidth();
            float btnW = 70f * uiScale;
            float footerH = ImGui.getStyle()
                .getItemSpacingY() + 1f
                + ImGui.getStyle()
                    .getItemSpacingY()
                + ImGui.getFrameHeight()
                + ImGui.getStyle()
                    .getWindowPaddingY();
            float childH = Math.max(0f, ImGui.getContentRegionAvailY() - footerH);
            ImGui.beginChild("##smooth_body", 0f, childH);

            if (!hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.separator();
            }

            float sliderW = ImGui.getContentRegionAvailX() * 0.6f;
            ImGui.setNextItemWidth(sliderW);
            ImGui.sliderInt(I18n.format("dimensium.ui.smooth.strength") + "##ssel_strength", strength, 1, 8);
            ImGui.setNextItemWidth(sliderW);
            ImGui.sliderFloat(
                I18n.format("dimensium.select.smooth.threshold") + "##ssel_threshold",
                threshold,
                0.01f,
                1f);

            ImGui.endChild();

            ImGui.separator();
            ImGui.setCursorPosX(
                windowW - ImGui.getStyle()
                    .getWindowPaddingX() - btnW);
            if (!hasSel) ImGui.beginDisabled();
            if (ImGui.button(I18n.format("dimensium.select.apply") + "##ssel_apply", btnW, 0)) {
                SelectionState sel = SelectionState.INSTANCE;
                if (sel.hasSelection()) {
                    sel.applyOp(
                        SelectionTransforms.smooth(sel.getSelectedBlocks(), strength[0], threshold[0]),
                        BooleanOp.REPLACE);
                }
                close();
            }
            if (!hasSel) ImGui.endDisabled();
        }

        ImGui.end();
        if (!pOpen.get()) close();
    }
}
