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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SpaceColonizationSkeletonNode implements PipelineNode {

    public static final String ID = "space_colonization_skeleton";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("sc.attractorCount", 200, 50, 1000, "dimensium.ui.pipeline.sc_attractor_count")
            .floatParam("sc.crownRadiusX", 8.0f, 2.0f, 20.0f, "dimensium.ui.pipeline.sc_crown_radius_x")
            .floatParam("sc.crownRadiusY", 6.0f, 2.0f, 20.0f, "dimensium.ui.pipeline.sc_crown_radius_y")
            .floatParam("sc.crownRadiusZ", 8.0f, 2.0f, 20.0f, "dimensium.ui.pipeline.sc_crown_radius_z")
            .floatParam("sc.crownOffsetY", 10.0f, 2.0f, 30.0f, "dimensium.ui.pipeline.sc_crown_offset_y")
            .floatParam("sc.influenceRadius", 6.0f, 1.0f, 15.0f, "dimensium.ui.pipeline.sc_influence_radius")
            .floatParam("sc.killRadius", 2.0f, 0.5f, 5.0f, "dimensium.ui.pipeline.sc_kill_radius")
            .floatParam("sc.stepSize", 1.0f, 0.5f, 3.0f, "dimensium.ui.pipeline.sc_step_size")
            .intParam("sc.maxIterations", 80, 10, 200, "dimensium.ui.pipeline.sc_max_iterations")
            .intParam("sc.trunkHeight", 6, 1, 20, "dimensium.ui.pipeline.sc_trunk_height")
            .floatParam("sc.branchRadius", 0.8f, 0.2f, 3.0f, "dimensium.ui.pipeline.sc_branch_radius")
            .description("dimensium.ui.pipeline.node.space_colonization_skeleton.desc")
            .outputPort("skeleton", PortType.SKELETON);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt origin = inputs.get("origin", Vec3DInt.class);
        if (origin == null) origin = context.origin;

        int attractorCount = params.getInt("sc.attractorCount", 200);
        float crownRX = params.getFloat("sc.crownRadiusX", 8.0f);
        float crownRY = params.getFloat("sc.crownRadiusY", 6.0f);
        float crownRZ = params.getFloat("sc.crownRadiusZ", 8.0f);
        float crownOffsetY = params.getFloat("sc.crownOffsetY", 10.0f);
        float influenceRadius = params.getFloat("sc.influenceRadius", 6.0f);
        float killRadius = params.getFloat("sc.killRadius", 2.0f);
        float stepSize = params.getFloat("sc.stepSize", 1.0f);
        int maxIterations = params.getInt("sc.maxIterations", 80);
        int trunkHeight = params.getInt("sc.trunkHeight", 6);
        float branchRadius = params.getFloat("sc.branchRadius", 0.8f);

        Random rand = new Random(context.nodeSeed(0));

        SkeletonNode root = buildTrunk(origin, trunkHeight, branchRadius);
        List<SkeletonNode> allNodes = collectAllNodes(root);

        Vec3DFloat crownCenter = Vec3DFloat.from(origin.x(), origin.y() + crownOffsetY, origin.z());
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
                if (growthDirs[ni] == null) {
                    growthDirs[ni] = dir;
                } else {
                    growthDirs[ni] = growthDirs[ni].plus(dir);
                }
            }

            boolean grew = false;
            int prevSize = allNodes.size();
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
                for (int ni = 0; ni < allNodes.size(); ni++) {
                    if (distSq(allNodes.get(ni).position.toFloat(), attractor) <= killRadSq) {
                        it.remove();
                        break;
                    }
                }
            }
        }

        outputs.set("skeleton", new Skeleton(root));
    }

    private static SkeletonNode buildTrunk(Vec3DInt origin, int height, float radius) {
        SkeletonNode current = new SkeletonNode(origin, radius * 1.5f);
        SkeletonNode head = current;
        for (int y = 1; y <= height; y++) {
            float r = radius * (1f - 0.2f * ((float) y / height));
            SkeletonNode next = new SkeletonNode(origin.plus(0, y, 0), r);
            current.children.add(next);
            current = next;
        }
        return head;
    }

    private static List<SkeletonNode> collectAllNodes(SkeletonNode root) {
        List<SkeletonNode> result = new ArrayList<>();
        collectRecursive(root, result);
        return result;
    }

    private static void collectRecursive(SkeletonNode node, List<SkeletonNode> result) {
        result.add(node);
        for (SkeletonNode child : node.children) {
            collectRecursive(child, result);
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
