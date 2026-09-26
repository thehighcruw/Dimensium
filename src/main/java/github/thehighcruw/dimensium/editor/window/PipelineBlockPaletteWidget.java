/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.window.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import imgui.ImGui;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Renders a {@code List<int[]>} block palette (each entry is {blockId, meta}) using item-icon
 * buttons, consistent with the style used in painting tool palette panels.
 */
@SideOnly(Side.CLIENT)
public final class PipelineBlockPaletteWidget {

    private PipelineBlockPaletteWidget() {}

    /**
     * Renders the palette inline. Mutates {@code palette} on add/remove actions.
     *
     * @param paramKey unique key used to disambiguate ImGui widget IDs
     * @param palette  mutable list of {blockId, meta} pairs
     */
    public static void render(String paramKey, List<int[]> palette) {
        render(paramKey, palette, null);
    }

    public static void render(String paramKey, List<int[]> palette, Runnable onChanged) {
        float buttonSize = 20f * ImGuiManager.INSTANCE.getUIScale();
        float panelWidth = ImGui.getContentRegionAvailX();

        int removeIdx = -1;

        for (int i = 0; i < palette.size(); i++) {
            int[] entry = palette.get(i);
            Block block = Block.getBlockById(entry[0]);
            Item item = block != null ? Item.getItemFromBlock(block) : null;
            ItemStack stack = item != null ? new ItemStack(item, 1, entry[1]) : null;

            if (stack != null) {
                if (DeferredItemRender.placeButton("##pip_pal_" + paramKey + "_" + i, stack, buttonSize)) {
                    final int idx = i;
                    OverlayRenderer.picker.open(picked -> {
                        if (picked != null) {
                            Block picked_block = Block.getBlockFromItem(picked.getItem());
                            palette.set(idx, new int[] {Block.getIdFromBlock(picked_block), picked.getItemDamage()});
                            if (onChanged != null) onChanged.run();
                        }
                    });
                }
            } else {
                ImGui.button("?##pip_pal_miss_" + paramKey + "_" + i, buttonSize, buttonSize);
            }

            ImGui.sameLine(0, 2);

            if (ImGui.button("x##pip_pal_rm_" + paramKey + "_" + i, buttonSize, buttonSize)) {
                removeIdx = i;
            }

            // Tooltip: full block name
            if (block != null && ImGui.isItemHovered()) {
                String blockName = Block.blockRegistry.getNameForObject(block);
                ImGui.setTooltip(blockName + ":" + entry[1]);
            }

            // Wrap to next line when running out of width
            float nextX = ImGui.getCursorPosX() + buttonSize * 2 + 4;
            if (i < palette.size() - 1 && nextX < panelWidth) {
                ImGui.sameLine(0, 6);
            }
        }

        if (removeIdx >= 0) {
            palette.remove(removeIdx);
            if (onChanged != null) onChanged.run();
        }

        if (ImGui.button(I18n.format("dimensium.ui.pipeline.palette.add") + "##pip_pal_add_" + paramKey)) {
            OverlayRenderer.picker.open(picked -> {
                if (picked != null) {
                    Block picked_block = Block.getBlockFromItem(picked.getItem());
                    palette.add(new int[] {Block.getIdFromBlock(picked_block), picked.getItemDamage()});
                    if (onChanged != null) onChanged.run();
                }
            });
        }
    }
}
