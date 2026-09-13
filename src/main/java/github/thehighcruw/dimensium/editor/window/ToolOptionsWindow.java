/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.overlay.MenuBar;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiWindow;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class ToolOptionsWindow extends ImGuiWindow {

    private final ToolWindow toolWindow;
    private boolean open = true;

    public ToolOptionsWindow(ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowToolOptionsPanelOpen(value);
    }

    public void render(int sh) {
        if (!open) return;
        float menuH = MenuBar.INSTANCE.height();
        float scale = ImGuiManager.INSTANCE.getUIScale();
        float physW = toolWindow.currentW * scale;
        ImGui.setNextWindowPos(0, menuH + 100 * scale, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(physW, sh - menuH - 100 * scale, ImGuiCond.FirstUseEver);

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8f * scale, 8f * scale);
        ImBoolean pOpen = new ImBoolean(true);
        ImGui.begin(I18n.format("dimensium.ui.panel.tool_options"), pOpen);
        captureBounds();
        if (!pOpen.get()) {
            setOpen(false);
            ImGui.end();
            ImGui.popStyleVar();
            return;
        }

        ToolSection section = toolWindow.sectionMap.get(DimensiumEditorMode.INSTANCE.selectedTool);
        if (section != null) {
            section.render();
        }

        ImGui.end();
        ImGui.popStyleVar();
    }
}
