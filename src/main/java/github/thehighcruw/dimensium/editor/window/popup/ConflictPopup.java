/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.popup;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import imgui.ImGui;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

/**
 * Modal popup shown when undo/redo detects that the world has changed since the
 * history entry was recorded. Offers three choices: apply regardless, skip the
 * entry (move pointer without touching the world), or cancel.
 */
@SideOnly(Side.CLIENT)
public class ConflictPopup {

    public static final ConflictPopup INSTANCE = new ConflictPopup();

    private boolean pendingOpen = false;
    private boolean open = false;
    private String message = "";
    private Runnable onApply;
    private Runnable onSkip;

    // ── Open / close ──────────────────────────────────────────────────────────

    public void show(int mismatchCount, Runnable onApply, Runnable onSkip) {
        open = true;
        pendingOpen = true;
        message = mismatchCount == 1 ? I18n.format("dimensium.popup.conflict.message.single")
            : I18n.format("dimensium.popup.conflict.message.plural", mismatchCount);
        this.onApply = onApply;
        this.onSkip = onSkip;
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
        pendingOpen = false;
        onApply = null;
        onSkip = null;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void renderImGui() {
        if (!open) return;

        if (pendingOpen) {
            ImGui.openPopup("conflict_modal");
            pendingOpen = false;
        }

        ImBoolean pOpen = new ImBoolean(true);
        if (ImGui.beginPopupModal(
            I18n.format("dimensium.popup.conflict.title") + "###conflict_modal",
            pOpen,
            ImGuiWindowFlags.AlwaysAutoResize)) {

            if (ImGui.isKeyPressed(ImGuiKey.Escape)) {
                close();
                ImGui.closeCurrentPopup();
                ImGui.endPopup();
                return;
            }

            ImGui.text(message);
            ImGui.separator();

            if (ImGui.button(I18n.format("dimensium.popup.conflict.apply") + "##conflict_apply")) {
                Runnable cb = onApply;
                close();
                if (cb != null) cb.run();
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.format("dimensium.popup.conflict.skip") + "##conflict_skip")) {
                Runnable cb = onSkip;
                close();
                if (cb != null) cb.run();
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.format("dimensium.popup.conflict.cancel") + "##conflict_cancel")) {
                close();
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }

        // If user clicked outside (ImGui closed the popup), sync our state.
        if (!pOpen.get()) {
            close();
        }
    }
}
