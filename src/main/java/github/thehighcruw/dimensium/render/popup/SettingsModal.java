/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.popup;

import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.render.imgui.ImGuiManager;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class SettingsModal {

    public static final SettingsModal INSTANCE = new SettingsModal();

    private boolean open = false;
    private boolean pendingOpen = false;
    private boolean pendingClose = false;

    private int selectedCategory = 0;
    private final float[] pendingScale = { 1.0f };
    private final float[] pendingWorldScrollSpeed = { 1.0f };
    private final float[] pendingUiScrollSpeed = { 1.0f };

    // Index into KEYBIND_LABELS of the binding being captured, -1 = not capturing.
    private int capturingIndex = -1;

    private static final String[] KEYBIND_LABELS = { "dimensium.settings.keybind.tool_select",
        "dimensium.settings.keybind.tool_draw", "dimensium.settings.keybind.tool_noise",
        "dimensium.settings.keybind.tool_smooth", "dimensium.settings.keybind.tool_extrude",
        "dimensium.settings.keybind.undo", "dimensium.settings.keybind.redo", "dimensium.settings.keybind.copy",
        "dimensium.settings.keybind.cut", "dimensium.settings.keybind.paste", "dimensium.settings.keybind.fill",
        "dimensium.settings.keybind.erase", "dimensium.settings.keybind.confirm",
        "dimensium.settings.keybind.save_blueprint", "dimensium.settings.keybind.blueprint_browser",
        "dimensium.settings.keybind.settings", };

    public void open() {
        open = true;
        pendingOpen = true;
        pendingScale[0] = ImGuiManager.INSTANCE.getUIScale();
        pendingWorldScrollSpeed[0] = DimensiumConfig.worldScrollSpeedModifier;
        pendingUiScrollSpeed[0] = DimensiumConfig.uiScrollSpeedModifier;
        capturingIndex = -1;
    }

    public void close() {
        doClose();
    }

    private void doClose() {
        open = false;
        pendingOpen = false;
        pendingClose = false;
        capturingIndex = -1;
    }

    /** Close from outside ImGui frame — schedules closeCurrentPopup() for next render. */
    private void scheduleClose() {
        if (!open) return;
        pendingClose = true;
        pendingOpen = false;
        capturingIndex = -1;
    }

    public void toggle() {
        if (open) scheduleClose();
        else open();
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * Called from KeyHandler on every key-down event (before any other processing).
     * Returns true if the key was consumed by keybind capture.
     */
    public boolean captureKeybind(int key) {
        if (capturingIndex < 0) return false;
        // Escape cancels capture.
        if (key == Keyboard.KEY_ESCAPE) {
            capturingIndex = -1;
            return true;
        }
        // Modifier-only keys: wait for the actual key.
        if (isModifierKey(key)) return true;
        int mods = captureCurrentMods();
        applyKeybind(capturingIndex, key, mods);
        capturingIndex = -1;
        return true;
    }

    private boolean isModifierKey(int key) {
        return key == Keyboard.KEY_LCONTROL || key == Keyboard.KEY_RCONTROL
            || key == Keyboard.KEY_LSHIFT
            || key == Keyboard.KEY_RSHIFT
            || key == Keyboard.KEY_LMENU
            || key == Keyboard.KEY_RMENU
            || key == Keyboard.KEY_LMETA
            || key == Keyboard.KEY_RMETA;
    }

    private int captureCurrentMods() {
        int m = 0;
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
            m |= Dimensium.MOD_CTRL;
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
            m |= Dimensium.MOD_SHIFT;
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) m |= Dimensium.MOD_ALT;
        return m;
    }

    private void applyKeybind(int index, int key, int mods) {
        switch (index) {
            case 0:
                Dimensium.toolSelect.setKeyCode(key);
                Dimensium.toolSelectMods = mods;
                DimensiumConfig.keyToolSelect = key;
                DimensiumConfig.modsToolSelect = mods;
                break;
            case 1:
                Dimensium.toolDraw.setKeyCode(key);
                Dimensium.toolDrawMods = mods;
                DimensiumConfig.keyToolDraw = key;
                DimensiumConfig.modsToolDraw = mods;
                break;
            case 2:
                Dimensium.toolNoise.setKeyCode(key);
                Dimensium.toolNoiseMods = mods;
                DimensiumConfig.keyToolNoise = key;
                DimensiumConfig.modsToolNoise = mods;
                break;
            case 3:
                Dimensium.toolSmooth.setKeyCode(key);
                Dimensium.toolSmoothMods = mods;
                DimensiumConfig.keyToolSmooth = key;
                DimensiumConfig.modsToolSmooth = mods;
                break;
            case 4:
                Dimensium.toolExtrude.setKeyCode(key);
                Dimensium.toolExtrudeMods = mods;
                DimensiumConfig.keyToolExtrude = key;
                DimensiumConfig.modsToolExtrude = mods;
                break;
            case 5:
                Dimensium.actionUndo.setKeyCode(key);
                Dimensium.actionUndoMods = mods;
                DimensiumConfig.keyActionUndo = key;
                DimensiumConfig.modsActionUndo = mods;
                break;
            case 6:
                Dimensium.actionRedo.setKeyCode(key);
                Dimensium.actionRedoMods = mods;
                DimensiumConfig.keyActionRedo = key;
                DimensiumConfig.modsActionRedo = mods;
                break;
            case 7:
                Dimensium.actionCopy.setKeyCode(key);
                Dimensium.actionCopyMods = mods;
                DimensiumConfig.keyActionCopy = key;
                DimensiumConfig.modsActionCopy = mods;
                break;
            case 8:
                Dimensium.actionCut.setKeyCode(key);
                Dimensium.actionCutMods = mods;
                DimensiumConfig.keyActionCut = key;
                DimensiumConfig.modsActionCut = mods;
                break;
            case 9:
                Dimensium.actionPaste.setKeyCode(key);
                Dimensium.actionPasteMods = mods;
                DimensiumConfig.keyActionPaste = key;
                DimensiumConfig.modsActionPaste = mods;
                break;
            case 10:
                Dimensium.actionFill.setKeyCode(key);
                Dimensium.actionFillMods = mods;
                DimensiumConfig.keyActionFill = key;
                DimensiumConfig.modsActionFill = mods;
                break;
            case 11:
                Dimensium.actionErase.setKeyCode(key);
                Dimensium.actionEraseMods = mods;
                DimensiumConfig.keyActionErase = key;
                DimensiumConfig.modsActionErase = mods;
                break;
            case 12:
                Dimensium.actionConfirm.setKeyCode(key);
                Dimensium.actionConfirmMods = mods;
                DimensiumConfig.keyActionConfirm = key;
                DimensiumConfig.modsActionConfirm = mods;
                break;
            case 13:
                Dimensium.actionSaveBlueprint.setKeyCode(key);
                Dimensium.actionSaveBlueprintMods = mods;
                DimensiumConfig.keyActionSaveBlueprint = key;
                DimensiumConfig.modsActionSaveBlueprint = mods;
                break;
            case 14:
                Dimensium.actionBlueprintBrowser.setKeyCode(key);
                Dimensium.actionBlueprintBrowserMods = mods;
                DimensiumConfig.keyActionBlueprintBrowser = key;
                DimensiumConfig.modsActionBlueprintBrowser = mods;
                break;
            case 15:
                Dimensium.actionSettings.setKeyCode(key);
                Dimensium.actionSettingsMods = mods;
                DimensiumConfig.keyActionSettings = key;
                DimensiumConfig.modsActionSettings = mods;
                break;
            default:
                break;
        }
        try {
            ConfigurationManager.save(DimensiumConfig.class);
        } catch (Exception e) {
            Dimensium.logger.warn("Failed to save keybind config", e);
        }
    }

    private int getKeyCode(int index) {
        switch (index) {
            case 0:
                return Dimensium.toolSelect.getKeyCode();
            case 1:
                return Dimensium.toolDraw.getKeyCode();
            case 2:
                return Dimensium.toolNoise.getKeyCode();
            case 3:
                return Dimensium.toolSmooth.getKeyCode();
            case 4:
                return Dimensium.toolExtrude.getKeyCode();
            case 5:
                return Dimensium.actionUndo.getKeyCode();
            case 6:
                return Dimensium.actionRedo.getKeyCode();
            case 7:
                return Dimensium.actionCopy.getKeyCode();
            case 8:
                return Dimensium.actionCut.getKeyCode();
            case 9:
                return Dimensium.actionPaste.getKeyCode();
            case 10:
                return Dimensium.actionFill.getKeyCode();
            case 11:
                return Dimensium.actionErase.getKeyCode();
            case 12:
                return Dimensium.actionConfirm.getKeyCode();
            case 13:
                return Dimensium.actionSaveBlueprint.getKeyCode();
            case 14:
                return Dimensium.actionBlueprintBrowser.getKeyCode();
            case 15:
                return Dimensium.actionSettings.getKeyCode();
            default:
                return Keyboard.KEY_NONE;
        }
    }

    private int getMods(int index) {
        switch (index) {
            case 0:
                return Dimensium.toolSelectMods;
            case 1:
                return Dimensium.toolDrawMods;
            case 2:
                return Dimensium.toolNoiseMods;
            case 3:
                return Dimensium.toolSmoothMods;
            case 4:
                return Dimensium.toolExtrudeMods;
            case 5:
                return Dimensium.actionUndoMods;
            case 6:
                return Dimensium.actionRedoMods;
            case 7:
                return Dimensium.actionCopyMods;
            case 8:
                return Dimensium.actionCutMods;
            case 9:
                return Dimensium.actionPasteMods;
            case 10:
                return Dimensium.actionFillMods;
            case 11:
                return Dimensium.actionEraseMods;
            case 12:
                return Dimensium.actionConfirmMods;
            case 13:
                return Dimensium.actionSaveBlueprintMods;
            case 14:
                return Dimensium.actionBlueprintBrowserMods;
            case 15:
                return Dimensium.actionSettingsMods;
            default:
                return 0;
        }
    }

    private String comboString(int key, int mods) {
        StringBuilder sb = new StringBuilder();
        if ((mods & Dimensium.MOD_CTRL) != 0) sb.append("Ctrl+");
        if ((mods & Dimensium.MOD_SHIFT) != 0) sb.append("Shift+");
        if ((mods & Dimensium.MOD_ALT) != 0) sb.append("Alt+");
        sb.append(Keyboard.getKeyName(key));
        return sb.toString();
    }

    private static final String POPUP_ID = "settings_modal";

    public void renderImGui() {
        if (pendingOpen) {
            ImGui.openPopup(POPUP_ID);
            pendingOpen = false;
        }

        if (!open && !pendingClose) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        float modalW = 600f * scale;
        float modalH = 400f * scale;
        float vpW = ImGui.getIO()
            .getDisplaySizeX();
        float vpH = ImGui.getIO()
            .getDisplaySizeY();
        ImGui.setNextWindowPos((vpW - modalW) * 0.5f, (vpH - modalH) * 0.5f);
        ImGui.setNextWindowSize(modalW, modalH);

        ImBoolean pOpen = new ImBoolean(!pendingClose);
        if (ImGui.beginPopupModal(
            I18n.format("dimensium.settings.title") + "###" + POPUP_ID,
            pOpen,
            ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoScrollWithMouse)) {

            if (ImGui.isKeyPressed(ImGuiKey.Escape)) {
                doClose();
                ImGui.closeCurrentPopup();
                ImGui.endPopup();
                return;
            }

            float sidebarW = 140f * scale;
            float contentW = modalW - sidebarW - 16f * scale;
            float bottomBarH = ImGui.getFrameHeight() + 8f;
            float separatorH = 1f + ImGui.getStyle()
                .getItemSpacingY() * 2f;
            float innerH = ImGui.getContentRegionAvailY() - bottomBarH - separatorH;

            // ── Left: category list ───────────────────────────────────────────
            ImGui.beginChild("##settings_cats", sidebarW, innerH, true);
            String[] categories = { I18n.format("dimensium.settings.category.general"),
                I18n.format("dimensium.settings.category.keybinds"), };
            for (int i = 0; i < categories.length; i++) {
                if (ImGui.selectable(categories[i] + "##cat_" + i, selectedCategory == i)) {
                    selectedCategory = i;
                    capturingIndex = -1;
                }
            }
            ImGui.endChild();

            ImGui.sameLine();

            // ── Right: settings panel ─────────────────────────────────────────
            ImGui.beginChild("##settings_content", contentW, innerH, false);
            if (selectedCategory == 0) {
                renderGeneralSettings(contentW);
            } else if (selectedCategory == 1) {
                renderKeybindSettings(contentW);
            }
            ImGui.endChild();

            // ── Bottom bar — pinned to bottom ─────────────────────────────────
            ImGui.setCursorPosY(
                ImGui.getWindowHeight() - bottomBarH
                    - ImGui.getStyle()
                        .getWindowPaddingY());
            ImGui.separator();
            float applyW = 80f * scale;
            float closeW = 70f * scale;
            float gap = 6f * scale;
            boolean hasChanges = pendingScale[0] != ImGuiManager.INSTANCE.getUIScale()
                || pendingWorldScrollSpeed[0] != DimensiumConfig.worldScrollSpeedModifier
                || pendingUiScrollSpeed[0] != DimensiumConfig.uiScrollSpeedModifier;
            ImGui.setCursorPosX(modalW - applyW - closeW - gap - 16f * scale);
            if (hasChanges) {
                ImGui.pushStyleColor(ImGuiCol.Button, 0.18f, 0.42f, 0.90f, 1.00f);
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.26f, 0.52f, 1.00f, 1.00f);
                ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.14f, 0.34f, 0.76f, 1.00f);
            }
            if (ImGui.button(I18n.format("dimensium.settings.apply") + "##settings_apply", applyW, 0)) {
                ImGuiManager.INSTANCE.setUIScale(pendingScale[0]);
                DimensiumConfig.setWorldScrollSpeedModifier(pendingWorldScrollSpeed[0]);
                DimensiumConfig.setUiScrollSpeedModifier(pendingUiScrollSpeed[0]);
                try {
                    ConfigurationManager.save(DimensiumConfig.class);
                } catch (Exception e) {
                    Dimensium.logger.warn("Failed to save config", e);
                }
                doClose();
                ImGui.closeCurrentPopup();
            }
            if (hasChanges) ImGui.popStyleColor(3);
            ImGui.sameLine(0, gap);
            if (ImGui.button(I18n.format("dimensium.settings.close") + "##settings_close", closeW, 0)) {
                doClose();
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }

        if (!pOpen.get()) {
            doClose();
        }
    }

    private void renderGeneralSettings(float width) {
        ImGui.text(I18n.format("dimensium.settings.general.ui_scale"));
        ImGui.setNextItemWidth(width - 4f);
        ImGui.sliderFloat("##ui_scale_slider", pendingScale, 0.5f, 3.0f, "%.2f");

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.settings.general.world_scroll_speed"));
        ImGui.setNextItemWidth(width - 4f);
        ImGui.sliderFloat("##world_scroll_speed_slider", pendingWorldScrollSpeed, 0.1f, 10.0f, "%.1f");

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.settings.general.ui_scroll_speed"));
        ImGui.setNextItemWidth(width - 4f);
        ImGui.sliderFloat("##ui_scroll_speed_slider", pendingUiScrollSpeed, 0.1f, 10.0f, "%.1f");
    }

    private void renderKeybindSettings(float width) {
        if (capturingIndex >= 0) {
            ImGui.textDisabled(I18n.format("dimensium.settings.keybind.press_key"));
            ImGui.spacing();
        }

        float s = ImGuiManager.INSTANCE.getUIScale();
        float btnW = 140f * s;
        float labelW = width - btnW - 12f * s;

        for (int i = 0; i < KEYBIND_LABELS.length; i++) {
            ImGui.text(I18n.format(KEYBIND_LABELS[i]));
            ImGui.sameLine(labelW);

            boolean listening = capturingIndex == i;
            String btnLabel = (listening ? I18n.format("dimensium.settings.keybind.press_key_short")
                : comboString(getKeyCode(i), getMods(i))) + "##kb_" + i;

            if (listening) ImGui.pushStyleColor(ImGuiCol.Button, 0.6f, 0.2f, 0.2f, 1.0f);
            if (ImGui.button(btnLabel, btnW, 0)) {
                capturingIndex = listening ? -1 : i;
            }
            if (listening) ImGui.popStyleColor();
        }
    }
}
