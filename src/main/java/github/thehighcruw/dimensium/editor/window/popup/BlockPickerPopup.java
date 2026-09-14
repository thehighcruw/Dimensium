/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.popup;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.window.RecentBlockHistory;
import github.thehighcruw.dimensium.editor.window.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ItemGrid;
import github.thehighcruw.dimensium.shared.util.BlockUtils;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

@SideOnly(Side.CLIENT)
public class BlockPickerPopup {

    private static final String POPUP_ID = "##block_picker";

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean open = false;
    private boolean pendingOpen = false;
    private Consumer<ItemStack> callback = null;
    private ItemStack initialSelection = null;
    private boolean initialIsAir = false;
    private final ImString searchBuf = new ImString(256);

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int COLS = 10;
    private static final float CELL_BASE = 20f;
    private static final float ITEM_GAP = 3f;
    private static final int VIS_ROWS = 6;
    private static final int RECENT_ROWS = 2;

    private float cell() {
        return CELL_BASE * ImGuiManager.INSTANCE.getUIScale();
    }

    private float itemSize() {
        return cell() * 2f + DeferredItemRender.ITEM_PAD * 2f;
    }

    private float gridW() {
        return ItemGrid.width(COLS, cell(), ITEM_GAP);
    }

    // 16 = 2×windowPadding(8), 14 = scrollbar width, 4 = extra margin
    private float popupW() {
        return gridW() + 16f * ImGuiManager.INSTANCE.getUIScale() + 14f * ImGuiManager.INSTANCE.getUIScale() + 4f;
    }

    // ── Search / block cache ──────────────────────────────────────────────────
    private List<ItemStack> cachedResults = new ArrayList<>();
    private String cachedQuery = null;

    // ── Public API ────────────────────────────────────────────────────────────

    public void open(Consumer<ItemStack> cb) {
        open(cb, null);
    }

    public void open(Consumer<ItemStack> cb, ItemStack initial) {
        open = true;
        pendingOpen = true;
        callback = cb;
        initialSelection = initial;
        initialIsAir = false;
        searchBuf.set("");
    }

    /** Open the picker with Air pre-selected. */
    public void openSelectAir(Consumer<ItemStack> cb) {
        open(cb, null);
        initialIsAir = true;
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    // ── Instance render ───────────────────────────────────────────────────────

    public void renderImGui() {
        // Trigger the ImGui popup open mechanism from within the frame.
        if (pendingOpen) {
            ImGui.openPopup(POPUP_ID);
            pendingOpen = false;
        }

        if (!open) return;

        List<ItemStack> results = getPickerResults();
        List<ItemStack> recent = RecentBlockHistory.get();

        float padY = 8f; // windowPadding.y
        float frameH = ImGui.getFrameHeight(); // input field: font + framePadding*2
        float spacing = ImGui.getStyle().getItemSpacingY(); // 4px
        float sepH = 1f + spacing; // separator line + trailing spacing

        float _cell = cell();
        float _itemSz = itemSize();
        float _popupW = popupW();
        float _padX = 8f * ImGuiManager.INSTANCE.getUIScale();

        // Each grid row is _itemSz tall + ItemSpacing.y gap between rows.
        float gridChildH = VIS_ROWS * _itemSz + (VIS_ROWS - 1) * spacing;
        float recentChildH = RECENT_ROWS * _itemSz + (RECENT_ROWS - 1) * spacing;

        // windowPadding + input + spacing + separator + spacing before grid
        float headerH = padY + frameH + spacing + sepH + spacing;
        // air button row: btnSz + spacing + separator + spacing
        float airBtnH = _itemSz + spacing + sepH + spacing;
        // separator + spacing + "Recent" label + spacing before child
        float recentLabelH = sepH + spacing + ImGui.getTextLineHeight() + spacing;
        float recentH = recentLabelH + recentChildH;

        float windowH = headerH + airBtnH + gridChildH + recentH + padY;

        ImVec2 display = new ImVec2();
        ImGui.getIO().getDisplaySize(display);
        ImGui.setNextWindowPos((display.x - _popupW) * 0.5f, (display.y - windowH) * 0.5f, ImGuiCond.Always);
        ImGui.setNextWindowSize(_popupW, windowH, ImGuiCond.Always);

        int flags = ImGuiWindowFlags.NoTitleBar
                | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoScrollWithMouse;

        if (!ImGui.beginPopupModal(POPUP_ID, flags)) {
            // ImGui closed it (e.g. ESC)
            open = false;
            callback = null;
            initialSelection = null;
            initialIsAir = false;
            return;
        }

        if (ImGui.isKeyPressed(ImGuiKey.Escape)) {
            close();
            ImGui.closeCurrentPopup();
            ImGui.endPopup();
            return;
        }

        // Search field
        ImGui.setNextItemWidth(_popupW - _padX * 2f);
        ImGui.inputText("##bp_search", searchBuf);
        if (ImGui.isWindowAppearing()) {
            ImGui.setKeyboardFocusHere(-1);
        }

        ImGui.separator();

        // Air button — always first, no ItemStack required
        float btnSz = _cell * 2f + DeferredItemRender.ITEM_PAD * 2f;
        boolean airSelected = initialIsAir;
        if (airSelected) ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, 0.2f, 0.5f, 0.2f, 1f);
        if (ImGui.button(I18n.format("dimensium.ui.block_picker.air") + "##air_btn", btnSz, btnSz)) {
            if (callback != null) callback.accept(null); // null = air
            ImGui.closeCurrentPopup();
            open = false;
            callback = null;
            initialSelection = null;
            initialIsAir = false;
            if (airSelected) ImGui.popStyleColor();
            ImGui.endPopup();
            return;
        }
        if (airSelected) ImGui.popStyleColor();

        ImGui.separator();

        // Compute which result index matches the initial selection (for highlight)
        int selectedIndex = -1;
        if (initialSelection != null) {
            for (int i = 0; i < results.size(); i++) {
                if (stacksSameBlock(results.get(i), initialSelection)) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        // Scrollable block grid
        ImGui.beginChild("##bp_grid", _popupW - _padX * 2f, gridChildH, false, ImGuiWindowFlags.HorizontalScrollbar);
        int clicked = ItemGrid.render("##bpg_", results, results.size(), COLS, _cell, ITEM_GAP, selectedIndex);
        ImGui.endChild();

        if (clicked >= 0 && clicked < results.size()) {
            confirmSelection(results, clicked);
            ImGui.endPopup();
            return;
        }

        ImGui.separator();
        ImGui.textDisabled(I18n.format("dimensium.ui.block_picker.recent"));

        // Recent blocks grid
        ImGui.beginChild("##bp_recent", _popupW - _padX * 2f, recentChildH, false);
        int recentClicked = ItemGrid.render("##bpr_", recent, RECENT_ROWS * COLS, COLS, _cell, ITEM_GAP);
        ImGui.endChild();

        if (recentClicked >= 0 && recentClicked < recent.size()) {
            confirmSelection(recent, recentClicked);
            initialIsAir = false;
            ImGui.endPopup();
            return;
        }

        ImGui.endPopup();
    }

    private void confirmSelection(List<ItemStack> results, int clicked) {
        ItemStack picked = results.get(clicked).copy();
        RecentBlockHistory.add(picked);
        if (callback != null) callback.accept(picked);
        ImGui.closeCurrentPopup();
        open = false;
        callback = null;
        initialSelection = null;
    }

    // ── Search ────────────────────────────────────────────────────────────────

    List<ItemStack> getPickerResults() {
        String query = searchBuf.get();
        if (query.equals(cachedQuery)) return cachedResults;
        cachedQuery = query;
        cachedResults = new ArrayList<>();
        String q = query.toLowerCase().trim();
        for (ItemStack stack : BlockUtils.collectPlaceableBlocks()) {
            if (q.isEmpty() || matchesStack(stack, q)) {
                cachedResults.add(stack);
                if (cachedResults.size() >= 500) break;
            }
        }
        return cachedResults;
    }

    private static boolean stacksSameBlock(ItemStack a, ItemStack b) {
        if (a == null || b == null) return a == b;
        if (a.getItem() != b.getItem()) return false;
        return a.getItemDamage() == b.getItemDamage();
    }

    private static boolean matchesStack(ItemStack stack, String q) {
        if (stack.getDisplayName().toLowerCase().contains(q)) return true;
        Block b = Block.getBlockFromItem(stack.getItem());
        if (b != null && b.getUnlocalizedName().toLowerCase().contains(q)) return true;
        for (int oreId : OreDictionary.getOreIDs(stack)) {
            if (OreDictionary.getOreName(oreId).toLowerCase().contains(q)) return true;
        }
        return false;
    }
}
