/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.overlay;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.history.ClientEditHistory;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMask;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;

@SideOnly(Side.CLIENT)
public final class StatusBar {

    public static final StatusBar INSTANCE = new StatusBar();

    private static final int FLAGS = ImGuiWindowFlags.NoDecoration
            | ImGuiWindowFlags.NoInputs
            | ImGuiWindowFlags.NoNav
            | ImGuiWindowFlags.NoMove
            | ImGuiWindowFlags.NoBringToFrontOnFocus
            | ImGuiWindowFlags.NoFocusOnAppearing
            | ImGuiWindowFlags.NoScrollbar
            | ImGuiWindowFlags.NoScrollWithMouse;

    private static final float PAD_V = 3f;
    private static final float PAD_H = 8f;

    /** Actual rendered height in physical pixels, captured each frame. Zero before first frame. */
    private float renderedHeight = 0f;

    /** Height in physical pixels — used by callers to shrink the dock area. */
    public float height() {
        return renderedHeight;
    }

    public void render(int sw, int sh) {
        float scale = ImGuiManager.INSTANCE.getUIScale();
        float padH = PAD_V * scale;
        float padW = PAD_H * scale;

        // Size: use renderedHeight if known, else ImGui's frame height as bootstrap estimate.
        float barH = renderedHeight > 0 ? renderedHeight : ImGui.getFrameHeight();

        ImGui.setNextWindowPos(0, sh - barH);
        ImGui.setNextWindowSize(sw, barH);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0.13f, 0.13f, 0.13f, 1.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, padW, padH);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0f);
        ImGui.begin("##status_bar", FLAGS);

        renderedHeight = ImGui.getWindowHeight();

        renderContents(scale);

        ImGui.end();
        ImGui.popStyleVar(2);
        ImGui.popStyleColor();
    }

    private static void renderContents(float scale) {
        float dimColor = 0.5f;
        float sep = 14f * scale;

        // ── Tool ─────────────────────────────────────────────────────────────
        String toolName = I18n.format(DimensiumEditorMode.INSTANCE.selectedTool.label);
        ImGui.text(toolName);

        divider(sep);

        // ── Selection ────────────────────────────────────────────────────────
        SelectionState sel = SelectionState.INSTANCE;
        if (sel.hasSelection()) {
            int w = sel.width(), h = sel.height(), d = sel.depth();
            int n = sel.size();
            ImGui.text(w + "×" + h + "×" + d + "  " + n + " blk");
        } else {
            ImGui.pushStyleColor(ImGuiCol.Text, dimColor, dimColor, dimColor, 1f);
            ImGui.text(I18n.format("dimensium.status.no_selection"));
            ImGui.popStyleColor();
        }

        divider(sep);

        // ── Clipboard ────────────────────────────────────────────────────────
        if (sel.clipboard != null) {
            ImGui.text(I18n.format("dimensium.status.clipboard") + " "
                    + sel.clipDim.x()
                    + "×"
                    + sel.clipDim.y()
                    + "×"
                    + sel.clipDim.z());
        } else {
            ImGui.pushStyleColor(ImGuiCol.Text, dimColor, dimColor, dimColor, 1f);
            ImGui.text(I18n.format("dimensium.status.no_clipboard"));
            ImGui.popStyleColor();
        }

        divider(sep);

        // ── Tool mask ────────────────────────────────────────────────────────
        ToolMask mask = ToolMaskRegistry.INSTANCE.getActiveMask();
        if (mask != null) {
            ImGui.pushStyleColor(ImGuiCol.Text, 0.55f, 0.85f, 1.0f, 1f);
            ImGui.text(I18n.format("dimensium.status.mask") + " " + mask.getName());
            ImGui.popStyleColor();
        } else {
            ImGui.pushStyleColor(ImGuiCol.Text, dimColor, dimColor, dimColor, 1f);
            ImGui.text(I18n.format("dimensium.status.no_mask"));
            ImGui.popStyleColor();
        }

        divider(sep);

        // ── Camera XYZ ───────────────────────────────────────────────────────
        EntityLivingBase cam = Minecraft.getMinecraft().renderViewEntity;
        if (cam != null) {
            Vec3DInt camPos = Vec3DInt.floor(Vec3DDouble.from(cam.posX, cam.posY, cam.posZ));
            ImGui.text(camPos.x() + ", " + camPos.y() + ", " + camPos.z());
        }

        divider(sep);

        // ── Undo history ──────────────────────────────────────────────────────
        ClientEditHistory hist = ClientEditHistory.INSTANCE;
        String undoAction = hist.peekUndoName();
        if (undoAction != null) {
            ImGui.pushStyleColor(ImGuiCol.Text, dimColor + 0.1f, dimColor + 0.1f, dimColor + 0.1f, 1f);
            ImGui.text(I18n.format("dimensium.status.undo") + " " + undoAction);
            ImGui.popStyleColor();
        } else {
            ImGui.pushStyleColor(ImGuiCol.Text, dimColor, dimColor, dimColor, 1f);
            ImGui.text(I18n.format("dimensium.status.no_undo"));
            ImGui.popStyleColor();
        }
    }

    private static void divider(float sep) {
        ImGui.sameLine(0, sep);
        ImGui.pushStyleColor(ImGuiCol.Text, 0.35f, 0.35f, 0.35f, 1f);
        ImGui.text("|");
        ImGui.popStyleColor();
        ImGui.sameLine(0, sep);
    }
}
