/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.freecam.FreecamUtils;
import github.thehighcruw.dimensium.handler.brushes.BrushInput;
import github.thehighcruw.dimensium.handler.brushes.BrushInputRegistry;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.render.MenuBar;
import github.thehighcruw.dimensium.render.OverlayRenderer;
import github.thehighcruw.dimensium.render.imgui.ImGuiManager;
import github.thehighcruw.dimensium.render.imgui.ImGuiWindowRegistry;
import github.thehighcruw.dimensium.tool.BuilderTool;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.BuilderToolState.AxisLock;
import github.thehighcruw.dimensium.tool.BuilderToolState.Phase;
import github.thehighcruw.dimensium.tool.DimensiumMode;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.SelectionState;
import github.thehighcruw.dimensium.util.RenderUtils;

@SideOnly(Side.CLIENT)
public class InputHandler {

    private final BuilderToolsHandler builderTools = new BuilderToolsHandler();

    @SubscribeEvent
    public void onMouseInput(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return;
        if (!OverlayRenderer.cheatsAllowed()) return;

        // ── Overlay mouse handling ────────────────────────────────────────────
        if (DimensiumMode.INSTANCE.isActive()) {
            FreecamState fs = FreecamState.INSTANCE;
            int sw = RenderUtils.scaledWidth();
            int sh = RenderUtils.scaledHeight();
            int sf = RenderUtils.scaleFactor();
            int mx = (int) fs.cursorX;
            int my = (int) fs.cursorY;

            // onPanel: cursor is over a real UI panel (not the viewport, not outside).
            // Use pixel-space bounds stored by each panel each frame — reliable with docking.
            float pmx = fs.cursorX * sf;
            float pmy = fs.cursorY * sf;
            boolean onPanel = ImGuiManager.INSTANCE.anyModalOpen()
                || MenuBar.INSTANCE.containsMouse(pmx, pmy, mc.displayWidth)
                || ImGuiWindowRegistry.INSTANCE.anyContainsMouse(pmx, pmy);

            if (event.dwheel != 0) {
                if (onPanel) {
                    ImGuiManager.INSTANCE.addMouseWheel(event.dwheel / 240f * DimensiumConfig.uiScrollSpeedModifier);
                    event.setCanceled(true);
                    return;
                }
                if (fs.walkMode) {
                    fs.adjustSpeed(event.dwheel > 0);
                } else {
                    fs.zoom((event.dwheel > 0 ? 1 : -1) * fs.speed * 3f * DimensiumConfig.worldScrollSpeedModifier);
                }
                event.setCanceled(true);
                return;
            }

            if (event.button >= 0) {
                if (event.buttonstate) {
                    if (onPanel) {
                        GuiDimensiumOverlay.handleClick(mx, my, sw, sh, event.button);
                    } else if (!fs.walkMode) {
                        if (isAltDown()) {
                            if (event.button == KeyConstants.LMB) fs.lmbDragging = true;
                            if (event.button == KeyConstants.RMB) fs.rmbDragging = true;
                            if (event.button == KeyConstants.MMB) fs.mmbDragging = true;
                        } else {
                            GuiDimensiumOverlay.handleClick(mx, my, sw, sh, event.button);
                        }
                    } else {
                        GuiDimensiumOverlay.handleClick(mx, my, sw, sh, event.button);
                    }
                } else {
                    if (event.button == KeyConstants.LMB) fs.lmbDragging = false;
                    if (event.button == KeyConstants.RMB) {
                        fs.rmbWasDragging = fs.rmbDragging;
                        fs.rmbDragging = false;
                    }
                    if (event.button == KeyConstants.MMB) {
                        fs.mmbDragging = false;
                        fs.orbiting = false;
                    }
                    GuiDimensiumOverlay.handleRelease(mx, my, event.button);
                }
                event.setCanceled(true);
                return;
            }
        }

        // ── 10th-slot scroll ──────────────────────────────────────────────────
        if (event.dwheel != 0) {
            if (handleSlotScroll(event, player, mc)) return;
        }

        // ── Builder tools mode ────────────────────────────────────────────────
        if (DimensiumMode.INSTANCE.isBuilderToolsActive()) {
            if (event.button >= 0) {
                if (event.buttonstate) {
                    builderTools.handle(event, player, mc);
                } else {
                    builderTools.handleRelease(event, player, mc);
                }
            }
            event.setCanceled(true);
            return;
        }

        // ── Tool-specific click handling ──────────────────────────────────────
        if (DimensiumMode.INSTANCE.isActive() && event.button >= 0
            && event.buttonstate
            && !ImGuiManager.INSTANCE.wantCaptureMouse()) {
            BrushInput input = BrushInputRegistry.get(DimensiumMode.INSTANCE.selectedTool);
            if (input != null) {
                if (input.requiresBlockTarget()) {
                    MovingObjectPosition mop = FreecamUtils.rayTrace(mc, FreecamUtils.REACH);
                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                        if (input.onMouseClick(event.button, mc, mop)) {
                            event.setCanceled(true);
                            return;
                        }
                    }
                } else {
                    if (input.onMouseClick(event.button, mc, null)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }

        // ── Shift+scroll: change brush size ───────────────────────────────────
        if (event.dwheel == 0) return;
        if (!DimensiumMode.INSTANCE.isActive()) return;
        if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) return;

        BrushState state = BrushState.INSTANCE;
        int brushStep = Math.max(1, Math.round(DimensiumConfig.worldScrollSpeedModifier));
        if (event.dwheel > 0) {
            state.brushRadius = Math.min(state.brushRadius + brushStep, DimensiumConfig.maxBrushRadius);
        } else {
            state.brushRadius = Math.max(state.brushRadius - brushStep, 0);
        }
        event.setCanceled(true);
    }

    // ── 10th-slot scroll ──────────────────────────────────────────────────────

    private boolean handleSlotScroll(MouseEvent event, EntityPlayer player, Minecraft mc) {
        BuilderToolState bts = BuilderToolState.INSTANCE;

        if (DimensiumMode.INSTANCE.isBuilderToolsActive()) {
            if (isAltDown()) {
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

            DimensiumMode.INSTANCE.exitBuilderTools();
            player.inventory.currentItem = event.dwheel < 0 ? 0 : 8;
            event.setCanceled(true);
            return true;
        }

        if (event.dwheel < 0 && player.inventory.currentItem == 8) {
            DimensiumMode.INSTANCE.activateBuilderTools();
            event.setCanceled(true);
            return true;
        }

        if (event.dwheel > 0 && player.inventory.currentItem == 0) {
            DimensiumMode.INSTANCE.activateBuilderTools();
            event.setCanceled(true);
            return true;
        }

        return false;
    }

    // ── Package-private helpers (used by BuilderToolsHandler) ─────────────────

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
            bts.stackX = Math.max(-64, Math.min(64, bts.stackX + (int) Math.signum(facing.xCoord) * dir));
        } else if (ay >= ax && ay >= az) {
            bts.stackY = Math.max(-64, Math.min(64, bts.stackY + (int) Math.signum(facing.yCoord) * dir));
        } else {
            bts.stackZ = Math.max(-64, Math.min(64, bts.stackZ + (int) Math.signum(facing.zCoord) * dir));
        }
    }

    static boolean isAltDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    public static boolean isCtrlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }
}
