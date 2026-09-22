/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.overlay.MenuBar;
import github.thehighcruw.dimensium.editor.tool.Tool;
import github.thehighcruw.dimensium.editor.tool.ToolRegistry;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.ToolStates;
import github.thehighcruw.dimensium.editor.window.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.editor.window.imgui.ToolIconCache;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.type.ImBoolean;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class ToolWindow extends ToggleableWindow {

    static final int INITIAL_WIDTH = 400;

    public ToolWindow() {
        super(true);
    }

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowToolPanelOpen(value);
    }

    // ── Tool grid ─────────────────────────────────────────────────────────────

    // Display order — categories grouped. Column count from DimensiumConfig.toolGridColumns.
    static final Tool[] GRID_TOOLS = {
        // selecting
        Tool.POINTER,
        Tool.SELECT,
        Tool.MAGIC_SELECT,
        Tool.FREEHAND_SELECT,
        Tool.LASSO_SELECT,
        // creating
        Tool.FREEHAND_DRAW,
        Tool.SCULPT_DRAW,
        Tool.SHAPE,
        Tool.FILL,
        Tool.STAMP,
        Tool.PATH,
        Tool.MODELLING,
        Tool.ROCK,
        // painting
        Tool.PAINTER,
        Tool.NOISE,
        Tool.GRADIENT,
        // manipulating
        Tool.SMOOTH,
        Tool.WELD,
        Tool.MELT,
        Tool.ROUGHEN,
        Tool.EXTRUDE,
        Tool.MOVE,
        Tool.ELEVATION,
        Tool.SLOPE,
        Tool.DISTORT,
        Tool.SHATTER,
        // utility
        Tool.RULER,
    };

    // ── Per-tool section cache ────────────────────────────────────────────────

    private static Map<Tool, ToolSection> buildSectionMap() {
        ToolStates s = ToolStates.INSTANCE;
        Map<Tool, ToolSection> map = new EnumMap<>(Tool.class);
        for (Tool t : Tool.values()) {
            ToolSection section = ToolRegistry.createSection(t, s);
            if (section != null) map.put(t, section);
        }
        return map;
    }

    final Map<Tool, ToolSection> sectionMap = buildSectionMap();

    public void render(int sh) {
        if (!open) return;
        float menuH = MenuBar.INSTANCE.height();
        float scale = ImGuiManager.INSTANCE.getUIScale();
        float physW = INITIAL_WIDTH * scale;
        ImGui.setNextWindowPos(0, menuH, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(physW, sh - menuH, ImGuiCond.FirstUseEver);

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8f * scale, 8f * scale);
        ImBoolean pOpen = new ImBoolean(true);
        ImGui.begin(I18n.format("dimensium.ui.panel.tools"), pOpen);
        captureBounds();
        if (!pOpen.get()) {
            setOpen(false);
            ImGui.end();
            ImGui.popStyleVar();
            return;
        }

        renderToolGrid();

        ImGui.end();
        ImGui.popStyleVar();
    }

    private void renderToolGrid() {
        float available = ImGui.getContentRegionAvailX();
        int cols = DimensiumConfig.toolGridColumns;
        float gap = 2f;
        float iconSize = (available - gap * (cols - 1)) / cols - DeferredItemRender.ITEM_PAD * 2f;
        Tool selectedTool = DimensiumEditorMode.INSTANCE.selectedTool;

        for (int i = 0; i < GRID_TOOLS.length; i++) {
            int col = i % cols;
            if (col != 0) ImGui.sameLine(0f, gap);
            Tool tool = GRID_TOOLS[i];
            int texId = ToolIconCache.INSTANCE.getTexture(tool);
            String tooltip = I18n.format(tool.label);
            if (DeferredItemRender.placeIconButton("##tool" + i, texId, iconSize, tool == selectedTool, tooltip)) {
                DimensiumEditorMode.INSTANCE.selectedTool = tool;
            }
        }
    }
}
