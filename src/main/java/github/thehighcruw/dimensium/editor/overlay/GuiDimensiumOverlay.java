/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.overlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.handler.SelectionOps;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.BrushInputRegistry;
import github.thehighcruw.dimensium.editor.tool.Tool;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingToolState;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapePlacementState;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.move.MoveToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.editor.tool.selecting.box.BoxSelectToolState;
import github.thehighcruw.dimensium.editor.tool.state.ClipboardPlacementState;
import github.thehighcruw.dimensium.editor.window.RecentBlockHistory;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.popup.BlueprintBrowserPopup;
import github.thehighcruw.dimensium.editor.window.popup.ConflictPopup;
import github.thehighcruw.dimensium.editor.window.popup.CreateBlueprintPopup;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportRegistry;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportState;
import github.thehighcruw.dimensium.editor.window.viewport.world.GizmoProjection;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.RotationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.SelectionRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.network.PacketHandler;
import github.thehighcruw.dimensium.network.PacketShapePlacement;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.ChangeProposal;

/**
 * Static helpers for overlay mouse interaction.
 * No longer a GuiScreen — the overlay renders as a HUD so the game stays
 * in in-game mode (mouse captured, WASD active, freecam works).
 */
public final class GuiDimensiumOverlay {

    private GuiDimensiumOverlay() {}

    // ── Raycast ───────────────────────────────────────────────────────────────

    /**
     * Cast a world ray from the 2D overlay mouse cursor.
     * Uses mc.renderViewEntity so freecam perspective is respected.
     */
    public static MovingObjectPosition raycastFromMouse(int mouseX, int mouseY, int scaledW, int scaledH) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase eye = mc.renderViewEntity;
        if (eye == null || mc.theWorld == null) return null;

        ViewportState vp = ViewportRegistry.INSTANCE.active();
        double ndcX = vp != null ? vp.cursorToNdcX(mouseX, scaledW) : 1.0 - (2.0 * mouseX / scaledW);
        double ndcY = vp != null ? vp.cursorToNdcY(mouseY, scaledH) : 1.0 - (2.0 * mouseY / scaledH);

        double tanHX = FreecamState.INSTANCE.projTanHX;
        double tanHY = FreecamState.INSTANCE.projTanHY;

        double yaw = Math.toRadians(eye.rotationYaw);
        double pitch = Math.toRadians(eye.rotationPitch);

        double lookX = -Math.sin(yaw) * Math.cos(pitch);
        double lookY = -Math.sin(pitch);
        double lookZ = Math.cos(yaw) * Math.cos(pitch);

        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);

        double upX = -Math.sin(yaw) * Math.sin(pitch);
        double upY = Math.cos(pitch);
        double upZ = Math.cos(yaw) * Math.sin(pitch);

        double rdx = lookX + rightX * ndcX * tanHX + upX * ndcY * tanHY;
        double rdy = lookY + upY * ndcY * tanHY;
        double rdz = lookZ + rightZ * ndcX * tanHX + upZ * ndcY * tanHY;
        double len = Math.sqrt(rdx * rdx + rdy * rdy + rdz * rdz);
        rdx /= len;
        rdy /= len;
        rdz /= len;

        double eyeX = eye.posX;
        double eyeY = eye.posY + eye.getEyeHeight();
        double eyeZ = eye.posZ;

        // Offset start slightly forward so the ray doesn't immediately hit the block the camera is inside.
        double near = github.thehighcruw.dimensium.DimensiumConfig.raycastNearClip;
        double far = github.thehighcruw.dimensium.DimensiumConfig.raycastDistance;
        Vec3 start = Vec3.createVectorHelper(eyeX + rdx * near, eyeY + rdy * near, eyeZ + rdz * near);
        Vec3 end = Vec3.createVectorHelper(eyeX + rdx * far, eyeY + rdy * far, eyeZ + rdz * far);
        return mc.theWorld.rayTraceBlocks(start, end);
    }

    // ── Click dispatch ────────────────────────────────────────────────────────

    public static void handleClick(int mouseX, int mouseY, int scaledW, int scaledH, int button) {
        // ImGui-based popups handle their own clicks via ImGui input routing.
        if (ConflictPopup.INSTANCE.isOpen() || CreateBlueprintPopup.INSTANCE.isOpen()
            || BlueprintBrowserPopup.INSTANCE.isOpen()
            || OverlayRenderer.picker.isOpen()) {
            return;
        }
        // mouseX/mouseY are in scaled GUI pixels; panel widths are physical pixels — convert.
        Minecraft _mc = Minecraft.getMinecraft();
        int _sf = RenderUtils.scaleFactor();
        int physX = mouseX * _sf;
        float _uiScale = ImGuiManager.INSTANCE.getUIScale();
        if (physX < OverlayRenderer.TOOL_WINDOW.currentW * _uiScale) {
            // Left panel is now ImGui — clicks handled by ImGui input routing.
        } else {
            if (button == 2) {
                MovingObjectPosition mop = raycastFromMouse(
                    (int) FreecamState.INSTANCE.cursorX,
                    (int) FreecamState.INSTANCE.cursorY,
                    scaledW,
                    scaledH);
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    Minecraft mc = Minecraft.getMinecraft();
                    Block b = mc.theWorld.getBlock(mop.blockX, mop.blockY, mop.blockZ);
                    int meta = mc.theWorld.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);
                    if (b != null && b != Blocks.air) {
                        ItemStack picked = new ItemStack(b, 1, meta);
                        RecentBlockHistory.add(picked);
                        SelectedBlockState.INSTANCE.selectedBlock = picked;
                    }
                }
                return;
            }
            ClipboardPlacementState _cps = ClipboardPlacementState.INSTANCE;
            if (_cps.active) {
                if (button == KeyConstants.LMB) {
                    EntityLivingBase _eye = _mc.renderViewEntity;
                    double ccx = _cps.centerX(), ccy = _cps.centerY(), ccz = _cps.centerZ();
                    if (_eye != null && _cps.gizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                        _cps.gizmo.startDrag(
                            mouseX,
                            mouseY,
                            ccx,
                            ccy,
                            ccz,
                            _cps.anchorFX,
                            _cps.anchorFY,
                            _cps.anchorFZ,
                            0,
                            0,
                            0);
                    } else if (_eye != null && _cps.planeGizmo.hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                        _cps.planeGizmo.startDrag(
                            mouseX,
                            mouseY,
                            ccx,
                            ccy,
                            ccz,
                            _cps.anchorFX,
                            _cps.anchorFY,
                            _cps.anchorFZ,
                            _cps.rotX,
                            _cps.rotY,
                            _cps.rotZ);
                    } else if (_eye != null && _cps.rotGizmo.hoveredAxis != RotationGizmo.Axis.NONE) {
                        _cps.rotDragBaseX = _cps.rotX;
                        _cps.rotDragBaseY = _cps.rotY;
                        _cps.rotDragBaseZ = _cps.rotZ;
                        _cps.rotGizmo.startDrag(mouseX, mouseY, ccx, ccy, ccz, _cps.rotX, _cps.rotY, _cps.rotZ);
                    }
                } else if (button == KeyConstants.RMB) {
                    _cps.cancel();
                }
                return;
            }

            Tool tool = DimensiumEditorMode.INSTANCE.selectedTool;
            BrushInput brushInput = BrushInputRegistry.get(tool);
            if (brushInput != null) {
                brushInput.onMouseClick(button, Minecraft.getMinecraft(), null);
            }
            // Brush tools are applied while held — see TickHandler.applyPaintIfHeld
        }
    }

    public static void handleRelease(int button) {
        if (button == KeyConstants.LMB) {
            ShapePlacementState ps = ShapePlacementState.INSTANCE;
            if (ps.active) {
                if (ps.gizmo.isDragging()) ps.gizmo.endDrag();
                if (ps.rotGizmo.isDragging()) ps.rotGizmo.endDrag();
                if (ps.scaleGizmo.isDragging()) {
                    // scaleX/Y/Z already reset to 1f each drag frame; ShapeToolState already updated
                    ps.scaleGizmo.endDrag();
                }
                if (ps.planeGizmo.isDragging()) ps.planeGizmo.endDrag();
                if (ps.viewPlaneGizmo.isDragging()) ps.viewPlaneGizmo.endDrag();
            }
            ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
            if (cps.active) {
                if (cps.gizmo.isDragging()) cps.gizmo.endDrag();
                if (cps.planeGizmo.isDragging()) cps.planeGizmo.endDrag();
                if (cps.rotGizmo.isDragging()) cps.rotGizmo.endDrag();
            }
            MoveToolState ms = MoveToolState.INSTANCE;
            if (ms.active) {
                if (ms.gizmo.isDragging()) ms.gizmo.endDrag();
                if (ms.planeGizmo.isDragging()) ms.planeGizmo.endDrag();
                if (ms.rotGizmo.isDragging()) ms.rotGizmo.endDrag();
            }
            SelectionState sel = SelectionState.INSTANCE;
            if (sel.boxConfirmed) {
                if (SelectionRenderer.boxPos1Gizmo.isDragging()) SelectionRenderer.boxPos1Gizmo.endDrag();
                if (SelectionRenderer.boxPos1PlaneGizmo.isDragging()) SelectionRenderer.boxPos1PlaneGizmo.endDrag();
                if (SelectionRenderer.boxPos2Gizmo.isDragging()) SelectionRenderer.boxPos2Gizmo.endDrag();
                if (SelectionRenderer.boxPos2PlaneGizmo.isDragging()) SelectionRenderer.boxPos2PlaneGizmo.endDrag();
                if (SelectionRenderer.boxCenterViewPlaneGizmo.isDragging())
                    SelectionRenderer.boxCenterViewPlaneGizmo.endDrag();
                if (SelectionRenderer.boxCenterGizmo.isDragging()) SelectionRenderer.boxCenterGizmo.endDrag();
                if (SelectionRenderer.boxCenterPlaneGizmo.isDragging()) SelectionRenderer.boxCenterPlaneGizmo.endDrag();
            }
            PathToolState pts = PathToolState.INSTANCE;
            if (pts.gizmo.isDragging()) pts.gizmo.endDrag();
            if (pts.planeGizmo.isDragging()) pts.planeGizmo.endDrag();
            ModellingToolState mtsDrag = ModellingToolState.INSTANCE;
            if (mtsDrag.gizmo.isDragging()) mtsDrag.gizmo.endDrag();
            if (mtsDrag.planeGizmo.isDragging()) mtsDrag.planeGizmo.endDrag();
        } else if (button == KeyConstants.RMB) {
            Tool tool = DimensiumEditorMode.INSTANCE.selectedTool;
            if (tool == Tool.SELECT) {
                SelectionState sel = SelectionState.INSTANCE;
                if (sel.pendingPos1) {
                    MovingObjectPosition mop = RenderUtils.raycastAtCursor();
                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                        sel.pendingX2 = mop.blockX;
                        sel.pendingY2 = mop.blockY;
                        sel.pendingZ2 = mop.blockZ;
                        sel.pendingPos1 = false;
                        sel.boxConfirmed = true;
                        SelectionRenderer.boxPos1Gizmo.reset();
                        SelectionRenderer.boxPos2Gizmo.reset();
                        SelectionRenderer.boxCenterViewPlaneGizmo.reset();
                        SelectionRenderer.boxCenterGizmo.reset();
                    }
                }
            }
        }
    }

    public static void confirmMove() {
        MoveToolState ms = MoveToolState.INSTANCE;
        SelectionState sel = SelectionState.INSTANCE;
        if (!ms.active || ms.ghostBlocks == null || ms.ghostBlocks.isEmpty() || !sel.hasSelection()) return;

        // Erase originals and place at new positions as a single history entry
        long _t0 = System.nanoTime();
        List<int[]> moveOps = new ArrayList<>();
        moveOps.addAll(SelectionOps.selectionToAirOps(sel));
        moveOps.addAll(ms.ghostBlocks);
        long _t1 = System.nanoTime();
        BlockSender.sendChunked(moveOps, I18n.format("dimensium.action.move"));
        long _t2 = System.nanoTime();
        long _buildMs = (_t1 - _t0) / 1_000_000;
        long _sendMs = (_t2 - _t1) / 1_000_000;
        System.err.println(
            "[DIMTIMER] confirmMove buildOps=" + _buildMs + "ms sendChunked=" + _sendMs + "ms ops=" + moveOps.size());

        // Build new snapshot from the placed blocks (no world-read — avoids server-packet timing gap)
        Map<Long, SelectionState.BlockData> newSnap = new HashMap<>(ms.ghostBlocks.size());
        float ncx = 0, ncy = 0, ncz = 0;
        for (int[] b : ms.ghostBlocks) {
            Block blk = Block.getBlockById(b[3]);
            if (blk != null && blk != Blocks.air) {
                newSnap.put(SelectionState.pack(b[0], b[1], b[2]), new SelectionState.BlockData(blk, b[4]));
            }
            ncx += b[0] + 0.5f;
            ncy += b[1] + 0.5f;
            ncz += b[2] + 0.5f;
        }
        ncx /= ms.ghostBlocks.size();
        ncy /= ms.ghostBlocks.size();
        ncz /= ms.ghostBlocks.size();

        // Update selection to new positions
        Set<Long> newSel = new HashSet<>(newSnap.keySet());
        sel.applyOp(newSel, BooleanOp.REPLACE);

        // Re-activate with known block data — selection renderVersion just changed via applyOp
        ms.activateFromSnapshot(sel, newSnap, ncx, ncy, ncz);
        MoveToolState.INSTANCE.preview = null;
    }

    public static void confirmPlacement() {
        ShapePlacementState ps = ShapePlacementState.INSTANCE;
        if (!ps.active) return;
        ShapeToolState s = ShapeToolState.INSTANCE;
        SelectedBlockState sbs = SelectedBlockState.INSTANCE;
        if (sbs.selectedBlock != null) {
            PacketHandler.CHANNEL.sendToServer(new PacketShapePlacement(ps, s, sbs));
        }
        ps.cancel();
    }

    public static void confirmClipboardPlacement() {
        ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
        if (!cps.active) return;
        BlockSender.sendChunked(cps.toOps(), I18n.format("dimensium.action.paste"));
        cps.cancel();
    }

    /** Cancels any pending fill proposal (called on LMB or tool switch). */
    public static void cancelFillPreview() {
        BuilderToolState.INSTANCE.fillPreview = null;
    }

    /**
     * Returns the index of the closest point (from {@code positions}) to the screen-space mouse
     * cursor, or -1 if none is within {@code thresholdPx} pixels. Skips {@code skipIndex}.
     * Each entry in {@code positions} is {worldX, worldY, worldZ}.
     */
    public static int findNearestPointOnScreen(List<int[]> positions, int skipIndex, int mouseX, int mouseY,
        GizmoProjection proj, double thresholdPx) {
        // GizmoProjection.project() already maps GL window coords to the viewport panel's
        // GUI-space position, so projected coords compare directly to mouseX/mouseY.
        int best = -1;
        double bestD2 = thresholdPx * thresholdPx;
        for (int i = 0; i < positions.size(); i++) {
            if (i == skipIndex) continue;
            int[] p = positions.get(i);
            double[] s = proj.project(p[0] + 0.5, p[1] + 0.5, p[2] + 0.5);
            if (s == null) continue;
            double dx = s[0] - mouseX, dy = s[1] - mouseY;
            double d2 = dx * dx + dy * dy;
            if (d2 < bestD2) {
                bestD2 = d2;
                best = i;
            }
        }
        return best;
    }

    public static void applyPath() {
        ChangeProposal p = PathToolState.INSTANCE.preview;
        if (p != null && !p.proposed.isEmpty()) {
            List<int[]> ops = new ArrayList<>(p.proposed.size());
            for (Map.Entry<Long, int[]> e : p.proposed.entrySet()) {
                long key = e.getKey();
                int[] bm = e.getValue();
                ops.add(
                    new int[] { ChangeProposal.unpackX(key), ChangeProposal.unpackY(key), ChangeProposal.unpackZ(key),
                        bm[0], bm[1] });
            }
            String pathAction = I18n
                .format("dimensium.action.path", I18n.format(PathToolState.INSTANCE.curveType.label));
            BlockSender.sendChunked(ops, pathAction);
        }
        PathToolState.INSTANCE.clear();
    }

    public static void applyModelling() {
        ModellingToolState mts = ModellingToolState.INSTANCE;
        SelectedBlockState sbs = SelectedBlockState.INSTANCE;
        mts.rebuildIfNeeded(sbs.selectedBlock);
        if (mts.preview == null || mts.preview.proposed.isEmpty()) return;

        boolean keepExisting = mts.pasteMode == ModellingToolState.PasteMode.KEEP_EXISTING;
        List<int[]> ops = new ArrayList<>(mts.preview.proposed.size());
        for (Map.Entry<Long, int[]> e : mts.preview.proposed.entrySet()) {
            long key = e.getKey();
            int[] bm = e.getValue();
            if (keepExisting) {
                int wx = ChangeProposal.unpackX(key), wy = ChangeProposal.unpackY(key),
                    wz = ChangeProposal.unpackZ(key);
                if (Minecraft.getMinecraft().theWorld.getBlock(wx, wy, wz) != Blocks.air) continue;
            }
            ops.add(
                new int[] { ChangeProposal.unpackX(key), ChangeProposal.unpackY(key), ChangeProposal.unpackZ(key),
                    bm[0], bm[1] });
        }
        if (!ops.isEmpty()) {
            BlockSender.sendChunked(ops, I18n.format("dimensium.action.modelling"));
        }
        mts.clear();
    }

    /**
     * Returns true if any tool gizmo is currently being dragged.
     * Used by TickHandler to suppress LMB camera rotation when a tool drag is active.
     */
    public static boolean anyGizmoDragging() {
        ShapePlacementState ps = ShapePlacementState.INSTANCE;
        if (ps.active && (ps.gizmo.isDragging() || ps.rotGizmo.isDragging()
            || ps.scaleGizmo.isDragging()
            || ps.viewPlaneGizmo.isDragging())) return true;
        ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
        if (cps.active && (cps.gizmo.isDragging() || cps.rotGizmo.isDragging())) return true;
        MoveToolState ms = MoveToolState.INSTANCE;
        if (ms.active && (ms.gizmo.isDragging() || ms.rotGizmo.isDragging())) return true;
        SelectionState sel = SelectionState.INSTANCE;
        if (sel.boxConfirmed
            && (SelectionRenderer.boxPos1Gizmo.isDragging() || SelectionRenderer.boxPos2Gizmo.isDragging()
                || SelectionRenderer.boxCenterViewPlaneGizmo.isDragging()
                || SelectionRenderer.boxCenterGizmo.isDragging()))
            return true;
        if (PathToolState.INSTANCE.gizmo.isDragging()) return true;
        if (ModellingToolState.INSTANCE.gizmo.isDragging()) return true;
        return false;
    }

    /** Commits the pending box selection (boxConfirmed state) and clears gizmo state. */
    public static void commitBoxSelection(SelectionState sel, BoxSelectToolState ts) {
        sel.applyOp(
            SelectionState
                .aabbBlocks(sel.pendingX, sel.pendingY, sel.pendingZ, sel.pendingX2, sel.pendingY2, sel.pendingZ2),
            ts.booleanOp);
        sel.boxConfirmed = false;
        SelectionRenderer.boxPos1Gizmo.reset();
        SelectionRenderer.boxPos2Gizmo.reset();
        SelectionRenderer.boxCenterViewPlaneGizmo.reset();
        SelectionRenderer.boxCenterGizmo.reset();
    }

}
