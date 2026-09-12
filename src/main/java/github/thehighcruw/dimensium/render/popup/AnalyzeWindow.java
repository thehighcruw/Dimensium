/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.popup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.render.imgui.ImGuiManager;
import github.thehighcruw.dimensium.render.imgui.ImGuiWindow;
import github.thehighcruw.dimensium.tool.state.SelectionState;
import imgui.ImGui;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class AnalyzeWindow extends ImGuiWindow {

    public static final AnalyzeWindow INSTANCE = new AnalyzeWindow();

    private static final String WINDOW_ID = "###analyze_window";

    private boolean open = false;

    private final List<String> blockNames = new ArrayList<>();
    private final List<Integer> blockCounts = new ArrayList<>();
    private int totalBlocks = 0;

    private AnalyzeWindow() {}

    @Override
    public boolean isOpen() {
        return open;
    }

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

        net.minecraft.world.World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;

        Map<String, Integer> counts = new HashMap<>();
        for (long key : sel.getSelectedBlocks()) {
            int x = SelectionState.unpackX(key);
            int y = SelectionState.unpackY(key);
            int z = SelectionState.unpackZ(key);
            Block b = world.getBlock(x, y, z);
            if (b == null || b == Blocks.air) continue;
            Item item = Item.getItemFromBlock(b);
            int meta = world.getBlockMetadata(x, y, z);
            String name;
            if (item != null) {
                name = new ItemStack(item, 1, meta).getDisplayName();
            } else {
                name = b.getLocalizedName();
            }
            counts.put(name, counts.getOrDefault(name, 0) + 1);
            totalBlocks++;
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {

            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return Integer.compare(b.getValue(), a.getValue());
            }
        });

        for (Map.Entry<String, Integer> e : entries) {
            blockNames.add(e.getKey());
            blockCounts.add(e.getValue());
        }
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui
            .begin(I18n.format("dimensium.op.analyze.title") + WINDOW_ID, pOpen, ImGuiWindowFlags.None);
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
