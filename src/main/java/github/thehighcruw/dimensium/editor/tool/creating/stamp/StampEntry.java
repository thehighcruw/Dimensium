/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.stamp;

import github.thehighcruw.dimensium.editor.blueprint.Blueprint;

public class StampEntry {

    public Blueprint blueprint;
    public float chance = 1.0f;
    public int offsetY = 0;

    public StampEntry(Blueprint blueprint) {
        this.blueprint = blueprint;
    }
}
