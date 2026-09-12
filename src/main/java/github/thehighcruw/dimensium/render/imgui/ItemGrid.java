/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.imgui;

import java.util.List;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import imgui.ImGui;

/**
 * Renders a row-wrapped grid of item icons using DeferredItemRender.placeButton.
 * cellSize is the logical size — actual reserved and rendered size is cellSize * 2 + padding.
 */
@SideOnly(Side.CLIENT)
public final class ItemGrid {

    private ItemGrid() {}

    /**
     * Total pixel width of a grid with the given parameters.
     * Accounts for the 2× render scaling and per-cell padding applied by placeButton.
     */
    public static float width(int cols, float cellSize, float gap) {
        float btnSize = cellSize * 2f + DeferredItemRender.ITEM_PAD * 2f;
        return cols * btnSize + (cols - 1) * gap;
    }

    /**
     * Render a grid of item icons.
     *
     * Items beyond {@code items.size()} up to {@code count} are filled with
     * invisible dummies so the grid always occupies a fixed number of cells.
     *
     * @return index of the clicked item, or -1 if nothing was clicked
     */
    public static int render(String idPrefix, List<ItemStack> items, int count, int cols, float cellSize, float gap) {
        return render(idPrefix, items, count, cols, cellSize, gap, -1);
    }

    /**
     * Render a grid of item icons, highlighting the item at {@code selectedIndex}.
     *
     * @return index of the clicked item, or -1 if nothing was clicked
     */
    public static int render(String idPrefix, List<ItemStack> items, int count, int cols, float cellSize, float gap,
        int selectedIndex) {
        int clicked = -1;
        float btnSize = cellSize * 2f + DeferredItemRender.ITEM_PAD * 2f;
        for (int i = 0; i < count; i++) {
            int col = i % cols;
            if (col != 0) ImGui.sameLine(0, gap);
            if (i < items.size()) {
                if (DeferredItemRender.placeButton(idPrefix + i, items.get(i), cellSize, i == selectedIndex)) {
                    clicked = i;
                }
            } else {
                ImGui.dummy(btnSize, btnSize);
            }
        }
        return clicked;
    }
}
