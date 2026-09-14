/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.shape;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithAxisTranslationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithPlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithRotationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithScalingGizmo;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.RotationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.ScalingGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.ViewPlaneGizmo;
import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;

/**
 * Client-side state for interactive shape placement.
 * Tracks anchor position, caches ghost block positions, and owns the gizmos.
 */
public class ShapePlacementState
    implements WithAxisTranslationGizmo, WithPlaneTranslationGizmo, WithRotationGizmo, WithScalingGizmo {

    public static final ShapePlacementState INSTANCE = new ShapePlacementState();

    public boolean active = false;
    public Vec3DInt anchor = Vec3DInt.ZERO;
    /** Sub-block float anchor used for rendering and gizmo positioning. */
    public Vec3DFloat anchorF = Vec3DFloat.ZERO;
    /** Pre-rotation bounding box (shape-type adjusted). */
    public int baseW, baseH, baseD;

    /** Rotation angles in degrees around each axis, applied X→Y→Z. Free-angle (not snapped). */
    public Vec3DFloat rot = Vec3DFloat.ZERO;
    /** Rotation snapshot when a rotation drag began. */
    public Vec3DFloat rotDragBase = Vec3DFloat.ZERO;

    /**
     * Pre-computed block offset positions (already rotation-applied).
     * Offsets may be negative (blocks extend before anchor when rotated).
     * Null = exceeds maxGhostBlocks, use bbox fallback.
     */
    public List<int[]> ghostBlocks = null;
    public ChangeProposal preview = null;

    /** Per-axis scale multipliers applied on top of the tool-state dimensions. */
    public Vec3DFloat scale = Vec3DFloat.ONE;
    /** ShapeToolState dimensions captured at scale-drag start; used to avoid per-frame runaway. */
    public int scaleDragBaseW, scaleDragBaseH, scaleDragBaseD;

    private final TranslationGizmo gizmo = new TranslationGizmo();
    private final RotationGizmo rotGizmo = new RotationGizmo();
    private final PlaneTranslationGizmo planeGizmo = new PlaneTranslationGizmo();
    private final ScalingGizmo scalingGizmo = new ScalingGizmo();
    public final ViewPlaneGizmo viewPlaneGizmo = new ViewPlaneGizmo();

    @Override
    public TranslationGizmo getAxisTranslationGizmo() {
        return gizmo;
    }

    @Override
    public PlaneTranslationGizmo getPlaneTranslationGizmo() {
        return planeGizmo;
    }

    @Override
    public RotationGizmo getRotationGizmo() {
        return rotGizmo;
    }

    @Override
    public ScalingGizmo getScalingGizmo() {
        return scalingGizmo;
    }

    private String shapeKey = "";

    public void start(int x, int y, int z) {
        active = true;
        anchor = Vec3DInt.from(x, y, z);
        anchorF = Vec3DFloat.from(x, y, z);
        rot = Vec3DFloat.ZERO;
        scale = Vec3DFloat.ONE;
        gizmo.reset();
        rotGizmo.reset();
        planeGizmo.reset();
        scalingGizmo.reset();
        viewPlaneGizmo.reset();
        shapeKey = "";
        ghostBlocks = null;
        rebuildIfNeeded();
    }

    public void cancel() {
        active = false;
        preview = null;
        gizmo.reset();
        rotGizmo.reset();
        planeGizmo.reset();
        scalingGizmo.reset();
        viewPlaneGizmo.reset();
    }

    /** Force ghost blocks to be rebuilt on next rebuildIfNeeded call. */
    public void invalidateGhost() {
        shapeKey = "";
        ghostBlocks = null;
    }

    /** Rotation center in world space — unchanged by rotation, always the base bbox center. */
    public double centerX() {
        return anchorF.x() + baseW / 2.0;
    }

    public double centerY() {
        return anchorF.y() + baseH / 2.0;
    }

    public double centerZ() {
        return anchorF.z() + baseD / 2.0;
    }

    /**
     * Recomputes effectiveW/H/D and ghostBlocks when ToolState shape params or rotation change.
     * No-op if nothing changed since last call.
     */
    public void rebuildIfNeeded() {
        ShapeToolState s = ShapeToolState.INSTANCE;
        String key = buildKey(s);
        if (key.equals(shapeKey) && ghostBlocks != null) return;
        shapeKey = key;

        int w = Math.max(1, Math.round(s.shapeWidth * scale.x()));
        int h = Math.max(1, Math.round(s.shapeHeight * scale.y()));
        int d = Math.max(1, Math.round(s.shapeDepth * scale.z()));
        if (s.shapeType == ShapeToolState.ShapeType.TORUS) {
            int outerX = s.torusRingRadius + s.torusTubeRadius;
            int outerZ = s.torusRingRadiusZ + s.torusTubeRadius;
            w = outerX * 2 + 1;
            h = s.torusTubeRadius * 2 + 1;
            d = outerZ * 2 + 1;
        } else if (s.shapeType == ShapeToolState.ShapeType.ARCHIMEDEAN_SPIRAL) {
            int r = (int) Math.ceil(s.shapeSpiralSpacing * s.shapeSpiralTurns);
            w = r * 2 + 1;
            h = 1;
            d = r * 2 + 1;
        } else if (!s.shapeSeparateAxes
            && (s.shapeType == ShapeToolState.ShapeType.CYLINDER || s.shapeType == ShapeToolState.ShapeType.CONE
                || s.shapeType == ShapeToolState.ShapeType.TUBE)) {
                    d = w;
                }
        baseW = w;
        baseH = h;
        baseD = d;

        Mat3DFloat R = ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z());

        int[] bounds = ShapeMath.computeRotatedBounds(R, w, h, d);
        int ix0 = bounds[0], iy0 = bounds[1], iz0 = bounds[2];
        int ix1 = bounds[3], iy1 = bounds[4], iz1 = bounds[5];

        ghostBlocks = buildGhostBlocks(s, w, h, d, R, ix0, iy0, iz0, ix1, iy1, iz1);
        rebuildShapeProposal();
    }

    private void rebuildShapeProposal() {
        if (ghostBlocks == null || ghostBlocks.isEmpty()) {
            preview = null;
            return;
        }
        Block blk = null;
        int meta = 0;
        SelectedBlockState sbs = SelectedBlockState.INSTANCE;
        ItemStack sel = sbs.selectedBlock;
        if (sel != null) {
            Block b = Block.getBlockFromItem(sel.getItem());
            if (b != null && b != Blocks.air) {
                blk = b;
                meta = sel.getItemDamage();
            }
        }
        if (blk == null) blk = sbs.getPaintBlock();
        if (blk == null) {
            preview = null;
            return;
        }

        int blockId = Block.getIdFromBlock(blk);
        ChangeProposal p = ChangeProposal.forPreview();
        for (int[] offset : ghostBlocks) {
            long key = ChangeProposal.packKey(anchor.x() + offset[0], anchor.y() + offset[1], anchor.z() + offset[2]);
            p.proposed.put(key, new int[] { blockId, meta });
        }
        preview = p;
    }

    private static List<int[]> buildGhostBlocks(ShapeToolState s, int w, int h, int d, Mat3DFloat R, int ix0, int iy0,
        int iz0, int ix1, int iy1, int iz1) {
        int maxGhost = DimensiumConfig.maxGhostBlocks;
        List<int[]> blocks = new ArrayList<>();
        ShapeMath.iterateRotatedShape(
            s.shapeType,
            w,
            h,
            d,
            s.shapeHollow,
            s.shapeExponent,
            s.torusRingRadius,
            s.torusRingRadiusZ,
            s.torusTubeRadius,
            s.tubeWallThickness,
            s.shapeSupersphereExp,
            s.shapePolygonSides,
            s.shapeSpiralSpacing,
            s.shapeSpiralTurns,
            DimensiumConfig.shapeThreshold,
            R,
            ix0,
            iy0,
            iz0,
            ix1,
            iy1,
            iz1,
            (ox, oy, oz) -> {
                blocks.add(new int[] { ox, oy, oz });
                return blocks.size() < maxGhost;
            });
        return blocks.size() >= maxGhost ? null : blocks;
    }

    private String buildKey(ShapeToolState s) {
        // Round angles to 0.5° to avoid rebuilding on float noise.
        int rx = Math.round(rot.x() * 2);
        int ry = Math.round(rot.y() * 2);
        int rz = Math.round(rot.z() * 2);
        return s.shapeType.ordinal() + ","
            + s.shapeWidth
            + ","
            + s.shapeHeight
            + ","
            + s.shapeDepth
            + ","
            + s.shapeHollow
            + ","
            + s.torusRingRadius
            + ","
            + s.torusRingRadiusZ
            + ","
            + s.torusTubeRadius
            + ","
            + s.shapeSeparateAxes
            + ","
            + s.shapeExponent
            + ","
            + s.shapeSupersphereExp
            + ","
            + s.shapePolygonSides
            + ","
            + s.shapeSpiralSpacing
            + ","
            + s.shapeSpiralTurns
            + ","
            + s.tubeWallThickness
            + ","
            + rx
            + ","
            + ry
            + ","
            + rz
            + ","
            + anchor.x()
            + ","
            + anchor.y()
            + ","
            + anchor.z()
            + ","
            + Math.round(scale.x() * 100)
            + ","
            + Math.round(scale.y() * 100)
            + ","
            + Math.round(scale.z() * 100)
            + ","
            + Math.round(DimensiumConfig.shapeThreshold * 1000);
    }

    public boolean isAnyGizmoDragging() {
        return getAxisTranslationGizmo().isDragging() || getRotationGizmo().isDragging()
            || getScalingGizmo().isDragging()
            || getPlaneTranslationGizmo().isDragging()
            || viewPlaneGizmo.isDragging();
    }
}
