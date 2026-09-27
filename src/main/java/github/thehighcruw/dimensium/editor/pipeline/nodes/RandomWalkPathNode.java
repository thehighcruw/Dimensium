/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline.nodes;

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
import java.util.Random;

/** Generates an organic wandering skeleton chain — each step randomly deviates from the previous direction. */
public class RandomWalkPathNode implements PipelineNode {

    public static final String ID = "random_walk_path";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("rwalk.steps", 12, 2, 50, "dimensium.ui.pipeline.rwalk_steps")
            .floatParam("rwalk.baseRadius", 1.2f, 0.2f, 5.0f, "dimensium.ui.pipeline.rwalk_base_radius")
            .floatParam("rwalk.taper", 0.4f, 0.0f, 1.0f, "dimensium.ui.pipeline.rwalk_taper")
            .floatParam("rwalk.deviationAngle", 25.0f, 0.0f, 70.0f, "dimensium.ui.pipeline.rwalk_deviation_angle")
            .floatParam("rwalk.upwardBias", 0.6f, 0.0f, 1.0f, "dimensium.ui.pipeline.rwalk_upward_bias")
            .description("dimensium.ui.pipeline.node.random_walk_path.desc")
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("skeleton", PortType.SKELETON);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt origin = inputs.get("origin", Vec3DInt.class);
        if (origin == null) origin = context.origin;

        int steps = params.getInt("rwalk.steps", 12);
        float baseRadius = params.getFloat("rwalk.baseRadius", 1.2f);
        float taper = params.getFloat("rwalk.taper", 0.4f);
        float deviationAngle = params.getFloat("rwalk.deviationAngle", 25.0f);
        float upwardBias = params.getFloat("rwalk.upwardBias", 0.6f);

        Random rand = new Random(context.nodeSeed(0));
        float maxDevRad = (float) Math.toRadians(deviationAngle);

        SkeletonNode root = new SkeletonNode(origin, baseRadius);
        SkeletonNode current = root;
        Vec3DFloat pos = origin.toFloat();
        Vec3DFloat direction = Vec3DFloat.from(0, 1, 0);

        for (int step = 1; step <= steps; step++) {
            // Blend random horizontal deviation with upward bias
            float azimuth = rand.nextFloat() * (float) (2 * Math.PI);
            float deviation = rand.nextFloat() * maxDevRad;
            Vec3DFloat randomDir = Vec3DFloat.from(
                    (float) (Math.sin(deviation) * Math.cos(azimuth)), (float) Math.cos(deviation), (float)
                            (Math.sin(deviation) * Math.sin(azimuth)));

            direction = direction
                    .times(upwardBias)
                    .plus(randomDir.times(1f - upwardBias))
                    .normalize();

            pos = pos.plus(direction);
            float radius = baseRadius * (taper + (1f - taper) * (1f - (float) step / steps));
            SkeletonNode next = new SkeletonNode(pos.round(), radius);
            current.children.add(next);
            current = next;
        }

        outputs.set("skeleton", new Skeleton(root));
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
