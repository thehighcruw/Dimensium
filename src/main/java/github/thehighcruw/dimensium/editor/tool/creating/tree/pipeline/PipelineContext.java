/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.HashMap;
import java.util.Map;

public class PipelineContext {

    public final Vec3DInt origin;
    public final long seed;
    private final Map<Class<?>, Object> slots = new HashMap<>();

    public PipelineContext(Vec3DInt origin, long seed) {
        this.origin = origin;
        this.seed = seed;
    }

    public <T> void put(Class<T> type, T value) {
        slots.put(type, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        return (T) slots.get(type);
    }

    /** Derive a deterministic per-node seed by mixing the global seed with a node-specific offset. */
    public long nodeSeed(int nodeIndex) {
        long mixed = seed ^ ((long) nodeIndex * 6364136223846793005L);
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        return mixed;
    }
}
