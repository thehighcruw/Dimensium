/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import github.thehighcruw.dimensium.tool.ChangeProposal;

public final class PipelinePreviewState {

    public static final PipelinePreviewState INSTANCE = new PipelinePreviewState();

    /** Set by PipelineEditorWindow when preview is triggered. Rendered by SelectionRenderer. */
    public ChangeProposal preview = null;

    private PipelinePreviewState() {}
}
