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
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.Random;

public class WeberPennSkeletonNode implements PipelineNode {

    public static final String ID = "weber_penn_skeleton";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("wp.trunkHeight", 12, 4, 40, "dimensium.ui.pipeline.trunk_height")
            .floatParam("wp.trunkRadius", 2.0f, 0.5f, 6.0f, "dimensium.ui.pipeline.trunk_radius")
            .floatParam("wp.trunkTaper", 0.7f, 0.1f, 1.0f, "dimensium.ui.pipeline.trunk_taper")
            .intParam("wp.levels", 2, 1, 4, "dimensium.ui.pipeline.levels")
            .intParam("wp.branchCount", 4, 1, 8, "dimensium.ui.pipeline.branch_count")
            .floatParam("wp.branchAngleSpread", 45.0f, 5.0f, 80.0f, "dimensium.ui.pipeline.branch_angle_spread")
            .floatParam("wp.branchLengthRatio", 0.55f, 0.2f, 0.9f, "dimensium.ui.pipeline.branch_length_ratio")
            .floatParam("wp.branchStartHeight", 0.55f, 0.1f, 0.9f, "dimensium.ui.pipeline.branch_start_height")
            .floatParam("wp.branchRadiusFactor", 0.5f, 0.2f, 0.8f, "dimensium.ui.pipeline.branch_radius_factor")
            .description("dimensium.ui.pipeline.node.weber_penn_skeleton.desc")
            .outputPort("skeleton", PortType.SKELETON);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt origin = inputs.get("origin", Vec3DInt.class);
        if (origin == null) origin = context.origin;

        int trunkHeight = params.getInt("wp.trunkHeight", 12);
        float trunkRadius = params.getFloat("wp.trunkRadius", 2.0f);
        float trunkTaper = params.getFloat("wp.trunkTaper", 0.7f);
        int levels = params.getInt("wp.levels", 2);
        int branchCount = params.getInt("wp.branchCount", 4);
        float branchAngleSpread = params.getFloat("wp.branchAngleSpread", 45.0f);
        float branchLengthRatio = params.getFloat("wp.branchLengthRatio", 0.55f);
        float branchStartHeight = params.getFloat("wp.branchStartHeight", 0.55f);
        float branchRadiusFactor = params.getFloat("wp.branchRadiusFactor", 0.5f);

        Random rand = new Random(context.nodeSeed(0));

        SkeletonNode root = buildTrunk(origin, trunkHeight, trunkRadius, trunkTaper);

        int branchStart = Math.round(trunkHeight * branchStartHeight);
        attachBranches(
                root,
                branchStart,
                trunkHeight,
                trunkRadius,
                trunkTaper,
                levels,
                branchCount,
                branchAngleSpread,
                branchLengthRatio,
                branchRadiusFactor,
                rand);

        outputs.set("skeleton", new Skeleton(root));
    }

    private static SkeletonNode buildTrunk(Vec3DInt origin, int height, float baseRadius, float taper) {
        SkeletonNode current = new SkeletonNode(origin, baseRadius);
        SkeletonNode head = current;
        for (int y = 1; y <= height; y++) {
            float radiusAtY = baseRadius * (1f - (1f - taper) * ((float) y / height));
            SkeletonNode next = new SkeletonNode(origin.plus(0, y, 0), radiusAtY);
            current.children.add(next);
            current = next;
        }
        return head;
    }

    private static void attachBranches(
            SkeletonNode trunkRoot,
            int branchStart,
            int trunkHeight,
            float trunkRadius,
            float trunkTaper,
            int levels,
            int branchCount,
            float angleSpreadDeg,
            float lengthRatio,
            float radiusFactor,
            Random rand) {

        if (levels <= 0) return;

        SkeletonNode current = trunkRoot;
        int y = 0;
        while (current != null) {
            if (y >= branchStart && !current.children.isEmpty()) {
                float parentLength = trunkHeight - y;
                float branchLength = parentLength * lengthRatio;
                if (branchLength < 1f) break;

                float parentRadiusAtY = trunkRadius * (1f - (1f - trunkTaper) * ((float) y / trunkHeight));
                float branchRadius = parentRadiusAtY * radiusFactor;
                float spreadRad = (float) Math.toRadians(angleSpreadDeg);

                for (int branchIndex = 0; branchIndex < branchCount; branchIndex++) {
                    float azimuth =
                            (float) (2 * Math.PI * branchIndex / branchCount) + (rand.nextFloat() - 0.5f) * 0.5f;
                    float polar = spreadRad * (0.5f + rand.nextFloat() * 0.5f);
                    Vec3DFloat dir = sphericalToDir(azimuth, polar);

                    SkeletonNode branchRoot = new SkeletonNode(current.position, branchRadius);
                    buildBranchChain(
                            branchRoot,
                            dir,
                            Math.round(branchLength),
                            branchRadius,
                            lengthRatio,
                            levels - 1,
                            branchCount,
                            angleSpreadDeg,
                            radiusFactor,
                            rand);
                    current.children.add(branchRoot);
                }
                break;
            }
            current = current.children.isEmpty() ? null : current.children.get(0);
            y++;
        }
    }

    private static void buildBranchChain(
            SkeletonNode branchRoot,
            Vec3DFloat direction,
            int length,
            float radius,
            float lengthRatio,
            int remainingLevels,
            int branchCount,
            float angleSpreadDeg,
            float radiusFactor,
            Random rand) {

        if (length < 1) return;

        SkeletonNode current = branchRoot;
        Vec3DFloat pos = branchRoot.position.toFloat();
        float radiusTaper = 0.6f;

        for (int step = 1; step <= length; step++) {
            pos = pos.plus(direction);
            float radiusAtStep = radius * (1f - (1f - radiusTaper) * ((float) step / length));
            SkeletonNode next = new SkeletonNode(pos.round(), radiusAtStep);
            current.children.add(next);
            current = next;
        }

        if (remainingLevels > 0 && length > 2) {
            float subLength = length * lengthRatio;
            float subRadius = radius * radiusFactor;
            float spreadRad = (float) Math.toRadians(angleSpreadDeg);

            for (int branchIndex = 0; branchIndex < branchCount; branchIndex++) {
                float azimuth = (float) (2 * Math.PI * branchIndex / branchCount) + (rand.nextFloat() - 0.5f) * 0.5f;
                float polar = spreadRad * (0.5f + rand.nextFloat() * 0.5f);
                Vec3DFloat subDir = perturbDirection(direction, azimuth, polar);

                SkeletonNode subRoot = new SkeletonNode(current.position, subRadius);
                buildBranchChain(
                        subRoot,
                        subDir,
                        Math.round(subLength),
                        subRadius,
                        lengthRatio,
                        remainingLevels - 1,
                        branchCount,
                        angleSpreadDeg,
                        radiusFactor,
                        rand);
                current.children.add(subRoot);
            }
        }
    }

    private static Vec3DFloat sphericalToDir(float azimuth, float polar) {
        float x = (float) (Math.sin(polar) * Math.cos(azimuth));
        float y = (float) Math.cos(polar);
        float z = (float) (Math.sin(polar) * Math.sin(azimuth));
        return Vec3DFloat.from(x, y, z);
    }

    private static Vec3DFloat perturbDirection(Vec3DFloat parentDir, float azimuth, float polar) {
        Vec3DFloat up = Vec3DFloat.from(0, 1, 0);
        Vec3DFloat right;
        float dot = parentDir.dot(up);
        if (Math.abs(dot) > 0.9f) {
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
