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

/** Paints skeleton segments at or beyond a minimum depth with a block palette. */
public class BranchDepthPainterNode implements PipelineNode {

    public static final String ID = "branch_depth_painter";

    private static final List<int[]> DEFAULT_PALETTE = Arrays.asList(new int[] {85, 0});

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("branch.minDepth", 2, 1, 10, "dimensium.ui.pipeline.branch_min_depth")
            .paletteParam("branch.palette", DEFAULT_PALETTE, "dimensium.ui.pipeline.branch_palette")
            .description("dimensium.ui.pipeline.node.branch_depth_painter.desc")
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

        int minDepth = params.getInt("branch.minDepth", 2);
        List<int[]> palette = params.getPalette("branch.palette", DEFAULT_PALETTE);
        if (palette.isEmpty()) {
            outputs.set("blocks", blockMap);
            return;
        }

        int[] entry = palette.get(0);
        int blockId = entry[0];
        int meta = entry[1];

        for (SkeletonNode root : skeleton.roots) {
            paintBranches(root, null, 0, minDepth, blockId, meta, blockMap);
        }
        outputs.set("blocks", blockMap);
    }

    private static void paintBranches(
            SkeletonNode node, SkeletonNode parent, int depth, int minDepth, int blockId, int meta, BlockMap map) {

        if (depth >= minDepth && parent != null) {
            paintLine(parent.position, node.position, blockId, meta, map);
        }
        for (SkeletonNode child : node.children) {
            paintBranches(child, node, depth + 1, minDepth, blockId, meta, map);
        }
    }

    private static void paintLine(Vec3DInt from, Vec3DInt to, int blockId, int meta, BlockMap map) {
        Vec3DFloat start = from.toFloat();
        Vec3DFloat delta = to.toFloat().minus(start);
        float length = delta.length();
        if (length < 0.001f) {
            map.put(from, blockId, meta);
            return;
        }
        int steps = Math.max(1, (int) Math.ceil(length));
        for (int step = 0; step <= steps; step++) {
            float t = (float) step / steps;
            Vec3DFloat pos = start.plus(delta.times(t));
            map.put(Vec3DInt.from(Math.round(pos.x()), Math.round(pos.y()), Math.round(pos.z())), blockId, meta);
        }
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
