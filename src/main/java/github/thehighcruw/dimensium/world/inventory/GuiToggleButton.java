/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.inventory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

@SideOnly(Side.CLIENT)
public class GuiToggleButton extends GuiButton {

    private boolean active;

    public GuiToggleButton(int id, int x, int y, int w, int h, String label, boolean active) {
        super(id, x, y, w, h, label);
        this.active = active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // MC panel colors — keep in sync with GuiColourPicker
    private static final int C_PANEL = 0xFFC6C6C6;
    private static final int C_PANEL_HI = 0xFFFFFFFF;
    private static final int C_PANEL_SH = 0xFF555555;
    private static final int C_ACTIVE = 0xFF7EA87E; // green tint for active state

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        boolean hovered = mouseX >= this.xPosition
                && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width
                && mouseY < this.yPosition + this.height;

        int fill = active ? C_ACTIVE : (hovered ? 0xFFD4D4D4 : C_PANEL);

        if (active) {
            // Depressed: shadow top-left, highlight bottom-right
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, fill);
            drawRect(xPosition, yPosition, xPosition + width, yPosition + 1, C_PANEL_SH);
            drawRect(xPosition, yPosition, xPosition + 1, yPosition + height, C_PANEL_SH);
            drawRect(xPosition, yPosition + height - 1, xPosition + width, yPosition + height, C_PANEL_HI);
            drawRect(xPosition + width - 1, yPosition, xPosition + width, yPosition + height, C_PANEL_HI);
        } else {
            // Raised: highlight top-left, shadow bottom-right
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, fill);
            drawRect(xPosition, yPosition, xPosition + width, yPosition + 1, C_PANEL_HI);
            drawRect(xPosition, yPosition, xPosition + 1, yPosition + height, C_PANEL_HI);
            drawRect(xPosition, yPosition + height - 1, xPosition + width, yPosition + height, C_PANEL_SH);
            drawRect(xPosition + width - 1, yPosition, xPosition + width, yPosition + height, C_PANEL_SH);
        }

        int textColor = active ? 0xFF204020 : 0xFF404040;
        mc.fontRenderer.drawString(
                this.displayString,
                xPosition + (width - mc.fontRenderer.getStringWidth(this.displayString)) / 2,
                yPosition + (height - 8) / 2,
                textColor);
    }
}
