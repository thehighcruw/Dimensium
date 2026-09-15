/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.popup;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.handler.InputState;
import github.thehighcruw.dimensium.editor.tool.Tool;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.shared.util.UIUtils;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class SettingsModal {

    public static final SettingsModal INSTANCE = new SettingsModal();

    private boolean open = false;
    private boolean pendingOpen = false;
    private boolean pendingClose = false;

    private int selectedCategory = 0;
    private final float[] pendingScale = {1.0f};
    private final float[] pendingWorldScrollSpeed = {1.0f};
    private final float[] pendingUiScrollSpeed = {1.0f};
    private final float[] pendingRotationSnap = {1.0f};
    private final float[] pendingMovementSpeed = {1.0f};
    private final ImBoolean pendingOrbitUseCursor = new ImBoolean(true);
    private final float[] pendingShapeThreshold = {0.75f};

    // Tool keybind capture: which Tool is being re-bound, null = not capturing a tool bind.
    private Tool capturingTool = null;
    // Action keybind capture: index into ACTIONS, -1 = not capturing an action bind.
    private int capturingActionIndex = -1;

    @FunctionalInterface
    private interface ActionApplier {

        void apply(int key, int mods);
    }

    @Desugar
    private record ActionBind(String label, IntSupplier keyGetter, IntSupplier modsGetter, ActionApplier applier) {

        int getKey() {
            return keyGetter.getAsInt();
        }

        int getMods() {
            return modsGetter.getAsInt();
        }

        void apply(int key, int mods) {
            applier.apply(key, mods);
        }
    }

    // @formatter:off
    private static final List<ActionBind> ACTIONS = Arrays.asList(
            new ActionBind(
                    "dimensium.settings.keybind.undo",
                    Dimensium.actionUndo::getKeyCode,
                    () -> Dimensium.actionUndoMods,
                    (k, m) -> {
                        Dimensium.actionUndo.setKeyCode(k);
                        Dimensium.actionUndoMods = m;
                        DimensiumConfig.keyActionUndo = k;
                        DimensiumConfig.modsActionUndo = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.redo",
                    Dimensium.actionRedo::getKeyCode,
                    () -> Dimensium.actionRedoMods,
                    (k, m) -> {
                        Dimensium.actionRedo.setKeyCode(k);
                        Dimensium.actionRedoMods = m;
                        DimensiumConfig.keyActionRedo = k;
                        DimensiumConfig.modsActionRedo = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.copy",
                    Dimensium.actionCopy::getKeyCode,
                    () -> Dimensium.actionCopyMods,
                    (k, m) -> {
                        Dimensium.actionCopy.setKeyCode(k);
                        Dimensium.actionCopyMods = m;
                        DimensiumConfig.keyActionCopy = k;
                        DimensiumConfig.modsActionCopy = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.cut",
                    Dimensium.actionCut::getKeyCode,
                    () -> Dimensium.actionCutMods,
                    (k, m) -> {
                        Dimensium.actionCut.setKeyCode(k);
                        Dimensium.actionCutMods = m;
                        DimensiumConfig.keyActionCut = k;
                        DimensiumConfig.modsActionCut = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.paste",
                    Dimensium.actionPaste::getKeyCode,
                    () -> Dimensium.actionPasteMods,
                    (k, m) -> {
                        Dimensium.actionPaste.setKeyCode(k);
                        Dimensium.actionPasteMods = m;
                        DimensiumConfig.keyActionPaste = k;
                        DimensiumConfig.modsActionPaste = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.fill",
                    Dimensium.actionFill::getKeyCode,
                    () -> Dimensium.actionFillMods,
                    (k, m) -> {
                        Dimensium.actionFill.setKeyCode(k);
                        Dimensium.actionFillMods = m;
                        DimensiumConfig.keyActionFill = k;
                        DimensiumConfig.modsActionFill = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.erase",
                    Dimensium.actionErase::getKeyCode,
                    () -> Dimensium.actionEraseMods,
                    (k, m) -> {
                        Dimensium.actionErase.setKeyCode(k);
                        Dimensium.actionEraseMods = m;
                        DimensiumConfig.keyActionErase = k;
                        DimensiumConfig.modsActionErase = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.confirm",
                    Dimensium.actionConfirm::getKeyCode,
                    () -> Dimensium.actionConfirmMods,
                    (k, m) -> {
                        Dimensium.actionConfirm.setKeyCode(k);
                        Dimensium.actionConfirmMods = m;
                        DimensiumConfig.keyActionConfirm = k;
                        DimensiumConfig.modsActionConfirm = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.save_blueprint",
                    Dimensium.actionSaveBlueprint::getKeyCode,
                    () -> Dimensium.actionSaveBlueprintMods,
                    (k, m) -> {
                        Dimensium.actionSaveBlueprint.setKeyCode(k);
                        Dimensium.actionSaveBlueprintMods = m;
                        DimensiumConfig.keyActionSaveBlueprint = k;
                        DimensiumConfig.modsActionSaveBlueprint = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.blueprint_browser",
                    Dimensium.actionBlueprintBrowser::getKeyCode,
                    () -> Dimensium.actionBlueprintBrowserMods,
                    (k, m) -> {
                        Dimensium.actionBlueprintBrowser.setKeyCode(k);
                        Dimensium.actionBlueprintBrowserMods = m;
                        DimensiumConfig.keyActionBlueprintBrowser = k;
                        DimensiumConfig.modsActionBlueprintBrowser = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.settings",
                    Dimensium.actionSettings::getKeyCode,
                    () -> Dimensium.actionSettingsMods,
                    (k, m) -> {
                        Dimensium.actionSettings.setKeyCode(k);
                        Dimensium.actionSettingsMods = m;
                        DimensiumConfig.keyActionSettings = k;
                        DimensiumConfig.modsActionSettings = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.gizmo_nudge_forward",
                    Dimensium.gizmoNudgeForward::getKeyCode,
                    () -> Dimensium.gizmoNudgeForwardMods,
                    (k, m) -> {
                        Dimensium.gizmoNudgeForward.setKeyCode(k);
                        Dimensium.gizmoNudgeForwardMods = m;
                        DimensiumConfig.keyGizmoNudgeForward = k;
                        DimensiumConfig.modsGizmoNudgeForward = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.gizmo_nudge_backward",
                    Dimensium.gizmoNudgeBackward::getKeyCode,
                    () -> Dimensium.gizmoNudgeBackwardMods,
                    (k, m) -> {
                        Dimensium.gizmoNudgeBackward.setKeyCode(k);
                        Dimensium.gizmoNudgeBackwardMods = m;
                        DimensiumConfig.keyGizmoNudgeBackward = k;
                        DimensiumConfig.modsGizmoNudgeBackward = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.gizmo_nudge_right",
                    Dimensium.gizmoNudgeRight::getKeyCode,
                    () -> Dimensium.gizmoNudgeRightMods,
                    (k, m) -> {
                        Dimensium.gizmoNudgeRight.setKeyCode(k);
                        Dimensium.gizmoNudgeRightMods = m;
                        DimensiumConfig.keyGizmoNudgeRight = k;
                        DimensiumConfig.modsGizmoNudgeRight = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.gizmo_nudge_left",
                    Dimensium.gizmoNudgeLeft::getKeyCode,
                    () -> Dimensium.gizmoNudgeLeftMods,
                    (k, m) -> {
                        Dimensium.gizmoNudgeLeft.setKeyCode(k);
                        Dimensium.gizmoNudgeLeftMods = m;
                        DimensiumConfig.keyGizmoNudgeLeft = k;
                        DimensiumConfig.modsGizmoNudgeLeft = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.gizmo_nudge_up",
                    Dimensium.gizmoNudgeUp::getKeyCode,
                    () -> Dimensium.gizmoNudgeUpMods,
                    (k, m) -> {
                        Dimensium.gizmoNudgeUp.setKeyCode(k);
                        Dimensium.gizmoNudgeUpMods = m;
                        DimensiumConfig.keyGizmoNudgeUp = k;
                        DimensiumConfig.modsGizmoNudgeUp = m;
                    }),
            new ActionBind(
                    "dimensium.settings.keybind.gizmo_nudge_down",
                    Dimensium.gizmoNudgeDown::getKeyCode,
                    () -> Dimensium.gizmoNudgeDownMods,
                    (k, m) -> {
                        Dimensium.gizmoNudgeDown.setKeyCode(k);
                        Dimensium.gizmoNudgeDownMods = m;
                        DimensiumConfig.keyGizmoNudgeDown = k;
                        DimensiumConfig.modsGizmoNudgeDown = m;
                    }));
    // @formatter:on

    public void open() {
        open = true;
        pendingOpen = true;
        pendingScale[0] = ImGuiManager.INSTANCE.getUIScale();
        pendingWorldScrollSpeed[0] = DimensiumConfig.worldScrollSpeedModifier;
        pendingUiScrollSpeed[0] = DimensiumConfig.uiScrollSpeedModifier;
        pendingRotationSnap[0] = DimensiumConfig.rotationSnapDegrees;
        pendingMovementSpeed[0] = DimensiumConfig.movementSpeedMultiplier;
        pendingOrbitUseCursor.set(DimensiumConfig.orbitUseCursor);
        pendingShapeThreshold[0] = DimensiumConfig.shapeThreshold;
        capturingTool = null;
        capturingActionIndex = -1;
    }

    private void doClose() {
        open = false;
        pendingOpen = false;
        pendingClose = false;
        capturingTool = null;
        capturingActionIndex = -1;
    }

    /** Close from outside ImGui frame — schedules closeCurrentPopup() for next render. */
    private void scheduleClose() {
        if (!open) return;
        pendingClose = true;
        pendingOpen = false;
        capturingTool = null;
        capturingActionIndex = -1;
    }

    public void toggle() {
        if (open) scheduleClose();
        else open();
    }

    /**
     * Called from KeyHandler on every key-down event (before any other processing).
     * Returns true if the key was consumed by keybind capture.
     */
    public boolean captureKeybind(int key) {
        if (capturingTool == null && capturingActionIndex < 0) return false;
        // Escape cancels capture.
        if (key == Keyboard.KEY_ESCAPE) {
            capturingTool = null;
            capturingActionIndex = -1;
            return true;
        }
        // Modifier-only keys: wait for the actual key.
        if (isModifierKey(key)) return true;
        int mods = captureCurrentMods();
        if (capturingTool != null) {
            applyToolKeybind(capturingTool, key, mods);
            capturingTool = null;
        } else {
            ACTIONS.get(capturingActionIndex).apply(key, mods);
            saveConfig();
            capturingActionIndex = -1;
        }
        return true;
    }

    private boolean isModifierKey(int key) {
        return key == Keyboard.KEY_LCONTROL
                || key == Keyboard.KEY_RCONTROL
                || key == Keyboard.KEY_LSHIFT
                || key == Keyboard.KEY_RSHIFT
                || key == Keyboard.KEY_LMENU
                || key == Keyboard.KEY_RMENU
                || key == Keyboard.KEY_LMETA
                || key == Keyboard.KEY_RMETA;
    }

    private int captureCurrentMods() {
        return InputState.currentMods();
    }

    private void applyToolKeybind(Tool tool, int key, int mods) {
        Dimensium.toolKeybinds.get(tool).setKeyCode(key);
        Dimensium.toolKeybindMods.put(tool, mods);
        saveToolKeybindToConfig(tool, key, mods);
        saveConfig();
    }

    private void saveToolKeybindToConfig(Tool tool, int key, int mods) {
        switch (tool) {
            case POINTER:
                DimensiumConfig.keyToolPointer = key;
                DimensiumConfig.modsToolPointer = mods;
                break;
            case SELECT:
                DimensiumConfig.keyToolSelect = key;
                DimensiumConfig.modsToolSelect = mods;
                break;
            case MAGIC_SELECT:
                DimensiumConfig.keyToolMagicSelect = key;
                DimensiumConfig.modsToolMagicSelect = mods;
                break;
            case FREEHAND_SELECT:
                DimensiumConfig.keyToolFreehandSelect = key;
                DimensiumConfig.modsToolFreehandSelect = mods;
                break;
            case LASSO_SELECT:
                DimensiumConfig.keyToolLassoSelect = key;
                DimensiumConfig.modsToolLassoSelect = mods;
                break;
            case FREEHAND_DRAW:
                DimensiumConfig.keyToolDraw = key;
                DimensiumConfig.modsToolDraw = mods;
                break;
            case SCULPT_DRAW:
                DimensiumConfig.keyToolSculptDraw = key;
                DimensiumConfig.modsToolSculptDraw = mods;
                break;
            case SHAPE:
                DimensiumConfig.keyToolShape = key;
                DimensiumConfig.modsToolShape = mods;
                break;
            case STAMP:
                DimensiumConfig.keyToolStamp = key;
                DimensiumConfig.modsToolStamp = mods;
                break;
            case FILL:
                DimensiumConfig.keyToolFill = key;
                DimensiumConfig.modsToolFill = mods;
                break;
            case PAINTER:
                DimensiumConfig.keyToolPainter = key;
                DimensiumConfig.modsToolPainter = mods;
                break;
            case NOISE:
                DimensiumConfig.keyToolNoise = key;
                DimensiumConfig.modsToolNoise = mods;
                break;
            case ROCK:
                DimensiumConfig.keyToolRock = key;
                DimensiumConfig.modsToolRock = mods;
                break;
            case GRADIENT:
                DimensiumConfig.keyToolGradient = key;
                DimensiumConfig.modsToolGradient = mods;
                break;
            case SMOOTH:
                DimensiumConfig.keyToolSmooth = key;
                DimensiumConfig.modsToolSmooth = mods;
                break;
            case EXTRUDE:
                DimensiumConfig.keyToolExtrude = key;
                DimensiumConfig.modsToolExtrude = mods;
                break;
            case MOVE:
                DimensiumConfig.keyToolMove = key;
                DimensiumConfig.modsToolMove = mods;
                break;
            case PATH:
                DimensiumConfig.keyToolPath = key;
                DimensiumConfig.modsToolPath = mods;
                break;
            case ELEVATION:
                DimensiumConfig.keyToolElevation = key;
                DimensiumConfig.modsToolElevation = mods;
                break;
            case DISTORT:
                DimensiumConfig.keyToolDistort = key;
                DimensiumConfig.modsToolDistort = mods;
                break;
            case WELD:
                DimensiumConfig.keyToolWeld = key;
                DimensiumConfig.modsToolWeld = mods;
                break;
            case MELT:
                DimensiumConfig.keyToolMelt = key;
                DimensiumConfig.modsToolMelt = mods;
                break;
            case ROUGHEN:
                DimensiumConfig.keyToolRoughen = key;
                DimensiumConfig.modsToolRoughen = mods;
                break;
            case SHATTER:
                DimensiumConfig.keyToolShatter = key;
                DimensiumConfig.modsToolShatter = mods;
                break;
            case RULER:
                DimensiumConfig.keyToolRuler = key;
                DimensiumConfig.modsToolRuler = mods;
                break;
            case MODELLING:
                DimensiumConfig.keyToolModelling = key;
                DimensiumConfig.modsToolModelling = mods;
                break;
            default:
                break;
        }
    }

    private void saveConfig() {
        try {
            ConfigurationManager.save(DimensiumConfig.class);
        } catch (Exception e) {
            Dimensium.logger.warn("Failed to save keybind config", e);
        }
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
        float vpW = ImGui.getIO().getDisplaySizeX();
        float vpH = ImGui.getIO().getDisplaySizeY();
        ImGui.setNextWindowPos((vpW - modalW) * 0.5f, (vpH - modalH) * 0.5f);
        ImGui.setNextWindowSize(modalW, modalH);

        ImBoolean pOpen = new ImBoolean(!pendingClose);
        if (ImGui.beginPopupModal(
                I18n.format("dimensium.settings.title") + "###" + POPUP_ID,
                pOpen,
                ImGuiWindowFlags.NoResize
                        | ImGuiWindowFlags.NoMove
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
            float separatorH = 1f + ImGui.getStyle().getItemSpacingY() * 2f;
            float innerH = ImGui.getContentRegionAvailY() - bottomBarH - separatorH;

            // ── Left: category list ───────────────────────────────────────────
            ImGui.beginChild("##settings_cats", sidebarW, innerH, true);
            String[] categories = {
                I18n.format("dimensium.settings.category.general"),
                I18n.format("dimensium.settings.category.navigation"),
                I18n.format("dimensium.settings.category.keybinds"),
            };
            for (int i = 0; i < categories.length; i++) {
                if (ImGui.selectable(categories[i] + "##cat_" + i, selectedCategory == i)) {
                    selectedCategory = i;
                    capturingTool = null;
                    capturingActionIndex = -1;
                }
            }
            ImGui.endChild();

            ImGui.sameLine();

            // ── Right: settings panel ─────────────────────────────────────────
            ImGui.beginChild("##settings_content", contentW, innerH, false);
            if (selectedCategory == 0) {
                renderGeneralSettings(contentW);
            } else if (selectedCategory == 1) {
                renderNavigationSettings(contentW);
            } else if (selectedCategory == 2) {
                renderKeybindSettings(contentW);
            }
            ImGui.endChild();

            // ── Bottom bar — pinned to bottom ─────────────────────────────────
            ImGui.setCursorPosY(
                    ImGui.getWindowHeight() - bottomBarH - ImGui.getStyle().getWindowPaddingY());
            ImGui.separator();
            float applyW = 80f * scale;
            float closeW = 70f * scale;
            float gap = 6f * scale;
            boolean hasChanges = pendingScale[0] != ImGuiManager.INSTANCE.getUIScale()
                    || pendingWorldScrollSpeed[0] != DimensiumConfig.worldScrollSpeedModifier
                    || pendingUiScrollSpeed[0] != DimensiumConfig.uiScrollSpeedModifier
                    || pendingRotationSnap[0] != DimensiumConfig.rotationSnapDegrees
                    || pendingMovementSpeed[0] != DimensiumConfig.movementSpeedMultiplier
                    || pendingOrbitUseCursor.get() != DimensiumConfig.orbitUseCursor
                    || pendingShapeThreshold[0] != DimensiumConfig.shapeThreshold;
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
                DimensiumConfig.setRotationSnapDegrees(pendingRotationSnap[0]);
                DimensiumConfig.setMovementSpeedMultiplier(pendingMovementSpeed[0]);
                DimensiumConfig.setOrbitUseCursor(pendingOrbitUseCursor.get());
                DimensiumConfig.setShapeThreshold(pendingShapeThreshold[0]);
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
        ImGui.text(I18n.format("dimensium.settings.general.ui_scroll_speed"));
        ImGui.setNextItemWidth(width - 4f);
        ImGui.sliderFloat("##ui_scroll_speed_slider", pendingUiScrollSpeed, 0.1f, 10.0f, "%.1f");

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.settings.general.rotation_snap"));
        ImGui.setNextItemWidth(width - 4f);
        ImGui.sliderFloat("##rotation_snap_slider", pendingRotationSnap, 0.0f, 45.0f, "%.1f°");

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.settings.general.shape_threshold"));
        ImGui.setNextItemWidth(width - 4f);
        ImGui.sliderFloat("##shape_threshold_slider", pendingShapeThreshold, 0.0f, 1.0f, "%.2f");
    }

    private void renderNavigationSettings(float width) {
        ImGui.text(I18n.format("dimensium.settings.navigation.world_scroll_speed"));
        ImGui.setNextItemWidth(width - 4f);
        ImGui.sliderFloat("##world_scroll_speed_slider", pendingWorldScrollSpeed, 0.1f, 10.0f, "%.1f");

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.settings.navigation.movement_speed"));
        ImGui.setNextItemWidth(width - 4f);
        ImGui.sliderFloat("##movement_speed_slider", pendingMovementSpeed, 0.1f, 20.0f, "%.1fx");

        ImGui.spacing();
        ImGui.checkbox(
                I18n.format("dimensium.settings.navigation.orbit_use_cursor") + "##orbit_use_cursor",
                pendingOrbitUseCursor);
        ImGui.textDisabled(I18n.format("dimensium.settings.navigation.orbit_use_cursor.hint"));
    }

    private void renderKeybindSettings(float width) {
        boolean capturing = capturingTool != null || capturingActionIndex >= 0;
        if (capturing) {
            ImGui.textDisabled(I18n.format("dimensium.settings.keybind.press_key"));
            ImGui.spacing();
        }

        float s = ImGuiManager.INSTANCE.getUIScale();
        float btnW = 140f * s;
        float labelW = width - btnW - 12f * s;

        // ── Tool keybinds ─────────────────────────────────────────────────────
        ImGui.separator();
        ImGui.text(I18n.format("dimensium.settings.category.tools"));
        ImGui.spacing();

        for (Tool tool : Tool.values()) {
            KeyBinding kb = Dimensium.toolKeybinds.get(tool);
            int mods = Dimensium.toolKeybindMods.getOrDefault(tool, 0);
            ImGui.text(I18n.format(tool.label));
            ImGui.sameLine(labelW);

            boolean listening = capturingTool == tool;
            String btnLabel = (listening
                            ? I18n.format("dimensium.settings.keybind.press_key_short")
                            : UIUtils.getKeyShortcutName(kb.getKeyCode(), mods))
                    + "##kbt_" + tool.name();

            if (listening) ImGui.pushStyleColor(ImGuiCol.Button, 0.6f, 0.2f, 0.2f, 1.0f);
            if (ImGui.button(btnLabel, btnW, 0)) {
                capturingTool = listening ? null : tool;
                capturingActionIndex = -1;
            }
            if (listening) ImGui.popStyleColor();
        }

        // ── Action keybinds ───────────────────────────────────────────────────
        ImGui.spacing();
        ImGui.separator();
        ImGui.text(I18n.format("dimensium.settings.category.actions"));
        ImGui.spacing();

        for (int i = 0; i < ACTIONS.size(); i++) {
            ActionBind action = ACTIONS.get(i);
            ImGui.text(I18n.format(action.label));
            ImGui.sameLine(labelW);

            boolean listening = capturingActionIndex == i;
            String btnLabel = (listening
                            ? I18n.format("dimensium.settings.keybind.press_key_short")
                            : UIUtils.getKeyShortcutName(action.getKey(), action.getMods()))
                    + "##kba_" + i;

            if (listening) ImGui.pushStyleColor(ImGuiCol.Button, 0.6f, 0.2f, 0.2f, 1.0f);
            if (ImGui.button(btnLabel, btnW, 0)) {
                capturingActionIndex = listening ? -1 : i;
                capturingTool = null;
            }
            if (listening) ImGui.popStyleColor();
        }
    }
}
