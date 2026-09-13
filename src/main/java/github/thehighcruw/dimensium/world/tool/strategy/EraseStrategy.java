/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.tool.strategy;

import github.thehighcruw.dimensium.editor.handler.SelectionOps;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.tool.BuilderToolState;

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
