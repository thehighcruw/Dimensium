/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.inventory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Collections;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;

@SideOnly(Side.CLIENT)
public abstract class AbstractFsotGuiContainer extends GuiContainer {

    protected static final int C_SLOT = 0xFF8B8B8B;
    protected static final int C_SLOT_HI = 0xFFFFFFFF;
    protected static final int C_SLOT_SH = 0xFF373737;

    protected AbstractFsotGuiContainer(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    /** Maps button ID to an i18n tooltip key, or null if none. */
    protected abstract String fsotTooltipKey(int id);

    protected void drawFsotTooltip(int mouseX, int mouseY) {
        for (GuiButton btn : buttonList) {
            if (mouseX >= btn.xPosition
                    && mouseX < btn.xPosition + btn.width
                    && mouseY >= btn.yPosition
                    && mouseY < btn.yPosition + btn.height) {
                String key = fsotTooltipKey(btn.id);
                if (key != null) {
                    drawHoveringText(Collections.singletonList(I18n.format(key)), mouseX, mouseY, fontRendererObj);
                }
                return;
            }
        }
    }

    protected void drawMcSlot(int x, int y) {
        drawRect(x, y, x + 18, y + 18, C_SLOT);
        drawRect(x, y, x + 18, y + 1, C_SLOT_SH);
        drawRect(x, y, x + 1, y + 18, C_SLOT_SH);
        drawRect(x, y + 17, x + 18, y + 18, C_SLOT_HI);
        drawRect(x + 17, y, x + 18, y + 18, C_SLOT_HI);
    }

    public static void drawBeveledRect(int x, int y, int w, int h, int fill, int topLeft, int bottomRight) {
        drawRect(x, y, x + w, y + h, fill);
        drawRect(x, y, x + w, y + 1, topLeft);
        drawRect(x, y, x + 1, y + h, topLeft);
        drawRect(x, y + h - 1, x + w, y + h, bottomRight);
        drawRect(x + w - 1, y, x + w, y + h, bottomRight);
    }
}
