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

public class CloneStrategy implements BuilderToolStrategy {

    @Override
    public boolean needsCapture() {
        return true;
    }

    @Override
    public void confirm(BuilderToolState bts, SelectionState sel) {
        Vec3DInt origin = Vec3DInt.from(sel.minX(), sel.minY(), sel.minZ()).plus(bts.offset);
        final SelectionState selSnap = sel;
        BlockSender.sendChunkedLazy(() -> SelectionOps.clipboardToPlacements(selSnap, origin), "Clone");
    }
}
