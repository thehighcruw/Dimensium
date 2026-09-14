/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.shared.SelectionState;
import imgui.ImGui;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
public class AnalyzeWindow extends ToggleableWindow {

    public static final AnalyzeWindow INSTANCE = new AnalyzeWindow();

    private static final String WINDOW_ID = "###analyze_window";

    private final List<String> blockNames = new ArrayList<>();
    private final List<Integer> blockCounts = new ArrayList<>();
    private int totalBlocks = 0;

    private AnalyzeWindow() {}

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowAnalyzeOpen(value);
        if (value) refresh();
    }

    public void open() {
        setOpen(true);
    }

    private void refresh() {
        blockNames.clear();
        blockCounts.clear();
        totalBlocks = 0;

        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection()) return;

        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;

        Map<String, Integer> counts = new HashMap<>();
        for (long key : sel.getSelectedBlocks()) {
            SelectionState.BlockInfo info = SelectionState.unpackBlock(key);
            if (info == null) continue;

            String name;
            if (info.item() != null) {
                name = new ItemStack(info.item(), 1, info.meta()).getDisplayName();
            } else {
                name = info.block().getLocalizedName();
            }
            counts.put(name, counts.getOrDefault(name, 0) + 1);
            totalBlocks++;
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        for (Map.Entry<String, Integer> e : entries) {
            blockNames.add(e.getKey());
            blockCounts.add(e.getValue());
        }
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        ImBoolean pOpen = new ImBoolean(true);
        boolean visible =
                ImGui.begin(I18n.format("dimensium.op.analyze.title") + WINDOW_ID, pOpen, ImGuiWindowFlags.None);
        captureBounds();
        if (visible && pOpen.get()) {
            if (!SelectionState.INSTANCE.hasSelection()) {
                ImGui.textDisabled(I18n.format("dimensium.op.analyze.no_selection"));
            } else {
                if (ImGui.button("##analyze_refresh")) {
                    refresh();
                }
                ImGui.sameLine();
                ImGui.text(I18n.format("dimensium.op.analyze.total") + ": " + totalBlocks);

                int tableFlags = ImGuiTableFlags.Borders | ImGuiTableFlags.RowBg | ImGuiTableFlags.ScrollY;
                float tableHeight = 300f * scale;
                if (ImGui.beginTable("##analyze_table", 3, tableFlags, 0f, tableHeight)) {
                    ImGui.tableSetupColumn(I18n.format("dimensium.op.analyze.block"));
                    ImGui.tableSetupColumn(I18n.format("dimensium.op.analyze.count"));
                    ImGui.tableSetupColumn(I18n.format("dimensium.op.analyze.percent"));
                    ImGui.tableHeadersRow();

                    for (int i = 0; i < blockNames.size(); i++) {
                        ImGui.tableNextRow();
                        ImGui.tableNextColumn();
                        ImGui.text(blockNames.get(i));
                        ImGui.tableNextColumn();
                        ImGui.text(String.valueOf(blockCounts.get(i)));
                        ImGui.tableNextColumn();
                        float pct = totalBlocks > 0 ? blockCounts.get(i) * 100f / totalBlocks : 0f;
                        ImGui.text(String.format("%.1f%%", pct));
                    }
                    ImGui.endTable();
                }
            }
        }
        ImGui.end();
        if (!pOpen.get()) setOpen(false);
    }
}
