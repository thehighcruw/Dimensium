/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.state;

public enum SelectionMode {

    BOX("dimensium.selection_mode.box", "Box"),
    MAGIC("dimensium.selection_mode.magic", "Mgc");

    public final String label;
    public final String abbr;

    SelectionMode(String label, String abbr) {
        this.label = label;
        this.abbr = abbr;
    }

    public SelectionMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public SelectionMode prev() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }
}
