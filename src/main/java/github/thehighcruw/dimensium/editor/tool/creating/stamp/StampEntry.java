/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.stamp;

import java.io.File;

import github.thehighcruw.dimensium.editor.blueprint.Blueprint;

public class StampEntry {

    public Blueprint blueprint;
    /** Source file — used for thumbnail lookup; null when created from clipboard. */
    public File file;
    public float chance = 1.0f;
    public int offsetY = 0;

    public StampEntry(Blueprint blueprint, File file) {
        this.blueprint = blueprint;
        this.file = file;
    }
}
