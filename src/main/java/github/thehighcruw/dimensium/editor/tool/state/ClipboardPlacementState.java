/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.state;

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
import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public Vec3DInt clipDim = Vec3DInt.ZERO;

    public double centerX() {
        return anchorF.x() + clipDim.x() / 2.0;
    }

    public double centerY() {
        return anchorF.y() + clipDim.y() / 2.0;
    }

    public double centerZ() {
        return anchorF.z() + clipDim.z() / 2.0;
    }

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

    public void start(SelectionState sel, Vec3DInt pos) {
        if (sel.clipboard == null) return;
        active = true;
        anchor = pos;
        anchorF = pos.toFloat();
        rot = Vec3DFloat.ZERO;
        clipDim = sel.clipDim;
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
                p.proposed.put(key, new int[] {o[3], o[4]});
            }
        } else {
            Mat3DFloat R = ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z());
            Vec3DFloat center = clipDim.toFloat().divide(2f);
            for (int[] o : offsets) {
                Vec3DFloat local = Vec3DFloat.from(o[0], o[1], o[2]).plus(0.5f).minus(center);
                Vec3DInt world = anchor.plus(R.mul(local).plus(center).floor());
                long key = ChangeProposal.packKey(world.x(), world.y(), world.z());
                p.proposed.put(key, new int[] {o[3], o[4]});
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
                ops.add(new int[] {
                    ChangeProposal.unpackX(key), ChangeProposal.unpackY(key), ChangeProposal.unpackZ(key), bm[0], bm[1]
                });
            }
            return ops;
        }
        List<int[]> ops = new ArrayList<>(offsets.size());
        for (int[] o : offsets) {
            ops.add(new int[] {anchor.x() + o[0], anchor.y() + o[1], anchor.z() + o[2], o[3], o[4]});
        }
        return ops;
    }

    public boolean isAnyGizmoDragging() {
        return getAxisTranslationGizmo().isDragging()
                || getPlaneTranslationGizmo().isDragging()
                || getRotationGizmo().isDragging();
    }
}
