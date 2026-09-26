/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.modify;

import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.world.World;

public class ModifyToolState {

    public static final ModifyToolState INSTANCE = new ModifyToolState();

    public enum ModifyMode {
        TRANSLATE_COPIES("dimensium.modify_mode.translate_copies");

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

    public static final int COUNT_MIN = 1;
    public static final int COUNT_MAX = 64;

    public ModifyMode mode = ModifyMode.TRANSLATE_COPIES;

    // Translate Copies params
    public Vec3DFloat translateCopiesOffset = Vec3DFloat.ZERO;
    public int translateCopiesCount = 1;
    public OffsetType translateCopiesOffsetType = OffsetType.RELATIVE;

    // Preview state
    public ChangeProposal preview = null;

    /** Flattened copy placements: [x, y, z, blockId, meta]. Matches preview; sent on confirm. */
    public List<int[]> ghostBlocks = null;

    // Staleness keys
    private long lastSelVersion = Long.MIN_VALUE;
    private int lastClipboardVersion = -1;
    private Vec3DFloat lastOffset = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
    private int lastCount = -1;
    private OffsetType lastOffsetType = null;

    private ModifyToolState() {}

    /** Full reset — called when the tool is deactivated. Forces re-capture on next activation. */
    public void cancel() {
        preview = null;
        ghostBlocks = null;
        lastSelVersion = Long.MIN_VALUE;
    }

    /** Clears the rendered preview after a confirm without invalidating the capture cache. */
    public void clearAfterConfirm() {
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

        boolean paramsChanged = !translateCopiesOffset.equals(lastOffset)
                || translateCopiesCount != lastCount
                || translateCopiesOffsetType != lastOffsetType
                || sel.clipboardVersion != lastClipboardVersion;

        if (!paramsChanged) return;

        lastOffset = translateCopiesOffset;
        lastCount = translateCopiesCount;
        lastOffsetType = translateCopiesOffsetType;
        lastClipboardVersion = sel.clipboardVersion;

        rebuildPreview(sel);
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

    private void invalidate() {
        lastOffset = Vec3DFloat.from(Float.NaN, Float.NaN, Float.NaN);
        lastCount = -1;
        lastOffsetType = null;
        preview = null;
        ghostBlocks = null;
    }
}
