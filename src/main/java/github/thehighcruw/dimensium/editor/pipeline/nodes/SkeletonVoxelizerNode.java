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
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;

/** Converts a Skeleton into a BlockMap by stamping spheres along each segment. */
public class SkeletonVoxelizerNode implements PipelineNode {

    public static final String ID = "skeleton_voxelizer";

    private static final List<int[]> DEFAULT_PALETTE = Arrays.asList(new int[] {17, 0});

    private static final NodeSchema SCHEMA = new NodeSchema()
            .paletteParam("vox.palette", DEFAULT_PALETTE, "dimensium.ui.pipeline.vox_palette")
            .description("dimensium.ui.pipeline.node.skeleton_voxelizer.desc")
            .inputPort("skeleton", PortType.SKELETON)
            .optionalInputPort("radiusScale", PortType.FLOAT)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Skeleton skeleton = inputs.get("skeleton", Skeleton.class);
        if (skeleton == null) return;

        List<int[]> palette = params.getPalette("vox.palette", DEFAULT_PALETTE);
        Float radiusScaleInput = inputs.get("radiusScale", Float.class);
        float radiusScale = radiusScaleInput != null ? radiusScaleInput : 1.0f;
        Random rand = new Random(context.nodeSeed(1));

        BlockMap map = new BlockMap();
        for (SkeletonNode root : skeleton.roots) {
            voxelizeNode(root, map, palette, rand, radiusScale);
        }
        outputs.set("blocks", map);
    }

    private static void voxelizeNode(
            SkeletonNode node, BlockMap map, List<int[]> palette, Random rand, float radiusScale) {
        for (SkeletonNode child : node.children) {
            voxelizeSegment(node, child, map, palette, rand, radiusScale);
            voxelizeNode(child, map, palette, rand, radiusScale);
        }
    }

    private static void voxelizeSegment(
            SkeletonNode from, SkeletonNode to, BlockMap map, List<int[]> palette, Random rand, float radiusScale) {
        Vec3DFloat start = from.position.toFloat();
        Vec3DFloat end = to.position.toFloat();
        Vec3DFloat delta = end.minus(start);
        float length = delta.length();

        if (length < 0.001f) {
            stampSphere(from.position, from.radius * radiusScale, map, palette, rand);
            return;
        }

        int steps = Math.max(1, (int) Math.ceil(length * 2));
        for (int step = 0; step <= steps; step++) {
            float t = (float) step / steps;
            Vec3DFloat pos = start.plus(delta.times(t));
            float radius = (from.radius + (to.radius - from.radius) * t) * radiusScale;
            stampSphere(pos.round(), radius, map, palette, rand);
        }
    }

    private static void stampSphere(Vec3DInt center, float radius, BlockMap map, List<int[]> palette, Random rand) {
        int r = Math.max(0, (int) Math.ceil(radius));
        Vec3DInt min = center.minus(r);
        Vec3DInt max = center.plus(r);
        float rSq = radius * radius;

        int[] entry = palette.get(rand.nextInt(palette.size()));
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

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
