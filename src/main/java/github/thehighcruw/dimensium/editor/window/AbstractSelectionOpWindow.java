/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.shared.SelectionState;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
abstract class AbstractSelectionOpWindow extends ToggleableWindow {

    protected abstract String titleKey();

    protected abstract String windowId();

    /** Default window height in unscaled pixels (will be multiplied by UI scale). */
    protected abstract float windowHeight();

    /** ImGui child widget ID, e.g. "##smooth_body". */
    protected abstract String bodyId();

    /** Suffix for the apply button widget ID, e.g. "ssel". */
    protected abstract String applyId();

    /** Render sliders/controls inside the scrollable body region. */
    protected abstract void renderBody(float sliderW);

    /** Called when the user clicks Apply and selection is non-empty. */
    protected abstract void applyOp(SelectionState sel);

    public void close() {
        open = false;
    }

    public void renderImGui() {
        if (!open) return;

        float uiScale = ImGuiManager.INSTANCE.getUIScale();
        float w = 380f * uiScale;
        float vpW = ImGui.getIO().getDisplaySizeX(), vpH = ImGui.getIO().getDisplaySizeY();
        ImGui.setNextWindowPos((vpW - w) * 0.5f, vpH * 0.35f, ImGuiCond.Appearing);
        ImGui.setNextWindowSize(w, windowHeight() * uiScale, ImGuiCond.Appearing);

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui.begin(I18n.format(titleKey()) + windowId(), pOpen);
        captureBounds();

        if (visible) {
            boolean hasSel = SelectionState.INSTANCE.hasSelection();

            float windowW = ImGui.getWindowWidth();
            float btnW = 70f * uiScale;
            float childH = Math.max(0f, ImGui.getContentRegionAvailY() - footerButtonHeight());
            ImGui.beginChild(bodyId(), 0f, childH);

            if (!hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.separator();
            }

            float sliderW = ImGui.getContentRegionAvailX() * 0.6f;
            renderBody(sliderW);

            ImGui.endChild();

            ImGui.separator();
            ImGui.setCursorPosX(windowW - ImGui.getStyle().getWindowPaddingX() - btnW);
            if (!hasSel) ImGui.beginDisabled();
            if (ImGui.button(I18n.format("dimensium.select.apply") + "##" + applyId() + "_apply", btnW, 0)) {
                SelectionState sel = SelectionState.INSTANCE;
                if (sel.hasSelection()) applyOp(sel);
                close();
            }
            if (!hasSel) ImGui.endDisabled();
        }

        ImGui.end();
        if (!pOpen.get()) close();
    }
}
