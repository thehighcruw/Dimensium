/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree;

import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.CrownMaskNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.CylinderVoxelizerNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.DensityPaletteNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.DepthPaletteNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.FenceSubbranchNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.GaussianBlurNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.LeafClusterNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.NoiseErodeNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.NoisePaletteNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SimpleRecursiveSkeletonNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SpaceColonizationSkeletonNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.WeberPennSkeletonNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.NodeRegistry;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.Pipeline;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.PipelinePreset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TreeToolState {

    static {
        NodeRegistry.register(WeberPennSkeletonNode.ID, WeberPennSkeletonNode::new);
        NodeRegistry.register(SimpleRecursiveSkeletonNode.ID, SimpleRecursiveSkeletonNode::new);
        NodeRegistry.register(SpaceColonizationSkeletonNode.ID, SpaceColonizationSkeletonNode::new);
        NodeRegistry.register(CylinderVoxelizerNode.ID, CylinderVoxelizerNode::new);
        NodeRegistry.register(FenceSubbranchNode.ID, FenceSubbranchNode::new);
        NodeRegistry.register(LeafClusterNode.ID, LeafClusterNode::new);
        NodeRegistry.register(NoiseErodeNode.ID, NoiseErodeNode::new);
        NodeRegistry.register(GaussianBlurNode.ID, GaussianBlurNode::new);
        NodeRegistry.register(CrownMaskNode.ID, CrownMaskNode::new);
        NodeRegistry.register(DepthPaletteNode.ID, DepthPaletteNode::new);
        NodeRegistry.register(DensityPaletteNode.ID, DensityPaletteNode::new);
        NodeRegistry.register(NoisePaletteNode.ID, NoisePaletteNode::new);
    }

    private static final List<String> OAK_NODE_IDS =
            Arrays.asList(WeberPennSkeletonNode.ID, CylinderVoxelizerNode.ID, LeafClusterNode.ID, DepthPaletteNode.ID);

    private static final List<String> PINE_NODE_IDS =
            Arrays.asList(WeberPennSkeletonNode.ID, CylinderVoxelizerNode.ID, LeafClusterNode.ID, DepthPaletteNode.ID);

    private static final List<String> WILLOW_NODE_IDS = Arrays.asList(
            WeberPennSkeletonNode.ID,
            CylinderVoxelizerNode.ID,
            LeafClusterNode.ID,
            NoiseErodeNode.ID,
            DepthPaletteNode.ID);

    private static final List<String> DEAD_TREE_NODE_IDS =
            Arrays.asList(SimpleRecursiveSkeletonNode.ID, CylinderVoxelizerNode.ID, FenceSubbranchNode.ID);

    private static final List<String> BAOBAB_NODE_IDS = Arrays.asList(
            WeberPennSkeletonNode.ID,
            CylinderVoxelizerNode.ID,
            LeafClusterNode.ID,
            CrownMaskNode.ID,
            DepthPaletteNode.ID);

    private static final List<String> GIANT_FANTASY_NODE_IDS = Arrays.asList(
            SpaceColonizationSkeletonNode.ID,
            CylinderVoxelizerNode.ID,
            LeafClusterNode.ID,
            NoisePaletteNode.ID,
            DepthPaletteNode.ID);

    public static final TreeToolState INSTANCE = new TreeToolState();

    public final List<PipelinePreset> presets = new ArrayList<>();
    public int selectedPresetIndex = 0;
    public long seed;
    private Pipeline activePipeline;

    private TreeToolState() {
        seed = ThreadLocalRandom.current().nextLong();
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
                .withParam("vox.logPalette", Arrays.asList(new int[] {17, 0}))
                .withParam("leaf.leafPalette", Arrays.asList(new int[] {18, 4}))
                .withParam("leaf.clusterRadius", 4.5f)
                .withParam("leaf.noisiness", 0.55f)
                .withParam("dp.palette", Arrays.asList(new int[] {17, 0}, new int[] {17, 0}, new int[] {18, 4}));
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
                .withParam("vox.logPalette", Arrays.asList(new int[] {17, 1}))
                .withParam("leaf.leafPalette", Arrays.asList(new int[] {18, 5}))
                .withParam("leaf.clusterRadius", 2.5f)
                .withParam("leaf.noisiness", 0.35f)
                .withParam("dp.palette", Arrays.asList(new int[] {17, 1}, new int[] {17, 1}, new int[] {18, 5}));
        presets.add(pine);

        PipelinePreset willow = new PipelinePreset("Willow", WILLOW_NODE_IDS)
                .withParam("wp.trunkHeight", 14)
                .withParam("wp.trunkRadius", 1.2f)
                .withParam("wp.trunkTaper", 0.5f)
                .withParam("wp.levels", 3)
                .withParam("wp.branchCount", 5)
                .withParam("wp.branchAngleSpread", 65.0f)
                .withParam("wp.branchLengthRatio", 0.65f)
                .withParam("wp.branchStartHeight", 0.4f)
                .withParam("vox.logPalette", Arrays.asList(new int[] {17, 1}))
                .withParam("leaf.leafPalette", Arrays.asList(new int[] {18, 5}))
                .withParam("leaf.clusterRadius", 5.0f)
                .withParam("leaf.noisiness", 0.7f)
                .withParam("erode.strength", 0.45f)
                .withParam("erode.noiseScale", 0.12f)
                .withParam("erode.onlyLeaves", true)
                .withParam("dp.palette", Arrays.asList(new int[] {17, 1}, new int[] {18, 5}, new int[] {18, 5}));
        presets.add(willow);

        PipelinePreset deadTree = new PipelinePreset("Dead tree", DEAD_TREE_NODE_IDS)
                .withParam("sr.trunkHeight", 10)
                .withParam("sr.trunkRadius", 1.2f)
                .withParam("sr.levels", 3)
                .withParam("sr.branchCount", 3)
                .withParam("sr.branchAngle", 40.0f)
                .withParam("sr.lengthDecay", 0.55f)
                .withParam("sr.radiusDecay", 0.4f)
                .withParam("vox.logPalette", Arrays.asList(new int[] {17, 0}))
                .withParam("fence.minDepth", 2)
                .withParam("fence.fencePalette", Arrays.asList(new int[] {85, 0}));
        presets.add(deadTree);

        PipelinePreset baobab = new PipelinePreset("Baobab", BAOBAB_NODE_IDS)
                .withParam("wp.trunkHeight", 8)
                .withParam("wp.trunkRadius", 4.0f)
                .withParam("wp.trunkTaper", 0.4f)
                .withParam("wp.levels", 2)
                .withParam("wp.branchCount", 6)
                .withParam("wp.branchAngleSpread", 70.0f)
                .withParam("wp.branchLengthRatio", 0.45f)
                .withParam("wp.branchStartHeight", 0.75f)
                .withParam("vox.logPalette", Arrays.asList(new int[] {17, 3}))
                .withParam("leaf.leafPalette", Arrays.asList(new int[] {18, 4}))
                .withParam("leaf.clusterRadius", 3.5f)
                .withParam("leaf.noisiness", 0.4f)
                .withParam("mask.radiusX", 10.0f)
                .withParam("mask.radiusY", 3.5f)
                .withParam("mask.radiusZ", 10.0f)
                .withParam("mask.centerOffsetY", 9.0f)
                .withParam("mask.onlyLeaves", true)
                .withParam("dp.palette", Arrays.asList(new int[] {17, 3}, new int[] {18, 4}));
        presets.add(baobab);

        PipelinePreset giantFantasy = new PipelinePreset("Giant fantasy", GIANT_FANTASY_NODE_IDS)
                .withParam("sc.attractorCount", 300)
                .withParam("sc.crownRadiusX", 14.0f)
                .withParam("sc.crownRadiusY", 10.0f)
                .withParam("sc.crownRadiusZ", 14.0f)
                .withParam("sc.crownOffsetY", 18.0f)
                .withParam("sc.influenceRadius", 8.0f)
                .withParam("sc.killRadius", 2.5f)
                .withParam("sc.stepSize", 1.5f)
                .withParam("sc.maxIterations", 100)
                .withParam("sc.trunkHeight", 10)
                .withParam("sc.branchRadius", 1.2f)
                .withParam("vox.logPalette", Arrays.asList(new int[] {17, 0}, new int[] {17, 3}))
                .withParam("leaf.leafPalette", Arrays.asList(new int[] {18, 4}, new int[] {18, 5}))
                .withParam("leaf.clusterRadius", 6.0f)
                .withParam("leaf.noisiness", 0.6f)
                .withParam("np.palette", Arrays.asList(new int[] {18, 4}, new int[] {18, 0}))
                .withParam("np.noiseScale", 0.08f)
                .withParam("np.onlyLeaves", true)
                .withParam(
                        "dp.palette",
                        Arrays.asList(new int[] {17, 0}, new int[] {17, 0}, new int[] {18, 4}, new int[] {18, 4}));
        presets.add(giantFantasy);
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
