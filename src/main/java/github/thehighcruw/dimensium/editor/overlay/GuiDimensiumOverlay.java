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
import github.thehighcruw.dimensium.editor.tool.creating.path.PathToolState;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeBlendUtil;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapePlacementState;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.modify.ModifyToolState;
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
import github.thehighcruw.dimensium.editor.window.viewport.world.ScalingGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.SelectionRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec2DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
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
import net.minecraft.world.World;

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
            ItemStack picked = WorldUtils.blockStackFromMop(mc.theWorld, mop);
            if (picked != null) {
                RecentBlockHistory.add(picked);
                SelectedBlockState.INSTANCE.selectedBlock = picked;
            }
            return;
        }
        ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
        if (cps.active) {
            if (button == KeyConstants.LMB) {
                EntityLivingBase eye = mc.renderViewEntity;
                Vec3DDouble cpsCenter = cps.center();
                Vec3DDouble anchor = cps.anchorF.toDouble();
                Vec3DFloat rot = cps.rot;
                if (eye != null && cps.getAxisTranslationGizmo().hoveredAxis != TranslationGizmo.Axis.NONE) {
                    cps.getAxisTranslationGizmo().startDrag(mouseX, mouseY, cpsCenter, anchor, rot);
                } else if (eye != null
                        && cps.getPlaneTranslationGizmo().hoveredPlane != PlaneTranslationGizmo.Plane.NONE) {
                    cps.getPlaneTranslationGizmo().startDrag(mouseX, mouseY, cpsCenter, anchor, rot);
                } else if (eye != null && cps.getRotationGizmo().hoveredAxis != RotationGizmo.Axis.NONE) {
                    cps.rotDragBase = rot;
                    cps.getRotationGizmo().startDrag(mouseX, mouseY, cpsCenter, rot);
                } else if (eye != null && cps.getScalingGizmo().hoveredAxis != ScalingGizmo.Axis.NONE) {
                    ScalingGizmo.Axis axis = cps.getScalingGizmo().hoveredAxis;
                    float currentScale = axis == ScalingGizmo.Axis.X
                            ? cps.scale.x()
                            : axis == ScalingGizmo.Axis.Y ? cps.scale.y() : cps.scale.z();
                    cps.getScalingGizmo().startDrag(mouseX, mouseY, cpsCenter, currentScale, rot);
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
                if (cps.viewPlaneGizmo.isDragging()) cps.viewPlaneGizmo.endDrag();
                if (cps.getAxisTranslationGizmo().isDragging())
                    cps.getAxisTranslationGizmo().endDrag();
                if (cps.getPlaneTranslationGizmo().isDragging())
                    cps.getPlaneTranslationGizmo().endDrag();
                if (cps.getRotationGizmo().isDragging()) cps.getRotationGizmo().endDrag();
                if (cps.getScalingGizmo().isDragging()) cps.getScalingGizmo().endDrag();
            }
            MoveToolState ms = MoveToolState.INSTANCE;
            if (ms.active) {
                if (ms.getAxisTranslationGizmo().isDragging())
                    ms.getAxisTranslationGizmo().endDrag();
                if (ms.getPlaneTranslationGizmo().isDragging())
                    ms.getPlaneTranslationGizmo().endDrag();
                if (ms.getRotationGizmo().isDragging()) ms.getRotationGizmo().endDrag();
                if (ms.getScalingGizmo().isDragging()) ms.getScalingGizmo().endDrag();
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
            ModifyToolState mods = ModifyToolState.INSTANCE;
            if (mods.getAxisTranslationGizmo().isDragging())
                mods.getAxisTranslationGizmo().endDrag();
            if (mods.getPlaneTranslationGizmo().isDragging())
                mods.getPlaneTranslationGizmo().endDrag();
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
                        SelectionRenderer.boxPos1ViewPlaneGizmo.reset();
                        SelectionRenderer.boxPos1PlaneGizmo.reset();
                        SelectionRenderer.boxPos1Gizmo.reset();
                        SelectionRenderer.boxPos2ViewPlaneGizmo.reset();
                        SelectionRenderer.boxPos2PlaneGizmo.reset();
                        SelectionRenderer.boxPos2Gizmo.reset();
                        SelectionRenderer.boxCenterViewPlaneGizmo.reset();
                        SelectionRenderer.boxCenterPlaneGizmo.reset();
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
        List<int[]> moveOps = new ArrayList<>();
        moveOps.addAll(SelectionOps.selectionToAirOps(sel));
        moveOps.addAll(ms.ghostBlocks);
        BlockSender.sendChunked(moveOps, I18n.format("dimensium.action.move"));

        // Build new snapshot from the placed blocks (no world-read — avoids server-packet timing gap).
        // Blocks outside the valid world Y range [0, 255] are skipped: SelectionState.pack truncates Y
        // to 8 bits, so negative Y corrupts the X bits and produces an invalid key.
        Map<Long, SelectionState.BlockData> newSnap = new HashMap<>(ms.ghostBlocks.size());
        float ncx = 0, ncy = 0, ncz = 0;
        int validCount = 0;
        for (int[] b : ms.ghostBlocks) {
            if (b[1] < 0 || b[1] > 255) continue;
            Block blk = Block.getBlockById(b[3]);
            if (blk != null && blk != Blocks.air) {
                newSnap.put(
                        SelectionState.pack(Vec3DInt.from(b[0], b[1], b[2])), new SelectionState.BlockData(blk, b[4]));
            }
            ncx += b[0] + 0.5f;
            ncy += b[1] + 0.5f;
            ncz += b[2] + 0.5f;
            validCount++;
        }
        int ghostCount = Math.max(1, validCount);

        // Update selection to new positions
        Set<Long> newSel = new HashSet<>(newSnap.keySet());
        sel.applyOp(newSel, BooleanOp.REPLACE);

        // Re-activate with known block data — selection renderVersion just changed via applyOp
        ms.activateFromSnapshot(sel, newSnap, Vec3DFloat.from(ncx / ghostCount, ncy / ghostCount, ncz / ghostCount));
        MoveToolState.INSTANCE.preview = null;
    }

    public static void confirmPlacement() {
        ShapePlacementState ps = ShapePlacementState.INSTANCE;
        if (!ps.active) return;
        ShapeToolState toolState = ShapeToolState.INSTANCE;
        SelectedBlockState sbs = SelectedBlockState.INSTANCE;
        if (sbs.selectedBlock == null) {
            ps.cancel();
            return;
        }

        // Snapshot all mutable state on the main thread before handing off to background.
        final Vec3DInt anchor = Vec3DInt.floor(ps.anchorF);
        final Vec3DInt dims = ps.baseDims;
        final ShapeToolState.ShapeType shapeType = toolState.shapeType;
        final boolean hollow = toolState.shapeHollow;
        final boolean keepExisting = toolState.shapeKeepExisting;
        final float exponent = toolState.shapeExponent;
        final int torusRingRadius = toolState.torusRingRadius;
        final int torusRingRadiusZ =
                toolState.torusSeparateAxes ? toolState.torusRingRadiusZ : toolState.torusRingRadius;
        final int torusTubeRadius = toolState.torusTubeRadius;
        final int tubeWallThickness = toolState.tubeWallThickness;
        final int polygonSides = toolState.shapePolygonSides;
        final float spiralSpacing = toolState.shapeSpiralSpacing;
        final float spiralTurns = toolState.shapeSpiralTurns;
        final float threshold = DimensiumConfig.shapeThreshold;
        final int blockId = Block.getIdFromBlock(Block.getBlockFromItem(sbs.selectedBlock.getItem()));
        final int meta = sbs.selectedBlock.getItemDamage();
        final boolean metaballBlend = toolState.metaballBlend;
        final int metaballBlendRadius = toolState.metaballBlendRadius;
        final String action =
                (hollow ? I18n.format("dimensium.ui.shape.hollow") + " " : "") + I18n.format(shapeType.label);

        final Mat3DFloat rotation = ShapeMath.buildRotationMatrix(ps.rot.x(), ps.rot.y(), ps.rot.z());
        final Vec3DInt[] bounds = ShapeMath.computeRotatedBounds(rotation, dims);

        // World queries must happen on the main thread; snapshot the result for the background supplier.
        final Set<Vec3DInt> existingNonAir;
        if (keepExisting || metaballBlend) {
            int blendRadius = metaballBlend ? metaballBlendRadius : 0;
            Vec3DInt scanMin = anchor.plus(bounds[0]).minus(Vec3DInt.from(blendRadius, blendRadius, blendRadius));
            Vec3DInt scanMax = anchor.plus(bounds[1]).plus(Vec3DInt.from(blendRadius, blendRadius, blendRadius));
            existingNonAir = new HashSet<>();
            World world = Minecraft.getMinecraft().theWorld;
            if (world != null) {
                Vec3DInt.forEachInclusive(scanMin, scanMax, pos -> {
                    if (WorldUtils.getBlock(world, pos) != Blocks.air) existingNonAir.add(pos);
                });
            }
        } else {
            existingNonAir = null;
        }

        BlockSender.sendChunkedLazy(
                () -> {
                    List<int[]> ops = new ArrayList<>();
                    ShapeMath.iterateRotatedShape(
                            shapeType,
                            dims,
                            hollow,
                            exponent,
                            torusRingRadius,
                            torusRingRadiusZ,
                            torusTubeRadius,
                            tubeWallThickness,
                            exponent,
                            polygonSides,
                            spiralSpacing,
                            spiralTurns,
                            threshold,
                            rotation,
                            bounds[0],
                            bounds[1],
                            offset -> {
                                Vec3DInt pos = anchor.plus(offset);
                                if (keepExisting && existingNonAir != null && existingNonAir.contains(pos)) return true;
                                ops.add(pos.toBlockOp(blockId, meta));
                                return true;
                            });

                    if (metaballBlend && metaballBlendRadius > 0 && !ops.isEmpty() && existingNonAir != null) {
                        Set<Vec3DInt> shapeVoxels = new HashSet<>(ops.size());
                        for (int[] op : ops) shapeVoxels.add(Vec3DInt.from(op[0], op[1], op[2]));
                        Set<Vec3DInt> terrain = new HashSet<>(existingNonAir);
                        terrain.removeAll(shapeVoxels);
                        if (!terrain.isEmpty()) {
                            Vec3DInt blendMin = anchor.plus(bounds[0])
                                    .minus(Vec3DInt.from(
                                            metaballBlendRadius, metaballBlendRadius, metaballBlendRadius));
                            Vec3DInt blendMax = anchor.plus(bounds[1])
                                    .plus(Vec3DInt.from(metaballBlendRadius, metaballBlendRadius, metaballBlendRadius));
                            for (Vec3DInt pos : ShapeBlendUtil.computeBlendPositions(
                                    shapeVoxels, terrain, blendMin, blendMax, metaballBlendRadius)) {
                                ops.add(pos.toBlockOp(blockId, meta));
                            }
                        }
                    }
                    return ops;
                },
                action);
        ps.cancel();
    }

    public static void confirmClipboardPlacement() {
        ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
        if (!cps.active) return;
        BlockSender.sendChunkedUnmasked(cps.toOps(), I18n.format("dimensium.action.paste"));
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
            List<Vec3DInt> positions, int skipIndex, int mouseX, int mouseY, GizmoProjection proj, double thresholdPx) {
        // GizmoProjection.project() already maps GL window coords to the viewport panel's
        // GUI-space position, so projected coords compare directly to mouseX/mouseY.
        int best = -1;
        double bestD2 = thresholdPx * thresholdPx;
        for (int i = 0; i < positions.size(); i++) {
            if (i == skipIndex) continue;
            Vec2DDouble s = proj.project(positions.get(i).toDouble().plus(0.5));
            if (s == null) continue;
            double d2 = s.minus(Vec2DDouble.from(mouseX, mouseY)).lengthSq();
            if (d2 < bestD2) {
                bestD2 = d2;
                best = i;
            }
        }
        return best;
    }

    private static List<int[]> proposalToOps(ChangeProposal proposal, boolean keepExisting) {
        if (!keepExisting) return proposal.toOps();
        List<int[]> ops = new ArrayList<>();
        for (Map.Entry<Long, int[]> e : proposal.proposed.entrySet()) {
            Vec3DInt wc = ChangeProposal.unpackKey(e.getKey());
            if (WorldUtils.getBlock(Minecraft.getMinecraft().theWorld, wc) == Blocks.air) {
                ops.add(wc.toBlockOp(e.getValue()[0], e.getValue()[1]));
            }
        }
        return ops;
    }

    public static void applyPath() {
        PathToolState pts = PathToolState.INSTANCE;
        ChangeProposal p = pts.preview;
        if (p != null && !p.proposed.isEmpty()) {
            String pathAction = I18n.format("dimensium.action.path", I18n.format(pts.curveType.labelKey));
            List<int[]> ops = proposalToOps(p, pts.keepExisting);
            if (!ops.isEmpty()) BlockSender.sendChunked(ops, pathAction);
        }
        pts.clear();
    }

    public static void confirmModify() {
        ModifyToolState mods = ModifyToolState.INSTANCE;
        if (mods.ghostBlocks == null || mods.ghostBlocks.isEmpty()) return;
        String actionKey;
        switch (mods.mode) {
            case REVOLVE:
                actionKey = "dimensium.action.revolve";
                break;
            case TWIST:
                actionKey = "dimensium.action.twist";
                break;
            default:
                actionKey = "dimensium.action.translate_copies";
                break;
        }
        if (mods.mode == ModifyToolState.ModifyMode.TWIST) {
            SelectionState sel = SelectionState.INSTANCE;
            List<int[]> ops = new ArrayList<>();
            ops.addAll(SelectionOps.selectionToAirOps(sel));
            ops.addAll(mods.ghostBlocks);
            BlockSender.sendChunked(ops, I18n.format(actionKey));
        } else {
            BlockSender.sendChunked(mods.ghostBlocks, I18n.format(actionKey));
        }
        mods.clearAfterConfirm();
    }

    public static void applyModelling() {
        ModellingToolState mts = ModellingToolState.INSTANCE;
        SelectedBlockState sbs = SelectedBlockState.INSTANCE;
        mts.rebuildIfNeeded(sbs.selectedBlock);
        if (mts.preview == null || mts.preview.proposed.isEmpty()) return;

        boolean keepExisting = mts.pasteMode == ModellingToolState.PasteMode.KEEP_EXISTING;
        List<int[]> ops = proposalToOps(mts.preview, keepExisting);
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
                        || SelectionRenderer.boxPos1PlaneGizmo.isDragging()
                        || SelectionRenderer.boxPos2Gizmo.isDragging()
                        || SelectionRenderer.boxPos2PlaneGizmo.isDragging()
                        || SelectionRenderer.boxCenterViewPlaneGizmo.isDragging()
                        || SelectionRenderer.boxCenterGizmo.isDragging()
                        || SelectionRenderer.boxCenterPlaneGizmo.isDragging())) return true;
        if (PathToolState.INSTANCE.getAxisTranslationGizmo().isDragging()
                || PathToolState.INSTANCE.getPlaneTranslationGizmo().isDragging()) return true;
        if (ModifyToolState.INSTANCE.getAxisTranslationGizmo().isDragging()
                || ModifyToolState.INSTANCE.getPlaneTranslationGizmo().isDragging()) return true;
        return ModellingToolState.INSTANCE.getAxisTranslationGizmo().isDragging()
                || ModellingToolState.INSTANCE.getPlaneTranslationGizmo().isDragging();
    }

    /** Commits the pending box selection (boxConfirmed state) and clears gizmo state. */
    public static void commitBoxSelection(SelectionState sel, BoxSelectToolState ts) {
        sel.applyOp(SelectionState.aabbBlocks(sel.pendingPos, sel.pendingPos2), ts.booleanOp);
        sel.boxConfirmed = false;
        SelectionRenderer.boxPos1ViewPlaneGizmo.reset();
        SelectionRenderer.boxPos1PlaneGizmo.reset();
        SelectionRenderer.boxPos1Gizmo.reset();
        SelectionRenderer.boxPos2ViewPlaneGizmo.reset();
        SelectionRenderer.boxPos2PlaneGizmo.reset();
        SelectionRenderer.boxPos2Gizmo.reset();
        SelectionRenderer.boxCenterViewPlaneGizmo.reset();
        SelectionRenderer.boxCenterPlaneGizmo.reset();
        SelectionRenderer.boxCenterGizmo.reset();
    }
}
