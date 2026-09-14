/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.clipboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;

import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;

public final class ClipboardUtils {

    private ClipboardUtils() {}

    public static List<int[]> toOffsets(Map<Long, SelectionState.BlockData> clipboard) {
        List<int[]> offsets = new ArrayList<>(clipboard.size());
        for (Map.Entry<Long, SelectionState.BlockData> e : clipboard.entrySet()) {
            Vec3DInt p = SelectionState.decodeClipboardKey(e.getKey());
            SelectionState.BlockData bd = e.getValue();
            offsets.add(new int[] { p.x(), p.y(), p.z(), Block.getIdFromBlock(bd.block()), bd.meta() });
        }
        return offsets;
    }
}
