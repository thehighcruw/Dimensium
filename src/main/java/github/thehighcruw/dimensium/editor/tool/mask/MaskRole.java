/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

public enum MaskRole {
    BOTH,
    DESTINATION,
    SOURCE;

    public boolean appliesToDestination() {
        return this != SOURCE;
    }

    public boolean appliesToSource() {
        return this != DESTINATION;
    }
}
