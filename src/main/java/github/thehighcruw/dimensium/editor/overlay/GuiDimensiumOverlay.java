/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.overlay;

import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.freecam.FreecamUtils;
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
import github.thehighcruw.dimensium.shared.math.Vec2DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.ChangeProposal;
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

        Vec3DDouble[] basis = FreecamUtils.cameraBasis(eye.rotationYaw, eye.rotationPitch);
        Vec3DDouble fwd = basis[0], rgt = basis[1], up = basis[2];

        Vec3DDouble rd = Vec3DDouble.from(
                        fwd.x() + rgt.x() * ndcX * tanHX + up.x() * ndcY * tanHY,
                        fwd.y() + up.y() * ndcY * tanHY,
                        fwd.z() + rgt.z() * ndcX * tanHX + up.z() * ndcY * tanHY)
                .normalize();
        Vec3DDouble eyePos = Vec3DDouble.from(eye.posX, eye.posY + eye.getEyeHeight(), eye.posZ);

        // Offset start slightly forward so the ray doesn't immediately hit the block the camera is inside.
        double near = DimensiumConfig.raycastNearClip;
        double far = DimensiumConfig.raycastDistance;
        Vec3 start = Vec3.createVectorHelper(
                eyePos.x() + rd.x() * near, eyePos.y() + rd.y() * near, eyePos.z() + rd.z() * near);
        Vec3 end = Vec3.createVectorHelper(
                eyePos.x() + rd.x() * far, eyePos.y() + rd.y() * far, eyePos.z() + rd.z() * far);
        return mc.theWorld.rayTraceBlocks(start, end);
    }

    // ── Click dispatch ────────────────────────────────────────────────────────

    public static void handleClick(int mouseX, int mouseY, int scaledW, int scaledH, int button) {
        // ImGui-based popups handle their own clicks via ImGui input routing.
        if (ConflictPopup.INSTANCE.isOpen()
                || CreateBlueprintPopup.INSTANCE.isOpen()
                || BlueprintBrowserPopup.INSTANCE.isOpen()
                || OverlayRenderer.picker.isOpen()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (button == 2) {
            MovingObjectPosition mop = raycastFromMouse(
                    (int) FreecamState.INSTANCE.cursorX, (int) FreecamState.INSTANCE.cursorY, scaledW, scaledH);
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
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
        ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
        if (cps.active) {
            if (button == KeyConstants.LMB) {
                EntityLivingBase eye = mc.renderViewEntity;
                double ccx = cps.centerX(), ccy = cps.centerY(), ccz = cps.centerZ();
                if (eye != null && cps.getAxisTranslationGizmo().hoveredAxis != TranslationGizmo.Axis.NONE) {
                    cps.getAxisTranslationGizmo()
                            .startDrag(
                                    mouseX,
                                    mouseY,
                                    ccx,
                                    ccy,
                                    ccz,
                                    cps.anchorF.x(),
                                    cps.anchorF.y(),
                                    cps.anchorF.z(),
                                    0,
                                    0,
                                    0);
                } else if (eye != null
                        && cps.getPlaneTranslationGizmo().hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                    cps.getPlaneTranslationGizmo()
                            .startDrag(
                                    mouseX,
                                    mouseY,
                                    ccx,
                                    ccy,
                                    ccz,
                                    cps.anchorF.x(),
                                    cps.anchorF.y(),
                                    cps.anchorF.z(),
                                    cps.rot.x(),
                                    cps.rot.y(),
                                    cps.rot.z());
                } else if (eye != null && cps.getRotationGizmo().hoveredAxis != RotationGizmo.Axis.NONE) {
                    cps.rotDragBase = cps.rot;
                    cps.getRotationGizmo()
                            .startDrag(mouseX, mouseY, ccx, ccy, ccz, cps.rot.x(), cps.rot.y(), cps.rot.z());
                }
            } else if (button == KeyConstants.RMB) {
                cps.cancel();
            }
            return;
        }

        Tool tool = DimensiumEditorMode.INSTANCE.selectedTool;
        BrushInput brushInput = BrushInputRegistry.get(tool);
        if (brushInput != null) {
            MovingObjectPosition mop = raycastFromMouse(
                    (int) FreecamState.INSTANCE.cursorX, (int) FreecamState.INSTANCE.cursorY, scaledW, scaledH);
            brushInput.onMouseClick(button, mc, mop);
        }
        // Brush tools are applied while held — see TickHandler.applyPaintIfHeld
    }

    public static void handleRelease(int button) {
        if (button == KeyConstants.LMB) {
            ShapePlacementState ps = ShapePlacementState.INSTANCE;
            if (ps.active) {
                if (ps.getAxisTranslationGizmo().isDragging())
                    ps.getAxisTranslationGizmo().endDrag();
                if (ps.getRotationGizmo().isDragging()) ps.getRotationGizmo().endDrag();
                if (ps.getScalingGizmo().isDragging()) {
                    // scaleX/Y/Z already reset to 1f each drag frame; ShapeToolState already updated
                    ps.getScalingGizmo().endDrag();
                }
                if (ps.getPlaneTranslationGizmo().isDragging())
                    ps.getPlaneTranslationGizmo().endDrag();
                if (ps.viewPlaneGizmo.isDragging()) ps.viewPlaneGizmo.endDrag();
            }
            ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
            if (cps.active) {
                if (cps.getAxisTranslationGizmo().isDragging())
                    cps.getAxisTranslationGizmo().endDrag();
                if (cps.getPlaneTranslationGizmo().isDragging())
                    cps.getPlaneTranslationGizmo().endDrag();
                if (cps.getRotationGizmo().isDragging()) cps.getRotationGizmo().endDrag();
            }
            MoveToolState ms = MoveToolState.INSTANCE;
            if (ms.active) {
                if (ms.getAxisTranslationGizmo().isDragging())
                    ms.getAxisTranslationGizmo().endDrag();
                if (ms.getPlaneTranslationGizmo().isDragging())
                    ms.getPlaneTranslationGizmo().endDrag();
                if (ms.getRotationGizmo().isDragging()) ms.getRotationGizmo().endDrag();
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
            if (pts.getAxisTranslationGizmo().isDragging())
                pts.getAxisTranslationGizmo().endDrag();
            if (pts.getPlaneTranslationGizmo().isDragging())
                pts.getPlaneTranslationGizmo().endDrag();
            ModellingToolState mtsDrag = ModellingToolState.INSTANCE;
            if (mtsDrag.getAxisTranslationGizmo().isDragging())
                mtsDrag.getAxisTranslationGizmo().endDrag();
            if (mtsDrag.getPlaneTranslationGizmo().isDragging())
                mtsDrag.getPlaneTranslationGizmo().endDrag();
        } else if (button == KeyConstants.RMB) {
            Tool tool = DimensiumEditorMode.INSTANCE.selectedTool;
            if (tool == Tool.SELECT) {
                SelectionState sel = SelectionState.INSTANCE;
                if (sel.pendingPos1) {
                    MovingObjectPosition mop = RenderUtils.raycastAtCursor();
                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                        sel.pendingPos2 = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
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
        System.err.println("[DIMTIMER] confirmMove buildOps=" + _buildMs + "ms sendChunked=" + _sendMs + "ms ops="
                + moveOps.size());

        // Build new snapshot from the placed blocks (no world-read — avoids server-packet timing gap)
        Map<Long, SelectionState.BlockData> newSnap = new HashMap<>(ms.ghostBlocks.size());
        float ncx = 0, ncy = 0, ncz = 0;
        for (int[] b : ms.ghostBlocks) {
            Block blk = Block.getBlockById(b[3]);
            if (blk != null && blk != Blocks.air) {
                newSnap.put(
                        SelectionState.pack(Vec3DInt.from(b[0], b[1], b[2])), new SelectionState.BlockData(blk, b[4]));
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
    public static int findNearestPointOnScreen(
            List<int[]> positions, int skipIndex, int mouseX, int mouseY, GizmoProjection proj, double thresholdPx) {
        // GizmoProjection.project() already maps GL window coords to the viewport panel's
        // GUI-space position, so projected coords compare directly to mouseX/mouseY.
        int best = -1;
        double bestD2 = thresholdPx * thresholdPx;
        for (int i = 0; i < positions.size(); i++) {
            if (i == skipIndex) continue;
            int[] p = positions.get(i);
            double[] s = proj.project(p[0] + 0.5, p[1] + 0.5, p[2] + 0.5);
            if (s == null) continue;
            double d2 = Vec2DDouble.from(s[0] - mouseX, s[1] - mouseY).lengthSq();
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
                ops.add(new int[] {
                    ChangeProposal.unpackX(key), ChangeProposal.unpackY(key), ChangeProposal.unpackZ(key), bm[0], bm[1]
                });
            }
            String pathAction =
                    I18n.format("dimensium.action.path", I18n.format(PathToolState.INSTANCE.curveType.label));
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
            Vec3DInt wc = ChangeProposal.unpackKey(key);
            if (keepExisting) {
                if (WorldUtils.getBlock(Minecraft.getMinecraft().theWorld, wc) != Blocks.air) continue;
            }
            ops.add(new int[] {wc.x(), wc.y(), wc.z(), bm[0], bm[1]});
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
        if (ps.active && ps.isAnyGizmoDragging()) return true;
        ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
        if (cps.active && cps.isAnyGizmoDragging()) return true;
        MoveToolState ms = MoveToolState.INSTANCE;
        if (ms.active && ms.isAnyGizmoDragging()) return true;
        SelectionState sel = SelectionState.INSTANCE;
        if (sel.boxConfirmed
                && (SelectionRenderer.boxPos1Gizmo.isDragging()
                        || SelectionRenderer.boxPos2Gizmo.isDragging()
                        || SelectionRenderer.boxCenterViewPlaneGizmo.isDragging()
                        || SelectionRenderer.boxCenterGizmo.isDragging())) return true;
        if (PathToolState.INSTANCE.getAxisTranslationGizmo().isDragging()
                || PathToolState.INSTANCE.getPlaneTranslationGizmo().isDragging()) return true;
        return ModellingToolState.INSTANCE.getAxisTranslationGizmo().isDragging()
                || ModellingToolState.INSTANCE.getPlaneTranslationGizmo().isDragging();
    }

    /** Commits the pending box selection (boxConfirmed state) and clears gizmo state. */
    public static void commitBoxSelection(SelectionState sel, BoxSelectToolState ts) {
        sel.applyOp(
                SelectionState.aabbBlocks(
                        sel.pendingPos.x(),
                        sel.pendingPos.y(),
                        sel.pendingPos.z(),
                        sel.pendingPos2.x(),
                        sel.pendingPos2.y(),
                        sel.pendingPos2.z()),
                ts.booleanOp);
        sel.boxConfirmed = false;
        SelectionRenderer.boxPos1Gizmo.reset();
        SelectionRenderer.boxPos2Gizmo.reset();
        SelectionRenderer.boxCenterViewPlaneGizmo.reset();
        SelectionRenderer.boxCenterGizmo.reset();
    }
}
