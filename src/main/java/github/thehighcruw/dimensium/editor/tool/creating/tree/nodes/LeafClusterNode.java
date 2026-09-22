/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.nodes;

import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.Skeleton;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.SkeletonNode;
import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;

public class LeafClusterNode implements PipelineNode<BlockMap, BlockMap> {

    public static final String ID = "leaf_cluster";

    private static final float FILL_THRESHOLD = 0.25f;
    private static final List<int[]> DEFAULT_LEAF_PALETTE = Arrays.asList(new int[] {18, 4});

    private static final NodeSchema SCHEMA = new NodeSchema()
            .paletteParam("leaf.leafPalette", DEFAULT_LEAF_PALETTE, "dimensium.ui.tree.leaf_palette")
            .floatParam("leaf.clusterRadius", 4.0f, 1.0f, 12.0f, "dimensium.ui.tree.leaf_radius")
            .floatParam("leaf.noisiness", 0.5f, 0.0f, 1.0f, "dimensium.ui.tree.leaf_noisiness");

    @Override
    public BlockMap apply(BlockMap blockMap, NodeParams params, PipelineContext context) {
        Skeleton skeleton = context.get(Skeleton.class);
        if (skeleton == null) return blockMap;

        List<int[]> leafPalette = params.getPalette("leaf.leafPalette", DEFAULT_LEAF_PALETTE);
        float clusterRadius = params.getFloat("leaf.clusterRadius", 4.0f);
        float noisiness = params.getFloat("leaf.noisiness", 0.5f);

        List<Vec3DInt> tips = new ArrayList<>();
        collectTips(skeleton.root, tips);

        long seed = context.nodeSeed(2);
        Random rand = new Random(seed);

        for (Vec3DInt tip : tips) {
            stampLeafCluster(tip, clusterRadius, noisiness, rand.nextLong(), leafPalette, blockMap);
        }

        return blockMap;
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

    private static void stampLeafCluster(
            Vec3DInt center, float radius, float noisiness, long seed, List<int[]> palette, BlockMap map) {

        int r = (int) Math.ceil(radius);
        Vec3DInt min = center.minus(r);
        Vec3DInt max = center.plus(r);
        float noiseRadius = Math.max(0.01f, radius * 0.5f);
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
                Block leafBlock = Block.getBlockById(entry[0]);
                map.put(pos, leafBlock, entry[1]);
            }
        });
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
