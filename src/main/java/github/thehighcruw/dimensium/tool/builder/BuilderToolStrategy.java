/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.builder;

import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.state.SelectionState;

public interface BuilderToolStrategy {

    /**
     * False = skip server capture and apply immediately after selection.
     * ERASE is the canonical example.
     */
    boolean needsCapture();

    void confirm(BuilderToolState bts, SelectionState sel);
}
