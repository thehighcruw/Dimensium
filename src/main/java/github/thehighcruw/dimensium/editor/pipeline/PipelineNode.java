/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

public interface PipelineNode {

    void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context);

    NodeSchema schema();
}
