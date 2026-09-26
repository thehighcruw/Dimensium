/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree;

import github.thehighcruw.dimensium.editor.pipeline.NodeRegistry;
import github.thehighcruw.dimensium.editor.pipeline.NodeRegistry.NodeGroup;
import github.thehighcruw.dimensium.editor.pipeline.PipelineGraph;
import github.thehighcruw.dimensium.editor.pipeline.PipelineLibrary;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.BranchDepthPainterNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.DensityPaletteNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.DepthPaletteNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.EllipsoidMaskNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.GaussianBlurNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.NoiseErodeNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.NoisePaletteNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SimpleRecursiveSkeletonNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SkeletonVoxelizerNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SpaceColonizationSkeletonNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.TipClusterFillNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.WeberPennSkeletonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PipelinePresets {

    public static void registerNodes() {
        NodeRegistry.register(WeberPennSkeletonNode.ID, NodeGroup.SKELETON, WeberPennSkeletonNode::new);
        NodeRegistry.register(SimpleRecursiveSkeletonNode.ID, NodeGroup.SKELETON, SimpleRecursiveSkeletonNode::new);
        NodeRegistry.register(SpaceColonizationSkeletonNode.ID, NodeGroup.SKELETON, SpaceColonizationSkeletonNode::new);
        NodeRegistry.register(SkeletonVoxelizerNode.ID, NodeGroup.CONVERT, SkeletonVoxelizerNode::new);
        NodeRegistry.register(BranchDepthPainterNode.ID, NodeGroup.CONVERT, BranchDepthPainterNode::new);
        NodeRegistry.register(TipClusterFillNode.ID, NodeGroup.CONVERT, TipClusterFillNode::new);
        NodeRegistry.register(NoiseErodeNode.ID, NodeGroup.FILTER, NoiseErodeNode::new);
        NodeRegistry.register(GaussianBlurNode.ID, NodeGroup.FILTER, GaussianBlurNode::new);
        NodeRegistry.register(EllipsoidMaskNode.ID, NodeGroup.FILTER, EllipsoidMaskNode::new);
        NodeRegistry.register(DepthPaletteNode.ID, NodeGroup.PALETTE, DepthPaletteNode::new);
        NodeRegistry.register(DensityPaletteNode.ID, NodeGroup.PALETTE, DensityPaletteNode::new);
        NodeRegistry.register(NoisePaletteNode.ID, NodeGroup.PALETTE, NoisePaletteNode::new);
        if (!PipelineLibrary.INSTANCE.hasDefaults()) {
            PipelineLibrary.INSTANCE.loadDefaults(buildDefaultGraphs());
        }
    }

    public static final String PRESETS_FOLDER = "Presets";

    public static List<PipelineGraph> buildDefaultGraphs() {
        List<PipelineGraph> graphs = new ArrayList<>();
        graphs.add(buildOak());
        graphs.add(buildPine());
        graphs.add(buildWillow());
        graphs.add(buildDeadTree());
        graphs.add(buildBaobab());
        graphs.add(buildGiantFantasy());
        for (PipelineGraph g : graphs) g.folder = PRESETS_FOLDER;
        return graphs;
    }

    private static PipelineGraph buildOak() {
        PipelineGraph g = new PipelineGraph("Oak");
        PipelineGraph.NodeInstance skeleton = g.addNode(WeberPennSkeletonNode.ID, 0, 40);
        skeleton.params.set("wp.trunkHeight", 16);
        skeleton.params.set("wp.trunkRadius", 1.5f);
        skeleton.params.set("wp.trunkTaper", 0.55f);
        skeleton.params.set("wp.levels", 3);
        skeleton.params.set("wp.branchCount", 3);
        skeleton.params.set("wp.branchAngleSpread", 65.0f);
        skeleton.params.set("wp.branchLengthRatio", 0.65f);
        skeleton.params.set("wp.branchStartHeight", 0.55f);
        PipelineGraph.NodeInstance vox = g.addNode(SkeletonVoxelizerNode.ID, 260, 40);
        vox.params.set("vox.palette", Arrays.asList(new int[] {17, 0}));
        PipelineGraph.NodeInstance cluster = g.addNode(TipClusterFillNode.ID, 520, 40);
        cluster.params.set("cluster.palette", Arrays.asList(new int[] {18, 4}));
        cluster.params.set("cluster.radius", 2.8f);
        cluster.params.set("cluster.noisiness", 0.8f);
        PipelineGraph.NodeInstance erode = g.addNode(NoiseErodeNode.ID, 780, 40);
        erode.params.set("erode.strength", 0.35f);
        erode.params.set("erode.noiseScale", 0.18f);
        PipelineGraph.NodeInstance depth = g.addNode(DepthPaletteNode.ID, 1040, 40);
        depth.params.set("dp.palette", Arrays.asList(new int[] {17, 0}, new int[] {17, 0}, new int[] {18, 4}));
        g.connect(skeleton.instanceId, "skeleton", vox.instanceId, "skeleton");
        g.connect(vox.instanceId, "blocks", cluster.instanceId, "blocks");
        g.connect(skeleton.instanceId, "skeleton", cluster.instanceId, "skeleton");
        g.connect(cluster.instanceId, "blocks", erode.instanceId, "blocks");
        g.connect(erode.instanceId, "blocks", depth.instanceId, "blocks");
        return g;
    }

    private static PipelineGraph buildPine() {
        PipelineGraph g = new PipelineGraph("Pine");
        PipelineGraph.NodeInstance skeleton = g.addNode(WeberPennSkeletonNode.ID, 0, 40);
        skeleton.params.set("wp.trunkHeight", 16);
        skeleton.params.set("wp.trunkRadius", 1.0f);
        skeleton.params.set("wp.trunkTaper", 0.3f);
        skeleton.params.set("wp.levels", 3);
        skeleton.params.set("wp.branchCount", 5);
        skeleton.params.set("wp.branchAngleSpread", 25.0f);
        skeleton.params.set("wp.branchLengthRatio", 0.6f);
        skeleton.params.set("wp.branchStartHeight", 0.3f);
        PipelineGraph.NodeInstance vox = g.addNode(SkeletonVoxelizerNode.ID, 260, 40);
        vox.params.set("vox.palette", Arrays.asList(new int[] {17, 1}));
        PipelineGraph.NodeInstance cluster = g.addNode(TipClusterFillNode.ID, 520, 40);
        cluster.params.set("cluster.palette", Arrays.asList(new int[] {18, 5}));
        cluster.params.set("cluster.radius", 2.2f);
        cluster.params.set("cluster.noisiness", 0.7f);
        PipelineGraph.NodeInstance depth = g.addNode(DepthPaletteNode.ID, 780, 40);
        depth.params.set("dp.palette", Arrays.asList(new int[] {17, 1}, new int[] {17, 1}, new int[] {18, 5}));
        g.connect(skeleton.instanceId, "skeleton", vox.instanceId, "skeleton");
        g.connect(vox.instanceId, "blocks", cluster.instanceId, "blocks");
        g.connect(skeleton.instanceId, "skeleton", cluster.instanceId, "skeleton");
        g.connect(cluster.instanceId, "blocks", depth.instanceId, "blocks");
        return g;
    }

    private static PipelineGraph buildWillow() {
        PipelineGraph g = new PipelineGraph("Willow");
        PipelineGraph.NodeInstance skeleton = g.addNode(WeberPennSkeletonNode.ID, 0, 40);
        skeleton.params.set("wp.trunkHeight", 14);
        skeleton.params.set("wp.trunkRadius", 1.2f);
        skeleton.params.set("wp.trunkTaper", 0.5f);
        skeleton.params.set("wp.levels", 3);
        skeleton.params.set("wp.branchCount", 5);
        skeleton.params.set("wp.branchAngleSpread", 65.0f);
        skeleton.params.set("wp.branchLengthRatio", 0.65f);
        skeleton.params.set("wp.branchStartHeight", 0.4f);
        PipelineGraph.NodeInstance vox = g.addNode(SkeletonVoxelizerNode.ID, 260, 40);
        vox.params.set("vox.palette", Arrays.asList(new int[] {17, 1}));
        PipelineGraph.NodeInstance cluster = g.addNode(TipClusterFillNode.ID, 520, 40);
        cluster.params.set("cluster.palette", Arrays.asList(new int[] {18, 5}));
        cluster.params.set("cluster.radius", 5.0f);
        cluster.params.set("cluster.noisiness", 0.7f);
        PipelineGraph.NodeInstance erode = g.addNode(NoiseErodeNode.ID, 780, 40);
        erode.params.set("erode.strength", 0.45f);
        erode.params.set("erode.noiseScale", 0.12f);
        PipelineGraph.NodeInstance depth = g.addNode(DepthPaletteNode.ID, 1040, 40);
        depth.params.set("dp.palette", Arrays.asList(new int[] {17, 1}, new int[] {18, 5}, new int[] {18, 5}));
        g.connect(skeleton.instanceId, "skeleton", vox.instanceId, "skeleton");
        g.connect(vox.instanceId, "blocks", cluster.instanceId, "blocks");
        g.connect(skeleton.instanceId, "skeleton", cluster.instanceId, "skeleton");
        g.connect(cluster.instanceId, "blocks", erode.instanceId, "blocks");
        g.connect(erode.instanceId, "blocks", depth.instanceId, "blocks");
        return g;
    }

    private static PipelineGraph buildDeadTree() {
        PipelineGraph g = new PipelineGraph("Dead tree");
        PipelineGraph.NodeInstance skeleton = g.addNode(SimpleRecursiveSkeletonNode.ID, 0, 40);
        skeleton.params.set("sr.trunkHeight", 10);
        skeleton.params.set("sr.trunkRadius", 1.2f);
        skeleton.params.set("sr.levels", 3);
        skeleton.params.set("sr.branchCount", 3);
        skeleton.params.set("sr.branchAngle", 40.0f);
        skeleton.params.set("sr.lengthDecay", 0.55f);
        skeleton.params.set("sr.radiusDecay", 0.4f);
        PipelineGraph.NodeInstance vox = g.addNode(SkeletonVoxelizerNode.ID, 260, 40);
        vox.params.set("vox.palette", Arrays.asList(new int[] {17, 0}));
        PipelineGraph.NodeInstance painter = g.addNode(BranchDepthPainterNode.ID, 520, 40);
        painter.params.set("branch.minDepth", 2);
        painter.params.set("branch.palette", Arrays.asList(new int[] {85, 0}));
        g.connect(skeleton.instanceId, "skeleton", vox.instanceId, "skeleton");
        g.connect(vox.instanceId, "blocks", painter.instanceId, "blocks");
        g.connect(skeleton.instanceId, "skeleton", painter.instanceId, "skeleton");
        return g;
    }

    private static PipelineGraph buildBaobab() {
        PipelineGraph g = new PipelineGraph("Baobab");
        PipelineGraph.NodeInstance skeleton = g.addNode(WeberPennSkeletonNode.ID, 0, 40);
        skeleton.params.set("wp.trunkHeight", 12);
        skeleton.params.set("wp.trunkRadius", 2.5f);
        skeleton.params.set("wp.trunkTaper", 0.35f);
        skeleton.params.set("wp.levels", 2);
        skeleton.params.set("wp.branchCount", 5);
        skeleton.params.set("wp.branchAngleSpread", 80.0f);
        skeleton.params.set("wp.branchLengthRatio", 0.55f);
        skeleton.params.set("wp.branchStartHeight", 0.72f);
        PipelineGraph.NodeInstance vox = g.addNode(SkeletonVoxelizerNode.ID, 260, 40);
        vox.params.set("vox.palette", Arrays.asList(new int[] {17, 3}));
        PipelineGraph.NodeInstance cluster = g.addNode(TipClusterFillNode.ID, 520, 40);
        cluster.params.set("cluster.palette", Arrays.asList(new int[] {18, 4}));
        cluster.params.set("cluster.radius", 3.0f);
        cluster.params.set("cluster.noisiness", 0.75f);
        PipelineGraph.NodeInstance mask = g.addNode(EllipsoidMaskNode.ID, 780, 40);
        mask.params.set("mask.radiusX", 12.0f);
        mask.params.set("mask.radiusY", 4.0f);
        mask.params.set("mask.radiusZ", 12.0f);
        mask.params.set("mask.centerOffsetY", 13.0f);
        PipelineGraph.NodeInstance depth = g.addNode(DepthPaletteNode.ID, 1040, 40);
        depth.params.set("dp.palette", Arrays.asList(new int[] {17, 3}, new int[] {17, 3}, new int[] {18, 4}));
        g.connect(skeleton.instanceId, "skeleton", vox.instanceId, "skeleton");
        g.connect(vox.instanceId, "blocks", cluster.instanceId, "blocks");
        g.connect(skeleton.instanceId, "skeleton", cluster.instanceId, "skeleton");
        g.connect(cluster.instanceId, "blocks", mask.instanceId, "blocks");
        g.connect(mask.instanceId, "blocks", depth.instanceId, "blocks");
        return g;
    }

    private static PipelineGraph buildGiantFantasy() {
        PipelineGraph g = new PipelineGraph("Giant fantasy");
        PipelineGraph.NodeInstance skeleton = g.addNode(SpaceColonizationSkeletonNode.ID, 0, 40);
        skeleton.params.set("sc.attractorCount", 300);
        skeleton.params.set("sc.crownRadiusX", 14.0f);
        skeleton.params.set("sc.crownRadiusY", 10.0f);
        skeleton.params.set("sc.crownRadiusZ", 14.0f);
        skeleton.params.set("sc.crownOffsetY", 18.0f);
        skeleton.params.set("sc.influenceRadius", 8.0f);
        skeleton.params.set("sc.killRadius", 2.5f);
        skeleton.params.set("sc.stepSize", 1.5f);
        skeleton.params.set("sc.maxIterations", 100);
        skeleton.params.set("sc.trunkHeight", 10);
        skeleton.params.set("sc.branchRadius", 1.2f);
        PipelineGraph.NodeInstance vox = g.addNode(SkeletonVoxelizerNode.ID, 260, 40);
        vox.params.set("vox.palette", Arrays.asList(new int[] {17, 0}, new int[] {17, 3}));
        PipelineGraph.NodeInstance cluster = g.addNode(TipClusterFillNode.ID, 520, 40);
        cluster.params.set("cluster.palette", Arrays.asList(new int[] {18, 4}, new int[] {18, 5}));
        cluster.params.set("cluster.radius", 6.0f);
        cluster.params.set("cluster.noisiness", 0.6f);
        PipelineGraph.NodeInstance noise = g.addNode(NoisePaletteNode.ID, 780, 40);
        noise.params.set("np.palette", Arrays.asList(new int[] {18, 4}, new int[] {18, 0}));
        noise.params.set("np.noiseScale", 0.08f);
        PipelineGraph.NodeInstance depth = g.addNode(DepthPaletteNode.ID, 1040, 40);
        depth.params.set(
                "dp.palette",
                Arrays.asList(new int[] {17, 0}, new int[] {17, 0}, new int[] {18, 4}, new int[] {18, 4}));
        g.connect(skeleton.instanceId, "skeleton", vox.instanceId, "skeleton");
        g.connect(vox.instanceId, "blocks", cluster.instanceId, "blocks");
        g.connect(skeleton.instanceId, "skeleton", cluster.instanceId, "skeleton");
        g.connect(cluster.instanceId, "blocks", noise.instanceId, "blocks");
        g.connect(noise.instanceId, "blocks", depth.instanceId, "blocks");
        return g;
    }

    private PipelinePresets() {}
}
