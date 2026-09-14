/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.popup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.blueprint.Blueprint;
import github.thehighcruw.dimensium.editor.blueprint.BlueprintIO;
import github.thehighcruw.dimensium.editor.blueprint.BlueprintRegistry;
import github.thehighcruw.dimensium.editor.blueprint.BlueprintThumbnailCache;
import github.thehighcruw.dimensium.shared.SelectionState;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;

@SideOnly(Side.CLIENT)
public class BlueprintBrowserPopup {

    public static final BlueprintBrowserPopup INSTANCE = new BlueprintBrowserPopup();

    private static final String POPUP_ID = "bp_browser_modal";

    private boolean open = false;
    private boolean pendingOpen = false;
    private final ImString nameSearchBuf = new ImString(256);
    private final ImString tagSearchBuf = new ImString(256);

    private final BlueprintThumbnailCache thumbCache = new BlueprintThumbnailCache();

    /** Tag counts computed on open, sorted by count desc. */
    private final List<String> tagCloud = new ArrayList<>();
    private final Map<String, Integer> tagCounts = new HashMap<>();

    private static final float POPUP_W = 760f;
    private static final float POPUP_H = 540f;
    private static final float PAD = 8f;
    private static final float CELL = 120f;
    private static final float LABEL_H = 18f;
    private static final int COLS = (int) ((POPUP_W - PAD * 2) / CELL);

    private static final char[] SPINNER_CHARS = { '|', '/', '-', '\\' };

    /**
     * When set, called instead of loading the blueprint into the clipboard.
     * Cleared after each selection or close.
     */
    private Consumer<Blueprint> selectionCallback = null;

    // ── Open / close ──────────────────────────────────────────────────────────

    public void open() {
        open(null);
    }

    /** Opens the browser; {@code callback} receives the selected blueprint instead of loading it to clipboard. */
    public void open(Consumer<Blueprint> callback) {
        open = true;
        pendingOpen = true;
        selectionCallback = callback;
        nameSearchBuf.set("");
        tagSearchBuf.set("");
        BlueprintRegistry.INSTANCE.refresh();
        buildTagCloud();
    }

    public boolean isOpen() {
        return open;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void renderImGui() {
        if (pendingOpen) {
            ImGui.openPopup(POPUP_ID);
            pendingOpen = false;
        }

        if (!open) return;

        ImVec2 display = new ImVec2();
        ImGui.getIO()
            .getDisplaySize(display);
        ImGui.setNextWindowPos((display.x - POPUP_W) * 0.5f, (display.y - POPUP_H) * 0.5f, ImGuiCond.Always);
        ImGui.setNextWindowSize(POPUP_W, POPUP_H, ImGuiCond.Always);

        int flags = ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove;

        if (!ImGui.beginPopupModal(I18n.format("dimensium.blueprint.browser.title") + "###" + POPUP_ID, flags)) {
            open = false;
            return;
        }

        if (ImGui.isKeyPressed(ImGuiKey.Escape)) {
            open = false;
            ImGui.closeCurrentPopup();
            ImGui.endPopup();
            return;
        }

        renderSearchBar();
        renderTagCloud();
        ImGui.separator();

        List<Map.Entry<File, Blueprint>> filtered = getFiltered();
        boolean registryLoading = BlueprintRegistry.INSTANCE.isLoading();

        float usedH = 86f + (tagCloud.isEmpty() ? 0f : 26f);
        float gridH = POPUP_H - usedH;
        ImGui.beginChild("##bb_grid", POPUP_W - PAD * 2, gridH, false);

        if (filtered.isEmpty()) {
            if (registryLoading) {
                ImGui.textDisabled(spinner() + " " + I18n.format("dimensium.blueprint.browser.loading"));
            } else {
                List<Map.Entry<File, Blueprint>> all = BlueprintRegistry.INSTANCE.getAll();
                String msg = all.isEmpty() ? I18n.format("dimensium.blueprint.browser.empty")
                    : I18n.format("dimensium.blueprint.browser.noresults");
                ImGui.textDisabled(msg);
            }
        } else {
            float cellTotal = CELL + LABEL_H;
            for (int i = 0; i < filtered.size(); i++) {
                Map.Entry<File, Blueprint> entry = filtered.get(i);
                File file = entry.getKey();
                Blueprint meta = entry.getValue();

                int col = i % COLS;
                if (col != 0) ImGui.sameLine(0, 2);

                ImVec2 pos = new ImVec2();
                ImGui.getCursorScreenPos(pos);

                ImGui.invisibleButton("##bb_cell_" + i, CELL, cellTotal);
                boolean clicked = ImGui.isItemClicked();
                boolean hovered = ImGui.isItemHovered();

                ImGui.getWindowDrawList()
                    .addRectFilled(pos.x, pos.y, pos.x + CELL - 2, pos.y + CELL - 2, hovered ? 0xFF444433 : 0xFF332222);

                int texId = thumbCache.get(file);
                if (texId != -1) {
                    ImGui.getWindowDrawList()
                        .addImage(texId, pos.x + 1, pos.y + 1, pos.x + CELL - 3, pos.y + CELL - 3);
                } else {
                    String spin = String.valueOf(spinner());
                    ImGui.getWindowDrawList()
                        .addText(pos.x + CELL * 0.5f - 4, pos.y + CELL * 0.5f - 8, 0xFF888877, spin);
                }

                String label = meta.name();
                ImGui.getWindowDrawList()
                    .addText(
                        pos.x + 2,
                        pos.y + CELL,
                        hovered ? 0xFFFFEEDD : 0xFFAA9988,
                        label.length() > 14 ? label.substring(0, 12) + ".." : label);

                if (clicked) {
                    try {
                        loadBlueprint(BlueprintIO.load(file));
                    } catch (Exception ignored) {}
                    ImGui.closeCurrentPopup();
                    open = false;
                    ImGui.endChild();
                    ImGui.endPopup();
                    return;
                }
            }
        }

        ImGui.endChild();
        ImGui.endPopup();
    }

    // ── Search bar ────────────────────────────────────────────────────────────

    private void renderSearchBar() {
        float halfW = (POPUP_W - PAD * 4 - 8f) * 0.5f;
        ImGui.setNextItemWidth(halfW);
        ImGui.inputText("##bb_name", nameSearchBuf);
        if (ImGui.isItemHovered()) ImGui.setTooltip(I18n.format("dimensium.blueprint.browser.search.name.tip"));
        ImGui.sameLine(0, 8f);
        ImGui.setNextItemWidth(halfW);
        ImGui.inputText("##bb_tag", tagSearchBuf);
        if (ImGui.isItemHovered()) ImGui.setTooltip(I18n.format("dimensium.blueprint.browser.search.tag.tip"));
        ImGui.separator();
    }

    // ── Tag cloud ─────────────────────────────────────────────────────────────

    private void renderTagCloud() {
        if (tagCloud.isEmpty()) return;
        String activeTag = tagSearchBuf.get()
            .trim()
            .toLowerCase();
        for (String tag : tagCloud) {
            int count = tagCounts.getOrDefault(tag, 0);
            boolean isActive = tag.equalsIgnoreCase(activeTag);
            String label = tag + " (" + count + ")##tc_" + tag;
            if (isActive) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, 0.24f, 0.50f, 1.00f, 0.85f);
            }
            if (ImGui.smallButton(label)) {
                tagSearchBuf.set(isActive ? "" : tag);
            }
            if (isActive) {
                ImGui.popStyleColor();
            }
            ImGui.sameLine(0, 4f);
        }
        ImGui.newLine();
    }

    // ── Filtering ─────────────────────────────────────────────────────────────

    private List<Map.Entry<File, Blueprint>> getFiltered() {
        List<Map.Entry<File, Blueprint>> all = BlueprintRegistry.INSTANCE.getAll();
        String nameQ = nameSearchBuf.get()
            .toLowerCase()
            .trim();
        String tagQ = tagSearchBuf.get()
            .toLowerCase()
            .trim();
        if (nameQ.isEmpty() && tagQ.isEmpty()) return all;

        List<Map.Entry<File, Blueprint>> result = new ArrayList<>();
        for (Map.Entry<File, Blueprint> e : all) {
            Blueprint meta = e.getValue();
            boolean nameOk = nameQ.isEmpty() || meta.name()
                .toLowerCase()
                .contains(nameQ);
            boolean tagOk = tagQ.isEmpty() || hasMatchingTag(meta, tagQ);
            if (nameOk && tagOk) result.add(e);
        }
        return result;
    }

    private static boolean hasMatchingTag(Blueprint meta, String tagQ) {
        for (String tag : meta.tags()) {
            if (tag.toLowerCase()
                .contains(tagQ)) return true;
        }
        return false;
    }

    // ── Tag cloud builder ─────────────────────────────────────────────────────

    private void buildTagCloud() {
        tagCloud.clear();
        tagCounts.clear();
        List<Map.Entry<File, Blueprint>> all = BlueprintRegistry.INSTANCE.getAll();
        for (Map.Entry<File, Blueprint> e : all) {
            for (String tag : e.getValue()
                .tags()) {
                tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + 1);
            }
        }
        tagCloud.addAll(tagCounts.keySet());
        tagCloud.sort((a, b) -> tagCounts.get(b) - tagCounts.get(a));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static char spinner() {
        int frame = (int) (System.currentTimeMillis() / 150) & 3;
        return SPINNER_CHARS[frame];
    }

    private void loadBlueprint(Blueprint bp) {
        if (selectionCallback != null) {
            Consumer<Blueprint> cb = selectionCallback;
            selectionCallback = null;
            cb.accept(bp);
            return;
        }
        SelectionState sel = SelectionState.INSTANCE;
        Map<Long, SelectionState.BlockData> clipboard = new HashMap<>(
            bp.offsets()
                .size());
        for (int[] o : bp.offsets()) {
            Block block = Block.getBlockById(o[3]);
            if (block == null || block == Blocks.air) continue;
            clipboard.put(SelectionState.clipboardKey(o[0], o[1], o[2]), new SelectionState.BlockData(block, o[4]));
        }
        sel.clipboard = clipboard;
        sel.clipDim = bp.clipDim();
        sel.clipboardVersion++;
    }
}
