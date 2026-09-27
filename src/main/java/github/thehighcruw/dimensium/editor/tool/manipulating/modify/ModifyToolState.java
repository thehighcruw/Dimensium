/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.modify;

import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithAxisTranslationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithPlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.BlockMetaRotator;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.world.World;

public class ModifyToolState implements WithAxisTranslationGizmo, WithPlaneTranslationGizmo {

    public static final ModifyToolState INSTANCE = new ModifyToolState();

    public enum ModifyMode {
        TRANSLATE_COPIES("dimensium.modify_mode.translate_copies"),
        REVOLVE("dimensium.modify_mode.revolve"),
        TWIST("dimensium.modify_mode.twist");

        public final String label;

        ModifyMode(String label) {
            this.label = label;
        }
    }

    public enum OffsetType {
        RELATIVE("dimensium.modify.offset_type.relative"),
        ABSOLUTE("dimensium.modify.offset_type.absolute");

        public final String label;

        OffsetType(String label) {
            this.label = label;
        }
    }

    public enum Axis {
        X("dimensium.modify.axis.x"),
        Y("dimensium.modify.axis.y"),
        Z("dimensium.modify.axis.z");

        public final String label;

        Axis(String label) {
            this.label = label;
        }
    }

    public static final float TWIST_ANGLE_MIN = -360f;
    public static final float TWIST_ANGLE_MAX = 360f;

    public static final int COUNT_MIN = 1;
    public static final int COUNT_MAX = 64;
    public static final float REVOLVE_ANGLE_MIN = 1f;
    public static final float REVOLVE_ANGLE_MAX = 360f;

    public ModifyMode mode = ModifyMode.TRANSLATE_COPIES;

    // Translate Copies params
    public Vec3DFloat translateCopiesOffset = Vec3DFloat.ZERO;
    public int translateCopiesCount = 1;
    public OffsetType translateCopiesOffsetType = OffsetType.RELATIVE;

    // Twist params
    public float twistAngleXDegrees = 0f;
    public float twistAngleYDegrees = 0f;
    public float twistAngleZDegrees = 0f;

    // Revolve params
    public Axis revolveAxis = Axis.Y;
    public float revolveAngleDegrees = 360f;
    public int revolveCount = 4;
    public boolean revolveHasTranslation = false;
    public Vec3DFloat revolveTranslation = Vec3DFloat.ZERO;

    /**
     * User-placed revolve center, null until right-clicked in the world.
     * Moved via translation gizmos after placement.
     */
    public Vec3DInt revolveCenter = null;

    // Revolve visual state — computed during preview rebuild for use by renderer
    public double revolveInnerRadius = 0;
    public double revolveOuterRadius = 0;

    // Gizmos for manipulating the revolve center point
    private final TranslationGizmo axisGizmo = new TranslationGizmo();
    private final PlaneTranslationGizmo planeGizmo = new PlaneTranslationGizmo();

    // Preview state
    public ChangeProposal preview = null;

    /** Flattened copy placements: [x, y, z, blockId, meta]. Matches preview; sent on confirm. */
    public List<int[]> ghostBlocks = null;

    // Staleness keys
    private long lastSelVersion = Long.MIN_VALUE;
    private int lastClipboardVersion = -1;
    private ModifyMode lastMode = null;
    // Translate Copies staleness
    private Vec3DFloat lastOffset = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
    private int lastCount = -1;
    private OffsetType lastOffsetType = null;
    // Twist staleness
    private float lastTwistAngleX = Float.NaN;
    private float lastTwistAngleY = Float.NaN;
    private float lastTwistAngleZ = Float.NaN;

    // Revolve staleness
    private Axis lastRevolveAxis = null;
    private float lastRevolveAngle = Float.NaN;
    private int lastRevolveCount = -1;
    private boolean lastRevolveHasTranslation = false;
    private Vec3DFloat lastRevolveTranslation = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
    private Vec3DInt lastRevolveCenter = null;

    private ModifyToolState() {}

    @Override
    public TranslationGizmo getAxisTranslationGizmo() {
        return axisGizmo;
    }

    @Override
    public PlaneTranslationGizmo getPlaneTranslationGizmo() {
        return planeGizmo;
    }

    /** Full reset — called when the tool is deactivated. Forces re-capture on next activation. */
    public void cancel() {
        preview = null;
        ghostBlocks = null;
        revolveCenter = null;
        axisGizmo.reset();
        planeGizmo.reset();
        lastSelVersion = Long.MIN_VALUE;
    }

    /** Clears the rendered preview after a confirm without invalidating the capture cache. */
    public void clearAfterConfirm() {
        preview = null;
        ghostBlocks = null;
    }

    /** Forces the preview to rebuild next frame, e.g. after revolveCenter is moved. */
    public void invalidatePreview() {
        lastRevolveCenter = null;
        preview = null;
        ghostBlocks = null;
    }

    /**
     * Called each frame from SelectionRenderer when Tool.MODIFY is active.
     * Snapshots the selection from world if stale, then rebuilds preview.
     */
    public void rebuildIfNeeded(SelectionState sel, World world) {
        if (!sel.hasSelection()) {
            cancel();
            return;
        }

        boolean selChanged = sel.renderVersion != lastSelVersion;
        if (selChanged) {
            sel.captureFromWorld(world);
            lastSelVersion = sel.renderVersion;
            lastClipboardVersion = sel.clipboardVersion;
            invalidate();
        }

        if (sel.clipboard == null || sel.clipboard.isEmpty()) {
            cancel();
            return;
        }

        boolean paramsChanged;
        if (mode == ModifyMode.REVOLVE) {
            paramsChanged = mode != lastMode
                    || revolveAxis != lastRevolveAxis
                    || revolveAngleDegrees != lastRevolveAngle
                    || revolveCount != lastRevolveCount
                    || revolveHasTranslation != lastRevolveHasTranslation
                    || !revolveTranslation.equals(lastRevolveTranslation)
                    || !Objects.equals(revolveCenter, lastRevolveCenter)
                    || sel.clipboardVersion != lastClipboardVersion;
        } else if (mode == ModifyMode.TWIST) {
            paramsChanged = mode != lastMode
                    || twistAngleXDegrees != lastTwistAngleX
                    || twistAngleYDegrees != lastTwistAngleY
                    || twistAngleZDegrees != lastTwistAngleZ
                    || sel.clipboardVersion != lastClipboardVersion;
        } else {
            paramsChanged = mode != lastMode
                    || !translateCopiesOffset.equals(lastOffset)
                    || translateCopiesCount != lastCount
                    || translateCopiesOffsetType != lastOffsetType
                    || sel.clipboardVersion != lastClipboardVersion;
        }

        if (!paramsChanged) return;

        lastMode = mode;
        lastClipboardVersion = sel.clipboardVersion;

        if (mode == ModifyMode.REVOLVE) {
            lastRevolveAxis = revolveAxis;
            lastRevolveAngle = revolveAngleDegrees;
            lastRevolveCount = revolveCount;
            lastRevolveHasTranslation = revolveHasTranslation;
            lastRevolveTranslation = revolveTranslation;
            lastRevolveCenter = revolveCenter;
            rebuildRevolvePreview(sel);
        } else if (mode == ModifyMode.TWIST) {
            lastTwistAngleX = twistAngleXDegrees;
            lastTwistAngleY = twistAngleYDegrees;
            lastTwistAngleZ = twistAngleZDegrees;
            rebuildTwistPreview(sel);
        } else {
            lastOffset = translateCopiesOffset;
            lastCount = translateCopiesCount;
            lastOffsetType = translateCopiesOffsetType;
            rebuildPreview(sel);
        }
    }

    private void rebuildPreview(SelectionState sel) {
        Vec3DInt step = computeStep(sel);
        Vec3DInt origin = sel.min();
        int copies = translateCopiesCount;

        List<int[]> blocks = new ArrayList<>(copies * sel.clipboard.size());
        for (int i = 1; i <= copies; i++) {
            Vec3DInt copyOrigin = origin.plus(step.times(i));
            for (Map.Entry<Long, SelectionState.BlockData> entry : sel.clipboard.entrySet()) {
                Vec3DInt dest = copyOrigin.plus(SelectionState.decodeClipboardKey(entry.getKey()));
                SelectionState.BlockData bd = entry.getValue();
                blocks.add(dest.toBlockOp(Block.getIdFromBlock(bd.block()), bd.meta()));
            }
        }
        ghostBlocks = blocks;

        ChangeProposal proposal = ChangeProposal.forPreview();
        for (int[] block : blocks) {
            proposal.proposed.put(
                    ChangeProposal.packKey(Vec3DInt.from(block[0], block[1], block[2])),
                    new int[] {block[3], block[4]});
        }
        preview = proposal;
    }

    private Vec3DInt computeStep(SelectionState sel) {
        Vec3DFloat rawOffset = translateCopiesOffset;
        if (translateCopiesOffsetType == OffsetType.RELATIVE) {
            return Vec3DInt.from(
                    Math.round(rawOffset.x() * sel.width()),
                    Math.round(rawOffset.y() * sel.height()),
                    Math.round(rawOffset.z() * sel.depth()));
        }
        return Vec3DInt.from(Math.round(rawOffset.x()), Math.round(rawOffset.y()), Math.round(rawOffset.z()));
    }

    private void rebuildRevolvePreview(SelectionState sel) {
        if (revolveCenter == null) {
            preview = null;
            ghostBlocks = null;
            revolveInnerRadius = 0;
            revolveOuterRadius = 0;
            return;
        }

        double cx = revolveCenter.x() + 0.5;
        double cy = revolveCenter.y() + 0.5;
        double cz = revolveCenter.z() + 0.5;

        // Compute inner (nearest) and outer (farthest) radii from center to selection AABB corners
        Vec3DInt selMin = sel.min();
        int srcMaxX = selMin.x() + sel.width();
        int srcMaxY = selMin.y() + sel.height();
        int srcMaxZ = selMin.z() + sel.depth();
        revolveInnerRadius = Double.MAX_VALUE;
        revolveOuterRadius = 0;
        for (int xi = 0; xi <= 1; xi++) {
            for (int yi = 0; yi <= 1; yi++) {
                for (int zi = 0; zi <= 1; zi++) {
                    double cornerX = xi == 0 ? selMin.x() : srcMaxX;
                    double cornerY = yi == 0 ? selMin.y() : srcMaxY;
                    double cornerZ = zi == 0 ? selMin.z() : srcMaxZ;
                    double dist;
                    switch (revolveAxis) {
                        case X:
                            dist = Math.sqrt((cornerY - cy) * (cornerY - cy) + (cornerZ - cz) * (cornerZ - cz));
                            break;
                        case Z:
                            dist = Math.sqrt((cornerX - cx) * (cornerX - cx) + (cornerY - cy) * (cornerY - cy));
                            break;
                        default: // Y
                            dist = Math.sqrt((cornerX - cx) * (cornerX - cx) + (cornerZ - cz) * (cornerZ - cz));
                            break;
                    }
                    if (dist < revolveInnerRadius) revolveInnerRadius = dist;
                    if (dist > revolveOuterRadius) revolveOuterRadius = dist;
                }
            }
        }
        if (revolveInnerRadius == Double.MAX_VALUE) revolveInnerRadius = 0;

        int copies = revolveCount;
        double totalAngleRad = Math.toRadians(revolveAngleDegrees);

        // Inverse mapping: for each destination position, rotate back to find the source block.
        // Forward mapping produces gaps when the arc spacing between adjacent source blocks
        // exceeds one block at the given radius.
        List<int[]> blocks = new ArrayList<>();
        for (int copyIndex = 1; copyIndex <= copies; copyIndex++) {
            double theta = totalAngleRad * copyIndex / copies;
            double cosTheta = Math.cos(theta);
            double sinTheta = Math.sin(theta);
            // Inverse rotation: cos(-θ)=cosθ, sin(-θ)=-sinθ
            double cosInv = cosTheta;
            double sinInv = -sinTheta;
            float thetaDeg = (float) Math.toDegrees(theta);
            Mat3DFloat metaRotation =
                    switch (revolveAxis) {
                        case X -> ShapeMath.buildRotationMatrix(-thetaDeg, 0f, 0f);
                        case Z -> ShapeMath.buildRotationMatrix(0f, 0f, -thetaDeg);
                        default -> ShapeMath.buildRotationMatrix(0f, -thetaDeg, 0f);
                    };

            float translationScale = revolveHasTranslation ? (float) copyIndex / copies : 0f;
            float translationX = revolveTranslation.x() * translationScale;
            float translationY = revolveTranslation.y() * translationScale;
            float translationZ = revolveTranslation.z() * translationScale;

            // Compute the destination AABB by rotating all 8 source corners forward.
            double dMinX = Double.MAX_VALUE, dMinY = Double.MAX_VALUE, dMinZ = Double.MAX_VALUE;
            double dMaxX = -Double.MAX_VALUE, dMaxY = -Double.MAX_VALUE, dMaxZ = -Double.MAX_VALUE;
            for (int xi = 0; xi <= 1; xi++) {
                for (int yi = 0; yi <= 1; yi++) {
                    for (int zi = 0; zi <= 1; zi++) {
                        double px = xi == 0 ? selMin.x() : srcMaxX;
                        double py = yi == 0 ? selMin.y() : srcMaxY;
                        double pz = zi == 0 ? selMin.z() : srcMaxZ;
                        double rx, ry, rz;
                        switch (revolveAxis) {
                            case X: {
                                double dy = py - cy, dz = pz - cz;
                                rx = px;
                                ry = cy + dy * cosTheta - dz * sinTheta;
                                rz = cz + dy * sinTheta + dz * cosTheta;
                                break;
                            }
                            case Z: {
                                double dx = px - cx, dy = py - cy;
                                rx = cx + dx * cosTheta - dy * sinTheta;
                                ry = cy + dx * sinTheta + dy * cosTheta;
                                rz = pz;
                                break;
                            }
                            default: {
                                double dx = px - cx, dz = pz - cz;
                                rx = cx + dx * cosTheta - dz * sinTheta;
                                ry = py;
                                rz = cz + dx * sinTheta + dz * cosTheta;
                                break;
                            }
                        }
                        rx += translationX;
                        ry += translationY;
                        rz += translationZ;
                        if (rx < dMinX) dMinX = rx;
                        if (rx > dMaxX) dMaxX = rx;
                        if (ry < dMinY) dMinY = ry;
                        if (ry > dMaxY) dMaxY = ry;
                        if (rz < dMinZ) dMinZ = rz;
                        if (rz > dMaxZ) dMaxZ = rz;
                    }
                }
            }

            int destMinX = (int) Math.floor(dMinX);
            int destMinY = (int) Math.floor(dMinY);
            int destMinZ = (int) Math.floor(dMinZ);
            int destMaxX = (int) Math.ceil(dMaxX);
            int destMaxY = (int) Math.ceil(dMaxY);
            int destMaxZ = (int) Math.ceil(dMaxZ);

            for (int dx = destMinX; dx < destMaxX; dx++) {
                for (int dy = destMinY; dy < destMaxY; dy++) {
                    for (int dz = destMinZ; dz < destMaxZ; dz++) {
                        // Undo translation, then rotate back by -theta to find source center
                        double px = dx + 0.5 - translationX;
                        double py = dy + 0.5 - translationY;
                        double pz = dz + 0.5 - translationZ;
                        double srcX, srcY, srcZ;
                        switch (revolveAxis) {
                            case X: {
                                double dy2 = py - cy, dz2 = pz - cz;
                                srcX = px;
                                srcY = cy + dy2 * cosInv - dz2 * sinInv;
                                srcZ = cz + dy2 * sinInv + dz2 * cosInv;
                                break;
                            }
                            case Z: {
                                double dx2 = px - cx, dy2 = py - cy;
                                srcX = cx + dx2 * cosInv - dy2 * sinInv;
                                srcY = cy + dx2 * sinInv + dy2 * cosInv;
                                srcZ = pz;
                                break;
                            }
                            default: {
                                double dx2 = px - cx, dz2 = pz - cz;
                                srcX = cx + dx2 * cosInv - dz2 * sinInv;
                                srcY = py;
                                srcZ = cz + dx2 * sinInv + dz2 * cosInv;
                                break;
                            }
                        }
                        // Block center is at (blockX+0.5, ...), so block = round(src - 0.5)
                        int srcBlockX = (int) Math.round(srcX - 0.5);
                        int srcBlockY = (int) Math.round(srcY - 0.5);
                        int srcBlockZ = (int) Math.round(srcZ - 0.5);
                        SelectionState.BlockData bd = sel.clipboardGet(
                                Vec3DInt.from(srcBlockX - selMin.x(), srcBlockY - selMin.y(), srcBlockZ - selMin.z()));
                        if (bd == SelectionState.BlockData.AIR) continue;
                        int rotatedMeta = BlockMetaRotator.rotateOrKeep(bd.block(), bd.meta(), metaRotation);
                        blocks.add(new int[] {dx, dy, dz, Block.getIdFromBlock(bd.block()), rotatedMeta});
                    }
                }
            }
        }
        ghostBlocks = blocks;

        ChangeProposal proposal = ChangeProposal.forPreview();
        for (int[] block : blocks) {
            proposal.proposed.put(
                    ChangeProposal.packKey(Vec3DInt.from(block[0], block[1], block[2])),
                    new int[] {block[3], block[4]});
        }
        preview = proposal;
    }

    private void rebuildTwistPreview(SelectionState sel) {
        Vec3DInt selMin = sel.min();
        int width = sel.width();
        int height = sel.height();
        int depth = sel.depth();

        double centerX = selMin.x() + width * 0.5;
        double centerY = selMin.y() + height * 0.5;
        double centerZ = selMin.z() + depth * 0.5;

        // Inverse mapping: iterate the destination AABB and back-project to source.
        // Forward mapping produces gaps because adjacent source blocks at the same layer
        // can rotate to non-adjacent destination positions.
        // Destination AABB is larger than the source AABB when corners rotate outward —
        // compute it by forward-rotating all 8 source corners at the maximum twist angles
        // (which occur at the selection boundary where t=1).
        Mat3DFloat maxRotation =
                ShapeMath.buildRotationMatrix(twistAngleXDegrees, twistAngleYDegrees, twistAngleZDegrees);
        double dMinX = Double.MAX_VALUE, dMinY = Double.MAX_VALUE, dMinZ = Double.MAX_VALUE;
        double dMaxX = -Double.MAX_VALUE, dMaxY = -Double.MAX_VALUE, dMaxZ = -Double.MAX_VALUE;
        for (int xi = 0; xi <= 1; xi++) {
            for (int yi = 0; yi <= 1; yi++) {
                for (int zi = 0; zi <= 1; zi++) {
                    double px = (xi == 0 ? selMin.x() : selMin.x() + width) + 0.5 - centerX;
                    double py = (yi == 0 ? selMin.y() : selMin.y() + height) + 0.5 - centerY;
                    double pz = (zi == 0 ? selMin.z() : selMin.z() + depth) + 0.5 - centerZ;
                    Vec3DFloat rotated = maxRotation.mul(Vec3DFloat.from((float) px, (float) py, (float) pz));
                    double rx = centerX + rotated.x();
                    double ry = centerY + rotated.y();
                    double rz = centerZ + rotated.z();
                    if (rx < dMinX) dMinX = rx;
                    if (rx > dMaxX) dMaxX = rx;
                    if (ry < dMinY) dMinY = ry;
                    if (ry > dMaxY) dMaxY = ry;
                    if (rz < dMinZ) dMinZ = rz;
                    if (rz > dMaxZ) dMaxZ = rz;
                }
            }
        }
        // Also include the unrotated (t=0) source AABB — rotation at t=0 is identity
        if (selMin.x() < dMinX) dMinX = selMin.x();
        if (selMin.x() + width > dMaxX) dMaxX = selMin.x() + width;
        if (selMin.y() < dMinY) dMinY = selMin.y();
        if (selMin.y() + height > dMaxY) dMaxY = selMin.y() + height;
        if (selMin.z() < dMinZ) dMinZ = selMin.z();
        if (selMin.z() + depth > dMaxZ) dMaxZ = selMin.z() + depth;
        int destMinX = (int) Math.floor(dMinX);
        int destMinY = (int) Math.floor(dMinY);
        int destMinZ = (int) Math.floor(dMinZ);
        int destMaxX = (int) Math.ceil(dMaxX);
        int destMaxY = (int) Math.ceil(dMaxY);
        int destMaxZ = (int) Math.ceil(dMaxZ);

        // Approximation: use destination block's normalized position to estimate the twist
        // angle — accurate for mild twists, sufficient for gap elimination in all cases.
        List<int[]> blocks = new ArrayList<>(sel.clipboard.size());
        for (int dx = destMinX; dx < destMaxX; dx++) {
            for (int dy = destMinY; dy < destMaxY; dy++) {
                for (int dz = destMinZ; dz < destMaxZ; dz++) {
                    float tx = width > 0 ? (float) (dx - selMin.x()) / width : 0.5f;
                    float ty = height > 0 ? (float) (dy - selMin.y()) / height : 0.5f;
                    float tz = depth > 0 ? (float) (dz - selMin.z()) / depth : 0.5f;

                    float angleX = twistAngleXDegrees * tx;
                    float angleY = twistAngleYDegrees * ty;
                    float angleZ = twistAngleZDegrees * tz;

                    Mat3DFloat rotation = ShapeMath.buildRotationMatrix(angleX, angleY, angleZ);
                    // Rotation matrices are orthogonal: inverse = transpose
                    Mat3DFloat invRotation = rotation.transpose();

                    double relX = dx + 0.5 - centerX;
                    double relY = dy + 0.5 - centerY;
                    double relZ = dz + 0.5 - centerZ;

                    Vec3DFloat srcVec = invRotation.mul(Vec3DFloat.from((float) relX, (float) relY, (float) relZ));

                    int srcBlockX = (int) Math.round(centerX + srcVec.x() - 0.5);
                    int srcBlockY = (int) Math.round(centerY + srcVec.y() - 0.5);
                    int srcBlockZ = (int) Math.round(centerZ + srcVec.z() - 0.5);

                    SelectionState.BlockData bd = sel.clipboardGet(
                            Vec3DInt.from(srcBlockX - selMin.x(), srcBlockY - selMin.y(), srcBlockZ - selMin.z()));
                    if (bd == SelectionState.BlockData.AIR) continue;

                    int rotatedMeta = BlockMetaRotator.rotateOrKeep(bd.block(), bd.meta(), rotation);
                    blocks.add(new int[] {dx, dy, dz, Block.getIdFromBlock(bd.block()), rotatedMeta});
                }
            }
        }
        ghostBlocks = blocks;

        ChangeProposal proposal = ChangeProposal.forPreview();
        for (int[] block : blocks) {
            proposal.proposed.put(
                    ChangeProposal.packKey(Vec3DInt.from(block[0], block[1], block[2])),
                    new int[] {block[3], block[4]});
        }
        preview = proposal;
    }

    private void invalidate() {
        lastMode = null;
        lastOffset = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
        lastCount = -1;
        lastOffsetType = null;
        lastTwistAngleX = Float.NaN;
        lastTwistAngleY = Float.NaN;
        lastTwistAngleZ = Float.NaN;
        lastRevolveAxis = null;
        lastRevolveAngle = Float.NaN;
        lastRevolveCount = -1;
        lastRevolveHasTranslation = false;
        lastRevolveTranslation = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
        lastRevolveCenter = null;
        preview = null;
        ghostBlocks = null;
    }
}
