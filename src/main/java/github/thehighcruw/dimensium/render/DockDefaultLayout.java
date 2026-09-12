/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render;

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
        String toolsTitle = I18n.format("dimensium.ui.panel.tools");
        String toolOptionsTitle = I18n.format("dimensium.ui.panel.tool_options");
        String propsTitle = I18n.format("dimensium.ui.panel.properties");
        String vpTitle = I18n.format("dimensium.ui.panel.viewport");

        // ImGuiDockNodeFlags_DockSpace = 1024 (internal flag, not in the public Java enum).
        // Required when calling DockBuilderAddNode for a node that DockSpace() manages.
        int dockSpaceFlag = 1024;
        ImGui.dockBuilderRemoveNode(dockspaceId);
        ImGui.dockBuilderAddNode(dockspaceId, ImGuiDockNodeFlags.None | dockSpaceFlag);
        ImGui.dockBuilderSetNodeSize(dockspaceId, sw, sh);

        // imgui-java dockBuilderSplitNode: first ImInt = at-dir node, second = opposite-dir node.
        // Return value = at-dir node (NOT opposite-dir as in C++) — do not use as remainder.
        ImInt propsId = new ImInt(); // at-dir (right, 316 px)
        ImInt leftNode = new ImInt(); // opposite (left remainder)
        ImInt toolsId = new ImInt(); // at-dir (left, 400 px)
        ImInt vpId = new ImInt(); // opposite (center remainder)
        ImInt toolsTopId = new ImInt(); // at-dir (top of left column, ~160 px)
        ImInt toolsOptId = new ImInt(); // opposite (bottom of left column)

        float rightRatio = 316f / sw;
        float leftRatio = 400f / (sw - 316f);
        float toolTypeRatio = 160f / sh;

        ImGui.dockBuilderSplitNode(dockspaceId, ImGuiDir.Right, rightRatio, propsId, leftNode);
        ImGui.dockBuilderSplitNode(leftNode.get(), ImGuiDir.Left, leftRatio, toolsId, vpId);
        ImGui.dockBuilderSplitNode(toolsId.get(), ImGuiDir.Up, toolTypeRatio, toolsTopId, toolsOptId);

        ImGui.dockBuilderDockWindow(toolsTitle, toolsTopId.get());
        ImGui.dockBuilderDockWindow(toolOptionsTitle, toolsOptId.get());
        ImGui.dockBuilderDockWindow(propsTitle, propsId.get());
        ImGui.dockBuilderDockWindow(vpTitle, vpId.get());

        ImGui.dockBuilderFinish(dockspaceId);
    }
}
