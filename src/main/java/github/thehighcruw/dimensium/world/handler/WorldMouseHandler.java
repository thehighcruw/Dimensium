/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.freecam.FreecamUtils;
import github.thehighcruw.dimensium.shared.InputHandler;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.BuilderTool;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.BuilderToolState.AxisLock;
import github.thehighcruw.dimensium.tool.BuilderToolState.Phase;

/**
 * Handles mouse input while the player is in-world (editor overlay closed).
 * Manages slot-scroll entry into builder-tools mode and builder-tool click dispatch.
 */
@SideOnly(Side.CLIENT)
public class WorldMouseHandler {

    private final BuilderToolsHandler builderTools = new BuilderToolsHandler();

    public void handle(MouseEvent event, EntityPlayer player, Minecraft mc) {
        if (event.dwheel != 0) {
            if (handleSlotScroll(event, player)) return;
        }

        if (DimensiumEditorMode.INSTANCE.isBuilderToolsActive()) {
            if (event.button >= 0) {
                if (event.buttonstate) {
                    builderTools.handle(event, player, mc);
                } else {
                    builderTools.handleRelease(event, mc);
                }
            }
            event.setCanceled(true);
        }
    }

    private boolean handleSlotScroll(MouseEvent event, EntityPlayer player) {
        BuilderToolState bts = BuilderToolState.INSTANCE;

        if (DimensiumEditorMode.INSTANCE.isBuilderToolsActive()) {
            if (InputHandler.isAltDown()) {
                if (event.dwheel > 0) bts.activeTool = bts.activeTool.next();
                else bts.activeTool = bts.activeTool.prev();
                bts.resetPhase();
                SelectionState.INSTANCE.clearSelection();
                event.setCanceled(true);
                return true;
            }

            if (bts.activeTool == BuilderTool.STACK && bts.phase == Phase.MANIPULATING) {
                nudgeStackCount(bts, event.dwheel > 0 ? 1 : -1, FreecamUtils.lookVec(player));
                event.setCanceled(true);
                return true;
            }

            if (bts.phase == Phase.MANIPULATING) {
                updateAxisLock(bts);
                int dir = event.dwheel > 0 ? 1 : -1;
                bts.nudgeOffset(dir, FreecamUtils.lookVec(player));
                event.setCanceled(true);
                return true;
            }

            DimensiumEditorMode.INSTANCE.exitBuilderTools();
            player.inventory.currentItem = event.dwheel < 0 ? 0 : 8;
            event.setCanceled(true);
            return true;
        }

        if (event.dwheel < 0 && player.inventory.currentItem == 8) {
            DimensiumEditorMode.INSTANCE.activateBuilderTools();
            event.setCanceled(true);
            return true;
        }

        if (event.dwheel > 0 && player.inventory.currentItem == 0) {
            DimensiumEditorMode.INSTANCE.activateBuilderTools();
            event.setCanceled(true);
            return true;
        }

        return false;
    }

    static void updateAxisLock(BuilderToolState bts) {
        if (Keyboard.isKeyDown(Keyboard.KEY_X)) bts.axisLock = AxisLock.X;
        else if (Keyboard.isKeyDown(Keyboard.KEY_Y)) bts.axisLock = AxisLock.Y;
        else if (Keyboard.isKeyDown(Keyboard.KEY_Z)) bts.axisLock = AxisLock.Z;
        else bts.axisLock = AxisLock.NONE;
    }

    private static void nudgeStackCount(BuilderToolState bts, int dir, Vec3 facing) {
        double ax = Math.abs(facing.xCoord);
        double ay = Math.abs(facing.yCoord);
        double az = Math.abs(facing.zCoord);
        if (ax >= ay && ax >= az) {
            bts.stack = Vec3DInt.from(
                Math.max(-64, Math.min(64, bts.stack.x() + (int) Math.signum(facing.xCoord) * dir)),
                bts.stack.y(),
                bts.stack.z());
        } else if (ay >= ax && ay >= az) {
            bts.stack = Vec3DInt.from(
                bts.stack.x(),
                Math.max(-64, Math.min(64, bts.stack.y() + (int) Math.signum(facing.yCoord) * dir)),
                bts.stack.z());
        } else {
            bts.stack = Vec3DInt.from(
                bts.stack.x(),
                bts.stack.y(),
                Math.max(-64, Math.min(64, bts.stack.z() + (int) Math.signum(facing.zCoord) * dir)));
        }
    }
}
