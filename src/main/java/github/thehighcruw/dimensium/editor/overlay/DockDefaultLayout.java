/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.overlay;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiDockNodeFlags;
import imgui.internal.ImGui;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
final class DockDefaultLayout {

    private DockDefaultLayout() {}

    static void apply(int dockspaceId, int sw, float sh) {
        // Window title strings must match exactly what each window passes to ImGui.begin().
        String toolsTitle = I18n.format("dimensium.ui.panel.tools");
        String toolOptTitle = I18n.format("dimensium.ui.panel.tool_options");
        String vpTitle = I18n.format("dimensium.ui.panel.viewport");
        String historyTitle = I18n.format("dimensium.ui.window.history");

        // Windows that use ###id suffixes — only the stable id part after ### is needed
        // for DockBuilderDockWindow (ImGui hashes the ###id portion for the window identity).
        String maskListTitle = I18n.format("dimensium.mask.list.title") + "###tool_mask_list";
        String maskEditorTitle = I18n.format("dimensium.mask.editor.title") + "###tool_mask_editor";
        String paletteTitle = I18n.format("dimensium.palette.window.title") + "###palette_window";
        String palEditorTitle = I18n.format("dimensium.palette.editor.title") + "###palette_editor_window";
        String selectionTitle = I18n.format("dimensium.ui.window.selection") + "###selection_window";
        String operationsTitle = I18n.format("dimensium.ui.window.operations") + "###operations_window";
        String clipboardTitle = I18n.format("dimensium.ui.window.clipboard") + "###clipboard_window";
        String blockInfoTitle = I18n.format("dimensium.block_info.title") + "###block_info_window";

        // ImGuiDockNodeFlags_DockSpace = 1024 (internal flag, not in the public Java enum).
        // Required when calling DockBuilderAddNode for a node that DockSpace() manages.
        int dockSpaceFlag = 1024;
        ImGui.dockBuilderRemoveNode(dockspaceId);
        ImGui.dockBuilderAddNode(dockspaceId, ImGuiDockNodeFlags.None | dockSpaceFlag);
        ImGui.dockBuilderSetNodeSize(dockspaceId, sw, sh);

        // imgui-java dockBuilderSplitNode: first ImInt = at-dir node, second = opposite-dir node.
        // Return value = at-dir node (NOT opposite-dir as in C++) — do not use as remainder.

        // ── Step 1: carve out the right column (316 px) ──────────────────────────
        ImInt rightColId = new ImInt();
        ImInt centerLeftId = new ImInt();
        float rightRatio = 316f / sw;
        ImGui.dockBuilderSplitNode(dockspaceId, ImGuiDir.Right, rightRatio, rightColId, centerLeftId);

        // ── Step 2: carve out the left column (400 px) from the remaining space ──
        ImInt leftColId = new ImInt();
        ImInt vpId = new ImInt();
        float leftRatio = 400f / (sw - 316f);
        ImGui.dockBuilderSplitNode(centerLeftId.get(), ImGuiDir.Left, leftRatio, leftColId, vpId);

        // ── Step 3: split left column into 3 rows ────────────────────────────────
        // Top slice: Tools panel (~160 px)
        ImInt toolsTopId = new ImInt();
        ImInt toolsBotHalf = new ImInt();
        float toolsTopRatio = 160f / sh;
        ImGui.dockBuilderSplitNode(leftColId.get(), ImGuiDir.Up, toolsTopRatio, toolsTopId, toolsBotHalf);

        // Remaining left column: split 50/50 → Tool Options (top) + Masks (bottom)
        ImInt toolOptId = new ImInt();
        ImInt maskNodeId = new ImInt();
        ImGui.dockBuilderSplitNode(toolsBotHalf.get(), ImGuiDir.Up, 0.5f, toolOptId, maskNodeId);

        // ── Step 4: split right column into 3 rows ───────────────────────────────
        // Top slice: Palette (~40 % of height)
        ImInt rightTopId = new ImInt();
        ImInt rightBotHalf = new ImInt();
        ImGui.dockBuilderSplitNode(rightColId.get(), ImGuiDir.Up, 0.4f, rightTopId, rightBotHalf);

        // Remaining right column: split ~58/42 → Selection+Ops+Clip (top) + BlockInfo+History (bottom)
        ImInt rightMidId = new ImInt();
        ImInt rightBotId = new ImInt();
        ImGui.dockBuilderSplitNode(rightBotHalf.get(), ImGuiDir.Up, 0.58f, rightMidId, rightBotId);

        // ── Step 5: dock windows ─────────────────────────────────────────────────
        // Left column
        ImGui.dockBuilderDockWindow(toolsTitle, toolsTopId.get());
        ImGui.dockBuilderDockWindow(toolOptTitle, toolOptId.get());
        ImGui.dockBuilderDockWindow(maskListTitle, maskNodeId.get());
        ImGui.dockBuilderDockWindow(maskEditorTitle, maskNodeId.get()); // tab alongside Tool Masks

        // Center
        ImGui.dockBuilderDockWindow(vpTitle, vpId.get());

        // Right column
        ImGui.dockBuilderDockWindow(paletteTitle, rightTopId.get());
        ImGui.dockBuilderDockWindow(palEditorTitle, rightTopId.get()); // tab alongside Palette

        ImGui.dockBuilderDockWindow(selectionTitle, rightMidId.get());
        ImGui.dockBuilderDockWindow(operationsTitle, rightMidId.get()); // tabs
        ImGui.dockBuilderDockWindow(clipboardTitle, rightMidId.get()); // tabs

        ImGui.dockBuilderDockWindow(blockInfoTitle, rightBotId.get());
        ImGui.dockBuilderDockWindow(historyTitle, rightBotId.get()); // tab alongside Block Info

        ImGui.dockBuilderFinish(dockspaceId);
    }
}
