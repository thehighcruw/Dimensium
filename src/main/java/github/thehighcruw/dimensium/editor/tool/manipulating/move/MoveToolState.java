/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.RotationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.ScaleGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.ViewPlaneGizmo;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.tool.ChangeProposal;

/**
 * Client-side state for the Move tool.
 * Owns its own block snapshot — never touches SelectionState.clipboard.
 */
public class MoveToolState {

    public static final MoveToolState INSTANCE = new MoveToolState();

    public boolean active = false;

    /** SelectionState.renderVersion at the time of last activation — used to detect selection changes. */
    public long capturedSelVersion = -2;

    /** Center of mass of the original selection (block centers averaged). */
    public float cmX, cmY, cmZ;

    /** Float translation delta applied by the gizmo drag. */
    public float deltaFX = 0f, deltaFY = 0f, deltaFZ = 0f;

    /** Rotation angles in degrees (X→Y→Z). */
    public float rotX = 0f, rotY = 0f, rotZ = 0f;
    /** Rotation snapshot at the start of a rotation drag. */
    public float rotDragBaseX, rotDragBaseY, rotDragBaseZ;

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
    public final PlaneTranslationGizmo planeGizmo = new PlaneTranslationGizmo();
    public final ScaleGizmo scaleGizmo = new ScaleGizmo();
    public final TranslationGizmo gizmo = new TranslationGizmo();
    public final RotationGizmo rotGizmo = new RotationGizmo();

    // Cache keys for ghost rebuild
    private float lastDFX = Float.NaN, lastDFY = Float.NaN, lastDFZ = Float.NaN;
    private float lastRX = Float.NaN, lastRY = Float.NaN, lastRZ = Float.NaN;
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
    public void activateFromSnapshot(SelectionState sel, Map<Long, SelectionState.BlockData> snap, float newCmX,
        float newCmY, float newCmZ) {
        cmX = newCmX;
        cmY = newCmY;
        cmZ = newCmZ;
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
        scaleGizmo.reset();
        gizmo.reset();
        rotGizmo.reset();
    }

    /** World-space gizmo anchor = center of mass + translation delta. */
    public double gizmoX() {
        return cmX + deltaFX;
    }

    public double gizmoY() {
        return cmY + deltaFY;
    }

    public double gizmoZ() {
        return cmZ + deltaFZ;
    }

    public void invalidateGhost() {
        lastDFX = Float.NaN;
        ghostBlocks = null;
    }

    /**
     * Recompute ghost blocks and preview when translation/rotation or snapshot changes.
     * No-op if nothing changed.
     */
    public void rebuildIfNeeded() {
        if (snapshot == null || snapshot.isEmpty()) return;
        if (lastDFX == deltaFX && lastDFY == deltaFY
            && lastDFZ == deltaFZ
            && lastRX == rotX
            && lastRY == rotY
            && lastRZ == rotZ
            && snapshotVersion == currentSnapshotVersion) return;

        lastDFX = deltaFX;
        lastDFY = deltaFY;
        lastDFZ = deltaFZ;
        lastRX = rotX;
        lastRY = rotY;
        lastRZ = rotZ;
        snapshotVersion = currentSnapshotVersion;

        float[] R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);

        List<int[]> blocks = new ArrayList<>(snapshot.size());
        for (Map.Entry<Long, SelectionState.BlockData> e : snapshot.entrySet()) {
            long k = e.getKey();
            int wx = SelectionState.unpackX(k);
            int wy = SelectionState.unpackY(k);
            int wz = SelectionState.unpackZ(k);

            float dx = wx + 0.5f - cmX;
            float dy = wy + 0.5f - cmY;
            float dz = wz + 0.5f - cmZ;

            float rx = R[0] * dx + R[1] * dy + R[2] * dz;
            float ry = R[3] * dx + R[4] * dy + R[5] * dz;
            float rz = R[6] * dx + R[7] * dy + R[8] * dz;

            int nx = (int) Math.floor(cmX + deltaFX + rx);
            int ny = (int) Math.floor(cmY + deltaFY + ry);
            int nz = (int) Math.floor(cmZ + deltaFZ + rz);

            SelectionState.BlockData bd = e.getValue();
            blocks.add(new int[] { nx, ny, nz, Block.getIdFromBlock(bd.block()), bd.meta() });
        }
        ghostBlocks = blocks;

        ChangeProposal p = ChangeProposal.forPreview();
        for (int[] b : blocks) {
            p.proposed.put(ChangeProposal.packKey(b[0], b[1], b[2]), new int[] { b[3], b[4] });
        }
        preview = p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void computeCoM(SelectionState sel) {
        double sx = 0, sy = 0, sz = 0;
        int cnt = 0;
        for (long key : sel.getSelectedBlocks()) {
            sx += SelectionState.unpackX(key) + 0.5;
            sy += SelectionState.unpackY(key) + 0.5;
            sz += SelectionState.unpackZ(key) + 0.5;
            cnt++;
        }
        cmX = (float) (sx / cnt);
        cmY = (float) (sy / cnt);
        cmZ = (float) (sz / cnt);
    }

    private void captureSnapshot(SelectionState sel, World world) {
        snapshot = new HashMap<>(sel.size());
        currentSnapshotVersion++;
        for (long key : sel.getSelectedBlocks()) {
            int bx = SelectionState.unpackX(key);
            int by = SelectionState.unpackY(key);
            int bz = SelectionState.unpackZ(key);
            Block blk = world.getBlock(bx, by, bz);
            if (blk != null && blk != Blocks.air) {
                int meta = world.getBlockMetadata(bx, by, bz);
                snapshot.put(key, new SelectionState.BlockData(blk, meta));
            }
        }
    }

    private void reset() {
        deltaFX = 0f;
        deltaFY = 0f;
        deltaFZ = 0f;
        rotX = 0f;
        rotY = 0f;
        rotZ = 0f;
        viewPlaneGizmo.reset();
        planeGizmo.reset();
        scaleGizmo.reset();
        gizmo.reset();
        rotGizmo.reset();
        ghostBlocks = null;
        lastDFX = Float.NaN;
    }
}
