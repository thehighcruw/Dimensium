/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.blueprint;

import java.util.List;
import java.util.Map;

import com.github.bsideup.jabel.Desugar;

import github.thehighcruw.dimensium.editor.clipboard.ClipboardUtils;
import github.thehighcruw.dimensium.shared.SelectionState;

@Desugar
public record Blueprint(String name, List<String> tags, int clipW, int clipH, int clipD, List<int[]> offsets,
    byte[] thumbnailPng) {

    public static Blueprint fromClipboard(String name, List<String> tags, Map<Long, SelectionState.BlockData> clipboard,
        int clipW, int clipH, int clipD, byte[] thumbnailPng) {
        List<int[]> offsets = ClipboardUtils.toOffsets(clipboard);
        return new Blueprint(name, tags, clipW, clipH, clipD, offsets, thumbnailPng);
    }
}
