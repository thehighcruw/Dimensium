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
 * Attaches radial whorls of branches at regular intervals along a trunk skeleton,
 * producing the characteristic cone silhouette of conifers. Lower whorls are longer
 * and more horizontal; upper whorls are shorter and steeper.
 */
public class WhorlBranchesNode implements PipelineNode {

    public static final String ID = "whorl_branches";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("whorl.branchesPerWhorl", 6, 2, 12, "dimensium.ui.pipeline.whorl_branches_per_whorl")
            .floatParam("whorl.baseAngle", 80.0f, 10.0f, 90.0f, "dimensium.ui.pipeline.whorl_base_angle")
            .floatParam("whorl.tipAngle", 25.0f, 5.0f, 85.0f, "dimensium.ui.pipeline.whorl_tip_angle")
            .intParam("whorl.baseLength", 8, 1, 20, "dimensium.ui.pipeline.whorl_base_length")
            .intParam("whorl.tipLength", 2, 1, 10, "dimensium.ui.pipeline.whorl_tip_length")
            .floatParam("whorl.branchRadius", 0.4f, 0.1f, 1.5f, "dimensium.ui.pipeline.whorl_branch_radius")
            .intParam("whorl.spacing", 2, 1, 6, "dimensium.ui.pipeline.whorl_spacing")
            .intParam("whorl.skipBase", 2, 0, 8, "dimensium.ui.pipeline.whorl_skip_base")
            .intParam("whorl.skipTip", 3, 0, 12, "dimensium.ui.pipeline.whorl_skip_tip")
            .floatParam("whorl.spiralOffset", 0.618f, 0.0f, 1.0f, "dimensium.ui.pipeline.whorl_spiral_offset")
            .description("dimensium.ui.pipeline.node.whorl_branches.desc")
            .inputPort("skeleton", PortType.SKELETON)
            .outputPort("skeleton", PortType.SKELETON);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Skeleton skeleton = inputs.get("skeleton", Skeleton.class);
        if (skeleton == null) return;

        int branchesPerWhorl = params.getInt("whorl.branchesPerWhorl", 6);
        float baseAngle = params.getFloat("whorl.baseAngle", 80.0f);
        float tipAngle = params.getFloat("whorl.tipAngle", 25.0f);
        int baseLength = params.getInt("whorl.baseLength", 8);
        int tipLength = params.getInt("whorl.tipLength", 2);
        float branchRadius = params.getFloat("whorl.branchRadius", 0.4f);
        int spacing = params.getInt("whorl.spacing", 2);
        int skipBase = params.getInt("whorl.skipBase", 2);
        int skipTip = params.getInt("whorl.skipTip", 3);
        float spiralOffset = params.getFloat("whorl.spiralOffset", 0.618f);

        Random rand = new Random(context.nodeSeed(0));

        for (SkeletonNode root : skeleton.roots) {
            List<SkeletonNode> chain = new ArrayList<>();
            collectTrunkChain(root, chain);
            attachWhorls(
                    chain,
                    branchesPerWhorl,
                    baseAngle,
                    tipAngle,
                    baseLength,
                    tipLength,
                    branchRadius,
                    spacing,
                    skipBase,
                    skipTip,
                    spiralOffset,
                    rand);
        }

        outputs.set("skeleton", skeleton);
    }

    private static void collectTrunkChain(SkeletonNode node, List<SkeletonNode> result) {
        result.add(node);
        // Only follow single-child chains — stops if trunk already branches
        if (node.children.size() == 1) {
            collectTrunkChain(node.children.get(0), result);
        }
    }

    private static void attachWhorls(
            List<SkeletonNode> chain,
            int branchesPerWhorl,
            float baseAngle,
            float tipAngle,
            int baseLength,
            int tipLength,
            float branchRadius,
            int spacing,
            int skipBase,
            int skipTip,
            float spiralOffset,
            Random rand) {

        int totalNodes = chain.size();
        if (totalNodes < 2) return;

        int lastIdx = totalNodes - 1 - skipTip;
        if (lastIdx < skipBase) return;

        // Count how many whorl positions exist so we can compute t correctly
        int whorlIndex = 0;
        int whorlCount = 0;
        for (int i = skipBase; i <= lastIdx; i += spacing) whorlCount++;
        if (whorlCount == 0) return;

        for (int nodeIdx = skipBase; nodeIdx <= lastIdx; nodeIdx += spacing) {
            SkeletonNode node = chain.get(nodeIdx);
            float t = whorlCount > 1 ? (float) whorlIndex / (whorlCount - 1) : 0f;

            Vec3DFloat trunkAxis = trunkAxisAt(chain, nodeIdx);
            float angleRad = (float) Math.toRadians(baseAngle + (tipAngle - baseAngle) * t);
            int branchLen = Math.max(1, Math.round(baseLength + (tipLength - baseLength) * t));
            float whorlAzimuthBase = (float) (2 * Math.PI * spiralOffset * whorlIndex);

            for (int i = 0; i < branchesPerWhorl; i++) {
                float azimuth = (float) (2 * Math.PI * i / branchesPerWhorl) + whorlAzimuthBase;
                Vec3DFloat dir = branchDirection(trunkAxis, azimuth, angleRad);

                SkeletonNode branchStart = new SkeletonNode(node.position, branchRadius);
                node.children.add(branchStart);
                buildChain(branchStart, dir, branchLen, branchRadius);
            }

            whorlIndex++;
        }
    }

    private static Vec3DFloat trunkAxisAt(List<SkeletonNode> chain, int nodeIdx) {
        Vec3DFloat axis;
        if (nodeIdx < chain.size() - 1) {
            Vec3DFloat current = chain.get(nodeIdx).position.toFloat();
            Vec3DFloat next = chain.get(nodeIdx + 1).position.toFloat();
            axis = next.minus(current).normalize();
        } else {
            Vec3DFloat prev = chain.get(nodeIdx - 1).position.toFloat();
            Vec3DFloat current = chain.get(nodeIdx).position.toFloat();
            axis = current.minus(prev).normalize();
        }
        return axis.length() < 0.001f ? Vec3DFloat.from(0, 1, 0) : axis;
    }

    private static Vec3DFloat branchDirection(Vec3DFloat trunkAxis, float azimuth, float angleFromAxis) {
        Vec3DFloat up = Vec3DFloat.from(0, 1, 0);
        Vec3DFloat right;
        if (Math.abs(trunkAxis.dot(up)) > 0.9f) {
            right = trunkAxis.cross(Vec3DFloat.from(1, 0, 0)).normalize();
        } else {
            right = trunkAxis.cross(up).normalize();
        }
        Vec3DFloat forward = right.cross(trunkAxis).normalize();

        float sinA = (float) Math.sin(angleFromAxis);
        float cosA = (float) Math.cos(angleFromAxis);

        return trunkAxis
                .times(cosA)
                .plus(right.times(sinA * (float) Math.cos(azimuth)))
                .plus(forward.times(sinA * (float) Math.sin(azimuth)))
                .normalize();
    }

    private static void buildChain(SkeletonNode root, Vec3DFloat dir, int length, float radius) {
        SkeletonNode current = root;
        Vec3DFloat pos = root.position.toFloat();
        for (int step = 1; step <= length; step++) {
            pos = pos.plus(dir);
            float r = radius * (1f - 0.5f * ((float) step / length));
            SkeletonNode next = new SkeletonNode(pos.round(), r);
            current.children.add(next);
            current = next;
        }
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
