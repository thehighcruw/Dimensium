/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.nodes;

import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.Skeleton;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.SkeletonNode;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.Random;

public class SimpleRecursiveSkeletonNode implements PipelineNode<Vec3DInt, Skeleton> {

    public static final String ID = "simple_recursive_skeleton";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("sr.trunkHeight", 8, 2, 30, "dimensium.ui.tree.sr_trunk_height")
            .floatParam("sr.trunkRadius", 1.5f, 0.3f, 5.0f, "dimensium.ui.tree.sr_trunk_radius")
            .intParam("sr.levels", 3, 1, 5, "dimensium.ui.tree.sr_levels")
            .intParam("sr.branchCount", 3, 1, 6, "dimensium.ui.tree.sr_branch_count")
            .floatParam("sr.branchAngle", 35.0f, 5.0f, 70.0f, "dimensium.ui.tree.sr_branch_angle")
            .floatParam("sr.lengthDecay", 0.6f, 0.2f, 0.9f, "dimensium.ui.tree.sr_length_decay")
            .floatParam("sr.radiusDecay", 0.5f, 0.1f, 0.9f, "dimensium.ui.tree.sr_radius_decay");

    @Override
    public Skeleton apply(Vec3DInt origin, NodeParams params, PipelineContext context) {
        int trunkHeight = params.getInt("sr.trunkHeight", 8);
        float trunkRadius = params.getFloat("sr.trunkRadius", 1.5f);
        int levels = params.getInt("sr.levels", 3);
        int branchCount = params.getInt("sr.branchCount", 3);
        float branchAngle = params.getFloat("sr.branchAngle", 35.0f);
        float lengthDecay = params.getFloat("sr.lengthDecay", 0.6f);
        float radiusDecay = params.getFloat("sr.radiusDecay", 0.5f);

        Random rand = new Random(context.nodeSeed(0));

        SkeletonNode root = buildTrunk(origin, trunkHeight, trunkRadius);
        SkeletonNode tip = getTip(root);

        recurse(
                tip,
                Vec3DFloat.from(0, 1, 0),
                trunkHeight,
                trunkRadius,
                levels,
                branchCount,
                branchAngle,
                lengthDecay,
                radiusDecay,
                rand);

        Skeleton skeleton = new Skeleton(root);
        context.put(Skeleton.class, skeleton);
        return skeleton;
    }

    private static SkeletonNode buildTrunk(Vec3DInt origin, int height, float radius) {
        SkeletonNode current = new SkeletonNode(origin, radius);
        SkeletonNode head = current;
        for (int y = 1; y <= height; y++) {
            float r = radius * (1f - 0.3f * ((float) y / height));
            SkeletonNode next = new SkeletonNode(origin.plus(0, y, 0), r);
            current.children.add(next);
            current = next;
        }
        return head;
    }

    private static SkeletonNode getTip(SkeletonNode node) {
        SkeletonNode current = node;
        while (!current.children.isEmpty()) {
            current = current.children.get(0);
        }
        return current;
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

            SkeletonNode tip = buildBranchChain(branchRoot, dir, branchLength, branchRadius);
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

    private static SkeletonNode buildBranchChain(SkeletonNode root, Vec3DFloat dir, int length, float radius) {
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
