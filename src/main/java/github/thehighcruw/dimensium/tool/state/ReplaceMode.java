/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.state;

public enum ReplaceMode {

    ANY("dimensium.replace_mode.any"),
    AIR_ONLY("dimensium.replace_mode.air_only"),
    SOLID_ONLY("dimensium.replace_mode.solid_only"),
    SAME_BLOCK("dimensium.replace_mode.same_block");

    public final String label;

    ReplaceMode(String label) {
        this.label = label;
    }
}
