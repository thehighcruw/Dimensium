/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool;

import github.thehighcruw.dimensium.tool.ChangeProposal;

/**
 * Singleton holding the currently-active drag proposal.
 * Replaces the static {@code ChangeProposal.activeDrag} field.
 */
public class ActiveDragState {

    public static final ActiveDragState INSTANCE = new ActiveDragState();

    private ActiveDragState() {}

    public ChangeProposal activeDrag = null;
}
