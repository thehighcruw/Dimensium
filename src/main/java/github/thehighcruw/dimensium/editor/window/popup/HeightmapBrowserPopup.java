/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.popup;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.HeightmapData;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.HeightmapRegistry;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.HeightmapRegistry.HeightmapEntry;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.resources.I18n;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class HeightmapBrowserPopup {

    public static final HeightmapBrowserPopup INSTANCE = new HeightmapBrowserPopup();

    private static final String POPUP_ID = "hm_browser_modal";

    private static final float POPUP_W = 640f;
    private static final float POPUP_H = 480f;
    private static final float PAD = 8f;
    private static final float CELL = 104f;
    private static final float LABEL_H = 18f;
    private static final int COLS = (int) ((POPUP_W - PAD * 2) / CELL);

    private boolean open = false;
    private boolean pendingOpen = false;
    private final ImString searchBuf = new ImString(256);

    private Consumer<HeightmapData> selectionCallback = null;

    private final Map<String, Integer> textures = new HashMap<>();

    private HeightmapBrowserPopup() {}

    public void open(Consumer<HeightmapData> callback) {
        open = true;
        pendingOpen = true;
        selectionCallback = callback;
        searchBuf.set("");
        reload();
    }

    private void reload() {
        for (int texId : textures.values()) {
            GL11.glDeleteTextures(texId);
        }
        textures.clear();
        HeightmapRegistry.INSTANCE.refresh();
    }

    public boolean isOpen() {
        return open;
    }

    public void renderImGui() {
        if (pendingOpen) {
            ImGui.openPopup(POPUP_ID);
            pendingOpen = false;
        }

        if (!open) return;

        ImVec2 display = new ImVec2();
        ImGui.getIO().getDisplaySize(display);
        ImGui.setNextWindowPos((display.x - POPUP_W) * 0.5f, (display.y - POPUP_H) * 0.5f, ImGuiCond.Always);
        ImGui.setNextWindowSize(POPUP_W, POPUP_H, ImGuiCond.Always);

        int flags = ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove;

        if (!ImGui.beginPopupModal(I18n.format("dimensium.heightmap.browser.title") + "###" + POPUP_ID, flags)) {
            open = false;
            return;
        }

        if (ImGui.isKeyPressed(ImGuiKey.Escape)) {
            close();
            ImGui.endPopup();
            return;
        }

        renderSearchBar();
        ImGui.separator();

        List<HeightmapEntry> filtered = getFiltered();

        float gridH = POPUP_H - 56f;
        ImGui.beginChild("##hm_grid", POPUP_W - PAD * 2, gridH, false);

        if (filtered.isEmpty()) {
            List<HeightmapEntry> all = HeightmapRegistry.INSTANCE.getAll();
            String msg = all.isEmpty()
                    ? I18n.format("dimensium.heightmap.browser.empty")
                    : I18n.format("dimensium.heightmap.browser.noresults");
            ImGui.textDisabled(msg);
        } else {
            float cellTotal = CELL + LABEL_H;

            // "None" cell first
            renderNoneCell();
            if (filtered.size() % COLS != 0 || !filtered.isEmpty()) ImGui.sameLine(0, 2);

            for (int i = 0; i < filtered.size(); i++) {
                HeightmapEntry entry = filtered.get(i);

                int col = (i + 1) % COLS;
                if (col != 0) ImGui.sameLine(0, 2);

                ImVec2 pos = new ImVec2();
                ImGui.getCursorScreenPos(pos);

                ImGui.invisibleButton("##hm_cell_" + i, CELL, cellTotal);
                boolean clicked = ImGui.isItemClicked();
                boolean hovered = ImGui.isItemHovered();

                ImGui.getWindowDrawList()
                        .addRectFilled(
                                pos.x, pos.y, pos.x + CELL - 2, pos.y + CELL - 2, hovered ? 0xFF444433 : 0xFF332222);

                int texId = getOrUploadTexture(entry);
                if (texId != -1) {
                    ImGui.getWindowDrawList().addImage(texId, pos.x + 1, pos.y + 1, pos.x + CELL - 3, pos.y + CELL - 3);
                }

                String label = entry.name();
                ImGui.getWindowDrawList()
                        .addText(
                                pos.x + 2,
                                pos.y + CELL,
                                hovered ? 0xFFFFEEDD : 0xFFAA9988,
                                label.length() > 13 ? label.substring(0, 11) + ".." : label);

                if (clicked) {
                    select(entry.data());
                    ImGui.endChild();
                    ImGui.endPopup();
                    return;
                }
            }
        }

        ImGui.endChild();
        ImGui.endPopup();
    }

    private void renderNoneCell() {
        ImVec2 pos = new ImVec2();
        ImGui.getCursorScreenPos(pos);
        float cellTotal = CELL + LABEL_H;

        ImGui.invisibleButton("##hm_none", CELL, cellTotal);
        boolean clicked = ImGui.isItemClicked();
        boolean hovered = ImGui.isItemHovered();

        ImGui.getWindowDrawList()
                .addRectFilled(pos.x, pos.y, pos.x + CELL - 2, pos.y + CELL - 2, hovered ? 0xFF444433 : 0xFF2A2A2A);
        // Diagonal cross to indicate "none"
        ImGui.getWindowDrawList().addLine(pos.x + 8, pos.y + 8, pos.x + CELL - 10, pos.y + CELL - 10, 0xFF666666, 2f);
        ImGui.getWindowDrawList().addLine(pos.x + CELL - 10, pos.y + 8, pos.x + 8, pos.y + CELL - 10, 0xFF666666, 2f);
        ImGui.getWindowDrawList()
                .addText(
                        pos.x + 2,
                        pos.y + CELL,
                        hovered ? 0xFFFFEEDD : 0xFFAA9988,
                        I18n.format("dimensium.heightmap.browser.none"));

        if (clicked) {
            select(null);
        }
    }

    private void renderSearchBar() {
        float searchW = POPUP_W - PAD * 2 - 80f;
        ImGui.setNextItemWidth(searchW);
        ImGui.inputText("##hm_search", searchBuf);
        if (ImGui.isItemHovered()) ImGui.setTooltip(I18n.format("dimensium.heightmap.browser.search.tip"));
        ImGui.sameLine(0, 8f);
        if (ImGui.button(I18n.format("dimensium.heightmap.browser.reload"))) {
            reload();
        }
    }

    private List<HeightmapEntry> getFiltered() {
        List<HeightmapEntry> all = HeightmapRegistry.INSTANCE.getAll();
        String query = searchBuf.get().toLowerCase().trim();
        if (query.isEmpty()) return all;

        List<HeightmapEntry> result = new ArrayList<>();
        for (HeightmapEntry entry : all) {
            if (entry.name().toLowerCase().contains(query)) result.add(entry);
        }
        return result;
    }

    private int getOrUploadTexture(HeightmapEntry entry) {
        Integer cached = textures.get(entry.name());
        if (cached != null) return cached;

        int id = uploadTexture(entry.data());
        if (id != -1) textures.put(entry.name(), id);
        return id;
    }

    private static int uploadTexture(HeightmapData data) {
        try {
            int id = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    data.width(),
                    data.height(),
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    data.toRgbaBuffer());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            return id;
        } catch (Exception e) {
            return -1;
        }
    }

    private void select(HeightmapData data) {
        Consumer<HeightmapData> cb = selectionCallback;
        selectionCallback = null;
        close();
        if (cb != null) cb.accept(data);
    }

    private void close() {
        open = false;
        ImGui.closeCurrentPopup();
    }
}
