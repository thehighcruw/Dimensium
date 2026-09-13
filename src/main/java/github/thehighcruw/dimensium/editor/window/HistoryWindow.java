/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import java.util.List;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.history.ClientEditHistory;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class HistoryWindow extends ToggleableWindow {

    public static final HistoryWindow INSTANCE = new HistoryWindow();

    private boolean skipFirstFrame = false;

    public void setOpen(boolean value) {
        open = value;
        if (value) skipFirstFrame = true;
        DimensiumConfig.setWindowHistoryOpen(value);
    }

    private static final float[] C_CURRENT = { 0.87f, 0.93f, 1.0f, 1.0f };
    private static final float[] C_REDO = { 0.55f, 0.70f, 0.55f, 0.80f };
    private static final float[] C_PAST = { 0.55f, 0.55f, 0.60f, 1.0f };
    private static final float[] C_DANGER = { 1.0f, 0.35f, 0.35f, 1.0f };
    private static final float[] C_SEP = { 1.0f, 1.0f, 1.0f, 0.06f };

    private HistoryWindow() {}

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();

        ClientEditHistory history = ClientEditHistory.INSTANCE;
        history.ensureLoaded();
        List<String> names = history.actionNames();
        ImGui.setNextWindowSize(400f * scale, 500f * scale, ImGuiCond.FirstUseEver);

        ImBoolean openBool = new ImBoolean(open);
        boolean visible = ImGui.begin(I18n.format("dimensium.ui.window.history"), openBool, ImGuiWindowFlags.None);
        if (!openBool.get()) setOpen(false);
        else open = true;
        captureBounds();
        if (!visible || !openBool.get() || skipFirstFrame) {
            skipFirstFrame = false;
            ImGui.end();
            return;
        }

        int pointer = history.pointer;
        int size = names.size();

        // ── Header: total size + Clear button ────────────────────────────────
        long totalBytes = history.totalBytes();
        String totalStr = I18n.format("dimensium.ui.history.total", formatBytes(totalBytes));
        float clearW = 80f * scale;
        float contentW = ImGui.getContentRegionAvailX();
        ImGui.text(totalStr);
        ImGui.sameLine(contentW - clearW);
        String clearLabel = I18n.format("dimensium.ui.history.clear") + "##hist_clear";
        ImGui.pushStyleColor(
            imgui.flag.ImGuiCol.Button,
            C_DANGER[0] * 0.4f,
            C_DANGER[1] * 0.4f,
            C_DANGER[2] * 0.4f,
            0.9f);
        ImGui.pushStyleColor(
            imgui.flag.ImGuiCol.ButtonHovered,
            C_DANGER[0] * 0.6f,
            C_DANGER[1] * 0.6f,
            C_DANGER[2] * 0.6f,
            0.9f);
        boolean doClear = size > 0 && ImGui.button(clearLabel, clearW, 0);
        ImGui.popStyleColor(2);
        if (doClear) {
            history.clear();
            ImGui.end();
            return;
        }

        ImGui.separator();

        if (size == 0) {
            ImGui.textDisabled(I18n.format("dimensium.ui.history.empty"));
            ImGui.end();
            return;
        }

        // ── Column setup ─────────────────────────────────────────────────────
        // Col 0: index (fixed) Col 1: marker (fixed) Col 2: action (fill) Col 3: size (fixed)
        float fullContentW = ImGui.getContentRegionAvailX();
        float col0W = 46f * scale;
        float col1W = 18f * scale;
        float col3W = 68f * scale;
        if (fullContentW < col0W + col1W + col3W + 1f) {
            ImGui.end();
            return;
        }
        ImGui.columns(4, "##hist_cols", false);
        float col2W = fullContentW - col0W - col1W - col3W;
        ImGui.setColumnWidth(0, col0W);
        ImGui.setColumnWidth(1, col1W);
        ImGui.setColumnWidth(2, col2W);
        ImGui.setColumnWidth(3, col3W);

        // ── Redo stack (entries above pointer, shown newest-first) ────────────
        for (int i = size - 1; i > pointer; i--) {
            renderRow(names, i, pointer, history, true, fullContentW);
        }

        // ── Current + past ────────────────────────────────────────────────────
        for (int i = pointer; i >= 0; i--) {
            renderRow(names, i, pointer, history, false, fullContentW);
        }

        ImGui.columns(1);
        ImGui.end();
    }

    private void renderRow(List<String> names, int i, int pointer, ClientEditHistory history, boolean isRedo,
        float fullContentW) {
        boolean isCurrent = (i == pointer);
        float scale = ImGuiManager.INSTANCE.getUIScale();

        // Separator line before each entry — drawn from col 0's left edge across full content width
        float cx = ImGui.getCursorScreenPosX();
        float cy = ImGui.getCursorScreenPosY();
        ImGui.getWindowDrawList()
            .addLine(
                cx,
                cy,
                cx + fullContentW,
                cy,
                ImGui.colorConvertFloat4ToU32(C_SEP[0], C_SEP[1], C_SEP[2], C_SEP[3]));

        // Align all columns to the same baseline by recording Y after padding and setting it per column
        float rowY = cy + 2f * scale;

        // Col 0: index
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), rowY);
        String indexStr = String.format("#%3d", i + 1);
        if (isRedo) {
            ImGui.textColored(C_REDO[0], C_REDO[1], C_REDO[2], C_REDO[3], indexStr);
        } else {
            ImGui.textDisabled(indexStr);
        }
        ImGui.nextColumn();

        // Col 1: current marker
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), rowY);
        if (isCurrent) {
            ImGui.textColored(C_CURRENT[0], C_CURRENT[1], C_CURRENT[2], C_CURRENT[3], ">");
        } else {
            ImGui.text(" ");
        }
        ImGui.nextColumn();

        // Col 2: action name
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), rowY);
        String action = names.get(i);
        if (isCurrent) {
            ImGui.textColored(C_CURRENT[0], C_CURRENT[1], C_CURRENT[2], C_CURRENT[3], action);
        } else if (isRedo) {
            ImGui.textColored(C_REDO[0], C_REDO[1], C_REDO[2], C_REDO[3], action);
        } else {
            ImGui.textColored(C_PAST[0], C_PAST[1], C_PAST[2], C_PAST[3], action);
        }
        ImGui.nextColumn();

        // Col 3: size (right-aligned within its column)
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), rowY);
        long bytes = history.entryBytes(i);
        String sizeStr = formatBytes(bytes);
        float sizeTextW = ImGui.calcTextSize(sizeStr).x;
        float col3W = ImGui.getColumnWidth(3);
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX() + Math.max(0, col3W - sizeTextW - 4f * scale), rowY);
        if (isRedo) {
            ImGui.textColored(C_REDO[0], C_REDO[1], C_REDO[2], C_REDO[3], sizeStr);
        } else {
            ImGui.textDisabled(sizeStr);
        }
        ImGui.nextColumn();
    }

    private static String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else if (bytes >= 1024L) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return bytes + " B";
        }
    }
}
