/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.imgui;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import imgui.ImGui;
import imgui.ImVec2;

/**
 * Draws MC item icons via ImGui's draw list so they participate in
 * ImGui's z-ordering (including modal dimming) without any deferred GL pass.
 *
 * Each call to schedule / placeDummy / placeButton immediately submits an
 * addImage command to the active window's draw list. ItemIconCache supplies
 * the GL texture id, baking it on first access.
 *
 * UV coordinates are flipped (uv0Y=1, uv1Y=0) to correct for OpenGL FBO
 * row-0-at-bottom vs. ImGui UV-origin-at-top-left.
 */
@SideOnly(Side.CLIENT)
public final class DeferredItemRender {

    private DeferredItemRender() {}

    // Padding around the icon inside the button cell, in screen pixels.
    public static final float ITEM_PAD = 2f;

    // Background colors in 0xAABBGGRR format.
    private static final int COL_BG_NORMAL = 0xCC1A2840; // dark navy, 80% alpha
    private static final int COL_BG_HOVERED = 0x50FF803D; // accent blue, 31% alpha
    private static final int COL_BG_SELECTED = 0x8CFF803D; // accent blue, 55% alpha
    private static final int COL_BG_SELECTED_HOVERED = 0xCCFF803D; // accent blue, 80% alpha

    /**
     * Draw an item icon at the given ImGui screen-space position.
     * Must be called from within an active ImGui Begin/End block.
     */
    public static void schedule(ItemStack stack, float x, float y, float size) {
        if (stack == null) return;
        int texId = ItemIconCache.INSTANCE.getTexture(stack);
        if (texId == 0) return;
        ImGui.getWindowDrawList()
            .addImage(texId, x, y, x + size, y + size, 0f, 1f, 1f, 0f);
    }

    /** Reserve a dummy widget at the cursor and draw the icon there. */
    public static void placeDummy(ItemStack stack, float size) {
        float renderSize = size * 2f;
        ImVec2 pos = new ImVec2();
        ImGui.getCursorScreenPos(pos);
        schedule(stack, pos.x, pos.y, renderSize);
        ImGui.dummy(renderSize, renderSize);
    }

    /**
     * Place an invisible button at the cursor and draw the icon on top.
     * Returns true if the button was clicked this frame.
     */
    public static boolean placeButton(String id, ItemStack stack, float size) {
        return placeButton(id, stack, size, false);
    }

    /**
     * Place an invisible button at the cursor and draw the icon on top,
     * with a blue background that brightens on hover and highlights when selected.
     * Returns true if the button was clicked this frame.
     */
    public static boolean placeButton(String id, ItemStack stack, float size, boolean selected) {
        float renderSize = size * 2f;
        float btnSize = renderSize + ITEM_PAD * 2f;
        ImVec2 pos = new ImVec2();
        ImGui.getCursorScreenPos(pos);
        ImGui.invisibleButton(id, btnSize, btnSize);
        boolean clicked = ImGui.isItemClicked();
        boolean hovered = ImGui.isItemHovered();

        int bg;
        if (selected && hovered) bg = COL_BG_SELECTED_HOVERED;
        else if (selected) bg = COL_BG_SELECTED;
        else if (hovered) bg = COL_BG_HOVERED;
        else bg = COL_BG_NORMAL;

        ImGui.getWindowDrawList()
            .addRectFilled(pos.x, pos.y, pos.x + btnSize, pos.y + btnSize, bg, 3f);
        schedule(stack, pos.x + ITEM_PAD, pos.y + ITEM_PAD, renderSize);

        if (hovered) {
            String name = stack.getDisplayName();
            if (name != null && !name.isEmpty()) {
                ImGui.setNextWindowPos(pos.x + btnSize * 0.5f, pos.y - 4f, 0, 0.5f, 1.0f);
                ImGui.beginTooltip();
                ImGui.text(name);
                ImGui.endTooltip();
            }
        }

        return clicked;
    }
}
