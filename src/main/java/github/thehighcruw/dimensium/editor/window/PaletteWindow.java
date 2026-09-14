/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.editor.tool.state.PaletteRegistry;
import github.thehighcruw.dimensium.editor.tool.state.PaletteRegistry.PaletteCategory;
import github.thehighcruw.dimensium.editor.window.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.editor.window.panel.PanelSection;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

@SideOnly(Side.CLIENT)
public class PaletteWindow extends ToggleableWindow {

    public static final PaletteWindow INSTANCE = new PaletteWindow();

    private boolean skipFirstFrame = false;

    private static final String DRAG_TYPE = "PALETTE_BLOCK";
    private static final int RECENT_COUNT = 16;
    private static final int COLS = 8;
    private static final float CELL_SIZE = 16f;
    private static final float ITEM_GAP = 2f;

    private PaletteWindow() {}

    public void setOpen(boolean value) {
        open = value;
        if (value) skipFirstFrame = true;
        DimensiumConfig.setWindowPaletteOpen(value);
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        ImGui.setNextWindowSize(320f * scale, 520f * scale, ImGuiCond.FirstUseEver);

        ImBoolean openBool = new ImBoolean(open);
        boolean visible = ImGui.begin(
                I18n.format("dimensium.palette.window.title") + "###palette_window", openBool, ImGuiWindowFlags.None);
        if (!openBool.get()) setOpen(false);
        else open = true;
        captureBounds();
        if (!visible || !openBool.get() || skipFirstFrame) {
            skipFirstFrame = false;
            ImGui.end();
            return;
        }

        renderActiveBlock(scale);
        ImGui.separator();

        if (PanelSection.begin(I18n.format("dimensium.palette.recent"))) {
            renderRecentBlocks(scale);
        }
        PanelSection.end();

        List<PaletteCategory> cats = PaletteRegistry.INSTANCE.getCategories();
        int pendingRemoveCat = -1;

        for (int i = 0; i < cats.size(); i++) {
            PaletteCategory cat = cats.get(i);
            String sectionLabel = cat.name + "##palcat_" + i;

            if (PanelSection.begin(sectionLabel)) {
                List<ItemStack> blocks = cat.blocks;
                int pendingRemoveBlock = -1;
                int pendingReplaceBlock = -1;

                for (int j = 0; j < blocks.size(); j++) {
                    int col = j % COLS;
                    if (col != 0) ImGui.sameLine(0, ITEM_GAP);
                    ItemStack block = blocks.get(j);

                    DeferredItemRender.placeButton("##palcatblock_" + i + "_" + j, block, CELL_SIZE * scale);

                    if (ImGui.beginDragDropTarget()) {
                        byte[] payload = ImGui.acceptDragDropPayload(DRAG_TYPE);
                        if (payload != null) {
                            ItemStack dropped = blockFromPayload(payload);
                            if (dropped != null) PaletteRegistry.INSTANCE.addBlock(i, dropped);
                        }
                        ImGui.endDragDropTarget();
                    }

                    if (ImGui.beginPopupContextItem("##palcatblock_ctx_" + i + "_" + j)) {
                        if (ImGui.menuItem(I18n.format("dimensium.palette.context.set_active"))) {
                            setActive(block);
                        }
                        if (ImGui.menuItem(I18n.format("dimensium.palette.remove_block"))) {
                            pendingRemoveBlock = j;
                        }
                        if (ImGui.menuItem(I18n.format("dimensium.palette.replace_block"))) {
                            pendingReplaceBlock = j;
                        }
                        ImGui.endPopup();
                    }
                }

                // Drop target on empty row below blocks
                ImGui.dummy(ImGui.getContentRegionAvailX(), 6f * scale);
                if (ImGui.beginDragDropTarget()) {
                    byte[] payload = ImGui.acceptDragDropPayload(DRAG_TYPE);
                    if (payload != null) {
                        ItemStack dropped = blockFromPayload(payload);
                        if (dropped != null) PaletteRegistry.INSTANCE.addBlock(i, dropped);
                    }
                    ImGui.endDragDropTarget();
                }

                if (pendingRemoveBlock >= 0) {
                    PaletteRegistry.INSTANCE.removeBlock(i, pendingRemoveBlock);
                }
                if (pendingReplaceBlock >= 0) {
                    final int ci = i, bi = pendingReplaceBlock;
                    OverlayRenderer.picker.open(picked -> PaletteRegistry.INSTANCE.replaceBlock(ci, bi, picked));
                }

                // Right-click on section header (context item for the collapsingHeader)
                if (ImGui.beginPopupContextItem("##palcat_hdr_ctx_" + i)) {
                    if (ImGui.menuItem(I18n.format("dimensium.palette.delete_category"))) {
                        pendingRemoveCat = i;
                    }
                    ImGui.endPopup();
                }
            }
            PanelSection.end();
        }

        if (pendingRemoveCat >= 0) {
            PaletteRegistry.INSTANCE.removeCategory(pendingRemoveCat);
        }

        ImGui.end();
    }

    private void renderActiveBlock(float scale) {
        SelectedBlockState sel = SelectedBlockState.INSTANCE;
        ItemStack active = sel.selectedBlock;
        if (active != null) {
            if (DeferredItemRender.placeButton("##pal_active_block", active, CELL_SIZE * scale)) {
                OverlayRenderer.picker.open(this::applyPickedBlock);
            }
            ImGui.sameLine(0, 8f * scale);
            ImGui.text(active.getDisplayName());
        } else if (sel.selectedIsAir) {
            if (ImGui.button(
                    I18n.format("dimensium.ui.block_picker.air") + "##pal_pick_active",
                    ImGui.getContentRegionAvailX(),
                    0)) {
                OverlayRenderer.picker.open(this::applyPickedBlock);
            }
        } else {
            if (ImGui.button(
                    I18n.format("dimensium.palette.active_block") + "##pal_pick_active",
                    ImGui.getContentRegionAvailX(),
                    0)) {
                OverlayRenderer.picker.open(this::applyPickedBlock);
            }
        }
    }

    private void renderRecentBlocks(float scale) {
        List<ItemStack> recent = RecentBlockHistory.get();
        int count = Math.min(recent.size(), RECENT_COUNT);
        float cellSize = CELL_SIZE * scale;

        for (int i = 0; i < count; i++) {
            int col = i % COLS;
            if (col != 0) ImGui.sameLine(0, ITEM_GAP);
            ItemStack stack = recent.get(i);

            if (DeferredItemRender.placeButton("##pal_recent_" + i, stack, cellSize)) {
                setActive(stack);
            }

            if (ImGui.beginDragDropSource(imgui.flag.ImGuiDragDropFlags.None)) {
                byte[] payload = blockToPayload(stack);
                ImGui.setDragDropPayload(DRAG_TYPE, payload, payload.length);
                DeferredItemRender.placeDummy(stack, cellSize);
                ImGui.endDragDropSource();
            }

            if (ImGui.beginPopupContextItem("##pal_recent_ctx_" + i)) {
                if (ImGui.menuItem(I18n.format("dimensium.palette.context.set_active"))) {
                    setActive(stack);
                }
                List<PaletteCategory> cats = PaletteRegistry.INSTANCE.getCategories();
                if (!cats.isEmpty() && ImGui.beginMenu(I18n.format("dimensium.palette.context.add_to"))) {
                    for (int c = 0; c < cats.size(); c++) {
                        if (ImGui.menuItem(cats.get(c).name + "##addto_" + c)) {
                            PaletteRegistry.INSTANCE.addBlock(c, stack);
                        }
                    }
                    ImGui.endMenu();
                }
                ImGui.endPopup();
            }
        }
    }

    private void applyPickedBlock(ItemStack picked) {
        if (picked == null) {
            SelectedBlockState.INSTANCE.selectedBlock = null;
            SelectedBlockState.INSTANCE.selectedIsAir = true;
        } else {
            SelectedBlockState.INSTANCE.selectedBlock = picked;
            SelectedBlockState.INSTANCE.selectedIsAir = false;
            RecentBlockHistory.add(picked);
        }
    }

    private void setActive(ItemStack stack) {
        SelectedBlockState.INSTANCE.selectedBlock = stack.copy();
        SelectedBlockState.INSTANCE.selectedIsAir = false;
        RecentBlockHistory.add(stack);
    }

    private static byte[] blockToPayload(ItemStack stack) {
        Block b = Block.getBlockFromItem(stack.getItem());
        String name = Block.blockRegistry.getNameForObject(b);
        String s = name + ":" + stack.getItemDamage();
        return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static ItemStack blockFromPayload(byte[] data) {
        String s = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        int last = s.lastIndexOf(':');
        if (last < 1) return null;
        String blockName = s.substring(0, last);
        int meta;
        try {
            meta = Integer.parseInt(s.substring(last + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        Block b = (Block) Block.blockRegistry.getObject(blockName);
        if (b == null || b == Blocks.air) return null;
        return new ItemStack(b, 1, meta);
    }
}
