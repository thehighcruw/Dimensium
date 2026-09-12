/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.builder;

import java.util.ArrayList;
import java.util.List;

import github.thehighcruw.dimensium.handler.BlockSender;
import github.thehighcruw.dimensium.handler.SelectionOps;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.state.SelectionState;

public class StackStrategy implements BuilderToolStrategy {

    @Override
    public boolean needsCapture() {
        return true;
    }

    @Override
    public void confirm(BuilderToolState bts, SelectionState sel) {
        int ox = sel.minX(), oy = sel.minY(), oz = sel.minZ();
        int w = sel.width(), h = sel.height(), d = sel.depth();
        int x0 = Math.min(bts.stackX, 0), x1 = Math.max(bts.stackX, 0);
        int y0 = Math.min(bts.stackY, 0), y1 = Math.max(bts.stackY, 0);
        int z0 = Math.min(bts.stackZ, 0), z1 = Math.max(bts.stackZ, 0);
        int copies = (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1) - 1;
        int clipSize = sel.clipboard == null ? 0 : sel.clipboard.size();
        final SelectionState selSnap = sel;
        BlockSender.sendChunkedLazy(() -> {
            List<int[]> ops = new ArrayList<>(copies * clipSize);
            for (int ix = x0; ix <= x1; ix++) {
                for (int iy = y0; iy <= y1; iy++) {
                    for (int iz = z0; iz <= z1; iz++) {
                        if (ix == 0 && iy == 0 && iz == 0) continue;
                        ops.addAll(SelectionOps.clipboardToPlacements(selSnap, ox + ix * w, oy + iy * h, oz + iz * d));
                    }
                }
            }
            return ops;
        }, "Stack");
    }
}
