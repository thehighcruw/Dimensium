/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.tool.strategy;

import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.tool.BuilderToolState;

public interface BuilderToolStrategy {

    /**
     * False = skip server capture and apply immediately after selection.
     * ERASE is the canonical example.
     */
    boolean needsCapture();

    void confirm(BuilderToolState bts, SelectionState sel);
}
