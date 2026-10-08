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
import java.util.List;
import java.util.Map;

/**
 * Places copies of a block map centred at each Nth point along a curve.
 * Useful for colonnades, fence posts, and other evenly-spaced repeated elements.
 */
public class CurveScatterNode implements PipelineNode {

    public static final String ID = "curve_scatter";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("curveScatter.stride", 1, 1, 64, "dimensium.ui.pipeline.curve_scatter_stride")
            .boolParam("curveScatter.alignToCurve", false, "dimensium.ui.pipeline.curve_scatter_align_to_curve")
            .description("dimensium.ui.pipeline.node.curve_scatter.desc")
            .inputPort("curve", PortType.CURVE)
            .inputPort("blocks", PortType.BLOCK_MAP)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Curve curve = inputs.get("curve", Curve.class);
        BlockMap input = inputs.get("blocks", BlockMap.class);
        if (curve == null || input == null || curve.points.isEmpty()) return;

        int stride = params.getInt("curveScatter.stride", 1);
        boolean alignToCurve = params.getBool("curveScatter.alignToCurve", false);
        Vec3DInt inputOrigin = context.origin;
        List<Vec3DFloat> points = curve.points;
        BlockMap result = new BlockMap();

        for (int index = 0; index < points.size(); index += stride) {
            Vec3DFloat point = points.get(index);
            int shiftX = Math.round(point.x()) - inputOrigin.x();
            int shiftY = Math.round(point.y()) - inputOrigin.y();
            int shiftZ = Math.round(point.z()) - inputOrigin.z();

            double cosYaw = 1.0;
            double sinYaw = 0.0;
            if (alignToCurve) {
                double yaw = computeTangentYaw(points, index, curve.closed);
                cosYaw = Math.cos(yaw);
                sinYaw = Math.sin(yaw);
            }

            for (Map.Entry<Long, int[]> entry : input.entries().entrySet()) {
                Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
                int dx = pos.x() - inputOrigin.x();
                int dy = pos.y() - inputOrigin.y();
                int dz = pos.z() - inputOrigin.z();

                int rotatedDx = (int) Math.round(dx * cosYaw - dz * sinYaw);
                int rotatedDz = (int) Math.round(dx * sinYaw + dz * cosYaw);

                Vec3DInt placed = Vec3DInt.from(
                        inputOrigin.x() + rotatedDx + shiftX,
                        inputOrigin.y() + dy + shiftY,
                        inputOrigin.z() + rotatedDz + shiftZ);
                result.put(placed, entry.getValue()[0], entry.getValue()[1]);
            }
        }
        outputs.set("blocks", result);
    }

    /**
     * Computes the tangent yaw angle (radians, XZ plane) at curve point {@code index}.
     * Uses central differences in the interior, forward/backward at the endpoints.
     * For closed curves the endpoints wrap around.
     */
    private static double computeTangentYaw(List<Vec3DFloat> points, int index, boolean closed) {
        int size = points.size();
        Vec3DFloat prev;
        Vec3DFloat next;

        if (closed) {
            prev = points.get((index - 1 + size) % size);
            next = points.get((index + 1) % size);
        } else if (index == 0) {
            prev = points.get(0);
            next = points.get(Math.min(1, size - 1));
        } else if (index >= size - 1) {
            prev = points.get(size - 2);
            next = points.get(size - 1);
        } else {
            prev = points.get(index - 1);
            next = points.get(index + 1);
        }

        float tangentX = next.x() - prev.x();
        float tangentZ = next.z() - prev.z();
        if (tangentX == 0 && tangentZ == 0) return 0.0;
        return Math.atan2(tangentZ, tangentX);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
