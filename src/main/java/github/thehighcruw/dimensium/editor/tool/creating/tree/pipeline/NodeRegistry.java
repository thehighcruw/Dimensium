/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class NodeRegistry {

    private static final Map<String, Supplier<PipelineNode<?, ?>>> REGISTRY = new HashMap<>();

    public static void register(String id, Supplier<PipelineNode<?, ?>> factory) {
        REGISTRY.put(id, factory);
    }

    public static PipelineNode<?, ?> create(String id) {
        Supplier<PipelineNode<?, ?>> factory = REGISTRY.get(id);
        if (factory == null) throw new IllegalArgumentException("Unknown pipeline node: " + id);
        return factory.get();
    }

    public static Map<String, Supplier<PipelineNode<?, ?>>> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    private NodeRegistry() {}
}
