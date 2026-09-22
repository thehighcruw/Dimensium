/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.path;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.blueprint.Blueprint;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState.CurveType;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState.PathFillMode;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState.PathInterp;
import github.thehighcruw.dimensium.editor.window.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.popup.BlueprintBrowserPopup;
import github.thehighcruw.dimensium.shared.SelectionState;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class PathSection implements ToolSection {

    private final PathToolState state;
    private final ImInt curveTypeIdx = new ImInt();
    private final ImInt interpIdx = new ImInt();
    private final ImInt fillModeIdx = new ImInt();
    private final int[] radiusBuf = new int[1];
    private final int[] stampSpacingBuf = new int[1];

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
        for (int i = 0; i < curves.length; i++) curveLabels[i] = I18n.format(curves[i].labelKey);
        curveTypeIdx.set(state.curveType.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.path.curve_type") + "##path_curve", curveTypeIdx, curveLabels)) {
            state.curveType = curves[curveTypeIdx.get()];
        }

        ImBoolean looped = new ImBoolean(state.looped);
        if (ImGui.checkbox(I18n.format("dimensium.ui.path.looped") + "##path_loop", looped)) {
            state.looped = looped.get();
        }

        if (state.curveType == CurveType.CATENARY) {
            float[] catenarySlack = {state.catenarySlack};
            if (ImGui.sliderFloat(
                    I18n.format("dimensium.ui.path.catenary_slack") + "##path_slack", catenarySlack, 0.0f, 5.0f)) {
                state.catenarySlack = catenarySlack[0];
            }
        }

        PathInterp[] interps = PathInterp.values();
        String[] interpLabels = new String[interps.length];
        for (int i = 0; i < interps.length; i++) interpLabels[i] = I18n.format(interps[i].labelKey);
        interpIdx.set(state.interp.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.path.interp") + "##path_interp", interpIdx, interpLabels)) {
            state.interp = interps[interpIdx.get()];
        }

        if (ImGui.button(I18n.format("dimensium.ui.path.randomize_seed") + "##path_seed")) {
            state.interpSeed = ThreadLocalRandom.current().nextLong();
        }

        ImGui.dummy(0f, 3f);
        ImGui.separator();
        ImGui.dummy(0f, 2f);
        ImGui.text(I18n.format("dimensium.ui.path.fill"));

        PathFillMode[] fillModes = PathFillMode.values();
        String[] fillModeLabels = new String[fillModes.length];
        for (int i = 0; i < fillModes.length; i++) fillModeLabels[i] = I18n.format(fillModes[i].labelKey);
        fillModeIdx.set(state.fillMode.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.path.fill_mode") + "##path_fill", fillModeIdx, fillModeLabels)) {
            state.fillMode = fillModes[fillModeIdx.get()];
            state.invalidatePath();
        }

        if (state.fillMode == PathFillMode.STAMP) {
            String stampLabel = state.stampBlueprint != null
                    ? state.stampBlueprint.name()
                    : I18n.format("dimensium.ui.path.stamp_clipboard");
            ImGui.textDisabled(stampLabel);

            if (ImGui.button(I18n.format("dimensium.ui.path.stamp_pick_blueprint") + "##path_stamp_bp")) {
                BlueprintBrowserPopup.INSTANCE.open(bp -> {
                    state.stampBlueprint = bp;
                    state.invalidatePath();
                });
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.format("dimensium.ui.path.stamp_use_clipboard") + "##path_stamp_clip")) {
                SelectionState sel2 = SelectionState.INSTANCE;
                if (sel2.clipboard != null && !sel2.clipboard.isEmpty()) {
                    state.stampBlueprint = Blueprint.fromClipboard(
                            I18n.format("dimensium.stamp.clipboard_name"),
                            new ArrayList<>(),
                            sel2.clipboard,
                            sel2.clipDim,
                            null);
                    state.invalidatePath();
                }
            }

            stampSpacingBuf[0] = state.stampSpacing;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.ui.path.stamp_spacing") + "##path_stamp_sp", stampSpacingBuf, 1, 64)) {
                state.stampSpacing = stampSpacingBuf[0];
                state.invalidatePath();
            }

            ImBoolean orientYaw = new ImBoolean(state.orientYaw);
            if (ImGui.checkbox(I18n.format("dimensium.ui.path.stamp_orient_yaw") + "##path_orient_yaw", orientYaw)) {
                state.orientYaw = orientYaw.get();
                if (!state.orientYaw) state.orientPitch = false;
                state.invalidatePath();
            }

            if (state.orientYaw) {
                ImBoolean orientPitch = new ImBoolean(state.orientPitch);
                if (ImGui.checkbox(
                        I18n.format("dimensium.ui.path.stamp_orient_pitch") + "##path_orient_pitch", orientPitch)) {
                    state.orientPitch = orientPitch.get();
                    state.invalidatePath();
                }
            }
        }

        PathToolState.PathPoint sel = state.selectedPoint();
        if (sel != null) {
            ImGui.dummy(0f, 3f);
            ImGui.separator();
            ImGui.dummy(0f, 2f);
            ImGui.text(I18n.format("dimensium.ui.path.selected_point"));

            if (DeferredItemRender.placeButton(
                    "##path_pt_block", sel.block, 16f * ImGuiManager.INSTANCE.getUIScale())) {
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
                state.removeCurrentPoint();
            }
            ImGui.popStyleColor(3);
        }
    }
}
