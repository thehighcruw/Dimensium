/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.freecam.FreecamEntity;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.freecam.FreecamUtils;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.overlay.MenuBar;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.tool.BrushApplicator;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.BrushInputRegistry;
import github.thehighcruw.dimensium.editor.tool.Tool;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapePlacementState;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.ElevationBrushInput;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.ElevationToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.move.MoveToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.smooth.SmoothBrushInput;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.editor.tool.painting.gradient.GradientToolState;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseToolState;
import github.thehighcruw.dimensium.editor.tool.state.ClipboardPlacementState;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportRegistry;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportState;
import github.thehighcruw.dimensium.editor.window.viewport.world.ScalingGizmo;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.PerfTrace;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

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
    private Vec3DInt lastDragBlock = null;

    private void cancelDrag() {
        ChangeProposal.cancel();
        lastDragBlock = null;
    }

    private void cancelDrag(BrushInput input, Minecraft mc) {
        if (input != null) input.onBrushRelease(mc);
        cancelDrag();
    }

    /** Read-only view of accumulated SMOOTH drag positions. */
    public Set<Vec3DInt> getSmoothDragPositions() {
        return SmoothBrushInput.INSTANCE.getDragPositions();
    }

    /** True while a brush paint drag is active (RMB held, first stroke fired). */
    public boolean isPaintDragging() {
        return lastDragBlock != null || ElevationBrushInput.INSTANCE.isPaintDragging();
    }

    // ── Mouse / camera ────────────────────────────────────────────────────────
    // RenderTickEvent.PRE fires before EntityRenderer.updateCameraAndRender().
    // Consuming getDX/getDY here leaves vanilla with 0 — player body stays still.

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        FreecamState fs = FreecamState.INSTANCE;
        Minecraft mc = Minecraft.getMinecraft();

        // Suppress first-person arm/item before EntityRenderer.renderHand fires.
        // renderHand checks hideGUI and skips the arm render entirely when true.
        // We already cancel all vanilla HUD elements via onHudPre, so this has no other visual effect.
        // Restore happens in OverlayRenderer.onRenderOverlay (ALL event) after renderHand.
        if (fs.active) mc.gameSettings.hideGUI = true;

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

        Vec3DDouble move = Vec3DDouble.from(dx, dy, dz);
        double len = move.length();
        if (len > 1.0) move = move.divide(len);

        cam.posX += move.x() * speed;
        cam.posY += move.y() * speed;
        cam.posZ += move.z() * speed;
    }

    // ── Hold-to-paint ─────────────────────────────────────────────────────────

    // All brush tools paint continuously while held (stroke behavior).
    // Called from onRenderTick (after cursor update) for frame-rate-accurate strokes.
    // Block-position dedup ensures each block gets one packet per drag pass even at 60+ fps.
    private void applyPaintIfHeld(Minecraft mc, int sw, int sh) {
        if (!DimensiumEditorMode.INSTANCE.isActive()) return;
        if (ImGuiManager.INSTANCE.anyModalOpen()) return;

        FreecamState fs = FreecamState.INSTANCE;
        int mx = (int) fs.cursorX, my = (int) fs.cursorY;
        boolean snap = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
        Tool tool = DimensiumEditorMode.INSTANCE.selectedTool;
        BrushInput input = BrushInputRegistry.get(tool);

        // Gizmo drag state update — runs before paint loop so world renderers see fresh state.
        if (input != null) input.onGizmoDrag(mx, my, snap);
        updatePlacementGizmos(mx, my, snap);

        int sf = RenderUtils.scaleFactor();

        // Tools that handle their own drag tick (e.g. LASSO_SELECT, FREEHAND_SELECT, ELEVATION).
        if (input != null && input.onDragTick(mc, sw, sh)) return;

        if (!BrushInputRegistry.usesDragLoop(tool)) {
            // Tool switched mid-drag — discard any pending proposal.
            cancelDrag(input, mc);
            return;
        }
        // Suppress paint during any camera movement (pan, orbit, LMB drag).
        // Cancel any in-progress stroke so it isn't flushed when the camera drag ends.
        if (fs.isMoving()) {
            if (lastDragBlock != null) cancelDrag(input, mc);
            return;
        }
        if (!Mouse.isButtonDown(KeyConstants.RMB)) {
            if (lastDragBlock != null) {
                // RMB released after an active drag — release, flush, send.
                PerfTrace.begin("brushRelease tool=" + tool);
                PerfTrace.push("onBrushRelease");
                if (input != null) input.onBrushRelease(mc);
                PerfTrace.pop();
                PerfTrace.push("flush");
                List<int[]> ops = ChangeProposal.flush();
                PerfTrace.pop();
                PerfTrace.push("sendChunked ops=" + ops.size());
                if (!ops.isEmpty()) BlockSender.sendChunked(ops, toolActionName(tool));
                PerfTrace.pop();
                PerfTrace.end(5);
                lastDragBlock = null;
            }
            return;
        }
        if (mx * sf < OverlayRenderer.TOOL_WINDOW.getWidth() || my * sf < (int) MenuBar.INSTANCE.height()) return;

        MovingObjectPosition mop = GuiDimensiumOverlay.raycastFromMouse(mx, my, sw, sh);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        // Deduplicate: skip if cursor hasn't moved into a new block since last stroke.
        Vec3DInt mopPos = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        if (mopPos.equals(lastDragBlock)) return;

        if (lastDragBlock == null) {
            // First stroke of a new drag — open a fresh proposal.
            ChangeProposal.startDrag(ToolMaskRegistry.INSTANCE.getActiveMask());
            if (input != null) input.onBrushDragStart(mc, mop);
        }
        lastDragBlock = mopPos;

        // Let tool handle the stroke; fall back to BrushApplicator for standard brush tools.
        PerfTrace.begin("brushStroke tool=" + tool);
        PerfTrace.push("onBrushHeld/applyTool");
        if (input == null || !input.onBrushHeld(mc, mop)) {
            BrushApplicator.applyTool(mc.theWorld, WorldUtils.mopToCoord(mop));
        }
        PerfTrace.pop();
        PerfTrace.end(16);
    }

    // ── Gizmo drag state ─────────────────────────────────────────────────────

    private void updatePlacementGizmos(int mx, int my, boolean snap) {
        ShapePlacementState ps = ShapePlacementState.INSTANCE;
        if (ps.active) {
            if (ps.getAxisTranslationGizmo().isDragging()
                    || ps.getPlaneTranslationGizmo().isDragging()
                    || ps.viewPlaneGizmo.isDragging()) {
                Vec3DDouble anchor = ps.getAxisTranslationGizmo().isDragging()
                        ? ps.getAxisTranslationGizmo().updateDrag(mx, my)
                        : ps.getPlaneTranslationGizmo().isDragging()
                                ? ps.getPlaneTranslationGizmo().updateDrag(mx, my)
                                : ps.viewPlaneGizmo.updateDrag(mx, my);
                if (anchor != null) {
                    ps.anchorF = Vec3DFloat.from(
                            AnchorSnap.toFloat(anchor.x(), snap),
                            AnchorSnap.toFloat(anchor.y(), snap),
                            AnchorSnap.toFloat(anchor.z(), snap));
                    ps.anchor = Vec3DInt.floor(ps.anchorF);
                }
            } else if (ps.getRotationGizmo().isDragging()) {
                Vec3DFloat angles = AnchorSnap.applyRotGizmo(ps.getRotationGizmo(), ps.rotDragBase, mx, my);
                if (angles.minus(ps.rot).abs().max() >= 0.5f) {
                    ps.rot = angles;
                    ps.invalidateGhost();
                }
            } else if (ps.getScalingGizmo().isDragging()) {
                float[] result = ps.getScalingGizmo().updateDrag(mx, my);
                if (result != null) {
                    ScalingGizmo.Axis axis = ps.getScalingGizmo().getDragAxis();
                    if (axis == ScalingGizmo.Axis.X) ps.scale = Vec3DFloat.from(result[0], ps.scale.y(), ps.scale.z());
                    else if (axis == ScalingGizmo.Axis.Y)
                        ps.scale = Vec3DFloat.from(ps.scale.x(), result[0], ps.scale.z());
                    else ps.scale = Vec3DFloat.from(ps.scale.x(), ps.scale.y(), result[0]);
                    ShapeToolState sts = ShapeToolState.INSTANCE;
                    if (axis == ScalingGizmo.Axis.X) {
                        sts.shapeWidth = Math.max(1, Math.round(ps.scaleDragBase.x() * ps.scale.x()));
                        ps.scale = Vec3DFloat.from(1f, ps.scale.y(), ps.scale.z());
                    } else if (axis == ScalingGizmo.Axis.Y) {
                        sts.shapeHeight = Math.max(1, Math.round(ps.scaleDragBase.y() * ps.scale.y()));
                        ps.scale = Vec3DFloat.from(ps.scale.x(), 1f, ps.scale.z());
                    } else {
                        sts.shapeDepth = Math.max(1, Math.round(ps.scaleDragBase.z() * ps.scale.z()));
                        ps.scale = Vec3DFloat.from(ps.scale.x(), ps.scale.y(), 1f);
                    }
                    ps.invalidateGhost();
                }
            }
        }

        ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
        if (cps.active) {
            if (cps.getAxisTranslationGizmo().isDragging()
                    || cps.getPlaneTranslationGizmo().isDragging()) {
                Vec3DDouble anchor = cps.getAxisTranslationGizmo().isDragging()
                        ? cps.getAxisTranslationGizmo().updateDrag(mx, my)
                        : cps.getPlaneTranslationGizmo().updateDrag(mx, my);
                if (anchor != null) {
                    Vec3DFloat newAnchorF = Vec3DFloat.from(
                            AnchorSnap.toFloat(anchor.x(), snap),
                            AnchorSnap.toFloat(anchor.y(), snap),
                            AnchorSnap.toFloat(anchor.z(), snap));
                    Vec3DInt newAnchor = Vec3DInt.floor(newAnchorF);
                    cps.anchorF = newAnchorF;
                    if (!newAnchor.equals(cps.anchor)) {
                        cps.anchor = newAnchor;
                        cps.rebuildPreview();
                    }
                }
            } else if (cps.getRotationGizmo().isDragging()) {
                cps.rot = AnchorSnap.applyRotGizmo(cps.getRotationGizmo(), cps.rotDragBase, mx, my);
                cps.rebuildPreview();
            }
        }

        MoveToolState ms = MoveToolState.INSTANCE;
        if (ms.active) {
            if (ms.getAxisTranslationGizmo().isDragging()
                    || ms.getPlaneTranslationGizmo().isDragging()) {
                Vec3DDouble anchor = ms.getAxisTranslationGizmo().isDragging()
                        ? ms.getAxisTranslationGizmo().updateDrag(mx, my)
                        : ms.getPlaneTranslationGizmo().updateDrag(mx, my);
                if (anchor != null) {
                    Vec3DFloat snapped = Vec3DFloat.from(
                            AnchorSnap.toFloat(anchor.x(), snap),
                            AnchorSnap.toFloat(anchor.y(), snap),
                            AnchorSnap.toFloat(anchor.z(), snap));
                    ms.delta = snapped.minus(ms.cm);
                    ms.invalidateGhost();
                }
            } else if (ms.getRotationGizmo().isDragging()) {
                Vec3DFloat angles = AnchorSnap.applyRotGizmo(ms.getRotationGizmo(), ms.rotDragBase, mx, my);
                if (angles.minus(ms.rot).abs().max() >= 0.5f) {
                    ms.rot = angles;
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
        Vec3DDouble[] basis = FreecamUtils.cameraBasis(cam.rotationYaw, cam.rotationPitch);
        Vec3DDouble rgt = basis[1], up = basis[2];
        float panScale = 0.05f;
        cam.posX += rgt.x() * rawDX * panScale - up.x() * rawDY * panScale;
        cam.posY += -up.y() * rawDY * panScale;
        cam.posZ += rgt.z() * rawDX * panScale - up.z() * rawDY * panScale;
    }

    private void startOrbit(FreecamState fs, FreecamEntity cam, Minecraft mc, boolean useCursor) {
        ScaledResolution sr = RenderUtils.scaledResolution();
        ViewportState vp = ViewportRegistry.INSTANCE.active();

        double ndcX, ndcY;
        if (useCursor) {
            ndcX = vp != null
                    ? vp.cursorToNdcX(fs.cursorX, sr.getScaledWidth())
                    : 1.0 - (2.0 * fs.cursorX / sr.getScaledWidth());
            ndcY = vp != null
                    ? vp.cursorToNdcY(fs.cursorY, sr.getScaledHeight())
                    : 1.0 - (2.0 * fs.cursorY / sr.getScaledHeight());
        } else {
            ndcX = 0.0;
            ndcY = 0.0;
        }

        Vec3DDouble[] basis = FreecamUtils.cameraBasis(cam.rotationYaw, cam.rotationPitch);
        Vec3DDouble cursorRay = basis[0].plus(basis[1].times(ndcX * fs.projTanHX))
                .plus(basis[2].times(ndcY * fs.projTanHY))
                .normalize();

        setPivotFromRay(fs, cam, mc, cursorRay);

        fs.orbitNdcX = ndcX;
        fs.orbitNdcY = ndcY;
        // orbitDist = depth of pivot along the cursor ray from the camera.
        // Guarantees C = pivot - orbitDist * cursorRay at start, and applyOrbit preserves this invariant.
        fs.orbitDist = Math.max(
                1.0,
                Vec3DDouble.from(cam.posX, cam.posY, cam.posZ).minus(fs.pivot).length());
        fs.orbiting = true;
    }

    private void applyOrbit(FreecamState fs, FreecamEntity cam, float rawDX, float rawDY, float scale) {
        cam.rotationYaw += rawDX * scale * 0.15f;
        cam.rotationPitch = Math.max(-89.9f, Math.min(89.9f, cam.rotationPitch - rawDY * scale * 0.15f));

        // Recompute the ray through the SAME cursor NDC position with the new camera orientation.
        // Placing the camera at pivot - orbitDist * cursorRay guarantees the pivot projects to
        // (orbitNdcX, orbitNdcY) every frame — the block under the cursor stays fixed on screen.
        Vec3DDouble[] basis = FreecamUtils.cameraBasis(cam.rotationYaw, cam.rotationPitch);
        Vec3DDouble cursorRay = basis[0].plus(basis[1].times(fs.orbitNdcX * fs.projTanHX))
                .plus(basis[2].times(fs.orbitNdcY * fs.projTanHY))
                .normalize();
        Vec3DDouble newCamPos = fs.pivot.minus(cursorRay.times(fs.orbitDist));
        cam.posX = newCamPos.x();
        cam.posY = newCamPos.y();
        cam.posZ = newCamPos.z();

        // Orbit updates position every render tick, but prevPos and lastTickPos are
        // normally only synced in client ticks. EntityRenderer uses lastTickPos for
        // world-space render offset and prevPos for interpolation — both must match
        // posX each render tick or blocks ghost/lag relative to the camera.
        FreecamState.copyPosition(cam, cam);
    }

    private static void setPivotFromRay(FreecamState fs, FreecamEntity cam, Minecraft mc, Vec3DDouble rd) {
        Vec3DDouble camPos = Vec3DDouble.from(cam.posX, cam.posY, cam.posZ);
        Vec3DDouble endPos = camPos.plus(rd.times(FreecamUtils.REACH));
        Vec3 start = Vec3.createVectorHelper(camPos.x(), camPos.y(), camPos.z());
        Vec3 end = Vec3.createVectorHelper(endPos.x(), endPos.y(), endPos.z());
        MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(start, end, false);
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            fs.pivot = Vec3DDouble.fromVec3(hit.hitVec);
        } else {
            fs.pivot = camPos.plus(rd.times(20));
        }
    }

    // ── World disconnect cleanup ──────────────────────────────────────────────

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        DimensiumEditorMode.INSTANCE.fullReset();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isSprinting(Minecraft mc) {
        return Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode());
    }

    private static String toolActionName(Tool tool) {
        return switch (tool) {
            case FREEHAND_DRAW ->
                I18n.format("dimensium.action.draw", I18n.format(BrushState.INSTANCE.brushShape.label));
            case PAINTER -> I18n.format("dimensium.action.paint", I18n.format(BrushState.INSTANCE.brushShape.label));
            case NOISE ->
                I18n.format(
                        "dimensium.action.noise", I18n.format(NoiseToolState.INSTANCE.noiseParams.noiseType().label));
            case GRADIENT ->
                I18n.format("dimensium.action.gradient", I18n.format(GradientToolState.INSTANCE.gradientShape.label));
            case SMOOTH -> I18n.format("dimensium.action.smooth");
            case ELEVATION ->
                I18n.format("dimensium.action.elevation", I18n.format(ElevationToolState.INSTANCE.elevationMode.label));
            case ROCK -> I18n.format("dimensium.action.rock");
            case SHATTER -> I18n.format("dimensium.action.shatter");
            default -> I18n.format("dimensium.action.edit");
        };
    }
}
