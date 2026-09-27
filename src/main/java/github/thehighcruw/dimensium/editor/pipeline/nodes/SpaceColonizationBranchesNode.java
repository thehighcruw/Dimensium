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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Grows space-colonization branches from all leaf nodes of the input skeleton.
 * Attractors are scattered in an ellipsoid whose center is offset from the skeleton's
 * highest tip position, producing an organic crown shape.
 */
public class SpaceColonizationBranchesNode implements PipelineNode {

    public static final String ID = "space_colonization_branches";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("sc.attractorCount", 200, 50, 1000, "dimensium.ui.pipeline.sc_attractor_count")
            .floatParam("sc.crownRadiusX", 8.0f, 2.0f, 20.0f, "dimensium.ui.pipeline.sc_crown_radius_x")
            .floatParam("sc.crownRadiusY", 6.0f, 2.0f, 20.0f, "dimensium.ui.pipeline.sc_crown_radius_y")
            .floatParam("sc.crownRadiusZ", 8.0f, 2.0f, 20.0f, "dimensium.ui.pipeline.sc_crown_radius_z")
            .floatParam("sc.crownOffsetY", 4.0f, 0.0f, 20.0f, "dimensium.ui.pipeline.sc_crown_offset_y")
            .floatParam("sc.influenceRadius", 6.0f, 1.0f, 15.0f, "dimensium.ui.pipeline.sc_influence_radius")
            .floatParam("sc.killRadius", 2.0f, 0.5f, 5.0f, "dimensium.ui.pipeline.sc_kill_radius")
            .floatParam("sc.stepSize", 1.0f, 0.5f, 3.0f, "dimensium.ui.pipeline.sc_step_size")
            .intParam("sc.maxIterations", 80, 10, 200, "dimensium.ui.pipeline.sc_max_iterations")
            .floatParam("sc.branchRadius", 0.8f, 0.2f, 3.0f, "dimensium.ui.pipeline.sc_branch_radius")
            .description("dimensium.ui.pipeline.node.space_colonization_branches.desc")
            .inputPort("skeleton", PortType.SKELETON)
            .outputPort("skeleton", PortType.SKELETON);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Skeleton skeleton = inputs.get("skeleton", Skeleton.class);
        if (skeleton == null) return;

        float crownRX = params.getFloat("sc.crownRadiusX", 8.0f);
        float crownRY = params.getFloat("sc.crownRadiusY", 6.0f);
        float crownRZ = params.getFloat("sc.crownRadiusZ", 8.0f);
        float crownOffsetY = params.getFloat("sc.crownOffsetY", 4.0f);
        float influenceRadius = params.getFloat("sc.influenceRadius", 6.0f);
        float killRadius = params.getFloat("sc.killRadius", 2.0f);
        float stepSize = params.getFloat("sc.stepSize", 1.0f);
        int maxIterations = params.getInt("sc.maxIterations", 80);
        float branchRadius = params.getFloat("sc.branchRadius", 0.8f);
        int attractorCount = params.getInt("sc.attractorCount", 200);

        Random rand = new Random(context.nodeSeed(0));

        // Collect all nodes and find the highest tip for crown placement
        List<SkeletonNode> allNodes = new ArrayList<>();
        for (SkeletonNode root : skeleton.roots) {
            collectAllNodes(root, allNodes);
        }

        Vec3DFloat highestTip = findHighestTip(skeleton.roots);
        Vec3DFloat crownCenter = Vec3DFloat.from(highestTip.x(), highestTip.y() + crownOffsetY, highestTip.z());
        List<Vec3DFloat> attractors = scatterAttractors(attractorCount, crownCenter, crownRX, crownRY, crownRZ, rand);

        float influenceRadSq = influenceRadius * influenceRadius;
        float killRadSq = killRadius * killRadius;

        for (int iteration = 0; iteration < maxIterations && !attractors.isEmpty(); iteration++) {
            int[] nearestIndex = new int[attractors.size()];
            float[] nearestDist = new float[attractors.size()];
            for (int ai = 0; ai < attractors.size(); ai++) {
                nearestIndex[ai] = -1;
                nearestDist[ai] = Float.MAX_VALUE;
            }

            for (int ni = 0; ni < allNodes.size(); ni++) {
                Vec3DFloat nodePos = allNodes.get(ni).position.toFloat();
                for (int ai = 0; ai < attractors.size(); ai++) {
                    float distSq = distSq(nodePos, attractors.get(ai));
                    if (distSq < influenceRadSq && distSq < nearestDist[ai]) {
                        nearestDist[ai] = distSq;
                        nearestIndex[ai] = ni;
                    }
                }
            }

            Vec3DFloat[] growthDirs = new Vec3DFloat[allNodes.size()];
            for (int ai = 0; ai < attractors.size(); ai++) {
                int ni = nearestIndex[ai];
                if (ni < 0) continue;
                Vec3DFloat dir = attractors
                        .get(ai)
                        .minus(allNodes.get(ni).position.toFloat())
                        .normalize();
                growthDirs[ni] = growthDirs[ni] == null ? dir : growthDirs[ni].plus(dir);
            }

            int prevSize = allNodes.size();
            boolean grew = false;
            for (int ni = 0; ni < prevSize; ni++) {
                if (growthDirs[ni] == null) continue;
                Vec3DFloat dir = growthDirs[ni].normalize();
                Vec3DFloat newPos = allNodes.get(ni).position.toFloat().plus(dir.times(stepSize));
                SkeletonNode newNode = new SkeletonNode(newPos.round(), branchRadius);
                allNodes.get(ni).children.add(newNode);
                allNodes.add(newNode);
                grew = true;
            }
            if (!grew) break;

            Iterator<Vec3DFloat> it = attractors.iterator();
            while (it.hasNext()) {
                Vec3DFloat attractor = it.next();
                for (SkeletonNode node : allNodes) {
                    if (distSq(node.position.toFloat(), attractor) <= killRadSq) {
                        it.remove();
                        break;
                    }
                }
            }
        }

        outputs.set("skeleton", skeleton);
    }

    private static Vec3DFloat findHighestTip(List<SkeletonNode> roots) {
        Vec3DFloat highest = Vec3DFloat.from(0, 0, 0);
        for (SkeletonNode root : roots) {
            Vec3DFloat tip = findHighestTipRecursive(root);
            if (tip.y() > highest.y()) highest = tip;
        }
        return highest;
    }

    private static Vec3DFloat findHighestTipRecursive(SkeletonNode node) {
        if (node.children.isEmpty()) return node.position.toFloat();
        Vec3DFloat highest = node.position.toFloat();
        for (SkeletonNode child : node.children) {
            Vec3DFloat candidate = findHighestTipRecursive(child);
            if (candidate.y() > highest.y()) highest = candidate;
        }
        return highest;
    }

    private static void collectAllNodes(SkeletonNode node, List<SkeletonNode> result) {
        result.add(node);
        for (SkeletonNode child : node.children) {
            collectAllNodes(child, result);
        }
    }

    private static List<Vec3DFloat> scatterAttractors(
            int count, Vec3DFloat center, float rx, float ry, float rz, Random rand) {
        List<Vec3DFloat> result = new ArrayList<>(count);
        int attempts = 0;
        while (result.size() < count && attempts < count * 10) {
            attempts++;
            float x = (rand.nextFloat() * 2f - 1f) * rx;
            float y = (rand.nextFloat() * 2f - 1f) * ry;
            float z = (rand.nextFloat() * 2f - 1f) * rz;
            if ((x * x) / (rx * rx) + (y * y) / (ry * ry) + (z * z) / (rz * rz) <= 1f) {
                result.add(center.plus(Vec3DFloat.from(x, y, z)));
            }
        }
        return result;
    }

    private static float distSq(Vec3DFloat a, Vec3DFloat b) {
        float dx = a.x() - b.x();
        float dy = a.y() - b.y();
        float dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
