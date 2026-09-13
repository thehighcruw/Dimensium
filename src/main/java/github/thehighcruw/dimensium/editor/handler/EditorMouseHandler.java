/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.handler;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.overlay.MenuBar;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiWindowRegistry;
import github.thehighcruw.dimensium.shared.InputHandler;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.util.RenderUtils;

/**
 * Handles mouse input while the editor overlay is open (DimensiumMode.isActive()).
 * Returns true if the event was consumed and should not propagate further.
 */
@SideOnly(Side.CLIENT)
public class EditorMouseHandler {

    public boolean handle(MouseEvent event, Minecraft mc) {
        FreecamState fs = FreecamState.INSTANCE;
        int sw = RenderUtils.scaledWidth();
        int sh = RenderUtils.scaledHeight();
        int sf = RenderUtils.scaleFactor();
        int mx = (int) fs.cursorX;
        int my = (int) fs.cursorY;

        float pmx = fs.cursorX * sf;
        float pmy = fs.cursorY * sf;
        boolean onPanel = ImGuiManager.INSTANCE.anyModalOpen()
            || MenuBar.INSTANCE.containsMouse(pmx, pmy, mc.displayWidth)
            || ImGuiWindowRegistry.INSTANCE.anyContainsMouse(pmx, pmy);

        if (event.dwheel != 0) {
            if (onPanel) {
                ImGuiManager.INSTANCE.addMouseWheel(event.dwheel / 240f * DimensiumConfig.uiScrollSpeedModifier);
                event.setCanceled(true);
                return true;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                BrushState state = BrushState.INSTANCE;
                int brushStep = Math.max(1, Math.round(DimensiumConfig.worldScrollSpeedModifier));
                if (event.dwheel > 0) {
                    state.brushRadius = Math.min(state.brushRadius + brushStep, DimensiumConfig.maxBrushRadius);
                } else {
                    state.brushRadius = Math.max(state.brushRadius - brushStep, 0);
                }
                event.setCanceled(true);
                return true;
            }
            fs.zoom((event.dwheel > 0 ? 1 : -1) * fs.speed * 3f * DimensiumConfig.worldScrollSpeedModifier);
            event.setCanceled(true);
            return true;
        }

        if (event.button >= 0) {
            if (event.buttonstate) {
                if (onPanel) {
                    GuiDimensiumOverlay.handleClick(mx, my, sw, sh, event.button);
                } else if (event.button == KeyConstants.LMB) {
                    if (InputHandler.isCameraModDown()) {
                        fs.cameraLmbDragActive = true;
                    } else if (!FreecamState.INSTANCE.isMoving()) {
                        fs.lmbPressing = true;
                        fs.lmbPressX = fs.cursorX;
                        fs.lmbPressY = fs.cursorY;
                        GuiDimensiumOverlay.handleClick(mx, my, sw, sh, event.button);
                    }
                } else if (event.button == KeyConstants.RMB) {
                    if (InputHandler.isCameraModDown()) {
                        fs.cameraRmbDragActive = true;
                        fs.rmbPressing = true;
                        fs.rmbPressX = fs.cursorX;
                        fs.rmbPressY = fs.cursorY;
                    } else if (!FreecamState.INSTANCE.isMoving()) {
                        GuiDimensiumOverlay.handleClick(mx, my, sw, sh, event.button);
                    }
                } else if (!FreecamState.INSTANCE.isMoving()) {
                    GuiDimensiumOverlay.handleClick(mx, my, sw, sh, event.button);
                }
            } else {
                if (event.button == KeyConstants.LMB) {
                    fs.cameraLmbDragActive = false;
                    fs.lmbPressing = false;
                    fs.lmbDragging = false;
                    fs.orbiting = false;
                }
                if (event.button == KeyConstants.RMB) {
                    fs.cameraRmbDragActive = false;
                    fs.rmbPressing = false;
                    fs.rmbDragging = false;
                }
                GuiDimensiumOverlay.handleRelease(event.button);
            }
            event.setCanceled(true);
            return true;
        }

        return false;
    }
}
