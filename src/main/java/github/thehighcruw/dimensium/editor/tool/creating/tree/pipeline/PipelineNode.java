/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline;

public interface PipelineNode<I, O> {

    O apply(I input, NodeParams params);

    NodeSchema schema();
}
