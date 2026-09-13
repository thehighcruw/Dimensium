/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.tool.Tool;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapePlacementState;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportRegistry;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.tool.BuilderToolState;

public class DimensiumEditorMode {

    public static final DimensiumEditorMode INSTANCE = new DimensiumEditorMode();

    // Overlay (panel / GUI) mode
    private boolean active = false;

    // Builder tools mode — independent of overlay, no GUI required
    private boolean builderToolsActive = false;

    public Tool selectedTool = Tool.FREEHAND_DRAW;

    public boolean isActive() {
        return active;
    }

    public boolean isBuilderToolsActive() {
        return builderToolsActive;
    }

    public void toggle() {
        active = !active;
        if (!active) {
            SelectionState sel = SelectionState.INSTANCE;
            sel.pendingPos1 = false;
            sel.boxConfirmed = false;
        }
    }

    public void activateBuilderTools() {
        builderToolsActive = true;
    }

    public void exitBuilderTools() {
        if (!builderToolsActive) return;
        builderToolsActive = false;
        BuilderToolState.INSTANCE.resetPhase();
        SelectionState.INSTANCE.clearSelection();
    }

    /**
     * Full teardown called on world disconnect/unload. Resets every piece of
     * client-side editor state so re-joining a world starts clean.
     */
    public void fullReset() {
        active = false;
        builderToolsActive = false;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.setInvisible(false);
        FreecamState.INSTANCE.deactivate();
        ViewportRegistry.INSTANCE.clear();
        BuilderToolState.INSTANCE.resetPhase();
        SelectionState sel = SelectionState.INSTANCE;
        sel.clearSelection();
        sel.clipboard = null;
        sel.clipboardVersion++;
        ShapePlacementState.INSTANCE.cancel();
        OverlayRenderer.picker.close();
    }
}
