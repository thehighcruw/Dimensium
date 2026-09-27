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
        if (R == null) {
            // No rotation: direct source iteration is gap-free.
            for (int sx = 0; sx < scaledW; sx++) {
                for (int sy = 0; sy < scaledH; sy++) {
                    for (int sz = 0; sz < scaledD; sz++) {
                        int cx = Math.min((int) (sx / scale.x()), clipDim.x() - 1);
                        int cy = Math.min((int) (sy / scale.y()), clipDim.y() - 1);
                        int cz = Math.min((int) (sz / scale.z()), clipDim.z() - 1);
                        int[] blockData = clipLookup.get(ChangeProposal.packKey(cx, cy, cz));
                        if (blockData == null) continue;
                        p.proposed.put(ChangeProposal.packKey(anchor.plus(Vec3DInt.from(sx, sy, sz))), blockData);
                    }
                }
            }
        } else {
            // Rotation active: use inverse mapping to avoid gaps caused by rotation moving
            // adjacent source cells more than one block apart in world space.
            Mat3DFloat Rinv = R.transpose();
            float dMinX = Float.MAX_VALUE, dMinY = Float.MAX_VALUE, dMinZ = Float.MAX_VALUE;
            float dMaxX = -Float.MAX_VALUE, dMaxY = -Float.MAX_VALUE, dMaxZ = -Float.MAX_VALUE;
            for (int xi = 0; xi <= 1; xi++) {
                for (int yi = 0; yi <= 1; yi++) {
                    for (int zi = 0; zi <= 1; zi++) {
                        Vec3DFloat corner = Vec3DFloat.from(xi * scaledW, yi * scaledH, zi * scaledD)
                                .minus(scaledCenter);
                        Vec3DFloat world = anchor.toFloat().plus(R.mul(corner).plus(scaledCenter));
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
            int wMinY = (int) Math.floor(dMinY);
            int wMinZ = (int) Math.floor(dMinZ);
            int wMaxX = (int) Math.ceil(dMaxX);
            int wMaxY = (int) Math.ceil(dMaxY);
            int wMaxZ = (int) Math.ceil(dMaxZ);

            // Unpack matrix and vector components once to avoid per-iteration object allocation.
            float ri00 = Rinv.r00(), ri01 = Rinv.r01(), ri02 = Rinv.r02();
            float ri10 = Rinv.r10(), ri11 = Rinv.r11(), ri12 = Rinv.r12();
            float ri20 = Rinv.r20(), ri21 = Rinv.r21(), ri22 = Rinv.r22();
            // worldCenter - anchor - scaledCenter, hoisted outside the loop
            float baseX = anchor.x() + scaledCenter.x();
            float baseY = anchor.y() + scaledCenter.y();
            float baseZ = anchor.z() + scaledCenter.z();
            float scx = scaledCenter.x(), scy = scaledCenter.y(), scz = scaledCenter.z();
            float scaleX = scale.x(), scaleY = scale.y(), scaleZ = scale.z();
            int clipDimX = clipDim.x(), clipDimY = clipDim.y(), clipDimZ = clipDim.z();

            for (int wx = wMinX; wx < wMaxX; wx++) {
                for (int wy = wMinY; wy < wMaxY; wy++) {
                    for (int wz = wMinZ; wz < wMaxZ; wz++) {
                        // Back-project: Rinv * (worldCenter - anchor - scaledCenter) + scaledCenter
                        float ox = wx + 0.5f - baseX;
                        float oy = wy + 0.5f - baseY;
                        float oz = wz + 0.5f - baseZ;
                        float lox = ri00 * ox + ri01 * oy + ri02 * oz + scx;
                        float loy = ri10 * ox + ri11 * oy + ri12 * oz + scy;
                        float loz = ri20 * ox + ri21 * oy + ri22 * oz + scz;
                        int sx = (int) Math.floor(lox);
                        int sy = (int) Math.floor(loy);
                        int sz = (int) Math.floor(loz);
                        if (sx < 0 || sy < 0 || sz < 0 || sx >= scaledW || sy >= scaledH || sz >= scaledD) continue;
                        int cx = Math.min((int) (sx / scaleX), clipDimX - 1);
                        int cy = Math.min((int) (sy / scaleY), clipDimY - 1);
                        int cz = Math.min((int) (sz / scaleZ), clipDimZ - 1);
                        int[] blockData = clipLookup.get(ChangeProposal.packKey(cx, cy, cz));
                        if (blockData == null) continue;
                        Block blk = Block.getBlockById(blockData[0]);
                        int rotatedMeta = BlockMetaRotator.rotateOrKeep(blk, blockData[1], R);
                        int[] placedData =
                                rotatedMeta == blockData[1] ? blockData : new int[] {blockData[0], rotatedMeta};
                        p.proposed.put(ChangeProposal.packKey(Vec3DInt.from(wx, wy, wz)), placedData);
                    }
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
