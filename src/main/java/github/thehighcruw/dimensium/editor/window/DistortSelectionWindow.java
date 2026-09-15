/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.manipulating.distort.DistortToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.SelectionTransforms;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class DistortSelectionWindow extends ToggleableWindow {

    public static final DistortSelectionWindow INSTANCE = new DistortSelectionWindow();

    private final float[] scale = {10f};
    private final float[] distX = {3f};
    private final float[] distY = {3f};
    private final float[] distZ = {3f};
    private long seed = 0L;

    private static final String WINDOW_ID = "###distort_selection_window";

    private DistortSelectionWindow() {}

    public void open() {
        DistortToolState s = DistortToolState.INSTANCE;
        scale[0] = s.distortScale;
        distX[0] = s.distortDistanceX;
        distY[0] = s.distortDistanceY;
        distZ[0] = s.distortDistanceZ;
        seed = s.distortSeed;
        open = true;
    }

    public void close() {
        open = false;
    }

    public void renderImGui() {
        if (!open) return;

        float uiScale = ImGuiManager.INSTANCE.getUIScale();
        float w = 380f * uiScale;
        float vpW = ImGui.getIO().getDisplaySizeX(), vpH = ImGui.getIO().getDisplaySizeY();
        ImGui.setNextWindowPos((vpW - w) * 0.5f, vpH * 0.35f, ImGuiCond.Appearing);
        ImGui.setNextWindowSize(w, 240f * uiScale, ImGuiCond.Appearing);

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui.begin(I18n.format("dimensium.select.distort.title") + WINDOW_ID, pOpen);
        captureBounds();

        if (visible) {
            boolean hasSel = SelectionState.INSTANCE.hasSelection();

            float windowW = ImGui.getWindowWidth();
            float btnW = 70f * uiScale;
            float footerH = ImGui.getStyle().getItemSpacingY()
                    + 1f
                    + ImGui.getStyle().getItemSpacingY()
                    + ImGui.getFrameHeight()
                    + ImGui.getStyle().getWindowPaddingY();
            float childH = Math.max(0f, ImGui.getContentRegionAvailY() - footerH);
            ImGui.beginChild("##distort_body", 0f, childH);

            if (!hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.separator();
            }

            float sliderW = ImGui.getContentRegionAvailX() * 0.6f;
            ImGui.setNextItemWidth(sliderW);
            ImGui.sliderFloat(I18n.format("dimensium.ui.distort.scale") + "##dsel_scale", scale, 1f, 100f);
            ImGui.setNextItemWidth(sliderW);
            ImGui.sliderFloat(I18n.format("dimensium.ui.distort.distance_x") + "##dsel_distx", distX, 0f, 20f);
            ImGui.setNextItemWidth(sliderW);
            ImGui.sliderFloat(I18n.format("dimensium.ui.distort.distance_y") + "##dsel_disty", distY, 0f, 20f);
            ImGui.setNextItemWidth(sliderW);
            ImGui.sliderFloat(I18n.format("dimensium.ui.distort.distance_z") + "##dsel_distz", distZ, 0f, 20f);

            ImGui.spacing();
            if (ImGui.button(I18n.format("dimensium.ui.distort.randomize_seed") + "##dsel_rnd")) {
                seed = ThreadLocalRandom.current().nextLong();
            }
            ImGui.sameLine();
            ImGui.textDisabled(Long.toHexString(seed).toUpperCase());

            ImGui.endChild();

            ImGui.separator();
            ImGui.setCursorPosX(windowW - ImGui.getStyle().getWindowPaddingX() - btnW);
            if (!hasSel) ImGui.beginDisabled();
            if (ImGui.button(I18n.format("dimensium.select.apply") + "##dsel_apply", btnW, 0)) {
                SelectionState sel = SelectionState.INSTANCE;
                if (sel.hasSelection()) {
                    sel.applyOp(
                            SelectionTransforms.distort(
                                    sel.getSelectedBlocks(), scale[0], seed, distX[0], distY[0], distZ[0]),
                            BooleanOp.REPLACE);
                }
                close();
            }
            if (!hasSel) ImGui.endDisabled();
        }

        ImGui.end();
        if (!pOpen.get()) close();
    }
}
