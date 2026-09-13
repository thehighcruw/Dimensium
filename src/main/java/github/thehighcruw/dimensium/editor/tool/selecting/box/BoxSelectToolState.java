/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.box;

import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;

public class BoxSelectToolState {

    public static final BoxSelectToolState INSTANCE = new BoxSelectToolState();

    public BooleanOp booleanOp = BooleanOp.ADD;
}
