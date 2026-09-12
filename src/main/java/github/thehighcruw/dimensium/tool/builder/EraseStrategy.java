/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.builder;

import github.thehighcruw.dimensium.handler.BlockSender;
import github.thehighcruw.dimensium.handler.SelectionOps;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.state.SelectionState;

public class EraseStrategy implements BuilderToolStrategy {

    @Override
    public boolean needsCapture() {
        return false;
    }

    @Override
    public void confirm(BuilderToolState bts, SelectionState sel) {
        final SelectionState selSnap = sel;
        BlockSender.sendChunkedLazy(() -> SelectionOps.selectionToAirOps(selSnap), "Erase");
    }
}
