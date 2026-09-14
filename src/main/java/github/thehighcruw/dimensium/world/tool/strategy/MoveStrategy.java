/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.tool.strategy;

import github.thehighcruw.dimensium.editor.handler.SelectionOps;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import java.util.ArrayList;
import java.util.List;

public class MoveStrategy implements BuilderToolStrategy {

    @Override
    public boolean needsCapture() {
        return true;
    }

    @Override
    public void confirm(BuilderToolState bts, SelectionState sel) {
        Vec3DInt origin = Vec3DInt.from(sel.minX(), sel.minY(), sel.minZ()).plus(bts.offset);

        // Snapshot air ops now — clearSelection() runs right after confirm() returns.
        final List<int[]> airOps = SelectionOps.selectionToAirOps(sel);
        final SelectionState selSnap = sel;
        BlockSender.sendChunkedLazy(
                () -> {
                    List<int[]> ops =
                            new ArrayList<>(airOps.size() + (selSnap.clipboard == null ? 0 : selSnap.clipboard.size()));
                    ops.addAll(airOps);
                    ops.addAll(SelectionOps.clipboardToPlacements(selSnap, origin));
                    return ops;
                },
                "Move");
    }
}
