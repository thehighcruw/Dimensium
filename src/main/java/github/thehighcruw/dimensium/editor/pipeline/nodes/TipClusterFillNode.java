/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline.nodes;

import github.thehighcruw.dimensium.editor.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import github.thehighcruw.dimensium.editor.pipeline.Skeleton;
import github.thehighcruw.dimensium.editor.pipeline.SkeletonNode;
import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Fills noisy spherical clusters around skeleton tip nodes using a block palette. */
public class TipClusterFillNode implements PipelineNode {

    public static final String ID = "tip_cluster_fill";

    private static final float FILL_THRESHOLD = 0.45f;
    private static final List<int[]> DEFAULT_PALETTE = Arrays.asList(new int[] {18, 4});

    private static final NodeSchema SCHEMA = new NodeSchema()
            .paletteParam("cluster.palette", DEFAULT_PALETTE, "dimensium.ui.pipeline.cluster_palette")
            .floatParam("cluster.radius", 4.0f, 1.0f, 12.0f, "dimensium.ui.pipeline.cluster_radius")
            .floatParam("cluster.noisiness", 0.5f, 0.0f, 1.0f, "dimensium.ui.pipeline.cluster_noisiness")
            .description("dimensium.ui.pipeline.node.tip_cluster_fill.desc")
            .inputPort("blocks", PortType.BLOCK_MAP)
            .inputPort("skeleton", PortType.SKELETON)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap blockMap = inputs.get("blocks", BlockMap.class);
        if (blockMap == null) blockMap = new BlockMap();

        Skeleton skeleton = inputs.get("skeleton", Skeleton.class);
        if (skeleton == null) {
            outputs.set("blocks", blockMap);
            return;
        }

        List<int[]> palette = params.getPalette("cluster.palette", DEFAULT_PALETTE);
        float clusterRadius = params.getFloat("cluster.radius", 4.0f);
        float noisiness = params.getFloat("cluster.noisiness", 0.5f);

        List<Vec3DInt> tips = new ArrayList<>();
        for (SkeletonNode root : skeleton.roots) {
            collectTips(root, tips);
        }

        long seed = context.nodeSeed(2);
        Random rand = new Random(seed);

        for (Vec3DInt tip : tips) {
            stampCluster(tip, clusterRadius, noisiness, rand.nextLong(), palette, blockMap);
        }

        outputs.set("blocks", blockMap);
    }

    private static void collectTips(SkeletonNode node, List<Vec3DInt> tips) {
        if (node.children.isEmpty()) {
            tips.add(node.position);
            return;
        }
        for (SkeletonNode child : node.children) {
            collectTips(child, tips);
        }
    }

    private static void stampCluster(
            Vec3DInt center, float radius, float noisiness, long seed, List<int[]> palette, BlockMap map) {

        int r = (int) Math.ceil(radius);
        Vec3DInt min = center.minus(r);
        Vec3DInt max = center.plus(r);
        float noiseRadius = Math.max(0.01f, radius * 0.25f);
        Random rand = new Random(seed);

        Vec3DInt.forEachInclusive(min, max, pos -> {
            float dx = pos.x() - center.x();
            float dy = pos.y() - center.y();
            float dz = pos.z() - center.z();
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / radius;
            if (dist > 1f) return;

            float sphereDensity = 1f - dist;
            Vec3DFloat noiseCoord = pos.toFloat().divide(noiseRadius);
            float noise = (NoiseSampler.rawSimplex3(noiseCoord.x(), noiseCoord.y(), noiseCoord.z(), seed) + 1f) * 0.5f;
            float density = sphereDensity + (noise - sphereDensity) * noisiness;

            if (density >= FILL_THRESHOLD && !map.contains(pos)) {
                int[] entry = palette.get(rand.nextInt(palette.size()));
                map.put(pos, entry[0], entry[1]);
            }
        });
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
