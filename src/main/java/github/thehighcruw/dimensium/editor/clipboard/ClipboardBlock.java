/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.clipboard;

import com.github.bsideup.jabel.Desugar;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;

@Desugar
public record ClipboardBlock(Vec3DInt offset, int blockId, int meta) {}
