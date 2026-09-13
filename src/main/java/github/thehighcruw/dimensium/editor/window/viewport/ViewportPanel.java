/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.overlay.MenuBar;
import github.thehighcruw.dimensium.editor.overlay.ViewState;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTabItemFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public final class ViewportPanel {

    public static final ViewportPanel INSTANCE = new ViewportPanel();

    private ViewportPanel() {}

    private boolean hovered = false;
    private int pendingSelectIndex = -1;

    /** Request that the tab at the given index is shown as selected on the next frame. */
    public void requestSelectIndex(int index) {
        pendingSelectIndex = index;
    }

    /** Always false; kept for API compatibility with OverlayRenderer cursor logic. */
    public boolean resizeCursorActive = false;

    /** True if the mouse was hovering the viewport in the last rendered frame. */
    public boolean isHovered() {
        return hovered;
    }

    /**
     * sw/sh are physical pixels (matching ImGui display units).
     */
    public void render(int sw, int sh) {
        if (ViewportRegistry.INSTANCE.viewports.isEmpty()) return;

        float menuH = MenuBar.INSTANCE.height();
        float scale = ImGuiManager.INSTANCE.getUIScale();
        float defaultLeftW = 400 * scale;
        float defaultRightW = 316 * scale;
        float vpW = sw - defaultLeftW - defaultRightW;
        float vpH = sh - menuH;

        ImGui.setNextWindowPos(defaultLeftW, menuH, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(vpW, vpH, ImGuiCond.FirstUseEver);

        // No border on outer window — border is drawn by the child image window only.
        // ItemSpacing.y=0 eliminates the gap between the tab bar and image content.
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0f);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 6f, 0f);
        // Tab colours: inactive blends with panel bg; active uses blue accent; hovered=active so hover on selected tab
        // is inert.
        ImGui.pushStyleColor(ImGuiCol.Tab, 0.08f, 0.08f, 0.12f, 1.00f);
        ImGui.pushStyleColor(ImGuiCol.TabHovered, 0.24f, 0.50f, 1.00f, 0.85f);
        ImGui.pushStyleColor(ImGuiCol.TabActive, 0.24f, 0.50f, 1.00f, 0.85f);
        ImGui.pushStyleColor(ImGuiCol.TabUnfocused, 0.06f, 0.06f, 0.09f, 1.00f);
        ImGui.pushStyleColor(ImGuiCol.TabUnfocusedActive, 0.14f, 0.30f, 0.60f, 1.00f);

        int flags = ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse;

        ImGui.begin(I18n.format("dimensium.ui.panel.viewport"), flags);
        hovered = ImGui.isWindowHovered(ImGuiHoveredFlags.ChildWindows);

        if (ImGui.beginTabBar("##viewportTabs")) {
            for (int i = 0; i < ViewportRegistry.INSTANCE.viewports.size(); i++) {
                ViewportState vp = ViewportRegistry.INSTANCE.viewports.get(i);
                int texId = vp.texId;

                ImBoolean open = ViewportRegistry.INSTANCE.viewports.size() > 1 ? new ImBoolean(true) : null;
                int tabFlags = (i == pendingSelectIndex) ? ImGuiTabItemFlags.SetSelected : ImGuiTabItemFlags.None;
                if (i == pendingSelectIndex) pendingSelectIndex = -1;
                boolean tabVisible = open != null ? ImGui.beginTabItem(vp.label + "##vp" + i, open, tabFlags)
                    : ImGui.beginTabItem(vp.label + "##vp" + i, tabFlags);
                boolean closed = open != null && !open.get();

                if (tabVisible) {
                    if (i != ViewportRegistry.INSTANCE.activeIndex()) ViewportRegistry.INSTANCE.setActive(i);

                    if (texId != -1) {
                        // Child window carries the 1px blue border; no padding so image fills edge-to-edge.
                        ImGui.pushStyleVar(ImGuiStyleVar.ChildBorderSize, 1f);
                        ImGui.pushStyleColor(ImGuiCol.Border, 0.24f, 0.50f, 1.00f, 0.60f);

                        boolean childOpen = ImGui.beginChild(
                            "##vpimg" + i,
                            0f,
                            0f,
                            true,
                            ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

                        if (childOpen) {
                            float imgX = ImGui.getCursorScreenPosX();
                            float imgY = ImGui.getCursorScreenPosY();
                            float imgW = ImGui.getContentRegionAvailX();
                            float imgH = ImGui.getContentRegionAvailY();
                            vp.contentX = imgX;
                            vp.contentY = imgY;
                            vp.contentW = imgW;
                            vp.contentH = imgH;
                            // Sample a viewport-sized window centered at the texture's midpoint (= camera forward).
                            // This recenters the world render on the viewport regardless of panel widths,
                            // and clips to the viewport bounds. Round to integer texels to avoid GL_NEAREST jitter.
                            float texW = Math.round(imgW);
                            float texH = Math.round(imgH);
                            float u0 = (sw / 2f - texW / 2f) / sw;
                            float u1 = (sw / 2f + texW / 2f) / sw;
                            float v0 = (sh / 2f + texH / 2f) / sh; // GL origin bottom-left; top of image = higher v
                            float v1 = (sh / 2f - texH / 2f) / sh;
                            if (ViewState.INSTANCE.flipCanvas) {
                                float tmp = u0;
                                u0 = u1;
                                u1 = tmp;
                            }
                            ImGui.image(texId, imgW, imgH, u0, v0, u1, v1);
                        }

                        ImGui.endChild();
                        ImGui.popStyleColor();
                        ImGui.popStyleVar();
                    }

                    ImGui.endTabItem();
                }

                if (closed) {
                    ViewportRegistry.INSTANCE.removeViewport(i);
                    break;
                }
            }

            if (ImGui.tabItemButton("+")) {
                ViewportRegistry.INSTANCE.addViewport();
                pendingSelectIndex = ViewportRegistry.INSTANCE.viewports.size() - 1;
            }

            ImGui.endTabBar();
        }

        ImGui.end();

        ImGui.popStyleColor(5); // Tab + TabHovered + TabActive + TabUnfocused + TabUnfocusedActive
        ImGui.popStyleVar(3); // WindowPadding + WindowBorderSize + ItemSpacing

        resizeCursorActive = false;
    }
}
