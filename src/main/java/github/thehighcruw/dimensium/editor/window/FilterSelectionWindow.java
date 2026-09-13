/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;
import github.thehighcruw.dimensium.editor.window.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiWindow;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.SelectionTransforms;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class FilterSelectionWindow extends ImGuiWindow {

    public static final FilterSelectionWindow INSTANCE = new FilterSelectionWindow();

    private boolean open = false;

    private boolean keepMatching = true;
    private boolean exactMeta = false;
    private int selectedIndex = -1;
    private final List<ItemStack> selectionBlocks = new ArrayList<>();

    private static final String WINDOW_ID = "###filter_selection_window";
    private static final int COLS = 8;
    private static final int MAX_ROWS = 4;

    private FilterSelectionWindow() {}

    public void open() {
        selectionBlocks.clear();
        selectedIndex = -1;
        scanSelectionBlocks();
        open = true;
    }

    private void scanSelectionBlocks() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection()) return;
        net.minecraft.world.World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;
        Set<String> seen = new HashSet<>();
        for (long key : sel.getSelectedBlocks()) {
            int x = SelectionState.unpackX(key);
            int y = SelectionState.unpackY(key);
            int z = SelectionState.unpackZ(key);
            Block b = world.getBlock(x, y, z);
            if (b == null || b == Blocks.air) continue;
            Item item = Item.getItemFromBlock(b);
            if (item == null) continue;
            int meta = world.getBlockMetadata(x, y, z);
            String uid = Item.getIdFromItem(item) + ":" + meta;
            if (!seen.contains(uid)) {
                seen.add(uid);
                selectionBlocks.add(new ItemStack(item, 1, meta));
                if (selectionBlocks.size() >= COLS * MAX_ROWS) break;
            }
        }
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

        float scale = ImGuiManager.INSTANCE.getUIScale();
        float cellSize = 20f * scale;
        float cellPad = 4f * scale;
        float gridW = COLS * (cellSize + cellPad);
        float w = gridW + ImGui.getStyle()
            .getWindowPaddingX() * 2f;

        float vpW = ImGui.getIO()
            .getDisplaySizeX(),
            vpH = ImGui.getIO()
                .getDisplaySizeY();
        ImGui.setNextWindowPos((vpW - w) * 0.5f, vpH * 0.3f, ImGuiCond.Appearing);
        ImGui.setNextWindowSize(w, 300f * scale, ImGuiCond.Appearing);

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui.begin(I18n.format("dimensium.select.filter.title") + WINDOW_ID, pOpen);
        captureBounds();

        if (visible) {
            boolean hasSel = SelectionState.INSTANCE.hasSelection();
            boolean canApply = selectedIndex >= 0 && selectedIndex < selectionBlocks.size() && hasSel;

            float windowW = ImGui.getWindowWidth();
            float btnW = 70f * scale;
            float footerH = ImGui.getStyle()
                .getItemSpacingY() + 1f
                + ImGui.getStyle()
                    .getItemSpacingY()
                + ImGui.getFrameHeight()
                + ImGui.getStyle()
                    .getWindowPaddingY();
            float childH = Math.max(0f, ImGui.getContentRegionAvailY() - footerH);
            ImGui.beginChild("##filter_body", 0f, childH);

            if (!hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.separator();
            }

            if (selectionBlocks.isEmpty()) {
                ImGui.textDisabled(I18n.format("dimensium.select.filter.no_block"));
            } else {
                for (int i = 0; i < selectionBlocks.size(); i++) {
                    ItemStack stack = selectionBlocks.get(i);
                    boolean selected = i == selectedIndex;

                    if (DeferredItemRender.placeButton("##flt_blk_" + i, stack, cellSize * 0.5f, selected)) {
                        selectedIndex = (selectedIndex == i) ? -1 : i;
                    }

                    if ((i + 1) % COLS != 0 && i < selectionBlocks.size() - 1) {
                        ImGui.sameLine(0f, cellPad);
                    }
                }
            }

            ImGui.spacing();

            if (ImGui.radioButton(I18n.format("dimensium.select.filter.keep") + "##flt_keep", keepMatching))
                keepMatching = true;
            ImGui.sameLine();
            if (ImGui.radioButton(I18n.format("dimensium.select.filter.remove") + "##flt_remove", !keepMatching))
                keepMatching = false;

            ImGui.spacing();
            ImBoolean cbExact = new ImBoolean(exactMeta);
            if (ImGui.checkbox(I18n.format("dimensium.select.filter.exact_meta") + "##flt_exact", cbExact))
                exactMeta = cbExact.get();

            ImGui.endChild();

            ImGui.separator();
            ImGui.setCursorPosX(
                windowW - ImGui.getStyle()
                    .getWindowPaddingX() - btnW);
            if (!canApply) ImGui.beginDisabled();
            if (ImGui.button(I18n.format("dimensium.select.apply") + "##flt_apply", btnW, 0)) {
                ItemStack chosen = selectionBlocks.get(selectedIndex);
                Block filterBlock = Block.getBlockFromItem(chosen.getItem());
                SelectionState sel = SelectionState.INSTANCE;
                if (sel.hasSelection() && filterBlock != null) {
                    sel.applyOp(
                        SelectionTransforms.filter(
                            sel.getSelectedBlocks(),
                            Minecraft.getMinecraft().theWorld,
                            filterBlock,
                            chosen.getItemDamage(),
                            keepMatching,
                            exactMeta),
                        BooleanOp.REPLACE);
                }
                close();
            }
            if (!canApply) ImGui.endDisabled();
        }

        ImGui.end();
        if (!pOpen.get()) close();
    }
}
