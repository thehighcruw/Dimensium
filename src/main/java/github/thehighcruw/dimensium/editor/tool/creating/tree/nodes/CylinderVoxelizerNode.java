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
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;

public class CylinderVoxelizerNode implements PipelineNode<Skeleton, BlockMap> {

    public static final String ID = "cylinder_voxelizer";

    private static final List<int[]> DEFAULT_LOG_PALETTE = Arrays.asList(new int[] {17, 0});

    private static final NodeSchema SCHEMA =
            new NodeSchema().paletteParam("vox.logPalette", DEFAULT_LOG_PALETTE, "dimensium.ui.tree.log_palette");

    @Override
    public BlockMap apply(Skeleton skeleton, NodeParams params, PipelineContext context) {
        List<int[]> logPalette = params.getPalette("vox.logPalette", DEFAULT_LOG_PALETTE);
        Random rand = new Random(context.nodeSeed(1));

        BlockMap map = new BlockMap();
        voxelizeNode(skeleton.root, map, logPalette, rand);
        return map;
    }

    private static void voxelizeNode(SkeletonNode node, BlockMap map, List<int[]> palette, Random rand) {
        for (SkeletonNode child : node.children) {
            voxelizeSegment(node, child, map, palette, rand);
            voxelizeNode(child, map, palette, rand);
        }
    }

    private static void voxelizeSegment(
            SkeletonNode from, SkeletonNode to, BlockMap map, List<int[]> palette, Random rand) {
        Vec3DFloat start = from.position.toFloat();
        Vec3DFloat end = to.position.toFloat();
        Vec3DFloat delta = end.minus(start);
        float length = delta.length();

        if (length < 0.001f) {
            stampSphere(from.position, from.radius, map, palette, rand);
            return;
        }

        int steps = Math.max(1, (int) Math.ceil(length * 2));
        for (int step = 0; step <= steps; step++) {
            float t = (float) step / steps;
            Vec3DFloat pos = start.plus(delta.times(t));
            float radius = from.radius + (to.radius - from.radius) * t;
            stampSphere(pos.round(), radius, map, palette, rand);
        }
    }

    private static void stampSphere(Vec3DInt center, float radius, BlockMap map, List<int[]> palette, Random rand) {
        int r = Math.max(0, (int) Math.ceil(radius));
        Vec3DInt min = center.minus(r);
        Vec3DInt max = center.plus(r);
        float rSq = radius * radius;

        int[] entry = samplePalette(palette, rand);
        Block block = Block.getBlockById(entry[0]);
        int meta = entry[1];

        Vec3DInt.forEachInclusive(min, max, pos -> {
            float dx = pos.x() - center.x();
            float dy = pos.y() - center.y();
            float dz = pos.z() - center.z();
            if (dx * dx + dy * dy + dz * dz <= rSq) {
                map.put(pos, block, meta);
            }
        });
    }

    private static int[] samplePalette(List<int[]> palette, Random rand) {
        if (palette.isEmpty()) return new int[] {17, 0};
        return palette.get(rand.nextInt(palette.size()));
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
