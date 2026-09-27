/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline.nodes;

import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import github.thehighcruw.dimensium.editor.pipeline.Skeleton;
import github.thehighcruw.dimensium.editor.pipeline.SkeletonNode;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Attaches recursive symmetric branches to every leaf node of the input skeleton. */
public class SimpleRecursiveBranchesNode implements PipelineNode {

    public static final String ID = "simple_recursive_branches";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("srb.levels", 2, 1, 5, "dimensium.ui.pipeline.srb_levels")
            .intParam("srb.branchCount", 3, 1, 6, "dimensium.ui.pipeline.srb_branch_count")
            .floatParam("srb.branchAngle", 35.0f, 5.0f, 70.0f, "dimensium.ui.pipeline.srb_branch_angle")
            .intParam("srb.initialLength", 5, 1, 20, "dimensium.ui.pipeline.srb_initial_length")
            .floatParam("srb.lengthDecay", 0.6f, 0.2f, 0.9f, "dimensium.ui.pipeline.srb_length_decay")
            .floatParam("srb.radiusDecay", 0.5f, 0.1f, 0.9f, "dimensium.ui.pipeline.srb_radius_decay")
            .description("dimensium.ui.pipeline.node.simple_recursive_branches.desc")
            .inputPort("skeleton", PortType.SKELETON)
            .outputPort("skeleton", PortType.SKELETON);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Skeleton skeleton = inputs.get("skeleton", Skeleton.class);
        if (skeleton == null) return;

        int levels = params.getInt("srb.levels", 2);
        int branchCount = params.getInt("srb.branchCount", 3);
        float branchAngle = params.getFloat("srb.branchAngle", 35.0f);
        int initialLength = params.getInt("srb.initialLength", 5);
        float lengthDecay = params.getFloat("srb.lengthDecay", 0.6f);
        float radiusDecay = params.getFloat("srb.radiusDecay", 0.5f);

        Random rand = new Random(context.nodeSeed(0));

        List<SkeletonNode> leaves = new ArrayList<>();
        for (SkeletonNode root : skeleton.roots) {
            collectLeaves(root, leaves);
        }
        for (SkeletonNode leaf : leaves) {
            recurse(
                    leaf,
                    Vec3DFloat.from(0, 1, 0),
                    initialLength,
                    leaf.radius,
                    levels,
                    branchCount,
                    branchAngle,
                    lengthDecay,
                    radiusDecay,
                    rand);
        }

        outputs.set("skeleton", skeleton);
    }

    private static void collectLeaves(SkeletonNode node, List<SkeletonNode> result) {
        if (node.children.isEmpty()) {
            result.add(node);
            return;
        }
        for (SkeletonNode child : node.children) {
            collectLeaves(child, result);
        }
    }

    private static void recurse(
            SkeletonNode parent,
            Vec3DFloat parentDir,
            int parentLength,
            float parentRadius,
            int remainingLevels,
            int branchCount,
            float angleDeg,
            float lengthDecay,
            float radiusDecay,
            Random rand) {

        if (remainingLevels <= 0) return;

        int branchLength = Math.max(1, Math.round(parentLength * lengthDecay));
        float branchRadius = parentRadius * radiusDecay;
        float spreadRad = (float) Math.toRadians(angleDeg);

        for (int i = 0; i < branchCount; i++) {
            float azimuth = (float) (2 * Math.PI * i / branchCount) + (rand.nextFloat() - 0.5f) * 0.4f;
            float polar = spreadRad * (0.4f + rand.nextFloat() * 0.6f);
            Vec3DFloat dir = perturbDirection(parentDir, azimuth, polar);

            SkeletonNode branchRoot = new SkeletonNode(parent.position, branchRadius);
            parent.children.add(branchRoot);

            SkeletonNode tip = buildChain(branchRoot, dir, branchLength, branchRadius);
            recurse(
                    tip,
                    dir,
                    branchLength,
                    branchRadius,
                    remainingLevels - 1,
                    branchCount,
                    angleDeg,
                    lengthDecay,
                    radiusDecay,
                    rand);
        }
    }

    private static SkeletonNode buildChain(SkeletonNode root, Vec3DFloat dir, int length, float radius) {
        SkeletonNode current = root;
        Vec3DFloat pos = root.position.toFloat();
        for (int step = 1; step <= length; step++) {
            pos = pos.plus(dir);
            float r = radius * (1f - 0.4f * ((float) step / length));
            SkeletonNode next = new SkeletonNode(pos.round(), r);
            current.children.add(next);
            current = next;
        }
        return current;
    }

    private static Vec3DFloat perturbDirection(Vec3DFloat parentDir, float azimuth, float polar) {
        Vec3DFloat up = Vec3DFloat.from(0, 1, 0);
        Vec3DFloat right;
        if (Math.abs(parentDir.dot(up)) > 0.9f) {
            right = parentDir.cross(Vec3DFloat.from(1, 0, 0)).normalize();
        } else {
            right = parentDir.cross(up).normalize();
        }
        Vec3DFloat forward = right.cross(parentDir).normalize();

        float localX = (float) (Math.sin(polar) * Math.cos(azimuth));
        float localY = (float) Math.cos(polar);
        float localZ = (float) (Math.sin(polar) * Math.sin(azimuth));

        return right.times(localX)
                .plus(forward.times(localZ))
                .plus(parentDir.times(localY))
                .normalize();
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
