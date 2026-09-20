/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.move;

import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithAxisTranslationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithPlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithRotationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithScalingGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.RotationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.ScalingGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.ViewPlaneGizmo;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * Client-side state for the Move tool.
 * Owns its own block snapshot — never touches SelectionState.clipboard.
 */
public class MoveToolState
        implements WithAxisTranslationGizmo, WithPlaneTranslationGizmo, WithRotationGizmo, WithScalingGizmo {

    public static final MoveToolState INSTANCE = new MoveToolState();

    public boolean active = false;

    /** SelectionState.renderVersion at the time of last activation — used to detect selection changes. */
    public long capturedSelVersion = -2;

    /** Center of mass of the original selection (block centers averaged). */
    public Vec3DFloat cm = Vec3DFloat.ZERO;

    /** Float translation delta applied by the gizmo drag. */
    public Vec3DFloat delta = Vec3DFloat.ZERO;

    /** Rotation angles in degrees (X→Y→Z). */
    public Vec3DFloat rot = Vec3DFloat.ZERO;
    /** Rotation snapshot at the start of a rotation drag. */
    public Vec3DFloat rotDragBase = Vec3DFloat.ZERO;

    /** Per-axis scale factors — resamples the selection via nearest-neighbour into a scaled bounding box. */
    public Vec3DFloat scale = Vec3DFloat.ONE;

    /**
     * Own snapshot of blocks being moved: world-packed key → BlockData.
     * Captured from world on activation; does NOT touch sel.clipboard.
     */
    private Map<Long, SelectionState.BlockData> snapshot = null;

    /**
     * Pre-transformed ghost positions: [worldX, worldY, worldZ, blockId, meta].
     * Null = needs rebuild.
     */
    public List<int[]> ghostBlocks = null;

    public ChangeProposal preview = null;

    public final ViewPlaneGizmo viewPlaneGizmo = new ViewPlaneGizmo();
    private final PlaneTranslationGizmo planeGizmo = new PlaneTranslationGizmo();
    private final ScalingGizmo scalingGizmo = new ScalingGizmo();
    private final TranslationGizmo gizmo = new TranslationGizmo();
    private final RotationGizmo rotGizmo = new RotationGizmo();

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

    // Cache keys for ghost rebuild
    private Vec3DFloat lastDelta = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
    private Vec3DFloat lastRot = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
    private Vec3DFloat lastScale = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
    private int snapshotVersion = -1;
    private int currentSnapshotVersion = 0;

    /** Activate by reading block data from the world. Called by SelectionRenderer. */
    public void activate(SelectionState sel, World world) {
        if (!sel.hasSelection()) return;
        computeCoM(sel);
        captureSnapshot(sel, world);
        reset();
        capturedSelVersion = sel.renderVersion;
        active = true;
    }

    /**
     * Re-activate after a confirm with known block data (no world read needed).
     * Avoids the server-packet timing gap.
     */
    public void activateFromSnapshot(SelectionState sel, Map<Long, SelectionState.BlockData> snap, Vec3DFloat newCm) {
        cm = newCm;
        snapshot = snap;
        currentSnapshotVersion++;
        reset();
        capturedSelVersion = sel.renderVersion;
        active = true;
    }

    public void cancel() {
        active = false;
        preview = null;
        reset();
    }

    /** World-space gizmo anchor = center of mass + translation delta. */
    public Vec3DDouble gizmoPos() {
        return cm.plus(delta).toDouble();
    }

    public void invalidateGhost() {
        lastDelta = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
        ghostBlocks = null;
    }

    /**
     * Recompute ghost blocks and preview when translation, rotation, scale, or snapshot changes.
     * No-op if nothing changed.
     */
    public void rebuildIfNeeded() {
        if (snapshot == null || snapshot.isEmpty()) return;
        if (lastDelta.equals(delta)
                && lastRot.equals(rot)
                && lastScale.equals(scale)
                && snapshotVersion == currentSnapshotVersion) return;

        lastDelta = delta;
        lastRot = rot;
        lastScale = scale;
        snapshotVersion = currentSnapshotVersion;

        // Build local-space nearest-neighbour lookup with bbox tracking.
        Map<Long, SelectionState.BlockData> localLookup = new HashMap<>(snapshot.size());
        int lMinX = Integer.MAX_VALUE, lMinY = Integer.MAX_VALUE, lMinZ = Integer.MAX_VALUE;
        int lMaxX = Integer.MIN_VALUE, lMaxY = Integer.MIN_VALUE, lMaxZ = Integer.MIN_VALUE;
        float fMinX = Float.MAX_VALUE, fMinY = Float.MAX_VALUE, fMinZ = Float.MAX_VALUE;
        float fMaxX = -Float.MAX_VALUE, fMaxY = -Float.MAX_VALUE, fMaxZ = -Float.MAX_VALUE;
        for (Map.Entry<Long, SelectionState.BlockData> e : snapshot.entrySet()) {
            Vec3DInt wv = SelectionState.unpack(e.getKey());
            Vec3DFloat centered = wv.toFloat().plus(0.5f).minus(cm);
            Vec3DInt lk = Vec3DInt.floor(centered);
            localLookup.put(ChangeProposal.packKey(lk), e.getValue());
            if (lk.x() < lMinX) lMinX = lk.x();
            if (lk.y() < lMinY) lMinY = lk.y();
            if (lk.z() < lMinZ) lMinZ = lk.z();
            if (lk.x() > lMaxX) lMaxX = lk.x();
            if (lk.y() > lMaxY) lMaxY = lk.y();
            if (lk.z() > lMaxZ) lMaxZ = lk.z();
            if (centered.x() < fMinX) fMinX = centered.x();
            if (centered.y() < fMinY) fMinY = centered.y();
            if (centered.z() < fMinZ) fMinZ = centered.z();
            if (centered.x() > fMaxX) fMaxX = centered.x();
            if (centered.y() > fMaxY) fMaxY = centered.y();
            if (centered.z() > fMaxZ) fMaxZ = centered.z();
        }

        int bboxW = lMaxX - lMinX + 1;
        int bboxH = lMaxY - lMinY + 1;
        int bboxD = lMaxZ - lMinZ + 1;
        int scaledW = Math.max(1, Math.round(bboxW * scale.x()));
        int scaledH = Math.max(1, Math.round(bboxH * scale.y()));
        int scaledD = Math.max(1, Math.round(bboxD * scale.z()));
        // Use exact float min/max to preserve correct world positions under nearest-neighbour resampling.
        Vec3DFloat bboxFloatCenter = Vec3DFloat.from((fMinX + fMaxX) / 2f, (fMinY + fMaxY) / 2f, (fMinZ + fMaxZ) / 2f);
        Vec3DFloat scaledCenter = Vec3DFloat.from(scaledW, scaledH, scaledD).divide(2f);

        Mat3DFloat R = ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z());
        Vec3DFloat gizmoPos = cm.plus(delta);

        List<int[]> blocks = new ArrayList<>();
        for (int sx = 0; sx < scaledW; sx++) {
            for (int sy = 0; sy < scaledH; sy++) {
                for (int sz = 0; sz < scaledD; sz++) {
                    // Nearest-neighbour reverse-map into local bbox space.
                    int srcX = Math.min((int) (sx / scale.x()), bboxW - 1);
                    int srcY = Math.min((int) (sy / scale.y()), bboxH - 1);
                    int srcZ = Math.min((int) (sz / scale.z()), bboxD - 1);
                    Vec3DInt lk = Vec3DInt.from(lMinX + srcX, lMinY + srcY, lMinZ + srcZ);
                    SelectionState.BlockData bd = localLookup.get(ChangeProposal.packKey(lk));
                    if (bd == null) continue;
                    Vec3DFloat offset =
                            Vec3DFloat.from(sx + 0.5f, sy + 0.5f, sz + 0.5f).minus(scaledCenter);
                    Vec3DInt nCoord = Vec3DInt.floor(gizmoPos.plus(R.mul(bboxFloatCenter.plus(offset))));
                    blocks.add(nCoord.toBlockOp(Block.getIdFromBlock(bd.block()), bd.meta()));
                }
            }
        }
        ghostBlocks = blocks;

        ChangeProposal p = ChangeProposal.forPreview();
        for (int[] b : blocks) {
            p.proposed.put(ChangeProposal.packKey(Vec3DInt.from(b[0], b[1], b[2])), new int[] {b[3], b[4]});
        }
        preview = p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void computeCoM(SelectionState sel) {
        Vec3DDouble sum = Vec3DDouble.ZERO;
        int cnt = 0;
        for (long key : sel.getSelectedBlocks()) {
            sum = sum.plus(SelectionState.unpack(key).toDouble().plus(0.5));
            cnt++;
        }
        cm = sum.divide(cnt).toFloat();
    }

    private void captureSnapshot(SelectionState sel, World world) {
        snapshot = new HashMap<>(sel.size());
        currentSnapshotVersion++;
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt bv = SelectionState.unpack(key);
            Block blk = WorldUtils.getBlock(world, bv);
            if (blk != null && blk != Blocks.air) {
                int meta = WorldUtils.getBlockMetadata(world, bv);
                snapshot.put(key, new SelectionState.BlockData(blk, meta));
            }
        }
    }

    private void reset() {
        delta = Vec3DFloat.ZERO;
        rot = Vec3DFloat.ZERO;
        scale = Vec3DFloat.ONE;
        viewPlaneGizmo.reset();
        planeGizmo.reset();
        scalingGizmo.reset();
        gizmo.reset();
        rotGizmo.reset();
        ghostBlocks = null;
        lastDelta = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
        lastRot = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
        lastScale = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
    }

    public boolean isAnyGizmoDragging() {
        return getAxisTranslationGizmo().isDragging()
                || getPlaneTranslationGizmo().isDragging()
                || getRotationGizmo().isDragging()
                || getScalingGizmo().isDragging();
    }
}
