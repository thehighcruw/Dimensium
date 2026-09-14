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

public class StackStrategy implements BuilderToolStrategy {

    @Override
    public boolean needsCapture() {
        return true;
    }

    @Override
    public void confirm(BuilderToolState bts, SelectionState sel) {
        Vec3DInt base = Vec3DInt.from(sel.minX(), sel.minY(), sel.minZ());
        Vec3DInt dims = Vec3DInt.from(sel.width(), sel.height(), sel.depth());
        Vec3DInt stackMin = bts.stack.min(Vec3DInt.ZERO);
        Vec3DInt stackMax = bts.stack.max(Vec3DInt.ZERO);
        int copies = stackMax.minus(stackMin).plus(1).product() - 1;
        int clipSize = sel.clipboard == null ? 0 : sel.clipboard.size();
        final SelectionState selSnap = sel;
        BlockSender.sendChunkedLazy(
                () -> {
                    List<int[]> ops = new ArrayList<>(copies * clipSize);
                    Vec3DInt.forEachInclusive(stackMin, stackMax, (ix, iy, iz) -> {
                        if (ix == 0 && iy == 0 && iz == 0) return;
                        ops.addAll(SelectionOps.clipboardToPlacements(
                                selSnap, base.plus(dims.times(Vec3DInt.from(ix, iy, iz)))));
                    });
                    return ops;
                },
                "Stack");
    }
}
