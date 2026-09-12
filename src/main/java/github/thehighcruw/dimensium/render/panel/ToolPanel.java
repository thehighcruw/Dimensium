/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.panel;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.render.imgui.ImGuiWindow;
import github.thehighcruw.dimensium.tool.DimensiumMode;
import github.thehighcruw.dimensium.tool.Tool;
import github.thehighcruw.dimensium.tool.ToolRegistry;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class ToolPanel extends ImGuiWindow {

    public int currentW = 400;
    public static final int MIN_W = 200;
    public static final int MAX_W = 700;

    private boolean open = true;

    @Override
    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowToolPanelOpen(value);
    }

    // ── Tool categories ───────────────────────────────────────────────────────

    private static String[] categoryNames() {
        return new String[] { I18n.format("dimensium.ui.category.selecting"),
            I18n.format("dimensium.ui.category.creating"), I18n.format("dimensium.ui.category.painting"),
            I18n.format("dimensium.ui.category.manipulating"), I18n.format("dimensium.ui.category.utility") };
    }

    static final Tool[][] CATEGORY_TOOLS = {
        { Tool.POINTER, Tool.SELECT, Tool.MAGIC_SELECT, Tool.FREEHAND_SELECT, Tool.LASSO_SELECT },
        { Tool.FREEHAND_DRAW, Tool.SCULPT_DRAW, Tool.SHAPE, Tool.FILL, Tool.STAMP, Tool.PATH, Tool.MODELLING,
            Tool.ROCK },
        { Tool.PAINTER, Tool.NOISE, Tool.GRADIENT }, { Tool.SMOOTH, Tool.WELD, Tool.MELT, Tool.ROUGHEN, Tool.EXTRUDE,
            Tool.MOVE, Tool.ELEVATION, Tool.DISTORT, Tool.SHATTER },
        { Tool.RULER } };

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
    private final ImInt catIdx = new ImInt(0);
    private final ImInt toolIdx = new ImInt(0);

    /** No-op. Mouse is now managed by ImGui. */
    public void updateMouse(int mx, int my) {}

    public void render(int sw, int sh) {
        if (!open) return;
        float menuH = github.thehighcruw.dimensium.render.MenuBar.INSTANCE.height();
        float scale = github.thehighcruw.dimensium.render.imgui.ImGuiManager.INSTANCE.getUIScale();
        float physW = this.currentW * scale;
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

        String[] cats = categoryNames();
        int currentCat = categoryFor(DimensiumMode.INSTANCE.selectedTool);
        catIdx.set(currentCat);
        ImGui.setNextItemWidth(-1);
        if (ImGui.combo("##cat", catIdx, cats) && catIdx.get() != currentCat) {
            DimensiumMode.INSTANCE.selectedTool = CATEGORY_TOOLS[catIdx.get()][0];
        }

        int cat = categoryFor(DimensiumMode.INSTANCE.selectedTool);
        Tool[] tools = CATEGORY_TOOLS[cat];
        String[] toolLabels = new String[tools.length];
        int selTool = 0;
        for (int i = 0; i < tools.length; i++) {
            toolLabels[i] = I18n.format(tools[i].label);
            if (tools[i] == DimensiumMode.INSTANCE.selectedTool) selTool = i;
        }
        toolIdx.set(selTool);
        ImGui.setNextItemWidth(-1);
        if (ImGui.combo("##tool", toolIdx, toolLabels)) {
            DimensiumMode.INSTANCE.selectedTool = tools[toolIdx.get()];
        }

        ImGui.end();
        ImGui.popStyleVar();
    }

    private static int categoryFor(Tool tool) {
        for (int c = 0; c < CATEGORY_TOOLS.length; c++) {
            for (Tool ct : CATEGORY_TOOLS[c]) {
                if (ct == tool) return c;
            }
        }
        return 0;
    }
}
