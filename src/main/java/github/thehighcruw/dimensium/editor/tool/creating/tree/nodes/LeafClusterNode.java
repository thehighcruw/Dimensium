/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.nodes;

import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.Skeleton;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.SkeletonNode;
import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;

public class LeafClusterNode implements PipelineNode<BlockMap, BlockMap> {

    public static final String ID = "leaf_cluster";

    // Minecraft leaves block ID (18)
    private static final int DEFAULT_LEAF_BLOCK_ID = 18;
    private static final float FILL_THRESHOLD = 0.25f;

    // Skeleton is injected here by CylinderVoxelizerNode before this node runs
    static final ThreadLocal<Skeleton> SKELETON_CONTEXT = new ThreadLocal<>();

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("leaf.leafBlockId", DEFAULT_LEAF_BLOCK_ID, 0, 4096, "dimensium.ui.tree.leaf_block")
            .intParam("leaf.leafMeta", 0, 0, 15, "dimensium.ui.tree.leaf_meta")
            .floatParam("leaf.clusterRadius", 4.0f, 1.0f, 12.0f, "dimensium.ui.tree.leaf_radius")
            .floatParam("leaf.noisiness", 0.5f, 0.0f, 1.0f, "dimensium.ui.tree.leaf_noisiness")
            .longParam("leaf.seed", 54321L, "dimensium.ui.tree.leaf_seed");

    @Override
    public BlockMap apply(BlockMap blockMap, NodeParams params) {
        Skeleton skeleton = SKELETON_CONTEXT.get();
        if (skeleton == null) return blockMap;

        int leafBlockId = params.getInt("leaf.leafBlockId", DEFAULT_LEAF_BLOCK_ID);
        int leafMeta = params.getInt("leaf.leafMeta", 0);
        Block leafBlock = Block.getBlockById(leafBlockId);
        float clusterRadius = params.getFloat("leaf.clusterRadius", 4.0f);
        float noisiness = params.getFloat("leaf.noisiness", 0.5f);
        long seed = params.getLong("leaf.seed", 54321L);

        List<Vec3DInt> tips = new ArrayList<>();
        collectTips(skeleton.root, tips);

        for (Vec3DInt tip : tips) {
            stampLeafCluster(tip, clusterRadius, noisiness, seed, leafBlock, leafMeta, blockMap);
            seed = seed * 6364136223846793005L + 1442695040888963407L;
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
            Vec3DInt center, float radius, float noisiness, long seed, Block leafBlock, int leafMeta, BlockMap map) {

        int r = (int) Math.ceil(radius);
        Vec3DInt min = center.minus(r);
        Vec3DInt max = center.plus(r);
        float noiseRadius = Math.max(0.01f, radius * 0.5f);

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
                map.put(pos, leafBlock, leafMeta);
            }
        });
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
