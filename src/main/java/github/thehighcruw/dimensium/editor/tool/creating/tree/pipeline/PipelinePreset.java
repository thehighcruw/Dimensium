/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PipelinePreset {

    public final String name;
    private final List<String> nodeIds;
    private final NodeParams params;

    public PipelinePreset(String name, List<String> nodeIds) {
        this.name = name;
        this.nodeIds = new ArrayList<>(nodeIds);
        this.params = new NodeParams();
        for (String id : this.nodeIds) {
            NodeRegistry.create(id).schema().applyDefaults(params);
        }
    }

    public PipelinePreset withParam(String key, Object value) {
        params.set(key, value);
        return this;
    }

    public NodeParams params() {
        return params;
    }

    public List<String> nodeIds() {
        return Collections.unmodifiableList(nodeIds);
    }

    @SuppressWarnings("rawtypes")
    public Pipeline build() {
        List<PipelineNode> nodes = new ArrayList<>();
        for (String id : nodeIds) {
            nodes.add(NodeRegistry.create(id));
        }
        return new Pipeline(nodes, params.copy());
    }
}
