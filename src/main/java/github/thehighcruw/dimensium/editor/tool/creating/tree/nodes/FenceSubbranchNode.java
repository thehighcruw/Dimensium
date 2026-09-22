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
import net.minecraft.block.Block;

public class FenceSubbranchNode implements PipelineNode<BlockMap, BlockMap> {

    public static final String ID = "fence_subbranch";

    private static final List<int[]> DEFAULT_FENCE_PALETTE = Arrays.asList(new int[] {85, 0});

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("fence.minDepth", 2, 1, 5, "dimensium.ui.tree.fence_min_depth")
            .paletteParam("fence.fencePalette", DEFAULT_FENCE_PALETTE, "dimensium.ui.tree.fence_palette");

    @Override
    public BlockMap apply(BlockMap blockMap, NodeParams params, PipelineContext context) {
        Skeleton skeleton = context.get(Skeleton.class);
        if (skeleton == null) return blockMap;

        int minDepth = params.getInt("fence.minDepth", 2);
        List<int[]> palette = params.getPalette("fence.fencePalette", DEFAULT_FENCE_PALETTE);
        if (palette.isEmpty()) return blockMap;

        int[] fenceEntry = palette.get(0);
        Block fenceBlock = Block.getBlockById(fenceEntry[0]);
        int fenceMeta = fenceEntry[1];

        stampFenceBranches(skeleton.root, null, 0, minDepth, fenceBlock, fenceMeta, blockMap);
        return blockMap;
    }

    private static void stampFenceBranches(
            SkeletonNode node,
            SkeletonNode parent,
            int depth,
            int minDepth,
            Block fenceBlock,
            int fenceMeta,
            BlockMap map) {

        if (depth >= minDepth && parent != null) {
            stampFenceLine(parent.position, node.position, fenceBlock, fenceMeta, map);
        }
        for (SkeletonNode child : node.children) {
            stampFenceBranches(child, node, depth + 1, minDepth, fenceBlock, fenceMeta, map);
        }
    }

    private static void stampFenceLine(Vec3DInt from, Vec3DInt to, Block block, int meta, BlockMap map) {
        Vec3DFloat start = from.toFloat();
        Vec3DFloat delta = to.toFloat().minus(start);
        float length = delta.length();
        if (length < 0.001f) {
            map.put(from, block, meta);
            return;
        }
        int steps = Math.max(1, (int) Math.ceil(length));
        for (int step = 0; step <= steps; step++) {
            float t = (float) step / steps;
            Vec3DFloat pos = start.plus(delta.times(t));
            map.put(Vec3DInt.from(Math.round(pos.x()), Math.round(pos.y()), Math.round(pos.z())), block, meta);
        }
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
