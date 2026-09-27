/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline.nodes;

import github.thehighcruw.dimensium.editor.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.pipeline.Curve;
import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;

/** Fills a tube of blocks along each point of a curve by stamping spheres of the given radius. */
public class CurveFillNode implements PipelineNode {

    public static final String ID = "curve_fill";

    private static final List<int[]> DEFAULT_PALETTE = Arrays.asList(new int[] {1, 0});

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("curveFill.radius", 1.5f, 0.5f, 10.0f, "dimensium.ui.pipeline.curve_fill_radius")
            .paletteParam("curveFill.palette", DEFAULT_PALETTE, "dimensium.ui.pipeline.curve_fill_palette")
            .description("dimensium.ui.pipeline.node.curve_fill.desc")
            .inputPort("curve", PortType.CURVE)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Curve curve = inputs.get("curve", Curve.class);
        if (curve == null || curve.points.isEmpty()) return;

        float radius = params.getFloat("curveFill.radius", 1.5f);
        List<int[]> palette = params.getPalette("curveFill.palette", DEFAULT_PALETTE);
        Random rand = new Random(context.nodeSeed(0));

        int r = (int) Math.ceil(radius);
        float rSq = radius * radius;
        BlockMap map = new BlockMap();

        for (Vec3DFloat point : curve.points) {
            int cx = Math.round(point.x());
            int cy = Math.round(point.y());
            int cz = Math.round(point.z());
            Vec3DInt min = Vec3DInt.from(cx - r, cy - r, cz - r);
            Vec3DInt max = Vec3DInt.from(cx + r, cy + r, cz + r);
            Vec3DInt.forEachInclusive(min, max, pos -> {
                float dx = pos.x() - point.x();
                float dy = pos.y() - point.y();
                float dz = pos.z() - point.z();
                if (dx * dx + dy * dy + dz * dz <= rSq) {
                    int[] entry = palette.get(rand.nextInt(palette.size()));
                    map.put(pos, Block.getBlockById(entry[0]), entry[1]);
                }
            });
        }
        outputs.set("blocks", map);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
