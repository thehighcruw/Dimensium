/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.tool.Tool;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapePlacementState;
import github.thehighcruw.dimensium.editor.tool.state.ClipboardPlacementState;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportRegistry;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportState;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import net.minecraft.client.Minecraft;

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

    /**
     * Tears down the overlay: teleports the player to the active camera position, deactivates
     * freecam and in-progress placements, and clears the active flag. The caller is responsible
     * for calling {@code mc.displayGuiScreen()} with the desired follow-up screen.
     */
    public void deactivate() {
        if (!active) return;
        Minecraft mc = Minecraft.getMinecraft();
        ViewportState activeViewport = ViewportRegistry.INSTANCE.active();
        if (activeViewport != null && mc.thePlayer != null) {
            mc.thePlayer.setPosition(
                    activeViewport.cameraEntity.posX,
                    activeViewport.cameraEntity.posY - mc.thePlayer.getEyeHeight(),
                    activeViewport.cameraEntity.posZ);
            mc.thePlayer.rotationYaw = activeViewport.cameraEntity.rotationYaw;
            mc.thePlayer.rotationPitch = activeViewport.cameraEntity.rotationPitch;
        }
        FreecamState.INSTANCE.deactivate();
        ShapePlacementState.INSTANCE.cancel();
        ClipboardPlacementState.INSTANCE.cancel();
        toggle();
        if (mc.thePlayer != null) mc.thePlayer.setInvisible(false);
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
        Minecraft mc = Minecraft.getMinecraft();
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
