/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import java.util.List;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.overlay.LayoutPresetRegistry;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;

@SideOnly(Side.CLIENT)
public class LayoutPresetManageWindow extends ToggleableWindow {

    public static final LayoutPresetManageWindow INSTANCE = new LayoutPresetManageWindow();

    private static final String WINDOW_ID = "###layout_preset_manage";

    private int renamingIndex = -1;
    private boolean renamingFocused = false;
    private final ImString renameBuffer = new ImString(128);

    private LayoutPresetManageWindow() {}

    public void open() {
        open = true;
        renamingIndex = -1;
        renamingFocused = false;
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        ImGui.setNextWindowSize(360f * scale, 300f * scale, imgui.flag.ImGuiCond.Appearing);

        ImBoolean openBool = new ImBoolean(open);
        boolean visible = ImGui
            .begin(I18n.format("dimensium.layout.preset.manage.title") + WINDOW_ID, openBool, ImGuiWindowFlags.None);
        captureBounds();
        if (!openBool.get()) open = false;

        if (visible) {
            List<String> presets = LayoutPresetRegistry.INSTANCE.list();
            String active = LayoutPresetRegistry.INSTANCE.getActive();

            if (presets.isEmpty()) {
                ImGui.textDisabled(I18n.format("dimensium.layout.preset.manage.empty"));
            }

            for (int i = 0; i < presets.size(); i++) {
                String name = presets.get(i);
                boolean isActive = name.equals(active);

                ImGui.pushID(i);

                if (renamingIndex == i) {
                    ImGui.setNextItemWidth(-1f);
                    ImGui.setKeyboardFocusHere(0);
                    if (ImGui.inputText("##rename", renameBuffer, ImGuiInputTextFlags.EnterReturnsTrue)) {
                        String newName = renameBuffer.get()
                            .trim();
                        if (!newName.isEmpty() && !newName.equals(name)) {
                            LayoutPresetRegistry.INSTANCE.rename(name, newName);
                        }
                        renamingIndex = -1;
                        renamingFocused = false;
                    }
                    if (ImGui.isItemActive() || ImGui.isItemFocused()) {
                        renamingFocused = true;
                    } else if (renamingFocused) {
                        renamingIndex = -1;
                        renamingFocused = false;
                    }
                } else {
                    if (isActive) {
                        ImGui.textColored(0.4f, 0.9f, 0.4f, 1f, "* " + name);
                    } else {
                        ImGui.text(name);
                    }

                    if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
                        renamingIndex = i;
                        renameBuffer.set(name);
                    }
                }

                ImGui.sameLine();
                float buttonW = 60f * scale;
                ImGui.setCursorPosX(
                    ImGui.getWindowWidth() - buttonW * 2f
                        - ImGui.getStyle()
                            .getItemSpacingX()
                        - ImGui.getStyle()
                            .getWindowPaddingX());

                if (ImGui.button(I18n.format("dimensium.layout.preset.manage.load") + "##load", buttonW, 0)) {
                    LayoutPresetRegistry.INSTANCE.load(name);
                    open = false;
                }
                ImGui.sameLine();
                if (ImGui.button(I18n.format("dimensium.layout.preset.manage.delete") + "##del", buttonW, 0)) {
                    LayoutPresetRegistry.INSTANCE.delete(name);
                }

                ImGui.popID();
            }

            if (!presets.isEmpty()) {
                ImGui.separator();
            }
            ImGui.textDisabled(I18n.format("dimensium.layout.preset.manage.hint_rename"));
        }

        ImGui.end();
    }
}
