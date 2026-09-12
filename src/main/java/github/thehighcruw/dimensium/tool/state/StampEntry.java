/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.state;

import java.io.File;

import github.thehighcruw.dimensium.blueprint.Blueprint;

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
