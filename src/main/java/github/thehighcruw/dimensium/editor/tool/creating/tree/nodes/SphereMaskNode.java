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

/** Fills a sphere of the given radius centred at the pipeline origin. */
public class SphereMaskNode implements PipelineNode {

    public static final String ID = "sphere_mask";

    private static final List<int[]> DEFAULT_PALETTE = Arrays.asList(new int[] {1, 0});

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("sphere.radius", 8.0f, 1.0f, 40.0f, "dimensium.ui.pipeline.sphere_radius")
            .paletteParam("sphere.palette", DEFAULT_PALETTE, "dimensium.ui.pipeline.sphere_palette")
            .description("dimensium.ui.pipeline.node.sphere_mask.desc")
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt center = inputs.get("origin", Vec3DInt.class);
        if (center == null) center = context.origin;

        float radius = params.getFloat("sphere.radius", 8.0f);
        List<int[]> palette = params.getPalette("sphere.palette", DEFAULT_PALETTE);
        Random rand = new Random(context.nodeSeed(0));

        int r = (int) Math.ceil(radius);
        float rSq = radius * radius;
        Vec3DInt min = center.minus(r);
        Vec3DInt max = center.plus(r);

        BlockMap map = new BlockMap();
        final Vec3DInt centerFinal = center;
        Vec3DInt.forEachInclusive(min, max, pos -> {
            float dx = pos.x() - centerFinal.x();
            float dy = pos.y() - centerFinal.y();
            float dz = pos.z() - centerFinal.z();
            if (dx * dx + dy * dy + dz * dz <= rSq) {
                int[] entry = palette.get(rand.nextInt(palette.size()));
                map.put(pos, Block.getBlockById(entry[0]), entry[1]);
            }
        });
        outputs.set("blocks", map);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
