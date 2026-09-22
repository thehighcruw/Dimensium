/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Pipeline {

    private final List<PipelineNode> nodes;
    private final NodeParams params;

    public Pipeline(List<PipelineNode> nodes, NodeParams params) {
        this.nodes = nodes;
        this.params = params;
    }

    public BlockMap execute(Vec3DInt origin) {
        Object current = origin;
        for (PipelineNode node : nodes) {
            current = node.apply(current, params);
        }
        return (BlockMap) current;
    }

    public NodeParams params() {
        return params;
    }
}
