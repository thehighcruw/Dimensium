/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.stamp;

import github.thehighcruw.dimensium.editor.blueprint.Blueprint;
import github.thehighcruw.dimensium.editor.pipeline.PipelineGraph;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SimpleRecursiveSkeletonNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SpaceColonizationSkeletonNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.WeberPennSkeletonNode;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.concurrent.ThreadLocalRandom;

public class StampEntry {

    /** Non-null when in blueprint mode. */
    public final Blueprint blueprint;
    /** Non-null when in pipeline mode. */
    public final PipelineGraph pipeline;

    public float chance = 1.0f;
    public int offsetY = 0;
    /** Per-entry seed for pipeline execution, randomised at creation. */
    public long seed;

    public StampEntry(Blueprint blueprint) {
        this.blueprint = blueprint;
        this.pipeline = null;
        this.seed = 0;
    }

    public StampEntry(PipelineGraph pipeline) {
        this.blueprint = null;
        this.pipeline = pipeline;
        this.seed = ThreadLocalRandom.current().nextLong();
    }

    public boolean isPipeline() {
        return pipeline != null;
    }

    public String displayName() {
        if (isPipeline()) return pipeline.name;
        String name = blueprint.name();
        return name.isEmpty() ? "Unnamed" : name;
    }

    public Vec3DInt clipDim() {
        if (!isPipeline()) return blueprint.clipDim();
        int trunkHeight = 12;
        float crownRadius = 6f;
        for (PipelineGraph.NodeInstance inst : pipeline.nodes()) {
            switch (inst.typeId) {
                case WeberPennSkeletonNode.ID:
                    trunkHeight = inst.params.getInt("wp.trunkHeight", 12);
                    crownRadius = inst.params.getFloat("leaf.clusterRadius", 6f);
                    break;
                case SimpleRecursiveSkeletonNode.ID:
                    trunkHeight = inst.params.getInt("sr.trunkHeight", 8);
                    break;
                case SpaceColonizationSkeletonNode.ID:
                    trunkHeight = inst.params.getInt("sc.trunkHeight", 6);
                    crownRadius = inst.params.getFloat("sc.crownRadiusX", 6f);
                    break;
                default:
                    break;
            }
        }
        int size = (int) (crownRadius * 2f + 4f);
        return Vec3DInt.from(size, trunkHeight + (int) crownRadius, size);
    }
}
