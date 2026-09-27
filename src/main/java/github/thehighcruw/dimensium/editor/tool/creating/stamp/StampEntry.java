/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.stamp;

import github.thehighcruw.dimensium.editor.blueprint.Blueprint;
import github.thehighcruw.dimensium.editor.pipeline.PipelineGraph;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.CurvedPathNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.LinePathNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.RandomWalkPathNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SpaceColonizationBranchesNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SplinePathNode;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;

public class StampEntry {

    /** Non-null when in blueprint mode. */
    public final Blueprint blueprint;
    /** Non-null when in pipeline mode. */
    public final PipelineGraph pipeline;

    public float chance = 1.0f;
    public int offsetY = 0;

    public StampEntry(Blueprint blueprint) {
        this.blueprint = blueprint;
        this.pipeline = null;
    }

    public StampEntry(PipelineGraph pipeline) {
        this.blueprint = null;
        this.pipeline = pipeline;
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
                case LinePathNode.ID:
                    trunkHeight = inst.params.getInt("line.height", 12);
                    break;
                case CurvedPathNode.ID:
                    trunkHeight = inst.params.getInt("curved.height", 12);
                    break;
                case RandomWalkPathNode.ID:
                    trunkHeight = inst.params.getInt("rwalk.steps", 12);
                    break;
                case SplinePathNode.ID:
                    trunkHeight = inst.params.getInt("spline.segments", 12);
                    break;
                case SpaceColonizationBranchesNode.ID:
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
