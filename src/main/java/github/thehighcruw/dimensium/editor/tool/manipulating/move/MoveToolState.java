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
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
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
    public void activateFromSnapshot(
            SelectionState sel, Map<Long, SelectionState.BlockData> snap, float newCmX, float newCmY, float newCmZ) {
        cm = Vec3DFloat.from(newCmX, newCmY, newCmZ);
        snapshot = snap;
        currentSnapshotVersion++;
        reset();
        capturedSelVersion = sel.renderVersion;
        active = true;
    }

    public void cancel() {
        active = false;
        preview = null;
        viewPlaneGizmo.reset();
        planeGizmo.reset();
        scalingGizmo.reset();
        gizmo.reset();
        rotGizmo.reset();
    }

    /** World-space gizmo anchor = center of mass + translation delta. */
    public double gizmoX() {
        return cm.x() + delta.x();
    }

    public double gizmoY() {
        return cm.y() + delta.y();
    }

    public double gizmoZ() {
        return cm.z() + delta.z();
    }

    public void invalidateGhost() {
        lastDelta = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
        ghostBlocks = null;
    }

    /**
     * Recompute ghost blocks and preview when translation/rotation or snapshot changes.
     * No-op if nothing changed.
     */
    public void rebuildIfNeeded() {
        if (snapshot == null || snapshot.isEmpty()) return;
        if (lastDelta.equals(delta) && lastRot.equals(rot) && snapshotVersion == currentSnapshotVersion) return;

        lastDelta = delta;
        lastRot = rot;
        snapshotVersion = currentSnapshotVersion;

        Mat3DFloat R = ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z());

        List<int[]> blocks = new ArrayList<>(snapshot.size());
        for (Map.Entry<Long, SelectionState.BlockData> e : snapshot.entrySet()) {
            long k = e.getKey();
            Vec3DInt wv = SelectionState.unpack(k);
            int wx = wv.x(), wy = wv.y(), wz = wv.z();

            float dx = wx + 0.5f - cm.x();
            float dy = wy + 0.5f - cm.y();
            float dz = wz + 0.5f - cm.z();

            Vec3DFloat rv = R.mul(Vec3DFloat.from(dx, dy, dz));
            float rx = rv.x(), ry = rv.y(), rz = rv.z();

            int nx = (int) Math.floor(cm.x() + delta.x() + rx);
            int ny = (int) Math.floor(cm.y() + delta.y() + ry);
            int nz = (int) Math.floor(cm.z() + delta.z() + rz);

            SelectionState.BlockData bd = e.getValue();
            blocks.add(new int[] {nx, ny, nz, Block.getIdFromBlock(bd.block()), bd.meta()});
        }
        ghostBlocks = blocks;

        ChangeProposal p = ChangeProposal.forPreview();
        for (int[] b : blocks) {
            p.proposed.put(ChangeProposal.packKey(b[0], b[1], b[2]), new int[] {b[3], b[4]});
        }
        preview = p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void computeCoM(SelectionState sel) {
        double sx = 0, sy = 0, sz = 0;
        int cnt = 0;
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt cv = SelectionState.unpack(key);
            sx += cv.x() + 0.5;
            sy += cv.y() + 0.5;
            sz += cv.z() + 0.5;
            cnt++;
        }
        cm = Vec3DFloat.from((float) (sx / cnt), (float) (sy / cnt), (float) (sz / cnt));
    }

    private void captureSnapshot(SelectionState sel, World world) {
        snapshot = new HashMap<>(sel.size());
        currentSnapshotVersion++;
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt bv = SelectionState.unpack(key);
            int bx = bv.x(), by = bv.y(), bz = bv.z();
            Block blk = world.getBlock(bx, by, bz);
            if (blk != null && blk != Blocks.air) {
                int meta = world.getBlockMetadata(bx, by, bz);
                snapshot.put(key, new SelectionState.BlockData(blk, meta));
            }
        }
    }

    private void reset() {
        delta = Vec3DFloat.ZERO;
        rot = Vec3DFloat.ZERO;
        viewPlaneGizmo.reset();
        planeGizmo.reset();
        scalingGizmo.reset();
        gizmo.reset();
        rotGizmo.reset();
        ghostBlocks = null;
        lastDelta = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
        lastRot = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
    }

    public boolean isAnyGizmoDragging() {
        return getAxisTranslationGizmo().isDragging()
                || getPlaneTranslationGizmo().isDragging()
                || getRotationGizmo().isDragging();
    }
}
