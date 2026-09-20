/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.elevation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.ElevationToolState.ElevationApply;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.ElevationToolState.ElevationFalloff;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.ElevationToolState.ElevationMode;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.ElevationToolState.FlattenDirection;
import github.thehighcruw.dimensium.editor.window.popup.HeightmapBrowserPopup;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.type.ImInt;
import net.minecraft.client.resources.I18n;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class ElevationSection implements ToolSection {

    private static final float HEIGHTMAP_PREVIEW_SIZE = 48f;

    private final ElevationToolState state;
    private final ImInt modeIdx = new ImInt();
    private final ImInt flattenDirIdx = new ImInt();
    private final ImInt applyIdx = new ImInt();
    private final ImInt falloffIdx = new ImInt();
    private final int[] radius = new int[1];
    private final float[] smoothing = new float[1];
    private final float[] rate = new float[1];
    private final int[] strength = new int[1];

    private HeightmapData lastTexturedHeightmap = null;
    private int heightmapPreviewTexId = -1;

    public ElevationSection(ElevationToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        ImGui.text(I18n.format("dimensium.ui.section.mode"));
        ImGui.separator();

        ElevationMode[] modes = ElevationMode.values();
        String[] modeLabels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) modeLabels[i] = I18n.format(modes[i].label);
        modeIdx.set(state.elevationMode.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.elevation.mode") + "##elev_mode", modeIdx, modeLabels)) {
            state.elevationMode = modes[modeIdx.get()];
        }

        if (state.elevationMode == ElevationMode.FLATTEN) {
            FlattenDirection[] dirs = FlattenDirection.values();
            String[] dirLabels = new String[dirs.length];
            for (int i = 0; i < dirs.length; i++) dirLabels[i] = I18n.format(dirs[i].label);
            flattenDirIdx.set(state.flattenDirection.ordinal());
            if (ImGui.combo(
                    I18n.format("dimensium.ui.elevation.flatten_dir") + "##elev_fdir", flattenDirIdx, dirLabels)) {
                state.flattenDirection = dirs[flattenDirIdx.get()];
            }
        }

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.brush"));
        ImGui.separator();

        radius[0] = state.elevationRadius;
        if (ImGui.sliderInt(
                I18n.format("dimensium.ui.elevation.radius") + "##elev_rad",
                radius,
                ElevationToolState.RADIUS_MIN,
                ElevationToolState.RADIUS_MAX)) {
            state.elevationRadius = radius[0];
        }

        smoothing[0] = state.elevationSmoothing;
        if (ImGui.sliderFloat(
                I18n.format("dimensium.ui.elevation.smoothing") + "##elev_smooth",
                smoothing,
                ElevationToolState.SMOOTHING_MIN,
                ElevationToolState.SMOOTHING_MAX)) {
            state.elevationSmoothing = smoothing[0];
        }

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.falloff"));
        ImGui.separator();

        ElevationFalloff[] falloffs = ElevationFalloff.values();
        String[] falloffLabels = new String[falloffs.length];
        for (int i = 0; i < falloffs.length; i++) falloffLabels[i] = I18n.format(falloffs[i].label);
        falloffIdx.set(state.elevationFalloff.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.elevation.falloff") + "##elev_fall", falloffIdx, falloffLabels)) {
            state.elevationFalloff = falloffs[falloffIdx.get()];
        }

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.apply"));
        ImGui.separator();

        ElevationApply[] applies = ElevationApply.values();
        String[] applyLabels = new String[applies.length];
        for (int i = 0; i < applies.length; i++) applyLabels[i] = I18n.format(applies[i].label);
        applyIdx.set(state.elevationApply.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.elevation.apply") + "##elev_apply", applyIdx, applyLabels)) {
            state.elevationApply = applies[applyIdx.get()];
        }

        if (state.elevationMode != ElevationMode.FLATTEN) {
            rate[0] = state.elevationRate;
            if (ImGui.sliderFloat(
                    I18n.format("dimensium.ui.elevation.rate") + "##elev_rate",
                    rate,
                    ElevationToolState.RATE_MIN,
                    ElevationToolState.RATE_MAX)) {
                state.elevationRate = rate[0];
            }

            strength[0] = state.elevationStrength;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.ui.elevation.strength") + "##elev_str",
                    strength,
                    ElevationToolState.STRENGTH_MIN,
                    ElevationToolState.STRENGTH_MAX)) {
                state.elevationStrength = strength[0];
            }
        }

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.heightmap"));
        ImGui.separator();
        renderHeightmapSection();
    }

    private void renderHeightmapSection() {
        ensureHeightmapPreviewTexture();

        if (heightmapPreviewTexId != -1) {
            ImVec2 pos = new ImVec2();
            ImGui.getCursorScreenPos(pos);
            ImGui.image(heightmapPreviewTexId, HEIGHTMAP_PREVIEW_SIZE, HEIGHTMAP_PREVIEW_SIZE);
            ImGui.sameLine(0, 8f);
        }

        ImGui.beginGroup();
        String heightmapName = state.activeHeightmap != null
                ? state.activeHeightmap.name()
                : I18n.format("dimensium.heightmap.browser.none");
        ImGui.textDisabled(heightmapName);
        if (ImGui.button(I18n.format("dimensium.ui.elevation.heightmap.browse") + "##elev_hm_browse")) {
            HeightmapBrowserPopup.INSTANCE.open(heightmap -> state.activeHeightmap = heightmap);
        }
        ImGui.endGroup();
    }

    private void ensureHeightmapPreviewTexture() {
        if (state.activeHeightmap == lastTexturedHeightmap) return;
        if (heightmapPreviewTexId != -1) {
            GL11.glDeleteTextures(heightmapPreviewTexId);
            heightmapPreviewTexId = -1;
        }
        lastTexturedHeightmap = state.activeHeightmap;
        if (state.activeHeightmap == null) return;
        try {
            int id = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    state.activeHeightmap.width(),
                    state.activeHeightmap.height(),
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    state.activeHeightmap.toRgbaBuffer());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            heightmapPreviewTexId = id;
        } catch (Exception ignored) {
        }
    }
}
