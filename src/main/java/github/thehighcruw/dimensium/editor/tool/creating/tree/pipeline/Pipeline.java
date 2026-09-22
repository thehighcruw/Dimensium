/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline;

import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Pipeline {

    private final List<PipelineNode> nodes;
    private final NodeParams params;

    public Pipeline(List<PipelineNode> nodes, NodeParams params) {
        this.nodes = nodes;
        this.params = params;
    }

    public BlockMap execute(PipelineContext context) {
        Object current = context.origin;
        for (PipelineNode node : nodes) {
            current = node.apply(current, params, context);
        }
        return (BlockMap) current;
    }

    public NodeParams params() {
        return params;
    }
}
