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
        int maxX = selMin.x() + sel.width();
        int maxY = selMin.y() + sel.height();
        int maxZ = selMin.z() + sel.depth();
        revolveInnerRadius = Double.MAX_VALUE;
        revolveOuterRadius = 0;
        for (int xi = 0; xi <= 1; xi++) {
            for (int yi = 0; yi <= 1; yi++) {
                for (int zi = 0; zi <= 1; zi++) {
                    double cornerX = xi == 0 ? selMin.x() : maxX;
                    double cornerY = yi == 0 ? selMin.y() : maxY;
                    double cornerZ = zi == 0 ? selMin.z() : maxZ;
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

        List<int[]> blocks = new ArrayList<>(copies * sel.clipboard.size());
        for (int copyIndex = 1; copyIndex <= copies; copyIndex++) {
            double theta = totalAngleRad * copyIndex / copies;
            double cosTheta = Math.cos(theta);
            double sinTheta = Math.sin(theta);
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

            for (Map.Entry<Long, SelectionState.BlockData> entry : sel.clipboard.entrySet()) {
                Vec3DInt rel = SelectionState.decodeClipboardKey(entry.getKey());
                double bx = selMin.x() + rel.x() + 0.5;
                double by = selMin.y() + rel.y() + 0.5;
                double bz = selMin.z() + rel.z() + 0.5;

                double rotatedX, rotatedY, rotatedZ;
                switch (revolveAxis) {
                    case X: {
                        double dy = by - cy;
                        double dz = bz - cz;
                        rotatedX = bx;
                        rotatedY = cy + dy * cosTheta - dz * sinTheta;
                        rotatedZ = cz + dy * sinTheta + dz * cosTheta;
                        break;
                    }
                    case Z: {
                        double dx = bx - cx;
                        double dy = by - cy;
                        rotatedX = cx + dx * cosTheta - dy * sinTheta;
                        rotatedY = cy + dx * sinTheta + dy * cosTheta;
                        rotatedZ = bz;
                        break;
                    }
                    default: { // Y
                        double dx = bx - cx;
                        double dz = bz - cz;
                        rotatedX = cx + dx * cosTheta - dz * sinTheta;
                        rotatedY = by;
                        rotatedZ = cz + dx * sinTheta + dz * cosTheta;
                        break;
                    }
                }

                int destX = (int) Math.round(rotatedX - 0.5 + translationX);
                int destY = (int) Math.round(rotatedY - 0.5 + translationY);
                int destZ = (int) Math.round(rotatedZ - 0.5 + translationZ);

                SelectionState.BlockData bd = entry.getValue();
                int rotatedMeta = BlockMetaRotator.rotateOrKeep(bd.block(), bd.meta(), metaRotation);
                blocks.add(new int[] {destX, destY, destZ, Block.getIdFromBlock(bd.block()), rotatedMeta});
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

        List<int[]> blocks = new ArrayList<>(sel.clipboard.size());
        for (Map.Entry<Long, SelectionState.BlockData> entry : sel.clipboard.entrySet()) {
            Vec3DInt rel = SelectionState.decodeClipboardKey(entry.getKey());
            int blockX = selMin.x() + rel.x();
            int blockY = selMin.y() + rel.y();
            int blockZ = selMin.z() + rel.z();

            float tx = width > 0 ? (float) rel.x() / width : 0.5f;
            float ty = height > 0 ? (float) rel.y() / height : 0.5f;
            float tz = depth > 0 ? (float) rel.z() / depth : 0.5f;

            float angleX = twistAngleXDegrees * tx;
            float angleY = twistAngleYDegrees * ty;
            float angleZ = twistAngleZDegrees * tz;

            Mat3DFloat rotation = ShapeMath.buildRotationMatrix(angleX, angleY, angleZ);

            double relX = blockX + 0.5 - centerX;
            double relY = blockY + 0.5 - centerY;
            double relZ = blockZ + 0.5 - centerZ;

            Vec3DFloat rotated = rotation.mul(Vec3DFloat.from((float) relX, (float) relY, (float) relZ));

            int destX = (int) Math.round(centerX + rotated.x() - 0.5);
            int destY = (int) Math.round(centerY + rotated.y() - 0.5);
            int destZ = (int) Math.round(centerZ + rotated.z() - 0.5);

            SelectionState.BlockData bd = entry.getValue();
            int rotatedMeta = BlockMetaRotator.rotateOrKeep(bd.block(), bd.meta(), rotation);
            blocks.add(new int[] {destX, destY, destZ, Block.getIdFromBlock(bd.block()), rotatedMeta});
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
