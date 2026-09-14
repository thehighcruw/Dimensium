/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.tool.strategy;

import java.util.ArrayList;
import java.util.List;

import github.thehighcruw.dimensium.editor.handler.SelectionOps;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.tool.BuilderToolState;

public class MoveStrategy implements BuilderToolStrategy {

    @Override
    public boolean needsCapture() {
        return true;
    }

    @Override
    public void confirm(BuilderToolState bts, SelectionState sel) {
        int ox = sel.minX(), oy = sel.minY(), oz = sel.minZ();
        int dx = bts.offset.x(), dy = bts.offset.y(), dz = bts.offset.z();

        // Snapshot air ops now — clearSelection() runs right after confirm() returns.
        final List<int[]> airOps = SelectionOps.selectionToAirOps(sel);
        final SelectionState selSnap = sel;
        BlockSender.sendChunkedLazy(() -> {
            List<int[]> ops = new ArrayList<>(
                airOps.size() + (selSnap.clipboard == null ? 0 : selSnap.clipboard.size()));
            ops.addAll(airOps);
            ops.addAll(SelectionOps.clipboardToPlacements(selSnap, ox + dx, oy + dy, oz + dz));
            return ops;
        }, "Move");
    }
}
