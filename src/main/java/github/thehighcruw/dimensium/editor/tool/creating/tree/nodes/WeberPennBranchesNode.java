/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.nodes;

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

/**
 * Attaches Weber-Penn style branches to every leaf node of the input skeleton.
 * Each leaf gets a set of sub-branches distributed evenly around it, with
 * recursive sub-branching controlled by the levels parameter.
 */
public class WeberPennBranchesNode implements PipelineNode {

    public static final String ID = "weber_penn_branches";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("wp.levels", 2, 1, 4, "dimensium.ui.pipeline.wp_levels")
            .intParam("wp.branchCount", 4, 1, 8, "dimensium.ui.pipeline.wp_branch_count")
            .floatParam("wp.branchAngleSpread", 45.0f, 5.0f, 80.0f, "dimensium.ui.pipeline.wp_branch_angle_spread")
            .intParam("wp.initialLength", 6, 1, 20, "dimensium.ui.pipeline.wp_initial_length")
            .floatParam("wp.lengthRatio", 0.55f, 0.2f, 0.9f, "dimensium.ui.pipeline.wp_length_ratio")
            .floatParam("wp.radiusFactor", 0.5f, 0.2f, 0.8f, "dimensium.ui.pipeline.wp_radius_factor")
            .description("dimensium.ui.pipeline.node.weber_penn_branches.desc")
            .inputPort("skeleton", PortType.SKELETON)
            .outputPort("skeleton", PortType.SKELETON);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Skeleton skeleton = inputs.get("skeleton", Skeleton.class);
        if (skeleton == null) return;

        int levels = params.getInt("wp.levels", 2);
        int branchCount = params.getInt("wp.branchCount", 4);
        float branchAngleSpread = params.getFloat("wp.branchAngleSpread", 45.0f);
        int initialLength = params.getInt("wp.initialLength", 6);
        float lengthRatio = params.getFloat("wp.lengthRatio", 0.55f);
        float radiusFactor = params.getFloat("wp.radiusFactor", 0.5f);

        Random rand = new Random(context.nodeSeed(0));

        List<SkeletonNode> leaves = new ArrayList<>();
        for (SkeletonNode root : skeleton.roots) {
            collectLeaves(root, leaves);
        }
        for (SkeletonNode leaf : leaves) {
            attachWPBranches(
                    leaf,
                    Vec3DFloat.from(0, 1, 0),
                    initialLength,
                    leaf.radius,
                    levels,
                    branchCount,
                    branchAngleSpread,
                    lengthRatio,
                    radiusFactor,
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

    private static void attachWPBranches(
            SkeletonNode parent,
            Vec3DFloat parentDir,
            int length,
            float radius,
            int remainingLevels,
            int branchCount,
            float angleSpreadDeg,
            float lengthRatio,
            float radiusFactor,
            Random rand) {

        if (remainingLevels <= 0 || length < 1) return;

        float subRadius = radius * radiusFactor;
        float spreadRad = (float) Math.toRadians(angleSpreadDeg);

        for (int i = 0; i < branchCount; i++) {
            float azimuth = (float) (2 * Math.PI * i / branchCount) + (rand.nextFloat() - 0.5f) * 0.5f;
            float polar = spreadRad * (0.5f + rand.nextFloat() * 0.5f);
            Vec3DFloat dir = perturbDirection(parentDir, azimuth, polar);

            SkeletonNode branchRoot = new SkeletonNode(parent.position, subRadius);
            parent.children.add(branchRoot);

            SkeletonNode tip = buildChain(branchRoot, dir, length, subRadius);

            int subLength = Math.round(length * lengthRatio);
            attachWPBranches(
                    tip,
                    dir,
                    subLength,
                    subRadius,
                    remainingLevels - 1,
                    branchCount,
                    angleSpreadDeg,
                    lengthRatio,
                    radiusFactor,
                    rand);
        }
    }

    private static SkeletonNode buildChain(SkeletonNode root, Vec3DFloat dir, int length, float radius) {
        SkeletonNode current = root;
        Vec3DFloat pos = root.position.toFloat();
        float radiusTaper = 0.6f;
        for (int step = 1; step <= length; step++) {
            pos = pos.plus(dir);
            float r = radius * (1f - (1f - radiusTaper) * ((float) step / length));
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
