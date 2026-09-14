/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import github.thehighcruw.dimensium.Dimensium;
import org.lwjgl.input.Keyboard;

public final class UIUtils {

    private UIUtils() {}

    public static String getKeyShortcutName(int key, int mods) {
        StringBuilder sb = new StringBuilder();
        if ((mods & Dimensium.MOD_CTRL) != 0) sb.append("Ctrl+");
        if ((mods & Dimensium.MOD_SHIFT) != 0) sb.append("Shift+");
        if ((mods & Dimensium.MOD_ALT) != 0) sb.append("Alt+");
        sb.append(Keyboard.getKeyName(key));
        return sb.toString();
    }
}
