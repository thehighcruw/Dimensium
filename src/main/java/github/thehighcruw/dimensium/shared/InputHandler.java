/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.MouseEvent;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.handler.EditorMouseHandler;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.world.handler.WorldMouseHandler;

@SideOnly(Side.CLIENT)
public class InputHandler {

    private final EditorMouseHandler editorMouse = new EditorMouseHandler();
    private final WorldMouseHandler worldMouse = new WorldMouseHandler();

    @SubscribeEvent
    public void onMouseInput(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return;
        if (!OverlayRenderer.cheatsAllowed()) return;

        if (DimensiumEditorMode.INSTANCE.isActive()) {
            if (editorMouse.handle(event, mc)) return;
        }

        worldMouse.handle(event, player, mc);
    }

    // ── Key helpers (used by EditorMouseHandler, WorldMouseHandler, and brush inputs) ──

    public static boolean isAltDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    public static boolean isCtrlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    /** Camera modifier: Ctrl on Windows/Linux, Option (Alt) on macOS. */
    public static boolean isCameraModDown() {
        if (IS_MAC) return isAltDown();
        return isCtrlDown();
    }

    private static final boolean IS_MAC = System.getProperty("os.name", "")
        .toLowerCase()
        .contains("mac");
}
