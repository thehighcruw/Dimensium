/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.path;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState.CurveType;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState.PathInterp;
import github.thehighcruw.dimensium.editor.window.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class PathSection implements ToolSection {

    private final PathToolState state;
    private final ImInt curveTypeIdx = new ImInt();
    private final ImInt interpIdx = new ImInt();
    private final int[] radiusBuf = new int[1];

    public PathSection(PathToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        ImGui.dummy(0f, 3f);
        ImGui.separator();
        ImGui.dummy(0f, 2f);
        ImGui.text(I18n.format("dimensium.ui.section.path"));

        CurveType[] curves = CurveType.values();
        String[] curveLabels = new String[curves.length];
        for (int i = 0; i < curves.length; i++) curveLabels[i] = curves[i].label;
        curveTypeIdx.set(state.curveType.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.path.curve_type") + "##path_curve", curveTypeIdx, curveLabels)) {
            state.curveType = curves[curveTypeIdx.get()];
        }

        ImBoolean looped = new ImBoolean(state.looped);
        if (ImGui.checkbox(I18n.format("dimensium.ui.path.looped") + "##path_loop", looped)) {
            state.looped = looped.get();
        }

        if (state.curveType == CurveType.CATENARY) {
            float[] catenarySlack = { state.catenarySlack };
            if (ImGui.sliderFloat(
                I18n.format("dimensium.ui.path.catenary_slack") + "##path_slack",
                catenarySlack,
                0.0f,
                5.0f)) {
                state.catenarySlack = catenarySlack[0];
            }
        }

        PathInterp[] interps = PathInterp.values();
        String[] interpLabels = new String[interps.length];
        for (int i = 0; i < interps.length; i++) interpLabels[i] = interps[i].label;
        interpIdx.set(state.interp.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.path.interp") + "##path_interp", interpIdx, interpLabels)) {
            state.interp = interps[interpIdx.get()];
        }

        if (ImGui.button(I18n.format("dimensium.ui.path.randomize_seed") + "##path_seed")) {
            state.interpSeed = ThreadLocalRandom.current()
                .nextLong();
        }

        PathToolState.PathPoint sel = state.selectedPoint();
        if (sel != null) {
            ImGui.dummy(0f, 3f);
            ImGui.separator();
            ImGui.dummy(0f, 2f);
            ImGui.text(I18n.format("dimensium.ui.path.selected_point"));

            if (DeferredItemRender
                .placeButton("##path_pt_block", sel.block, 16f * ImGuiManager.INSTANCE.getUIScale())) {
                OverlayRenderer.picker.open(picked -> {
                    sel.block = picked;
                    state.invalidatePath();
                });
            }
            ImGui.sameLine();
            ImGui.textDisabled(
                sel.block != null ? sel.block.getDisplayName() : I18n.format("dimensium.ui.path.use_active_block"));

            radiusBuf[0] = sel.radius;
            ImGui.setNextItemWidth(120f);
            if (ImGui.sliderInt(I18n.format("dimensium.ui.path.point_radius") + "##path_pt_r", radiusBuf, 0, 16)) {
                sel.radius = radiusBuf[0];
                state.invalidatePath();
            }
            if (ImGui.button(I18n.format("dimensium.ui.path.clear_point_block") + "##path_pt_clr")
                && sel.block != null) {
                sel.block = null;
                state.invalidatePath();
            }
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Button, 0.65f, 0.10f, 0.10f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.80f, 0.20f, 0.20f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.50f, 0.05f, 0.05f, 1.0f);
            if (ImGui.button(I18n.format("dimensium.ui.path.remove_point") + "##path_rm_pt")) {
                int idx = state.selectedIndex;
                state.points.remove(idx);
                state.selectedIndex = state.points.isEmpty() ? -1 : Math.min(idx, state.points.size() - 1);
                state.getAxisTranslationGizmo()
                    .reset();
                state.invalidatePath();
            }
            ImGui.popStyleColor(3);
        }
    }
}
