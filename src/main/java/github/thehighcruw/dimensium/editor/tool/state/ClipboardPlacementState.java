/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import github.thehighcruw.dimensium.editor.clipboard.ClipboardUtils;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithAxisTranslationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithPlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithRotationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.RotationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.ViewPlaneGizmo;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.Vec3DFloat;
import github.thehighcruw.dimensium.shared.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;

public class ClipboardPlacementState implements WithAxisTranslationGizmo, WithPlaneTranslationGizmo, WithRotationGizmo {

    public static final ClipboardPlacementState INSTANCE = new ClipboardPlacementState();

    public boolean active = false;
    public Vec3DInt anchor = Vec3DInt.ZERO;
    public Vec3DFloat anchorF = Vec3DFloat.ZERO;

    public Vec3DFloat rot = Vec3DFloat.ZERO;
    public Vec3DFloat rotDragBase = Vec3DFloat.ZERO;

    /** Clipboard local offsets as int[]{lx, ly, lz, blockId, meta}. */
    public List<int[]> offsets = null;

    /** Discrete block preview rebuilt whenever anchor or rotation changes. */
    public ChangeProposal preview = null;

    public final ViewPlaneGizmo viewPlaneGizmo = new ViewPlaneGizmo();
    private final TranslationGizmo gizmo = new TranslationGizmo();
    private final PlaneTranslationGizmo planeGizmo = new PlaneTranslationGizmo();
    private final RotationGizmo rotGizmo = new RotationGizmo();

    public double centerX() {
        return anchorF.x() + clipW / 2.0;
    }

    public double centerY() {
        return anchorF.y() + clipH / 2.0;
    }

    public double centerZ() {
        return anchorF.z() + clipD / 2.0;
    }

    public int clipW, clipH, clipD;

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

    public void start(SelectionState sel, int x, int y, int z) {
        if (sel.clipboard == null) return;
        active = true;
        anchor = Vec3DInt.from(x, y, z);
        anchorF = Vec3DFloat.from(x, y, z);
        rot = Vec3DFloat.ZERO;
        clipW = sel.clipW;
        clipH = sel.clipH;
        clipD = sel.clipD;
        offsets = ClipboardUtils.toOffsets(sel.clipboard);
        viewPlaneGizmo.reset();
        gizmo.reset();
        planeGizmo.reset();
        rotGizmo.reset();
        rebuildPreview();
    }

    public void cancel() {
        active = false;
        offsets = null;
        preview = null;
        viewPlaneGizmo.reset();
        gizmo.reset();
        planeGizmo.reset();
        rotGizmo.reset();
    }

    /** Rebuild the discrete-position ChangeProposal from current anchor + rotation. */
    public void rebuildPreview() {
        if (offsets == null) {
            preview = null;
            return;
        }
        ChangeProposal p = ChangeProposal.forPreview();
        if (rot.equals(Vec3DFloat.ZERO)) {
            for (int[] o : offsets) {
                long key = ChangeProposal.packKey(anchor.x() + o[0], anchor.y() + o[1], anchor.z() + o[2]);
                p.proposed.put(key, new int[] { o[3], o[4] });
            }
        } else {
            float[] R = ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z());
            float cx = clipW / 2f, cy = clipH / 2f, cz = clipD / 2f;
            for (int[] o : offsets) {
                float dx = o[0] + 0.5f - cx, dy = o[1] + 0.5f - cy, dz = o[2] + 0.5f - cz;
                float wx = R[0] * dx + R[1] * dy + R[2] * dz + cx;
                float wy = R[3] * dx + R[4] * dy + R[5] * dz + cy;
                float wz = R[6] * dx + R[7] * dy + R[8] * dz + cz;
                long key = ChangeProposal.packKey(
                    anchor.x() + (int) Math.floor(wx),
                    anchor.y() + (int) Math.floor(wy),
                    anchor.z() + (int) Math.floor(wz));
                p.proposed.put(key, new int[] { o[3], o[4] });
            }
        }
        preview = p;
    }

    /** Returns ops ready for BlockSender.sendChunked, with rotation applied. */
    public List<int[]> toOps() {
        if (preview != null) {
            List<int[]> ops = new ArrayList<>(preview.proposed.size());
            for (Map.Entry<Long, int[]> e : preview.proposed.entrySet()) {
                long key = e.getKey();
                int[] bm = e.getValue();
                ops.add(
                    new int[] { ChangeProposal.unpackX(key), ChangeProposal.unpackY(key), ChangeProposal.unpackZ(key),
                        bm[0], bm[1] });
            }
            return ops;
        }
        List<int[]> ops = new ArrayList<>(offsets.size());
        for (int[] o : offsets) {
            ops.add(new int[] { anchor.x() + o[0], anchor.y() + o[1], anchor.z() + o[2], o[3], o[4] });
        }
        return ops;
    }

    public boolean isAnyGizmoDragging() {
        return getAxisTranslationGizmo().isDragging() || getPlaneTranslationGizmo().isDragging()
            || getRotationGizmo().isDragging();
    }
}
