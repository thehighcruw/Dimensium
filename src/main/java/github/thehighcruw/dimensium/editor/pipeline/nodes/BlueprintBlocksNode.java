/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline.nodes;

import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.editor.blueprint.Blueprint;
import github.thehighcruw.dimensium.editor.blueprint.BlueprintIO;
import github.thehighcruw.dimensium.editor.blueprint.BlueprintRegistry;
import github.thehighcruw.dimensium.editor.clipboard.ClipboardBlock;
import github.thehighcruw.dimensium.editor.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import java.io.File;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/** Loads a saved blueprint and outputs its blocks as a BlockMap. */
public class BlueprintBlocksNode implements PipelineNode {

    public static final String ID = "blueprint_blocks";

    private static final String PARAM_FILE = "blueprintFile";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .description("dimensium.ui.pipeline.node.blueprint_blocks.desc")
            .blueprintParam(PARAM_FILE, "dimensium.ui.pipeline.node.blueprint_blocks.param.file")
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        String filename = params.getString(PARAM_FILE, "");
        if (filename.isEmpty()) {
            outputs.set("blocks", new BlockMap());
            return;
        }

        Blueprint blueprint = resolveBlueprint(filename);
        if (blueprint == null) {
            outputs.set("blocks", new BlockMap());
            return;
        }

        BlockMap blockMap = new BlockMap();
        for (ClipboardBlock clipboardBlock : blueprint.offsets()) {
            Block block = Block.getBlockById(clipboardBlock.blockId());
            if (block == null || block == Blocks.air) continue;
            blockMap.put(clipboardBlock.offset(), block, clipboardBlock.meta());
        }
        outputs.set("blocks", blockMap);
    }

    private static Blueprint resolveBlueprint(String name) {
        List<Map.Entry<File, Blueprint>> all = BlueprintRegistry.INSTANCE.getAll();
        for (Map.Entry<File, Blueprint> entry : all) {
            if (entry.getValue().name().equals(name)) {
                try {
                    return BlueprintIO.load(entry.getKey());
                } catch (Exception exception) {
                    Dimensium.logger.warn("BlueprintBlocksNode: failed to load blueprint '{}'", name, exception);
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
