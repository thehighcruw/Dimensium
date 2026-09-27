/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class NodeRegistry {

    public enum NodeGroup {
        GENERATE,
        BRANCH,
        VOXELIZE,
        COMBINE,
        TRANSFORM,
        FILTER,
        PAINT,
        MATH
    }

    private static final Map<String, Supplier<PipelineNode>> REGISTRY = new LinkedHashMap<>();
    private static final Map<String, NodeGroup> GROUPS = new LinkedHashMap<>();
    /** Optional i18n key for a labelled section within a group. Null means no separator. */
    private static final Map<String, String> SUB_GROUPS = new LinkedHashMap<>();

    public static void register(String id, NodeGroup group, Supplier<PipelineNode> factory) {
        register(id, group, null, factory);
    }

    public static void register(String id, NodeGroup group, String subGroupKey, Supplier<PipelineNode> factory) {
        REGISTRY.put(id, factory);
        GROUPS.put(id, group);
        SUB_GROUPS.put(id, subGroupKey);
    }

    public static PipelineNode create(String id) {
        Supplier<PipelineNode> factory = REGISTRY.get(id);
        if (factory == null) throw new IllegalArgumentException("Unknown pipeline node: " + id);
        return factory.get();
    }

    public static NodeGroup groupOf(String id) {
        return GROUPS.get(id);
    }

    /** Returns the i18n key for this node's sub-group label, or null if it has none. */
    public static String subGroupKeyOf(String id) {
        return SUB_GROUPS.get(id);
    }

    /** Returns node IDs in this group in registration order. */
    public static List<String> idsInGroup(NodeGroup group) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, NodeGroup> entry : GROUPS.entrySet()) {
            if (entry.getValue() == group) result.add(entry.getKey());
        }
        return result;
    }

    public static Map<String, Supplier<PipelineNode>> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    private NodeRegistry() {}
}
