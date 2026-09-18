/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.blueprint;

import com.github.bsideup.jabel.Desugar;
import github.thehighcruw.dimensium.editor.clipboard.ClipboardBlock;
import github.thehighcruw.dimensium.editor.clipboard.ClipboardUtils;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.List;
import java.util.Map;

@Desugar
public record Blueprint(
        String name, List<String> tags, Vec3DInt clipDim, List<ClipboardBlock> offsets, byte[] thumbnailPng) {

    public static Blueprint fromClipboard(
            String name,
            List<String> tags,
            Map<Long, SelectionState.BlockData> clipboard,
            Vec3DInt clipDim,
            byte[] thumbnailPng) {
        List<ClipboardBlock> offsets = ClipboardUtils.toOffsets(clipboard);
        return new Blueprint(name, tags, clipDim, offsets, thumbnailPng);
    }
}
