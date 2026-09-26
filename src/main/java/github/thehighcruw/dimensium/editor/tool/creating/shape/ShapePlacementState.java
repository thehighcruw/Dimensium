/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.shape;

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
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.StairSlabSmoother;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

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
    public Vec3DInt baseDims = Vec3DInt.ZERO;

    /** Rotation angles in degrees around each axis, applied X→Y→Z. Free-angle (not snapped). */
    public Vec3DFloat rot = Vec3DFloat.ZERO;
    /** Rotation snapshot when a rotation drag began. */
    public Vec3DFloat rotDragBase = Vec3DFloat.ZERO;

    /**
     * Pre-computed block offset positions (already rotation-applied).
     * Offsets may be negative (blocks extend before anchor when rotated).
     * Null = exceeds maxGhostBlocks, use bbox fallback.
     */
    public List<Vec3DInt> ghostBlocks = null;

    /** Rotated bounding box offsets relative to anchor; updated alongside ghostBlocks. */
    public Vec3DInt boundsMin = Vec3DInt.ZERO;

    public Vec3DInt boundsMax = Vec3DInt.ZERO;

    public ChangeProposal preview = null;

    /** Per-axis scale multipliers applied on top of the tool-state dimensions. */
    public Vec3DFloat scale = Vec3DFloat.ONE;
    /** ShapeToolState dimensions captured at scale-drag start; used to avoid per-frame runaway. */
    public Vec3DInt scaleDragBase = Vec3DInt.ZERO;

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

    public void start(Vec3DInt pos) {
        active = true;
        anchor = pos;
        anchorF = pos.toFloat();
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
    public Vec3DDouble center() {
        return anchorF.toDouble().plus(baseDims.toDouble().times(0.5));
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

        int rawWidth = Math.max(1, Math.round(s.shapeWidth * scale.x()));
        int rawHeight = Math.max(1, Math.round(s.shapeHeight * scale.y()));
        int rawDepth = Math.max(1, Math.round(s.shapeDepth * scale.z()));
        baseDims = s.effectiveDimensions(rawWidth, rawHeight, rawDepth);

        Mat3DFloat R = ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z());

        Vec3DInt[] bounds = ShapeMath.computeRotatedBounds(R, baseDims);
        boundsMin = bounds[0];
        boundsMax = bounds[1];

        ghostBlocks = buildGhostBlocks(s, baseDims, R, boundsMin, boundsMax);
        rebuildShapeProposal(R);
    }

    private void rebuildShapeProposal(Mat3DFloat rotation) {
        if (ghostBlocks == null || ghostBlocks.isEmpty()) {
            preview = null;
            return;
        }
        Block block = null;
        int meta = 0;
        SelectedBlockState sbs = SelectedBlockState.INSTANCE;
        ItemStack sel = sbs.selectedBlock;
        if (sel != null) {
            Block retrievedBlock = Block.getBlockFromItem(sel.getItem());
            if (retrievedBlock != null && retrievedBlock != Blocks.air) {
                block = retrievedBlock;
                meta = sel.getItemDamage();
            }
        }
        if (block == null) block = sbs.getPaintBlock();
        if (block == null) {
            preview = null;
            return;
        }

        int blockId = Block.getIdFromBlock(block);
        Map<Long, int[]> blockMap = new HashMap<>();
        for (Vec3DInt offset : ghostBlocks) {
            Vec3DInt pos = anchor.plus(offset);
            blockMap.put(ChangeProposal.packKey(pos), new int[] {blockId, meta});
        }

        ShapeToolState toolState = ShapeToolState.INSTANCE;
        if (toolState.useStairsAndSlabs) {
            StairSlabSmoother.InsidePredicate insideFn = ShapeMath.buildInsidePredicate(
                    toolState.shapeType,
                    baseDims,
                    toolState.shapeHollow,
                    toolState.shapeExponent,
                    toolState.torusRingRadius,
                    toolState.torusRingRadiusZ,
                    toolState.torusTubeRadius,
                    toolState.tubeWallThickness,
                    toolState.shapeSupersphereExp,
                    toolState.shapePolygonSides,
                    toolState.shapeSpiralSpacing,
                    toolState.shapeSpiralTurns,
                    DimensiumConfig.shapeThreshold,
                    rotation,
                    anchor);
            blockMap = StairSlabSmoother.smooth(blockMap, insideFn);
        }

        ChangeProposal p = ChangeProposal.forPreview();
        p.proposed.putAll(blockMap);

        if (toolState.metaballBlend && toolState.metaballBlendRadius > 0) {
            int blendRadius = toolState.metaballBlendRadius;
            Vec3DInt blendMin = anchor.plus(boundsMin).minus(Vec3DInt.from(blendRadius, blendRadius, blendRadius));
            Vec3DInt blendMax = anchor.plus(boundsMax).plus(Vec3DInt.from(blendRadius, blendRadius, blendRadius));

            Set<Vec3DInt> shapeVoxels = new HashSet<>();
            for (Vec3DInt offset : ghostBlocks) shapeVoxels.add(anchor.plus(offset));

            Set<Vec3DInt> terrain = new HashSet<>();
            World world = Minecraft.getMinecraft().theWorld;
            if (world != null) {
                Vec3DInt.forEachInclusive(blendMin, blendMax, pos -> {
                    if (!shapeVoxels.contains(pos) && WorldUtils.getBlock(world, pos) != Blocks.air) terrain.add(pos);
                });
                for (Vec3DInt pos :
                        ShapeBlendUtil.computeBlendPositions(shapeVoxels, terrain, blendMin, blendMax, blendRadius)) {
                    p.proposed.put(ChangeProposal.packKey(pos), new int[] {blockId, meta});
                }
            }
        }

        preview = p;
    }

    private static List<Vec3DInt> buildGhostBlocks(
            ShapeToolState s, Vec3DInt dims, Mat3DFloat R, Vec3DInt boundsMin, Vec3DInt boundsMax) {
        int maxGhost = DimensiumConfig.maxGhostBlocks;
        List<Vec3DInt> blocks = new ArrayList<>();
        ShapeMath.iterateRotatedShape(
                s.shapeType,
                dims,
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
                boundsMin,
                boundsMax,
                offset -> {
                    blocks.add(offset);
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
                + s.useStairsAndSlabs
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
                + Math.round(DimensiumConfig.shapeThreshold * 1000)
                + ","
                + s.metaballBlend
                + ","
                + s.metaballBlendRadius;
    }

    public boolean isAnyGizmoDragging() {
        return getAxisTranslationGizmo().isDragging()
                || getRotationGizmo().isDragging()
                || getScalingGizmo().isDragging()
                || getPlaneTranslationGizmo().isDragging()
                || viewPlaneGizmo.isDragging();
    }
}
