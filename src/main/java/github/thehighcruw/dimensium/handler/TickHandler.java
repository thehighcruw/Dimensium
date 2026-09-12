/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.freecam.FreecamEntity;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.handler.brushes.BrushInput;
import github.thehighcruw.dimensium.handler.brushes.BrushInputRegistry;
import github.thehighcruw.dimensium.handler.brushes.ElevationBrushInput;
import github.thehighcruw.dimensium.handler.brushes.SmoothBrushInput;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.render.HandRenderer;
import github.thehighcruw.dimensium.render.MenuBar;
import github.thehighcruw.dimensium.render.OverlayRenderer;
import github.thehighcruw.dimensium.render.UICoords;
import github.thehighcruw.dimensium.render.imgui.ImGuiManager;
import github.thehighcruw.dimensium.render.world.RotationGizmo;
import github.thehighcruw.dimensium.render.world.ScaleGizmo;
import github.thehighcruw.dimensium.tool.BrushApplicator;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.DimensiumMode;
import github.thehighcruw.dimensium.tool.Tool;
import github.thehighcruw.dimensium.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.tool.math.ShapeMath;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.ClipboardPlacementState;
import github.thehighcruw.dimensium.tool.state.ElevationToolState;
import github.thehighcruw.dimensium.tool.state.GradientToolState;
import github.thehighcruw.dimensium.tool.state.MoveToolState;
import github.thehighcruw.dimensium.tool.state.NoiseToolState;
import github.thehighcruw.dimensium.tool.state.ShapePlacementState;
import github.thehighcruw.dimensium.tool.state.ShapeToolState;
import github.thehighcruw.dimensium.util.PerfTrace;
import github.thehighcruw.dimensium.util.RenderUtils;

@SideOnly(Side.CLIENT)
public class TickHandler {

    public static TickHandler INSTANCE;

    public TickHandler() {
        INSTANCE = this;
    }

    // Drag threshold: squared scaled-pixel distance before a button-press becomes a camera drag.
    private static final float DRAG_THRESHOLD_SQ = 3.0f * 3.0f;

    // Normal Minecraft player walk speed in blocks per client tick (~0.215 blocks/tick = ~4.3 blocks/s).
    private static final float BASE_MOVEMENT_SPEED = 0.215f;

    // Last block position where freehand paint was sent — used to deduplicate
    // per-render-frame packets so each block gets exactly one packet per drag pass.
    private int lastFreehandX = Integer.MIN_VALUE;
    private int lastFreehandY = Integer.MIN_VALUE;
    private int lastFreehandZ = Integer.MIN_VALUE;

    /** Read-only view of accumulated SMOOTH drag positions. */
    public java.util.Set<Long> getSmoothDragPositions() {
        return SmoothBrushInput.INSTANCE.getDragPositions();
    }

    /** True while a brush paint drag is active (RMB held, first stroke fired). */
    public boolean isPaintDragging() {
        return lastFreehandX != Integer.MIN_VALUE || ElevationBrushInput.INSTANCE.isPaintDragging();
    }

    // ── Mouse / camera ────────────────────────────────────────────────────────
    // RenderTickEvent.PRE fires before EntityRenderer.updateCameraAndRender().
    // Consuming getDX/getDY here leaves vanilla with 0 — player body stays still.

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        FreecamState fs = FreecamState.INSTANCE;
        Minecraft mc = Minecraft.getMinecraft();

        // Suppress first-person arm before EntityRenderer.renderHand fires.
        // Restore happens in OverlayRenderer.onRenderOverlay (ALL event) after renderHand.
        if (fs.active) HandRenderer.INSTANCE.suppress(mc);

        if (!fs.active || fs.cameraEntity == null) return;
        if (!Mouse.isInsideWindow()) return;

        ScaledResolution sr = RenderUtils.scaledResolution();
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        int sf = sr.getScaleFactor();

        // Always consume delta so vanilla never rotates the player.
        float rawDX = Mouse.getDX();
        float rawDY = Mouse.getDY();

        // Move software cursor (physical pixels → scaled pixels, Y inverted).
        fs.cursorX = Math.max(0, Math.min(sw - 1, fs.cursorX + rawDX / sf));
        fs.cursorY = Math.max(0, Math.min(sh - 1, fs.cursorY - rawDY / sf));

        // Paint here — cursor is freshest and render ticks match frame rate, giving
        // smooth continuous strokes. Block-position dedup prevents packet spam.
        applyPaintIfHeld(mc, sw, sh);

        FreecamEntity cam = fs.cameraEntity;
        cam.prevRotationYaw = cam.rotationYaw;
        cam.prevRotationPitch = cam.rotationPitch;

        // Sensitivity curve matching vanilla EntityRenderer.
        float sens = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        float scale = sens * sens * sens * 8.0f;

        // Promote pressing → dragging once cursor moves past the threshold.
        if (fs.lmbPressing) {
            float ddx = fs.cursorX - fs.lmbPressX;
            float ddy = fs.cursorY - fs.lmbPressY;
            if (ddx * ddx + ddy * ddy > DRAG_THRESHOLD_SQ) {
                fs.lmbDragging = true;
                fs.lmbPressing = false;
            }
        }
        if (fs.rmbPressing) {
            float ddx = fs.cursorX - fs.rmbPressX;
            float ddy = fs.cursorY - fs.rmbPressY;
            if (ddx * ddx + ddy * ddy > DRAG_THRESHOLD_SQ) {
                fs.rmbDragging = true;
                fs.rmbPressing = false;
            }
        }

        boolean lmb = Mouse.isButtonDown(KeyConstants.LMB);

        // CameraMod+LMB orbit: persists until LMB is released (cameraLmbDragActive cleared on release).
        if (fs.cameraLmbDragActive) {
            if (lmb) {
                if (!fs.orbiting) startOrbit(fs, cam, mc, DimensiumConfig.orbitUseCursor);
                applyOrbit(fs, cam, rawDX, rawDY, scale);
                return;
            }
            // LMB released but flag not yet cleared by InputHandler — clear it now.
            fs.cameraLmbDragActive = false;
        }

        fs.orbiting = false;

        // LMB drag = rotate camera; suppress when a tool gizmo is being dragged.
        if (fs.lmbDragging && !GuiDimensiumOverlay.anyGizmoDragging()) {
            applyRotate(cam, rawDX, rawDY, scale);
        } else if (fs.rmbDragging) {
            applyPan(cam, rawDX, rawDY);
        }
    }

    // ── WASD movement — walk mode only ────────────────────────────────────────

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!ImGuiManager.INSTANCE.isInitialized() && Minecraft.getMinecraft().theWorld != null) {
            ImGuiManager.INSTANCE.ensureInit();
        }
        BlockSender.flushSendQueue();

        FreecamState fs = FreecamState.INSTANCE;
        if (!fs.active || fs.cameraEntity == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        FreecamEntity cam = fs.cameraEntity;

        // Sync both prevPos and lastTickPos — EntityRenderer.orientCamera uses prevPos
        // for camera interpolation; lastTickPos is used for render-offset math.
        // Neither is updated by onUpdate() (which is intentionally empty).
        cam.prevPosX = cam.posX;
        cam.prevPosY = cam.posY;
        cam.prevPosZ = cam.posZ;
        cam.lastTickPosX = cam.posX;
        cam.lastTickPosY = cam.posY;
        cam.lastTickPosZ = cam.posZ;

        // Sync player yaw/pitch to freecam so server-side player.rayTrace() uses
        // the freecam look direction for tool application and objectMouseOver.
        if (mc.thePlayer != null) {
            mc.thePlayer.rotationYaw = cam.rotationYaw;
            mc.thePlayer.rotationPitch = cam.rotationPitch;
        }

        if (fs.orbiting) return;
        if (mc.thePlayer == null) return;

        float speed = BASE_MOVEMENT_SPEED * DimensiumConfig.movementSpeedMultiplier;
        if (isSprinting(mc)) speed *= 5.0f;

        double yaw = Math.toRadians(cam.rotationYaw);
        // Flat XZ forward — W/S move horizontally regardless of pitch.
        double flatFwdX = -Math.sin(yaw);
        double flatFwdZ = Math.cos(yaw);
        // Strafe right = 90° CW from forward in XZ.
        double rgtX = Math.cos(yaw);
        double rgtZ = Math.sin(yaw);

        double dx = 0, dy = 0, dz = 0;
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
            dx += flatFwdX;
            dz += flatFwdZ;
        }
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
            dx -= flatFwdX;
            dz -= flatFwdZ;
        }
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) {
            dx -= rgtX;
            dz -= rgtZ;
        }
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())) {
            dx += rgtX;
            dz += rgtZ;
        }
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) dy += 1.0;
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) dy -= 1.0;

        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 1.0) {
            dx /= len;
            dy /= len;
            dz /= len;
        }

        cam.posX += dx * speed;
        cam.posY += dy * speed;
        cam.posZ += dz * speed;
    }

    // ── Hold-to-paint ─────────────────────────────────────────────────────────

    // All brush tools paint continuously while held (stroke behavior).
    // Called from onRenderTick (after cursor update) for frame-rate-accurate strokes.
    // Block-position dedup ensures each block gets one packet per drag pass even at 60+ fps.
    private void applyPaintIfHeld(Minecraft mc, int sw, int sh) {
        if (!DimensiumMode.INSTANCE.isActive()) return;
        if (ImGuiManager.INSTANCE.anyModalOpen()) return;

        FreecamState fs = FreecamState.INSTANCE;
        int mx = (int) fs.cursorX, my = (int) fs.cursorY;
        boolean snap = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
        Tool tool = DimensiumMode.INSTANCE.selectedTool;
        BrushInput input = BrushInputRegistry.get(tool);

        // Gizmo drag state update — runs before paint loop so world renderers see fresh state.
        if (input != null) input.onGizmoDrag(mx, my, snap, mc);
        updatePlacementGizmos(mx, my, snap);

        int sf = RenderUtils.scaleFactor();

        // Tools that handle their own drag tick (e.g. LASSO_SELECT, FREEHAND_SELECT, ELEVATION).
        if (input != null && input.onDragTick(mc, sw, sh)) return;

        if (!BrushInputRegistry.usesDragLoop(tool)) {
            // Tool switched mid-drag — discard any pending proposal.
            ChangeProposal.cancel();
            lastFreehandX = Integer.MIN_VALUE;
            return;
        }
        // Suppress paint during any camera movement (pan, orbit, LMB drag).
        if (fs.isMoving()) return;
        if (!Mouse.isButtonDown(KeyConstants.RMB)) {
            // RMB released — let tool handle release, then flush accumulated proposal.
            PerfTrace.begin("brushRelease tool=" + tool);
            PerfTrace.push("onBrushRelease");
            if (input != null) input.onBrushRelease(mc);
            PerfTrace.pop();
            if (lastFreehandX != Integer.MIN_VALUE) {
                PerfTrace.push("flush");
                java.util.List<int[]> ops = ChangeProposal.flush();
                PerfTrace.pop();
                PerfTrace.push("sendChunked ops=" + ops.size());
                if (!ops.isEmpty()) BlockSender.sendChunked(ops, toolActionName(tool));
                PerfTrace.pop();
            }
            PerfTrace.end(5);
            lastFreehandX = Integer.MIN_VALUE;
            return;
        }
        if (mx * sf < OverlayRenderer.toolPanel.currentW || my * sf < (int) MenuBar.INSTANCE.height()) return;

        MovingObjectPosition mop = GuiDimensiumOverlay.raycastFromMouse(mx, my, sw, sh);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        // Deduplicate: skip if cursor hasn't moved into a new block since last stroke.
        if (mop.blockX == lastFreehandX && mop.blockY == lastFreehandY && mop.blockZ == lastFreehandZ) return;

        if (lastFreehandX == Integer.MIN_VALUE) {
            // First stroke of a new drag — open a fresh proposal.
            ChangeProposal.startDrag(ToolMaskRegistry.INSTANCE.getActiveMask());
            if (input != null) input.onBrushDragStart(mc, mop);
        }
        lastFreehandX = mop.blockX;
        lastFreehandY = mop.blockY;
        lastFreehandZ = mop.blockZ;

        // Let tool handle the stroke; fall back to BrushApplicator for standard brush tools.
        PerfTrace.begin("brushStroke tool=" + tool);
        PerfTrace.push("onBrushHeld/applyTool");
        if (input == null || !input.onBrushHeld(mc, mop)) {
            BrushApplicator.applyTool(mc.theWorld, mop.blockX, mop.blockY, mop.blockZ);
        }
        PerfTrace.pop();
        PerfTrace.end(16);
    }

    // ── Gizmo drag state ─────────────────────────────────────────────────────

    private void updatePlacementGizmos(int mx, int my, boolean snap) {
        ShapePlacementState ps = ShapePlacementState.INSTANCE;
        if (ps.active) {
            if (ps.gizmo.isDragging()) {
                double[] anchor = ps.gizmo.updateDrag(mx, my);
                if (anchor != null) {
                    ps.anchorFX = snap ? (float) Math.floor(anchor[0] + 0.5) : (float) anchor[0];
                    ps.anchorFY = snap ? (float) Math.floor(anchor[1] + 0.5) : (float) anchor[1];
                    ps.anchorFZ = snap ? (float) Math.floor(anchor[2] + 0.5) : (float) anchor[2];
                    ps.anchorX = (int) Math.floor(ps.anchorFX);
                    ps.anchorY = (int) Math.floor(ps.anchorFY);
                    ps.anchorZ = (int) Math.floor(ps.anchorFZ);
                }
            } else if (ps.rotGizmo.isDragging()) {
                float delta = ps.rotGizmo.updateDrag(mx, my);
                RotationGizmo.Axis axis = ps.rotGizmo.getDragAxis();
                float[] Rbase = ShapeMath.buildRotationMatrix(ps.rotDragBaseX, ps.rotDragBaseY, ps.rotDragBaseZ);
                float[] dR = axis == RotationGizmo.Axis.X ? ShapeMath.buildRotationMatrix(delta, 0, 0)
                    : axis == RotationGizmo.Axis.Y ? ShapeMath.buildRotationMatrix(0, delta, 0)
                        : ShapeMath.buildRotationMatrix(0, 0, delta);
                float[] Rnew = ShapeMath.multiplyRotationMatrices(Rbase, dR);
                float[] angles = ShapeMath.decomposeRotationMatrix(Rnew);
                if (Math.abs(angles[0] - ps.rotX) >= 0.5f || Math.abs(angles[1] - ps.rotY) >= 0.5f
                    || Math.abs(angles[2] - ps.rotZ) >= 0.5f) {
                    ps.rotX = angles[0];
                    ps.rotY = angles[1];
                    ps.rotZ = angles[2];
                    ps.invalidateGhost();
                }
            } else if (ps.scaleGizmo.isDragging()) {
                float[] result = ps.scaleGizmo.updateDrag(mx, my);
                if (result != null) {
                    ScaleGizmo.Axis axis = ps.scaleGizmo.getDragAxis();
                    if (axis == ScaleGizmo.Axis.X) ps.scaleX = result[0];
                    else if (axis == ScaleGizmo.Axis.Y) ps.scaleY = result[0];
                    else ps.scaleZ = result[0];
                    ShapeToolState sts = ShapeToolState.INSTANCE;
                    if (axis == ScaleGizmo.Axis.X) {
                        sts.shapeWidth = Math.max(1, Math.round(ps.scaleDragBaseW * ps.scaleX));
                        ps.scaleX = 1f;
                    } else if (axis == ScaleGizmo.Axis.Y) {
                        sts.shapeHeight = Math.max(1, Math.round(ps.scaleDragBaseH * ps.scaleY));
                        ps.scaleY = 1f;
                    } else {
                        sts.shapeDepth = Math.max(1, Math.round(ps.scaleDragBaseD * ps.scaleZ));
                        ps.scaleZ = 1f;
                    }
                    ps.invalidateGhost();
                }
            } else if (ps.planeGizmo.isDragging()) {
                double[] anchor = ps.planeGizmo.updateDrag(mx, my);
                if (anchor != null) {
                    ps.anchorFX = snap ? (float) Math.floor(anchor[0] + 0.5) : (float) anchor[0];
                    ps.anchorFY = snap ? (float) Math.floor(anchor[1] + 0.5) : (float) anchor[1];
                    ps.anchorFZ = snap ? (float) Math.floor(anchor[2] + 0.5) : (float) anchor[2];
                    ps.anchorX = (int) Math.floor(ps.anchorFX);
                    ps.anchorY = (int) Math.floor(ps.anchorFY);
                    ps.anchorZ = (int) Math.floor(ps.anchorFZ);
                }
            } else if (ps.viewPlaneGizmo.isDragging()) {
                double[] anchor = ps.viewPlaneGizmo.updateDrag(mx, my);
                if (anchor != null) {
                    ps.anchorFX = snap ? (float) Math.floor(anchor[0] + 0.5) : (float) anchor[0];
                    ps.anchorFY = snap ? (float) Math.floor(anchor[1] + 0.5) : (float) anchor[1];
                    ps.anchorFZ = snap ? (float) Math.floor(anchor[2] + 0.5) : (float) anchor[2];
                    ps.anchorX = (int) Math.floor(ps.anchorFX);
                    ps.anchorY = (int) Math.floor(ps.anchorFY);
                    ps.anchorZ = (int) Math.floor(ps.anchorFZ);
                }
            }
        }

        ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
        if (cps.active) {
            if (cps.gizmo.isDragging()) {
                double[] anchor = cps.gizmo.updateDrag(mx, my);
                if (anchor != null) {
                    cps.anchorFX = snap ? (float) Math.floor(anchor[0] + 0.5) : (float) anchor[0];
                    cps.anchorFY = snap ? (float) Math.floor(anchor[1] + 0.5) : (float) anchor[1];
                    cps.anchorFZ = snap ? (float) Math.floor(anchor[2] + 0.5) : (float) anchor[2];
                    int newAX = (int) Math.floor(cps.anchorFX);
                    int newAY = (int) Math.floor(cps.anchorFY);
                    int newAZ = (int) Math.floor(cps.anchorFZ);
                    if (newAX != cps.anchorX || newAY != cps.anchorY || newAZ != cps.anchorZ) {
                        cps.anchorX = newAX;
                        cps.anchorY = newAY;
                        cps.anchorZ = newAZ;
                        cps.rebuildPreview();
                    }
                }
            } else if (cps.planeGizmo.isDragging()) {
                double[] anchor = cps.planeGizmo.updateDrag(mx, my);
                if (anchor != null) {
                    cps.anchorFX = snap ? (float) Math.floor(anchor[0] + 0.5) : (float) anchor[0];
                    cps.anchorFY = snap ? (float) Math.floor(anchor[1] + 0.5) : (float) anchor[1];
                    cps.anchorFZ = snap ? (float) Math.floor(anchor[2] + 0.5) : (float) anchor[2];
                    int newAX = (int) Math.floor(cps.anchorFX);
                    int newAY = (int) Math.floor(cps.anchorFY);
                    int newAZ = (int) Math.floor(cps.anchorFZ);
                    if (newAX != cps.anchorX || newAY != cps.anchorY || newAZ != cps.anchorZ) {
                        cps.anchorX = newAX;
                        cps.anchorY = newAY;
                        cps.anchorZ = newAZ;
                        cps.rebuildPreview();
                    }
                }
            } else if (cps.rotGizmo.isDragging()) {
                float delta = cps.rotGizmo.updateDrag(mx, my);
                RotationGizmo.Axis axis = cps.rotGizmo.getDragAxis();
                float[] Rbase = ShapeMath.buildRotationMatrix(cps.rotDragBaseX, cps.rotDragBaseY, cps.rotDragBaseZ);
                float[] dR = axis == RotationGizmo.Axis.X ? ShapeMath.buildRotationMatrix(delta, 0, 0)
                    : axis == RotationGizmo.Axis.Y ? ShapeMath.buildRotationMatrix(0, delta, 0)
                        : ShapeMath.buildRotationMatrix(0, 0, delta);
                float[] Rnew = ShapeMath.multiplyRotationMatrices(Rbase, dR);
                float[] angles = ShapeMath.decomposeRotationMatrix(Rnew);
                cps.rotX = angles[0];
                cps.rotY = angles[1];
                cps.rotZ = angles[2];
                cps.rebuildPreview();
            }
        }

        MoveToolState ms = MoveToolState.INSTANCE;
        if (ms.active) {
            if (ms.gizmo.isDragging()) {
                double[] anchor = ms.gizmo.updateDrag(mx, my);
                if (anchor != null) {
                    float nx = snap ? (float) Math.floor(anchor[0] + 0.5) : (float) anchor[0];
                    float ny = snap ? (float) Math.floor(anchor[1] + 0.5) : (float) anchor[1];
                    float nz = snap ? (float) Math.floor(anchor[2] + 0.5) : (float) anchor[2];
                    ms.deltaFX = nx - ms.cmX;
                    ms.deltaFY = ny - ms.cmY;
                    ms.deltaFZ = nz - ms.cmZ;
                    ms.invalidateGhost();
                }
            } else if (ms.planeGizmo.isDragging()) {
                double[] anchor = ms.planeGizmo.updateDrag(mx, my);
                if (anchor != null) {
                    float nx = snap ? (float) Math.floor(anchor[0] + 0.5) : (float) anchor[0];
                    float ny = snap ? (float) Math.floor(anchor[1] + 0.5) : (float) anchor[1];
                    float nz = snap ? (float) Math.floor(anchor[2] + 0.5) : (float) anchor[2];
                    ms.deltaFX = nx - ms.cmX;
                    ms.deltaFY = ny - ms.cmY;
                    ms.deltaFZ = nz - ms.cmZ;
                    ms.invalidateGhost();
                }
            } else if (ms.rotGizmo.isDragging()) {
                float delta = ms.rotGizmo.updateDrag(mx, my);
                RotationGizmo.Axis axis = ms.rotGizmo.getDragAxis();
                float[] Rbase = ShapeMath.buildRotationMatrix(ms.rotDragBaseX, ms.rotDragBaseY, ms.rotDragBaseZ);
                float[] dR = axis == RotationGizmo.Axis.X ? ShapeMath.buildRotationMatrix(delta, 0, 0)
                    : axis == RotationGizmo.Axis.Y ? ShapeMath.buildRotationMatrix(0, delta, 0)
                        : ShapeMath.buildRotationMatrix(0, 0, delta);
                float[] Rnew = ShapeMath.multiplyRotationMatrices(Rbase, dR);
                float[] angles = ShapeMath.decomposeRotationMatrix(Rnew);
                if (Math.abs(angles[0] - ms.rotX) >= 0.5f || Math.abs(angles[1] - ms.rotY) >= 0.5f
                    || Math.abs(angles[2] - ms.rotZ) >= 0.5f) {
                    ms.rotX = angles[0];
                    ms.rotY = angles[1];
                    ms.rotZ = angles[2];
                    ms.invalidateGhost();
                }
            }
        }
    }

    // ── Camera operations ─────────────────────────────────────────────────────

    private void applyRotate(FreecamEntity cam, float rawDX, float rawDY, float scale) {
        cam.rotationYaw += rawDX * scale * 0.15f; // match vanilla: yaw += getDX()
        cam.rotationPitch -= rawDY * scale * 0.15f; // match vanilla: pitch -= getDY()
        cam.rotationPitch = Math.max(-90.0f, Math.min(90.0f, cam.rotationPitch));
    }

    private void applyPan(FreecamEntity cam, float rawDX, float rawDY) {
        double yaw = Math.toRadians(cam.rotationYaw);
        double pitch = Math.toRadians(cam.rotationPitch);

        // Right vector (flat, perpendicular to yaw).
        double rgtX = Math.cos(yaw);
        double rgtZ = Math.sin(yaw);

        // Up vector = fwd × right (right-handed camera frame).
        double upX = -Math.sin(yaw) * Math.sin(pitch);
        double upY = Math.cos(pitch);
        double upZ = Math.cos(yaw) * Math.sin(pitch);

        float panScale = 0.05f;
        cam.posX += rgtX * rawDX * panScale - upX * rawDY * panScale;
        cam.posY += -upY * rawDY * panScale;
        cam.posZ += rgtZ * rawDX * panScale - upZ * rawDY * panScale;
    }

    private void startOrbit(FreecamState fs, FreecamEntity cam, Minecraft mc, boolean useCursor) {
        double rdx, rdy, rdz;

        if (useCursor) {
            double ndcX = UICoords.guiToNdcX(fs.cursorX);
            double ndcY = UICoords.guiToNdcY(fs.cursorY);

            double yaw = Math.toRadians(cam.rotationYaw);
            double pitch = Math.toRadians(cam.rotationPitch);
            double fwdX = -Math.sin(yaw) * Math.cos(pitch);
            double fwdY = -Math.sin(pitch);
            double fwdZ = Math.cos(yaw) * Math.cos(pitch);
            double rgtX = Math.cos(yaw);
            double rgtZ = Math.sin(yaw);
            double upX = -Math.sin(yaw) * Math.sin(pitch);
            double upY = Math.cos(pitch);
            double upZ = Math.cos(yaw) * Math.sin(pitch);

            rdx = fwdX + ndcX * fs.projTanHX * rgtX + ndcY * fs.projTanHY * upX;
            rdy = fwdY + ndcY * fs.projTanHY * upY;
            rdz = fwdZ + ndcX * fs.projTanHX * rgtZ + ndcY * fs.projTanHY * upZ;
            double len = Math.sqrt(rdx * rdx + rdy * rdy + rdz * rdz);
            rdx /= len;
            rdy /= len;
            rdz /= len;

            Vec3 start = Vec3.createVectorHelper(cam.posX, cam.posY, cam.posZ);
            Vec3 end = Vec3.createVectorHelper(cam.posX + rdx * 512, cam.posY + rdy * 512, cam.posZ + rdz * 512);
            MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(start, end, false);
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                fs.pivotX = hit.blockX + 0.5;
                fs.pivotY = hit.blockY + 0.5;
                fs.pivotZ = hit.blockZ + 0.5;
            } else {
                fs.pivotX = cam.posX + rdx * 20;
                fs.pivotY = cam.posY + rdy * 20;
                fs.pivotZ = cam.posZ + rdz * 20;
            }
        } else {
            double yaw = Math.toRadians(cam.rotationYaw);
            double pitch = Math.toRadians(cam.rotationPitch);
            rdx = -Math.sin(yaw) * Math.cos(pitch);
            rdy = -Math.sin(pitch);
            rdz = Math.cos(yaw) * Math.cos(pitch);

            Vec3 start = Vec3.createVectorHelper(cam.posX, cam.posY, cam.posZ);
            Vec3 end = Vec3.createVectorHelper(cam.posX + rdx * 512, cam.posY + rdy * 512, cam.posZ + rdz * 512);
            MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(start, end, false);

            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                fs.pivotX = hit.blockX + 0.5;
                fs.pivotY = hit.blockY + 0.5;
                fs.pivotZ = hit.blockZ + 0.5;
            } else {
                fs.pivotX = cam.posX + rdx * 20;
                fs.pivotY = cam.posY + rdy * 20;
                fs.pivotZ = cam.posZ + rdz * 20;
            }
        }

        double ox = cam.posX - fs.pivotX;
        double oy = cam.posY - fs.pivotY;
        double oz = cam.posZ - fs.pivotZ;
        fs.orbitDist = Math.max(1.0, Math.sqrt(ox * ox + oy * oy + oz * oz));

        // Store the angular offset from camera look direction to pivot direction so the
        // pivot stays at the same screen-space position throughout the orbit.
        double pivotDist = fs.orbitDist;
        double pivotYawRad = Math.atan2(ox, -oz);
        double pivotPitchRad = Math.asin(Math.max(-1.0, Math.min(1.0, oy / pivotDist)));
        fs.pivotOffsetYaw = (float) Math.toDegrees(pivotYawRad) - cam.rotationYaw;
        fs.pivotOffsetPitch = (float) Math.toDegrees(pivotPitchRad) - cam.rotationPitch;

        fs.orbiting = true;
    }

    private void applyOrbit(FreecamState fs, FreecamEntity cam, float rawDX, float rawDY, float scale) {
        cam.rotationYaw += rawDX * scale * 0.15f;
        cam.rotationPitch -= rawDY * scale * 0.15f;
        cam.rotationPitch = Math.max(-89.9f, Math.min(89.9f, cam.rotationPitch));

        // Pivot direction = camera look direction + fixed angular offset recorded at drag start.
        // This keeps the pivot at the same screen position as the orbit rotates.
        double pivotYaw = Math.toRadians(cam.rotationYaw + fs.pivotOffsetYaw);
        double pivotPitch = Math.toRadians(cam.rotationPitch + fs.pivotOffsetPitch);
        double pFwdX = -Math.sin(pivotYaw) * Math.cos(pivotPitch);
        double pFwdY = -Math.sin(pivotPitch);
        double pFwdZ = Math.cos(pivotYaw) * Math.cos(pivotPitch);

        cam.posX = fs.pivotX - pFwdX * fs.orbitDist;
        cam.posY = fs.pivotY - pFwdY * fs.orbitDist;
        cam.posZ = fs.pivotZ - pFwdZ * fs.orbitDist;

        // Orbit updates position every render tick, but prevPos and lastTickPos are
        // normally only synced in client ticks. EntityRenderer uses lastTickPos for
        // world-space render offset and prevPos for interpolation — both must match
        // posX each render tick or blocks ghost/lag relative to the camera.
        cam.prevPosX = cam.posX;
        cam.prevPosY = cam.posY;
        cam.prevPosZ = cam.posZ;
        cam.lastTickPosX = cam.posX;
        cam.lastTickPosY = cam.posY;
        cam.lastTickPosZ = cam.posZ;
        cam.prevRotationYaw = cam.rotationYaw;
        cam.prevRotationPitch = cam.rotationPitch;
    }

    // ── World disconnect cleanup ──────────────────────────────────────────────

    @SubscribeEvent
    public void onClientDisconnect(
        cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        DimensiumMode.INSTANCE.fullReset();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isSprinting(Minecraft mc) {
        return Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode());
    }

    private boolean isKeyComboDown(int key, int mods) {
        if (!Keyboard.isKeyDown(key)) return false;
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        boolean alt = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        boolean needCtrl = (mods & Dimensium.MOD_CTRL) != 0;
        boolean needShift = (mods & Dimensium.MOD_SHIFT) != 0;
        boolean needAlt = (mods & Dimensium.MOD_ALT) != 0;
        return ctrl == needCtrl && shift == needShift && alt == needAlt;
    }

    private static String toolActionName(Tool tool) {
        return switch (tool) {
            case FREEHAND_DRAW -> I18n
                .format("dimensium.action.draw", I18n.format(BrushState.INSTANCE.brushShape.label));
            case PAINTER -> I18n.format("dimensium.action.paint", I18n.format(BrushState.INSTANCE.brushShape.label));
            case NOISE -> I18n
                .format("dimensium.action.noise", I18n.format(NoiseToolState.INSTANCE.noiseParams.noiseType.label));
            case GRADIENT -> I18n
                .format("dimensium.action.gradient", I18n.format(GradientToolState.INSTANCE.gradientShape.label));
            case SMOOTH -> I18n.format("dimensium.action.smooth");
            case ELEVATION -> I18n
                .format("dimensium.action.elevation", I18n.format(ElevationToolState.INSTANCE.elevationMode.label));
            case ROCK -> I18n.format("dimensium.action.rock");
            case SHATTER -> I18n.format("dimensium.action.shatter");
            default -> I18n.format("dimensium.action.edit");
        };
    }
}
