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

        int topLeft = active ? C_PANEL_SH : C_PANEL_HI;
        int bottomRight = active ? C_PANEL_HI : C_PANEL_SH;
        AbstractFsotGuiContainer.drawBeveledRect(xPosition, yPosition, width, height, fill, topLeft, bottomRight);

        int textColor = active ? 0xFF204020 : 0xFF404040;
        mc.fontRenderer.drawString(
                this.displayString,
                xPosition + (width - mc.fontRenderer.getStringWidth(this.displayString)) / 2,
                yPosition + (height - 8) / 2,
                textColor);
    }
}
