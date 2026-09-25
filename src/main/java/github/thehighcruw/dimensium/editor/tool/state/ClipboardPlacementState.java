/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.state;

import github.thehighcruw.dimensium.editor.clipboard.ClipboardBlock;
import github.thehighcruw.dimensium.editor.clipboard.ClipboardUtils;
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
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;

public class ClipboardPlacementState
        implements WithAxisTranslationGizmo, WithPlaneTranslationGizmo, WithRotationGizmo, WithScalingGizmo {

    public static final ClipboardPlacementState INSTANCE = new ClipboardPlacementState();

    public boolean active = false;
    public Vec3DInt anchor = Vec3DInt.ZERO;
    public Vec3DFloat anchorF = Vec3DFloat.ZERO;

    public Vec3DFloat rot = Vec3DFloat.ZERO;
    public Vec3DFloat rotDragBase = Vec3DFloat.ZERO;

    /** Per-axis scale factors for resampling the clipboard on commit. */
    public Vec3DFloat scale = Vec3DFloat.ONE;

    public List<ClipboardBlock> offsets = null;

    /** Discrete block preview rebuilt whenever anchor or rotation changes. */
    public ChangeProposal preview = null;

    public final ViewPlaneGizmo viewPlaneGizmo = new ViewPlaneGizmo();
    private final TranslationGizmo gizmo = new TranslationGizmo();
    private final PlaneTranslationGizmo planeGizmo = new PlaneTranslationGizmo();
    private final RotationGizmo rotGizmo = new RotationGizmo();
    private final ScalingGizmo scalingGizmo = new ScalingGizmo();

    public Vec3DInt clipDim = Vec3DInt.ZERO;

    public Vec3DDouble center() {
        return anchorF.toDouble().plus(clipDim.toDouble().times(0.5));
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

    @Override
    public ScalingGizmo getScalingGizmo() {
        return scalingGizmo;
    }

    public void start(SelectionState sel, Vec3DInt pos) {
        if (sel.clipboard == null) return;
        active = true;
        anchor = pos;
        anchorF = pos.toFloat();
        rot = Vec3DFloat.ZERO;
        scale = Vec3DFloat.ONE;
        clipDim = sel.clipDim;
        offsets = ClipboardUtils.toOffsets(sel.clipboard);
        viewPlaneGizmo.reset();
        gizmo.reset();
        planeGizmo.reset();
        rotGizmo.reset();
        scalingGizmo.reset();
        rebuildPreview();
    }

    public void cancel() {
        active = false;
        offsets = null;
        preview = null;
        scale = Vec3DFloat.ONE;
        viewPlaneGizmo.reset();
        gizmo.reset();
        planeGizmo.reset();
        rotGizmo.reset();
        scalingGizmo.reset();
    }

    /** Rebuild the discrete-position ChangeProposal from current anchor + rotation + scale. */
    public void rebuildPreview() {
        if (offsets == null) {
            preview = null;
            return;
        }

        // Build a lookup from clipboard local-space integer position to block data.
        Map<Long, int[]> clipLookup = new HashMap<>(offsets.size());
        for (ClipboardBlock o : offsets) {
            clipLookup.put(ChangeProposal.packKey(o.offset()), new int[] {o.blockId(), o.meta()});
        }

        Mat3DFloat R = rot.equals(Vec3DFloat.ZERO) ? null : ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z());
        int scaledW = Math.max(1, Math.round(clipDim.x() * scale.x()));
        int scaledH = Math.max(1, Math.round(clipDim.y() * scale.y()));
        int scaledD = Math.max(1, Math.round(clipDim.z() * scale.z()));
        Vec3DFloat scaledCenter = Vec3DFloat.from(scaledW, scaledH, scaledD).divide(2f);

        ChangeProposal p = ChangeProposal.forPreview();
        for (int sx = 0; sx < scaledW; sx++) {
            for (int sy = 0; sy < scaledH; sy++) {
                for (int sz = 0; sz < scaledD; sz++) {
                    // Nearest-neighbour reverse-map into clipboard space.
                    int cx = Math.min((int) (sx / scale.x()), clipDim.x() - 1);
                    int cy = Math.min((int) (sy / scale.y()), clipDim.y() - 1);
                    int cz = Math.min((int) (sz / scale.z()), clipDim.z() - 1);
                    int[] blockData = clipLookup.get(ChangeProposal.packKey(Vec3DInt.from(cx, cy, cz)));
                    if (blockData == null) continue;

                    Vec3DInt world;
                    int[] placedData;
                    if (R == null) {
                        world = anchor.plus(Vec3DInt.from(sx, sy, sz));
                        placedData = blockData;
                    } else {
                        Vec3DFloat local =
                                Vec3DFloat.from(sx + 0.5f, sy + 0.5f, sz + 0.5f).minus(scaledCenter);
                        world = anchor.plus(R.mul(local).plus(scaledCenter).floor());
                        Block blk = Block.getBlockById(blockData[0]);
                        int rotatedMeta = blk == null ? blockData[1] : BlockMetaRotator.rotate(blk, blockData[1], R);
                        placedData = rotatedMeta == blockData[1] ? blockData : new int[] {blockData[0], rotatedMeta};
                    }
                    p.proposed.put(ChangeProposal.packKey(world), placedData);
                }
            }
        }
        preview = p;
    }

    /** Returns ops ready for BlockSender.sendChunked, with rotation and scale applied. */
    public List<int[]> toOps() {
        if (preview != null) {
            return ChangeProposal.mapToOps(preview.proposed);
        }
        // Fallback: no scale/rotation, direct placement.
        List<int[]> ops = new ArrayList<>(offsets.size());
        for (ClipboardBlock o : offsets) {
            Vec3DInt world = anchor.plus(o.offset());
            ops.add(world.toBlockOp(o.blockId(), o.meta()));
        }
        return ops;
    }

    public boolean isAnyGizmoDragging() {
        return getAxisTranslationGizmo().isDragging()
                || getPlaneTranslationGizmo().isDragging()
                || getRotationGizmo().isDragging()
                || getScalingGizmo().isDragging();
    }
}
