/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.panel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** Static drawing primitives and shared layout constants for all panel classes. */
public class PanelDraw {

    private PanelDraw() {}

    // ── Widgets ───────────────────────────────────────────────────────────────

    public static void renderItemIcon(Minecraft mc, ItemStack stack, int x, int y) {
        if (stack == null || stack.getItem() == null) return;
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        new RenderItem().renderItemAndEffectIntoGUI(mc.fontRenderer, mc.renderEngine, stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

}
