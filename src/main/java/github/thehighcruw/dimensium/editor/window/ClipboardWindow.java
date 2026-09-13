/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.handler.EditorActions;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.editor.window.popup.BlueprintBrowserPopup;
import github.thehighcruw.dimensium.editor.window.popup.CreateBlueprintPopup;
import github.thehighcruw.dimensium.editor.window.viewport.world.ClipboardRenderer;
import github.thehighcruw.dimensium.network.PacketHandler;
import github.thehighcruw.dimensium.network.PacketPaste;
import github.thehighcruw.dimensium.shared.SelectionState;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class ClipboardWindow extends ToggleableWindow {

    public static final ClipboardWindow INSTANCE = new ClipboardWindow();

    private static final String WINDOW_ID = "###clipboard_window";

    private boolean pendingBlueprintOpen = false;

    private final ClipboardRenderer clipRenderer = new ClipboardRenderer();

    private ClipboardWindow() {}

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowClipboardOpen(value);
    }

    public void open() {
        setOpen(true);
    }

    public void requestBlueprintOpen() {
        pendingBlueprintOpen = true;
    }

    /** Rebake clipboard FBO after ImGui has rendered. Also handles deferred PNG capture for blueprints. */
    public void prebake() {
        clipRenderer.maybeRebake(SelectionState.INSTANCE);
        if (pendingBlueprintOpen) {
            pendingBlueprintOpen = false;
            clipRenderer.resetCamera();
            CreateBlueprintPopup.INSTANCE.open(clipRenderer);
        }
    }

    public void renderImGui() {
        if (!open) return;

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui
            .begin(I18n.format("dimensium.ui.window.clipboard") + WINDOW_ID, pOpen, ImGuiWindowFlags.None);
        captureBounds();

        if (visible && pOpen.get()) {
            SelectionState sel = SelectionState.INSTANCE;
            boolean hasSel = sel.hasSelection();
            boolean hasClipboard = sel.clipboard != null && !sel.clipboard.isEmpty();
            float w = ImGui.getContentRegionAvailX();
            float halfW = (w - 2) / 2;

            if (!hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.separator();
            }

            ImGui.beginDisabled(!hasSel);
            if (ImGui.button(I18n.format("dimensium.settings.keybind.cut"), w, 0)) {
                EditorActions.cut();
            }
            if (ImGui.button(I18n.format("dimensium.settings.keybind.copy"), w, 0)) {
                EditorActions.copy();
            }
            ImGui.endDisabled();

            ImGui.separator();

            ImGui.beginDisabled(!hasClipboard);
            if (ImGui.button(I18n.format("dimensium.ui.op.paste"), w, 0)) {
                executePaste(sel);
            }
            ImGui.endDisabled();

            ImGui.separator();

            if (hasClipboard) {
                ImGui.text(String.format("%d × %d × %d blocks", sel.clipW, sel.clipH, sel.clipD));
                int texId = clipRenderer.getTexture(sel);
                if (texId != -1) {
                    ImGui.image(texId, w, w, 0, 1, 1, 0);
                } else if (clipRenderer.failReason != null) {
                    ImGui.textColored(1f, 0.27f, 0.27f, 1f, I18n.format("dimensium.ui.hint.fbo_failed"));
                    ImGui.textColored(0.67f, 0.20f, 0.20f, 1f, clipRenderer.failReason);
                }
                if (ImGui.button(I18n.format("dimensium.blueprint.save") + "##bp_save", halfW, 0)) {
                    pendingBlueprintOpen = true;
                }
                ImGui.sameLine(0, 2);
                if (ImGui.button(I18n.format("dimensium.blueprint.browse") + "##bp_browse", halfW, 0)) {
                    BlueprintBrowserPopup.INSTANCE.open();
                }
            } else {
                if (ImGui.button(I18n.format("dimensium.blueprint.browse") + "##bp_browse_only", w, 0)) {
                    BlueprintBrowserPopup.INSTANCE.open();
                }
            }
        }

        ImGui.end();
        if (!pOpen.get()) setOpen(false);
    }

    private static void executePaste(SelectionState sel) {
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition mop = mc.objectMouseOver;
        int ox = (int) mc.thePlayer.posX, oy = (int) mc.thePlayer.posY, oz = (int) mc.thePlayer.posZ;
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            ox = mop.blockX;
            oy = mop.blockY;
            oz = mop.blockZ;
        }
        PacketHandler.CHANNEL.sendToServer(new PacketPaste(ox, oy, oz, sel.clipboard, sel.clipW, sel.clipH, sel.clipD));
    }
}
