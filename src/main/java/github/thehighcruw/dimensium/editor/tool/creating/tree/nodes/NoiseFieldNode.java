/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.nodes;

import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import github.thehighcruw.dimensium.editor.tool.noise.NoiseSampler;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;

/**
 * Evaluates 3D Perlin/simplex noise at the pipeline origin and outputs a scalar float in [0, 1].
 * Useful for driving radiusScale, map_range inputs, or math nodes.
 */
public class NoiseFieldNode implements PipelineNode {

    public static final String ID = "noise_field";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .floatParam("noise.frequency", 0.1f, 0.001f, 2.0f, "dimensium.ui.pipeline.noise_frequency")
            .intParam("noise.octaves", 3, 1, 8, "dimensium.ui.pipeline.noise_octaves")
            .floatParam("noise.amplitude", 1.0f, 0.0f, 4.0f, "dimensium.ui.pipeline.noise_amplitude")
            .description("dimensium.ui.pipeline.node.noise_field.desc")
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("value", PortType.FLOAT);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt pos = inputs.get("origin", Vec3DInt.class);
        if (pos == null) pos = context.origin;

        float frequency = params.getFloat("noise.frequency", 0.1f);
        int octaves = params.getInt("noise.octaves", 3);
        float amplitude = params.getFloat("noise.amplitude", 1.0f);
        long seed = context.nodeSeed(0);

        float value = 0f;
        float freq = frequency;
        float amp = 1f;
        float totalAmp = 0f;
        for (int octave = 0; octave < octaves; octave++) {
            float sample =
                    (NoiseSampler.rawSimplex3(pos.x() * freq, pos.y() * freq, pos.z() * freq, seed + octave) + 1f)
                            * 0.5f;
            value += sample * amp;
            totalAmp += amp;
            freq *= 2f;
            amp *= 0.5f;
        }
        value = totalAmp > 0f ? (value / totalAmp) * amplitude : 0f;
        outputs.set("value", value);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
