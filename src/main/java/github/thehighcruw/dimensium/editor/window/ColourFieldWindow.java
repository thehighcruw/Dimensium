/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.handler.EditorActions;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.shared.BlockColorCache;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class ColourFieldWindow extends ToggleableWindow {

    public static final ColourFieldWindow INSTANCE = new ColourFieldWindow();

    private final ImBoolean includeSolid = new ImBoolean(true);
    private final ImBoolean includeTranslucent = new ImBoolean(false);
    private final ImBoolean includeTileEntities = new ImBoolean(false);

    private static final String WINDOW_ID = "###colour_field_window";

    private ColourFieldWindow() {}

    public void open() {
        open = true;
    }

    public void close() {
        open = false;
    }

    public void renderImGui() {
        if (!open) return;

        float uiScale = ImGuiManager.INSTANCE.getUIScale();
        float w = 320f * uiScale;
        float vpW = ImGui.getIO()
            .getDisplaySizeX(),
            vpH = ImGui.getIO()
                .getDisplaySizeY();
        ImGui.setNextWindowPos((vpW - w) * 0.5f, vpH * 0.35f, ImGuiCond.Appearing);
        ImGui.setNextWindowSize(w, 200f * uiScale, ImGuiCond.Appearing);

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui.begin(I18n.format("dimensium.op.colour_field.title") + WINDOW_ID, pOpen);
        captureBounds();

        if (visible) {
            boolean canApply = includeSolid.get() || includeTranslucent.get() || includeTileEntities.get();

            float windowW = ImGui.getWindowWidth();
            float btnW = 70f * uiScale;
            float footerH = ImGui.getStyle()
                .getItemSpacingY() + 1f
                + ImGui.getStyle()
                    .getItemSpacingY()
                + ImGui.getFrameHeight()
                + ImGui.getStyle()
                    .getWindowPaddingY();
            float childH = Math.max(0f, ImGui.getContentRegionAvailY() - footerH);
            ImGui.beginChild("##cf_body", 0f, childH);

            ImGui.text(I18n.format("dimensium.op.colour_field.include"));
            ImGui.spacing();

            ImGui.checkbox(I18n.format("dimensium.op.colour_field.solid") + "##cf_solid", includeSolid);
            ImGui.checkbox(I18n.format("dimensium.op.colour_field.translucent") + "##cf_trans", includeTranslucent);
            ImGui.checkbox(I18n.format("dimensium.op.colour_field.tile_entities") + "##cf_te", includeTileEntities);

            ImGui.endChild();

            ImGui.separator();
            ImGui.setCursorPosX(
                windowW - ImGui.getStyle()
                    .getWindowPaddingX() - btnW);
            if (!canApply) ImGui.beginDisabled();
            if (ImGui.button(I18n.format("dimensium.op.apply") + "##cf_apply", btnW, 0)) {
                int mask = 0;
                if (includeSolid.get()) mask |= BlockColorCache.CAT_SOLID;
                if (includeTranslucent.get()) mask |= BlockColorCache.CAT_TRANSLUCENT;
                if (includeTileEntities.get()) mask |= BlockColorCache.CAT_TILE_ENTITY;
                EditorActions.generateColourField(mask);
                close();
            }
            if (!canApply) ImGui.endDisabled();
        }

        ImGui.end();
        if (!pOpen.get()) close();
    }
}
