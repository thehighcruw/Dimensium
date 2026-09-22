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
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.block.Block;

public class CylinderVoxelizerNode implements PipelineNode<Skeleton, BlockMap> {

    public static final String ID = "cylinder_voxelizer";

    // Minecraft log block ID (17)
    private static final int DEFAULT_LOG_BLOCK_ID = 17;

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("vox.logBlockId", DEFAULT_LOG_BLOCK_ID, 0, 4096, "dimensium.ui.tree.log_block")
            .intParam("vox.logMeta", 0, 0, 15, "dimensium.ui.tree.log_meta");

    @Override
    public BlockMap apply(Skeleton skeleton, NodeParams params) {
        // Inject skeleton context for LeafClusterNode which runs after this
        LeafClusterNode.SKELETON_CONTEXT.set(skeleton);

        int logBlockId = params.getInt("vox.logBlockId", DEFAULT_LOG_BLOCK_ID);
        int logMeta = params.getInt("vox.logMeta", 0);
        Block logBlock = Block.getBlockById(logBlockId);

        BlockMap map = new BlockMap();
        voxelizeNode(skeleton.root, map, logBlock, logMeta);
        return map;
    }

    private static void voxelizeNode(SkeletonNode node, BlockMap map, Block logBlock, int logMeta) {
        for (SkeletonNode child : node.children) {
            voxelizeSegment(node, child, map, logBlock, logMeta);
            voxelizeNode(child, map, logBlock, logMeta);
        }
    }

    private static void voxelizeSegment(SkeletonNode from, SkeletonNode to, BlockMap map, Block logBlock, int logMeta) {
        Vec3DFloat start = from.position.toFloat();
        Vec3DFloat end = to.position.toFloat();
        Vec3DFloat delta = end.minus(start);
        float length = delta.length();

        if (length < 0.001f) {
            stampSphere(from.position, from.radius, map, logBlock, logMeta);
            return;
        }

        int steps = Math.max(1, (int) Math.ceil(length * 2));
        for (int step = 0; step <= steps; step++) {
            float t = (float) step / steps;
            Vec3DFloat pos = start.plus(delta.times(t));
            float radius = from.radius + (to.radius - from.radius) * t;
            stampSphere(pos.round(), radius, map, logBlock, logMeta);
        }
    }

    private static void stampSphere(Vec3DInt center, float radius, BlockMap map, Block block, int meta) {
        int r = Math.max(0, (int) Math.ceil(radius));
        Vec3DInt min = center.minus(r);
        Vec3DInt max = center.plus(r);
        float rSq = radius * radius;

        Vec3DInt.forEachInclusive(min, max, pos -> {
            float dx = pos.x() - center.x();
            float dy = pos.y() - center.y();
            float dz = pos.z() - center.z();
            if (dx * dx + dy * dy + dz * dz <= rSq) {
                map.put(pos, block, meta);
            }
        });
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
