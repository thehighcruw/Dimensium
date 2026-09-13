/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.tool.state.PaletteRegistry;
import github.thehighcruw.dimensium.editor.tool.state.PaletteRegistry.PaletteCategory;
import github.thehighcruw.dimensium.editor.window.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;

@SideOnly(Side.CLIENT)
public class PaletteEditorWindow extends ToggleableWindow {

    public static final PaletteEditorWindow INSTANCE = new PaletteEditorWindow();

    private boolean skipFirstFrame = false;

    private int selectedCategory = -1;
    private int renamingCategory = -1;
    private final ImString renameBuf = new ImString(128);

    private static final float WIN_W = 620f;
    private static final float WIN_H = 460f;
    private static final float LEFT_W_DEFAULT = 200f;
    private static final float SPLITTER_W = 5f;
    private static final float MIN_LEFT = 80f;
    private static final float MIN_RIGHT = 120f;
    private static final float CELL_SIZE = 14f;
    private static final float ITEM_GAP = 2f;
    private static final int COLS = 7;

    private float splitPx = -1f;

    private PaletteEditorWindow() {}

    public void setOpen(boolean value) {
        if (value) {
            skipFirstFrame = true;
            renamingCategory = -1;
        }
        open = value;
        DimensiumConfig.setWindowPaletteEditorOpen(value);
    }

    public void open() {
        setOpen(true);
    }

    public void close() {
        setOpen(false);
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        ImGui.setNextWindowSize(WIN_W * scale, WIN_H * scale, ImGuiCond.FirstUseEver);

        ImBoolean openBool = new ImBoolean(open);
        boolean visible = ImGui.begin(
            I18n.format("dimensium.palette.editor.title") + "###palette_editor_window",
            openBool,
            ImGuiWindowFlags.None);
        if (!openBool.get()) close();
        else open = true;
        captureBounds();
        if (!visible || !openBool.get() || skipFirstFrame) {
            skipFirstFrame = false;
            ImGui.end();
            return;
        }

        float contentH = ImGui.getContentRegionAvailY();
        float totalW = ImGui.getContentRegionAvailX();
        float splitterW = SPLITTER_W * scale;
        float minLeft = MIN_LEFT * scale;
        float minRight = MIN_RIGHT * scale;

        if (splitPx < 0) splitPx = LEFT_W_DEFAULT * scale;
        splitPx = Math.max(minLeft, Math.min(totalW - minRight - splitterW, splitPx));
        float rightW = totalW - splitPx - splitterW;

        renderCategoryList(splitPx, contentH);
        ImGui.sameLine(0, 0);
        renderSplitter(splitterW, contentH, totalW, minLeft, minRight);
        ImGui.sameLine(0, 0);
        renderBlockPanel(rightW, contentH, scale);

        ImGui.end();
    }

    private void renderSplitter(float splitterW, float contentH, float totalW, float minLeft, float minRight) {
        ImVec2 pos = new ImVec2();
        ImGui.getCursorScreenPos(pos);
        ImGui.invisibleButton("##paled_split", splitterW, contentH);
        if (ImGui.isItemActive()) {
            ImVec2 delta = new ImVec2();
            ImGui.getIO()
                .getMouseDelta(delta);
            splitPx += delta.x;
            splitPx = Math.max(minLeft, Math.min(totalW - minRight - splitterW, splitPx));
        }
        int lineColor = ImGui.isItemHovered() || ImGui.isItemActive()
            ? ImGui.colorConvertFloat4ToU32(0.5f, 0.6f, 0.8f, 0.9f)
            : ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.35f, 0.7f);
        float midX = pos.x + splitterW * 0.5f;
        ImGui.getWindowDrawList()
            .addLine(midX, pos.y + 4f, midX, pos.y + contentH - 4f, lineColor, 1.5f);
    }

    private void renderCategoryList(float leftW, float contentH) {
        float addBtnH = ImGui.getFrameHeight() + ImGui.getStyle()
            .getItemSpacingY() * 2f;
        float listH = contentH - addBtnH;

        ImGui.beginChild("##paled_cats", leftW, contentH, false);
        ImGui.beginChild("##paled_cats_list", 0, listH, true);

        List<PaletteCategory> cats = PaletteRegistry.INSTANCE.getCategories();
        int pendingRemove = -1;
        int pendingMoveUp = -1;
        int pendingMoveDown = -1;

        for (int i = 0; i < cats.size(); i++) {
            PaletteCategory cat = cats.get(i);

            if (renamingCategory == i) {
                if (!ImGui.isAnyItemActive()) ImGui.setKeyboardFocusHere();
                ImGui.setNextItemWidth(-1);
                boolean enter = ImGui.inputText("##paled_rename_" + i, renameBuf, ImGuiInputTextFlags.EnterReturnsTrue);
                if (enter || (ImGui.isItemDeactivated() && !ImGui.isItemActive())) {
                    String trimmed = renameBuf.get()
                        .trim();
                    if (!trimmed.isEmpty()) PaletteRegistry.INSTANCE.renameCategory(i, trimmed);
                    renamingCategory = -1;
                }
            } else {
                boolean isSelected = (selectedCategory == i);
                if (ImGui.selectable(cat.name + "##paled_cat_" + i, isSelected)) {
                    selectedCategory = i;
                }
                if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
                    renamingCategory = i;
                    renameBuf.set(cat.name);
                }
                if (ImGui.beginPopupContextItem("##paled_cat_ctx_" + i)) {
                    if (selectedCategory != i) selectedCategory = i;
                    if (ImGui.menuItem(I18n.format("dimensium.palette.rename_category"))) {
                        renamingCategory = i;
                        renameBuf.set(cat.name);
                    }
                    ImGui.separator();
                    if (ImGui.menuItem(I18n.format("dimensium.palette.move_up"), null, false, i > 0)) {
                        pendingMoveUp = i;
                    }
                    if (ImGui.menuItem(I18n.format("dimensium.palette.move_down"), null, false, i < cats.size() - 1)) {
                        pendingMoveDown = i;
                    }
                    ImGui.separator();
                    if (ImGui.menuItem(I18n.format("dimensium.palette.delete_category"))) {
                        pendingRemove = i;
                    }
                    ImGui.endPopup();
                }
            }
        }

        ImGui.endChild();

        if (pendingMoveUp >= 0) {
            PaletteRegistry.INSTANCE.moveUp(pendingMoveUp);
            if (selectedCategory == pendingMoveUp) selectedCategory--;
        }
        if (pendingMoveDown >= 0) {
            PaletteRegistry.INSTANCE.moveDown(pendingMoveDown);
            if (selectedCategory == pendingMoveDown) selectedCategory++;
        }
        if (pendingRemove >= 0) {
            PaletteRegistry.INSTANCE.removeCategory(pendingRemove);
            int size = PaletteRegistry.INSTANCE.getCategories()
                .size();
            if (selectedCategory >= size) selectedCategory = size - 1;
        }

        if (ImGui.button(I18n.format("dimensium.palette.add_category") + "##paled_add_cat", -1, 0)) {
            PaletteRegistry.INSTANCE.addCategory(I18n.format("dimensium.palette.new_category_name"));
            selectedCategory = PaletteRegistry.INSTANCE.getCategories()
                .size() - 1;
            renamingCategory = selectedCategory;
            renameBuf.set("");
        }

        ImGui.endChild();
    }

    private void renderBlockPanel(float rightW, float contentH, float scale) {
        ImGui.beginChild("##paled_blocks", rightW, contentH, true);

        List<PaletteCategory> cats = PaletteRegistry.INSTANCE.getCategories();
        if (selectedCategory >= 0 && selectedCategory < cats.size()) {
            PaletteCategory cat = cats.get(selectedCategory);
            List<ItemStack> blocks = cat.blocks;
            float cellSize = CELL_SIZE * scale;

            int pendingRemoveBlock = -1;
            int pendingReplaceBlock = -1;

            for (int j = 0; j < blocks.size(); j++) {
                int col = j % COLS;
                if (col != 0) ImGui.sameLine(0, ITEM_GAP);
                ItemStack block = blocks.get(j);

                DeferredItemRender.placeButton("##paled_block_" + j, block, cellSize);

                if (ImGui.beginPopupContextItem("##paled_block_ctx_" + j)) {
                    if (ImGui.menuItem(I18n.format("dimensium.palette.remove_block"))) {
                        pendingRemoveBlock = j;
                    }
                    if (ImGui.menuItem(I18n.format("dimensium.palette.replace_block"))) {
                        pendingReplaceBlock = j;
                    }
                    ImGui.endPopup();
                }
            }

            if (pendingRemoveBlock >= 0) {
                PaletteRegistry.INSTANCE.removeBlock(selectedCategory, pendingRemoveBlock);
            }
            if (pendingReplaceBlock >= 0) {
                final int ci = selectedCategory, bi = pendingReplaceBlock;
                OverlayRenderer.picker
                    .open(picked -> { if (picked != null) PaletteRegistry.INSTANCE.replaceBlock(ci, bi, picked); });
            }

            ImGui.spacing();
            if (ImGui.button(I18n.format("dimensium.palette.add_block") + "##paled_add_block")) {
                final int ci = selectedCategory;
                OverlayRenderer.picker
                    .open(picked -> { if (picked != null) PaletteRegistry.INSTANCE.addBlock(ci, picked); });
            }
        } else {
            ImGui.textDisabled(I18n.format("dimensium.palette.editor.no_category_selected"));
        }

        ImGui.endChild();
    }
}
