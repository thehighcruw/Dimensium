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
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;

public class GaussianBlurNode implements PipelineNode {

    public static final String ID = "gaussian_blur";

    private static final Vec3DInt[] NEIGHBORS = {
        Vec3DInt.from(1, 0, 0),
        Vec3DInt.from(-1, 0, 0),
        Vec3DInt.from(0, 1, 0),
        Vec3DInt.from(0, -1, 0),
        Vec3DInt.from(0, 0, 1),
        Vec3DInt.from(0, 0, -1)
    };

    private static final NodeSchema SCHEMA = new NodeSchema()
            .intParam("blur.passes", 1, 1, 3, "dimensium.ui.pipeline.blur_passes")
            .intParam("blur.threshold", 3, 1, 6, "dimensium.ui.pipeline.blur_threshold")
            .description("dimensium.ui.pipeline.node.gaussian_blur.desc")
            .inputPort("blocks", PortType.BLOCK_MAP)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        BlockMap blockMap = inputs.get("blocks", BlockMap.class);
        if (blockMap == null) return;

        int passes = params.getInt("blur.passes", 1);
        int threshold = params.getInt("blur.threshold", 3);

        for (int pass = 0; pass < passes; pass++) {
            blockMap = blurPass(blockMap, threshold);
        }
        outputs.set("blocks", blockMap);
    }

    private static BlockMap blurPass(BlockMap source, int threshold) {
        Map<Long, int[]> existing = source.entries();
        Map<Long, int[]> additions = new HashMap<>();

        for (Long key : existing.keySet()) {
            Vec3DInt pos = BlockMap.unpackKey(key);
            for (Vec3DInt offset : NEIGHBORS) {
                Vec3DInt candidate = pos.plus(offset);
                long candidateKey = BlockMap.packKey(candidate);
                if (existing.containsKey(candidateKey)) continue;

                int filled = 0;
                int[] mostCommon = null;
                Map<Integer, Integer> counts = new HashMap<>();
                for (Vec3DInt neighborOffset : NEIGHBORS) {
                    Vec3DInt neighbor = candidate.plus(neighborOffset);
                    int[] entry = existing.get(BlockMap.packKey(neighbor));
                    if (entry == null) continue;
                    filled++;
                    int packedId = entry[0] * 16 + entry[1];
                    counts.merge(packedId, 1, Integer::sum);
                    if (mostCommon == null) mostCommon = entry;
                }
                if (filled >= threshold && mostCommon != null) {
                    int bestPacked = counts.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(mostCommon[0] * 16 + mostCommon[1]);
                    additions.put(candidateKey, new int[] {bestPacked / 16, bestPacked % 16});
                }
            }
        }

        for (Map.Entry<Long, int[]> entry : additions.entrySet()) {
            Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
            source.put(pos, Block.getBlockById(entry.getValue()[0]), entry.getValue()[1]);
        }
        return source;
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
