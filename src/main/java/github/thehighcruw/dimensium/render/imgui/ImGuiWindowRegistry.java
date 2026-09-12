/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.imgui;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ImGuiWindowRegistry {

    public static final ImGuiWindowRegistry INSTANCE = new ImGuiWindowRegistry();

    private final List<ImGuiWindow> windows = new ArrayList<>();

    private ImGuiWindowRegistry() {}

    public void register(ImGuiWindow window) {
        windows.add(window);
    }

    public boolean anyContainsMouse(float mx, float my) {
        for (ImGuiWindow window : windows) {
            if (window.containsMouse(mx, my)) return true;
        }
        return false;
    }
}
