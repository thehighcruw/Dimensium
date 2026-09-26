/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;

public class PipelineContext {

    public final Vec3DInt origin;
    public final long seed;

    public PipelineContext(Vec3DInt origin, long seed) {
        this.origin = origin;
        this.seed = seed;
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
