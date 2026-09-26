/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class NodeRegistry {

    public enum NodeGroup {
        SKELETON,
        CONVERT,
        FILTER,
        PALETTE
    }

    private static final Map<String, Supplier<PipelineNode>> REGISTRY = new HashMap<>();
    private static final Map<String, NodeGroup> GROUPS = new HashMap<>();

    public static void register(String id, NodeGroup group, Supplier<PipelineNode> factory) {
        REGISTRY.put(id, factory);
        GROUPS.put(id, group);
    }

    public static PipelineNode create(String id) {
        Supplier<PipelineNode> factory = REGISTRY.get(id);
        if (factory == null) throw new IllegalArgumentException("Unknown pipeline node: " + id);
        return factory.get();
    }

    public static NodeGroup groupOf(String id) {
        return GROUPS.get(id);
    }

    public static List<String> idsInGroup(NodeGroup group) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, NodeGroup> entry : GROUPS.entrySet()) {
            if (entry.getValue() == group) result.add(entry.getKey());
        }
        Collections.sort(result);
        return result;
    }

    public static Map<String, Supplier<PipelineNode>> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    private NodeRegistry() {}
}
