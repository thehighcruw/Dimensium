/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree;

import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.CylinderVoxelizerNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.LeafClusterNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.WeberPennSkeletonNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeRegistry;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.Pipeline;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.PipelinePreset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TreeToolState {

    public static final TreeToolState INSTANCE = new TreeToolState();

    static {
        NodeRegistry.register(WeberPennSkeletonNode.ID, WeberPennSkeletonNode::new);
        NodeRegistry.register(CylinderVoxelizerNode.ID, CylinderVoxelizerNode::new);
        NodeRegistry.register(LeafClusterNode.ID, LeafClusterNode::new);
    }

    private static final List<String> OAK_NODE_IDS =
            Arrays.asList(WeberPennSkeletonNode.ID, CylinderVoxelizerNode.ID, LeafClusterNode.ID);

    private static final List<String> PINE_NODE_IDS =
            Arrays.asList(WeberPennSkeletonNode.ID, CylinderVoxelizerNode.ID, LeafClusterNode.ID);

    public final List<PipelinePreset> presets = new ArrayList<>();
    public int selectedPresetIndex = 0;
    private Pipeline activePipeline;

    private TreeToolState() {
        buildPresets();
        rebuildPipeline();
    }

    private void buildPresets() {
        PipelinePreset oak = new PipelinePreset("Oak", OAK_NODE_IDS)
                .withParam("wp.trunkHeight", 10)
                .withParam("wp.trunkRadius", 1.5f)
                .withParam("wp.levels", 2)
                .withParam("wp.branchCount", 4)
                .withParam("wp.branchAngleSpread", 50.0f)
                .withParam("wp.branchLengthRatio", 0.55f)
                .withParam("wp.branchStartHeight", 0.55f)
                .withParam("vox.logBlockId", 17)
                .withParam("vox.logMeta", 0)
                .withParam("leaf.leafBlockId", 18)
                .withParam("leaf.leafMeta", 4)
                .withParam("leaf.clusterRadius", 4.5f)
                .withParam("leaf.noisiness", 0.55f);
        presets.add(oak);

        PipelinePreset pine = new PipelinePreset("Pine", PINE_NODE_IDS)
                .withParam("wp.trunkHeight", 16)
                .withParam("wp.trunkRadius", 1.0f)
                .withParam("wp.trunkTaper", 0.3f)
                .withParam("wp.levels", 3)
                .withParam("wp.branchCount", 5)
                .withParam("wp.branchAngleSpread", 25.0f)
                .withParam("wp.branchLengthRatio", 0.6f)
                .withParam("wp.branchStartHeight", 0.3f)
                .withParam("vox.logBlockId", 17)
                .withParam("vox.logMeta", 1)
                .withParam("leaf.leafBlockId", 18)
                .withParam("leaf.leafMeta", 5)
                .withParam("leaf.clusterRadius", 2.5f)
                .withParam("leaf.noisiness", 0.35f);
        presets.add(pine);
    }

    public void rebuildPipeline() {
        if (selectedPresetIndex < 0 || selectedPresetIndex >= presets.size()) return;
        activePipeline = presets.get(selectedPresetIndex).build();
    }

    public Pipeline pipeline() {
        if (activePipeline == null) rebuildPipeline();
        return activePipeline;
    }

    public NodeParams activeParams() {
        return pipeline().params();
    }

    public PipelinePreset selectedPreset() {
        return presets.get(selectedPresetIndex);
    }
}
