/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.tool.strategy;

import github.thehighcruw.dimensium.editor.handler.SelectionOps;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.tool.BuilderToolState;

public class CloneStrategy implements BuilderToolStrategy {

    @Override
    public boolean needsCapture() {
        return true;
    }

    @Override
    public void confirm(BuilderToolState bts, SelectionState sel) {
        int ox = sel.minX(), oy = sel.minY(), oz = sel.minZ();
        int dx = bts.offset.x(), dy = bts.offset.y(), dz = bts.offset.z();
        final SelectionState selSnap = sel;
        BlockSender
            .sendChunkedLazy(() -> SelectionOps.clipboardToPlacements(selSnap, ox + dx, oy + dy, oz + dz), "Clone");
    }
}
