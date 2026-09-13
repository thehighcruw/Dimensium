/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.overlay;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.InputEvent;

/**
 * Dummy GuiScreen held open while the Dimensium overlay is active.
 *
 * When currentScreen != null, Minecraft skips processKeyBinds() and zeroes
 * movement input — this is what blocks WASD, inventory, chat, etc.
 *
 * Side-effect: the GuiScreen drains the Keyboard/Mouse event queues via
 * handleKeyboardInput() / handleMouseInput() BEFORE Forge's own handlers
 * get to fire InputEvent.KeyInputEvent and MouseEvent. We fix this by
 * re-firing those events ourselves from inside the overridden methods.
 *
 * Mouse is re-grabbed in initGui() so raw delta reads (getDX/getDY) still
 * work for the freecam software cursor.
 */
public class EditingModeScreen extends GuiScreen {

    @Override
    public void initGui() {
        // displayGuiScreen() ungrabs the mouse; re-grab so getDX/getDY still work.
        mc.mouseHelper.grabMouseCursor();
    }

    @Override
    public void onGuiClosed() {
        // Release mouse grab so cursor appears normally in subsequent screens
        // (inventory, pause menu). displayGuiScreen(null) will re-grab for game mode;
        // displayGuiScreen(someScreen) will keep it ungrabbed as expected.
        mc.mouseHelper.ungrabMouseCursor();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // OverlayRenderer draws the actual UI via RenderGameOverlayEvent.
    }

    @Override
    public void handleKeyboardInput() {
        // Re-fire FML's KeyInputEvent so KeyHandler.onKeyInput still runs.
        // Keyboard.getEventKey() / getEventKeyState() are valid here — we're
        // inside the keyboard event loop, just routed through the screen path.
        FMLCommonHandler.instance()
            .bus()
            .post(new InputEvent.KeyInputEvent());
        // Do NOT call super — that would invoke keyTyped() which could trigger
        // vanilla actions (e.g. GuiScreen default ESC = close screen bypassing
        // our deactivate() cleanup).
    }

    @Override
    public void handleMouseInput() {
        // Re-fire Forge's MouseEvent so InputHandler.onMouseInput still runs.
        // MouseEvent() reads Mouse.getEvent*() internally — valid here since
        // we're called from within the LWJGL mouse event loop.
        MinecraftForge.EVENT_BUS.post(new MouseEvent());
        // Do NOT call super — that would invoke mouseClicked() / mouseReleased()
        // on the blank GuiScreen, plus Forge would fire a second MouseEvent.
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        // Never called (handleKeyboardInput doesn't call super), but kept as
        // an explicit no-op to prevent accidental vanilla handling if the
        // call path ever changes.
    }
}
