/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.builder;

import github.thehighcruw.dimensium.handler.BlockSender;
import github.thehighcruw.dimensium.handler.SelectionOps;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.state.SelectionState;

public class CloneStrategy implements BuilderToolStrategy {

    @Override
    public boolean needsCapture() {
        return true;
    }

    @Override
    public void confirm(BuilderToolState bts, SelectionState sel) {
        int ox = sel.minX(), oy = sel.minY(), oz = sel.minZ();
        int dx = bts.offsetX, dy = bts.offsetY, dz = bts.offsetZ;
        final SelectionState selSnap = sel;
        BlockSender
            .sendChunkedLazy(() -> SelectionOps.clipboardToPlacements(selSnap, ox + dx, oy + dy, oz + dz), "Clone");
    }
}
