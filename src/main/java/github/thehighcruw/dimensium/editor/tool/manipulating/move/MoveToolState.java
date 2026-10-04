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
import github.thehighcruw.dimensium.shared.util.BlockMetaRotator;
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
        Mat3DFloat Rinv = R.transpose();
        Vec3DFloat gizmoPos = cm.plus(delta);

        // Inverse mapping: iterate world-space AABB of the rotated bbox, back-project each cell
        // through R^-1 to find its source block. Avoids gaps from arc spacing > 1 block/step.
        float dMinX = Float.MAX_VALUE, dMinY = Float.MAX_VALUE, dMinZ = Float.MAX_VALUE;
        float dMaxX = -Float.MAX_VALUE, dMaxY = -Float.MAX_VALUE, dMaxZ = -Float.MAX_VALUE;
        for (int xi = 0; xi <= 1; xi++) {
            for (int yi = 0; yi <= 1; yi++) {
                for (int zi = 0; zi <= 1; zi++) {
                    Vec3DFloat corner = Vec3DFloat.from(xi * scaledW, yi * scaledH, zi * scaledD)
                            .minus(scaledCenter);
                    Vec3DFloat world = gizmoPos.plus(R.mul(bboxFloatCenter.plus(corner)));
                    if (world.x() < dMinX) dMinX = world.x();
                    if (world.x() > dMaxX) dMaxX = world.x();
                    if (world.y() < dMinY) dMinY = world.y();
                    if (world.y() > dMaxY) dMaxY = world.y();
                    if (world.z() < dMinZ) dMinZ = world.z();
                    if (world.z() > dMaxZ) dMaxZ = world.z();
                }
            }
        }
        int wMinX = (int) Math.floor(dMinX);
        int wMinY = Math.max(0, (int) Math.floor(dMinY));
        int wMinZ = (int) Math.floor(dMinZ);
        int wMaxX = (int) Math.ceil(dMaxX);
        int wMaxY = Math.min(256, (int) Math.ceil(dMaxY));
        int wMaxZ = (int) Math.ceil(dMaxZ);

        // Unpack matrix and vector components once to avoid per-iteration object allocation.
        float ri00 = Rinv.r00(), ri01 = Rinv.r01(), ri02 = Rinv.r02();
        float ri10 = Rinv.r10(), ri11 = Rinv.r11(), ri12 = Rinv.r12();
        float ri20 = Rinv.r20(), ri21 = Rinv.r21(), ri22 = Rinv.r22();
        float gpx = gizmoPos.x(), gpy = gizmoPos.y(), gpz = gizmoPos.z();
        float bfcx = bboxFloatCenter.x(), bfcy = bboxFloatCenter.y(), bfcz = bboxFloatCenter.z();
        float scx = scaledCenter.x(), scy = scaledCenter.y(), scz = scaledCenter.z();
        float scaleX = scale.x(), scaleY = scale.y(), scaleZ = scale.z();

        List<int[]> blocks = new ArrayList<>();
        for (int wx = wMinX; wx < wMaxX; wx++) {
            for (int wy = wMinY; wy < wMaxY; wy++) {
                for (int wz = wMinZ; wz < wMaxZ; wz++) {
                    // Back-project world cell center through R^-1 into scaled source space.
                    float ox = wx + 0.5f - gpx;
                    float oy = wy + 0.5f - gpy;
                    float oz = wz + 0.5f - gpz;
                    float lox = ri00 * ox + ri01 * oy + ri02 * oz - bfcx + scx;
                    float loy = ri10 * ox + ri11 * oy + ri12 * oz - bfcy + scy;
                    float loz = ri20 * ox + ri21 * oy + ri22 * oz - bfcz + scz;
                    int sx = (int) Math.floor(lox);
                    int sy = (int) Math.floor(loy);
                    int sz = (int) Math.floor(loz);
                    if (sx < 0 || sy < 0 || sz < 0 || sx >= scaledW || sy >= scaledH || sz >= scaledD) continue;
                    // Nearest-neighbour reverse-map into local bbox space.
                    int srcX = Math.min((int) (sx / scaleX), bboxW - 1);
                    int srcY = Math.min((int) (sy / scaleY), bboxH - 1);
                    int srcZ = Math.min((int) (sz / scaleZ), bboxD - 1);
                    SelectionState.BlockData bd =
                            localLookup.get(ChangeProposal.packKey(lMinX + srcX, lMinY + srcY, lMinZ + srcZ));
                    if (bd == null) continue;
                    int rotatedMeta = BlockMetaRotator.rotateOrKeep(bd.block(), bd.meta(), R);
                    blocks.add(Vec3DInt.from(wx, wy, wz).toBlockOp(Block.getIdFromBlock(bd.block()), rotatedMeta));
                }
            }
        }
        ghostBlocks = blocks;

        ChangeProposal p = ChangeProposal.forPreview();
        // Original positions shown as removals (air) so renderProposalPreview draws the orange erase tint.
        for (long key : snapshot.keySet()) {
            Vec3DInt src = SelectionState.unpack(key);
            p.proposed.putIfAbsent(ChangeProposal.packKey(src), new int[] {0, 0});
        }
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
