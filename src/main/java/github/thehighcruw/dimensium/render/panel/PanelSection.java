/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.panel;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTreeNodeFlags;

/**
 * Standardized collapsible section for Dimensium panels.
 *
 * Usage:
 * if (PanelSection.begin("Label")) {
 * // ImGui widgets...
 * }
 * PanelSection.end();
 *
 * Always call end() regardless of begin()'s return value.
 */
@SideOnly(Side.CLIENT)
public final class PanelSection {

    private static final float R = 0.30f;
    private static final float G = 0.35f;
    private static final float B = 0.45f;

    private PanelSection() {}

    public static boolean begin(String label) {
        ImGui.pushStyleColor(ImGuiCol.Header, R * 0.70f, G * 0.70f, B * 0.70f, 0.85f);
        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, R * 1.10f, G * 1.10f, B * 1.10f, 0.90f);
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, R * 1.40f, G * 1.40f, B * 1.40f, 1.00f);
        boolean open = ImGui.collapsingHeader(label, ImGuiTreeNodeFlags.DefaultOpen);
        ImGui.popStyleColor(3);
        return open;
    }

    public static void end() {
        ImGui.spacing();
    }
}
