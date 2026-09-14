/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.inventory;

import java.util.List;

import net.minecraft.client.gui.GuiButton;

public class CreativeGuiUtils {

    // Button IDs
    private static final int BTN_F = 10, BTN_S = 11, BTN_O = 12, BTN_T = 13;

    public static void addFsotButtons(List<GuiButton> buttonList, int guiLeft, int guiTop, int panelWidth) {
        // FSOT — top-right, matching gradient helper layout
        int fsotRightEdge = guiLeft + panelWidth - 6;
        int fsotY = guiTop + 5;

        buttonList.add(new GuiToggleButton(BTN_T, fsotRightEdge - 16, fsotY, 16, 12, "T", false));
        buttonList.add(new GuiToggleButton(BTN_O, fsotRightEdge - 35, fsotY, 16, 12, "O", false));
        buttonList.add(new GuiToggleButton(BTN_S, fsotRightEdge - 54, fsotY, 16, 12, "S", false));
        buttonList.add(new GuiToggleButton(BTN_F, fsotRightEdge - 73, fsotY, 16, 12, "F", false));
    }
}
