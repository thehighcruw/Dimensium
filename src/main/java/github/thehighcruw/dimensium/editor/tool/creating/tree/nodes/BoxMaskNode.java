/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.nodes;

import github.thehighcruw.dimensium.editor.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;

/** Fills an axis-aligned box centred at the pipeline origin. */
public class BoxMaskNode implements PipelineNode {

    public static final String ID = "box_mask";

    private static final List<int[]> DEFAULT_PALETTE = Arrays.asList(new int[] {1, 0});

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("box.width", 10, 1, 80, "dimensium.ui.pipeline.box_width")
            .intParam("box.height", 10, 1, 80, "dimensium.ui.pipeline.box_height")
            .intParam("box.depth", 10, 1, 80, "dimensium.ui.pipeline.box_depth")
            .paletteParam("box.palette", DEFAULT_PALETTE, "dimensium.ui.pipeline.box_palette")
            .description("dimensium.ui.pipeline.node.box_mask.desc")
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt center = inputs.get("origin", Vec3DInt.class);
        if (center == null) center = context.origin;

        int halfWidth = params.getInt("box.width", 10) / 2;
        int halfHeight = params.getInt("box.height", 10) / 2;
        int halfDepth = params.getInt("box.depth", 10) / 2;
        List<int[]> palette = params.getPalette("box.palette", DEFAULT_PALETTE);
        Random rand = new Random(context.nodeSeed(0));

        Vec3DInt min = center.minus(new Vec3DInt(halfWidth, halfHeight, halfDepth));
        Vec3DInt max = center.plus(new Vec3DInt(halfWidth, halfHeight, halfDepth));

        BlockMap map = new BlockMap();
        Vec3DInt.forEachInclusive(min, max, pos -> {
            int[] entry = palette.get(rand.nextInt(palette.size()));
            map.put(pos, Block.getBlockById(entry[0]), entry[1]);
        });
        outputs.set("blocks", map);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
