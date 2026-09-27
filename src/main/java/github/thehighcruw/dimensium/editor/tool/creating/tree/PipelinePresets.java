/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree;

import github.thehighcruw.dimensium.editor.pipeline.NodeRegistry;
import github.thehighcruw.dimensium.editor.pipeline.NodeRegistry.NodeGroup;
import github.thehighcruw.dimensium.editor.pipeline.PipelineGraph;
import github.thehighcruw.dimensium.editor.pipeline.PipelineLibrary;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.BoxMaskNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.BranchDepthPainterNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.ConstantFloatNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.CurvedPathNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.DensityPaletteNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.DepthPaletteNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.EllipsoidMaskNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.GaussianBlurNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.IntersectBlocksNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.LinePathNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.MapRangeNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.MathNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.MergeBlocksNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.NoiseErodeNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.NoiseFieldNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.NoisePaletteNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.RandomFloatNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.RandomWalkPathNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SimpleRecursiveBranchesNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SkeletonVoxelizerNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SpaceColonizationBranchesNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SphereMaskNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SplinePathNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.SubtractBlocksNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.TipClusterFillNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.WeberPennBranchesNode;
import github.thehighcruw.dimensium.editor.tool.creating.tree.nodes.WhorlBranchesNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PipelinePresets {

    public static void registerNodes() {
        // Path generators
        NodeRegistry.register(LinePathNode.ID, NodeGroup.PATH, LinePathNode::new);
        NodeRegistry.register(CurvedPathNode.ID, NodeGroup.PATH, CurvedPathNode::new);
        NodeRegistry.register(RandomWalkPathNode.ID, NodeGroup.PATH, RandomWalkPathNode::new);
        NodeRegistry.register(SplinePathNode.ID, NodeGroup.PATH, SplinePathNode::new);
        // Branch nodes
        NodeRegistry.register(WhorlBranchesNode.ID, NodeGroup.BRANCH, WhorlBranchesNode::new);
        NodeRegistry.register(SimpleRecursiveBranchesNode.ID, NodeGroup.BRANCH, SimpleRecursiveBranchesNode::new);
        NodeRegistry.register(SpaceColonizationBranchesNode.ID, NodeGroup.BRANCH, SpaceColonizationBranchesNode::new);
        NodeRegistry.register(WeberPennBranchesNode.ID, NodeGroup.BRANCH, WeberPennBranchesNode::new);
        // Convert
        NodeRegistry.register(SkeletonVoxelizerNode.ID, NodeGroup.CONVERT, SkeletonVoxelizerNode::new);
        NodeRegistry.register(BranchDepthPainterNode.ID, NodeGroup.CONVERT, BranchDepthPainterNode::new);
        NodeRegistry.register(TipClusterFillNode.ID, NodeGroup.CONVERT, TipClusterFillNode::new);
        // Boolean
        NodeRegistry.register(MergeBlocksNode.ID, NodeGroup.BOOLEAN, MergeBlocksNode::new);
        NodeRegistry.register(SubtractBlocksNode.ID, NodeGroup.BOOLEAN, SubtractBlocksNode::new);
        NodeRegistry.register(IntersectBlocksNode.ID, NodeGroup.BOOLEAN, IntersectBlocksNode::new);
        // Math
        NodeRegistry.register(ConstantFloatNode.ID, NodeGroup.MATH, ConstantFloatNode::new);
        NodeRegistry.register(RandomFloatNode.ID, NodeGroup.MATH, RandomFloatNode::new);
        NodeRegistry.register(NoiseFieldNode.ID, NodeGroup.MATH, NoiseFieldNode::new);
        NodeRegistry.register(MapRangeNode.ID, NodeGroup.MATH, MapRangeNode::new);
        NodeRegistry.register(MathNode.ID, NodeGroup.MATH, MathNode::new);
        // Mask
        NodeRegistry.register(SphereMaskNode.ID, NodeGroup.MASK, SphereMaskNode::new);
        NodeRegistry.register(BoxMaskNode.ID, NodeGroup.MASK, BoxMaskNode::new);
        NodeRegistry.register(EllipsoidMaskNode.ID, NodeGroup.MASK, EllipsoidMaskNode::new);
        // Filter
        NodeRegistry.register(NoiseErodeNode.ID, NodeGroup.FILTER, NoiseErodeNode::new);
        NodeRegistry.register(GaussianBlurNode.ID, NodeGroup.FILTER, GaussianBlurNode::new);
        // Palette
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
        for (PipelineGraph graph : graphs) graph.folder = PRESETS_FOLDER;
        return graphs;
    }

    /** Oak: curved trunk + WP branches + tip clusters + depth repaint. */
    private static PipelineGraph buildOak() {
        PipelineGraph g = new PipelineGraph("Oak");
        PipelineGraph.NodeInstance trunk = g.addNode(CurvedPathNode.ID, 0, 40);
        trunk.params.set("curved.height", 16);
        trunk.params.set("curved.baseRadius", 1.5f);
        trunk.params.set("curved.taper", 0.55f);
        trunk.params.set("curved.leanAngle", 8.0f);
        PipelineGraph.NodeInstance branches = g.addNode(WeberPennBranchesNode.ID, 260, 40);
        branches.params.set("wp.levels", 3);
        branches.params.set("wp.branchCount", 3);
        branches.params.set("wp.branchAngleSpread", 65.0f);
        branches.params.set("wp.initialLength", 7);
        branches.params.set("wp.lengthRatio", 0.65f);
        branches.params.set("wp.radiusFactor", 0.5f);
        PipelineGraph.NodeInstance vox = g.addNode(SkeletonVoxelizerNode.ID, 520, 40);
        vox.params.set("vox.palette", Arrays.asList(new int[] {17, 0}));
        PipelineGraph.NodeInstance cluster = g.addNode(TipClusterFillNode.ID, 780, 40);
        cluster.params.set("cluster.palette", Arrays.asList(new int[] {18, 4}));
        cluster.params.set("cluster.radius", 2.8f);
        cluster.params.set("cluster.noisiness", 0.8f);
        g.connect(trunk.instanceId, "skeleton", branches.instanceId, "skeleton");
        g.connect(branches.instanceId, "skeleton", vox.instanceId, "skeleton");
        g.connect(vox.instanceId, "blocks", cluster.instanceId, "blocks");
        g.connect(branches.instanceId, "skeleton", cluster.instanceId, "skeleton");
        return g;
    }

    /** Pine: straight trunk + whorled conifer branches tapering into a cone. */
    private static PipelineGraph buildPine() {
        PipelineGraph g = new PipelineGraph("Pine");
        PipelineGraph.NodeInstance trunk = g.addNode(LinePathNode.ID, 0, 40);
        trunk.params.set("line.height", 20);
        trunk.params.set("line.baseRadius", 1.2f);
        trunk.params.set("line.taper", 0.5f);
        PipelineGraph.NodeInstance branches = g.addNode(WhorlBranchesNode.ID, 260, 40);
        branches.params.set("whorl.branchesPerWhorl", 6);
        branches.params.set("whorl.baseAngle", 82.0f);
        branches.params.set("whorl.tipAngle", 22.0f);
        branches.params.set("whorl.baseLength", 9);
        branches.params.set("whorl.tipLength", 2);
        branches.params.set("whorl.branchRadius", 0.35f);
        branches.params.set("whorl.spacing", 2);
        branches.params.set("whorl.skipBase", 2);
        branches.params.set("whorl.spiralOffset", 0.618f);
        PipelineGraph.NodeInstance vox = g.addNode(SkeletonVoxelizerNode.ID, 520, 40);
        vox.params.set("vox.palette", Arrays.asList(new int[] {17, 1}));
        PipelineGraph.NodeInstance cluster = g.addNode(TipClusterFillNode.ID, 780, 40);
        cluster.params.set("cluster.palette", Arrays.asList(new int[] {18, 5}));
        cluster.params.set("cluster.radius", 1.4f);
        cluster.params.set("cluster.noisiness", 0.5f);
        g.connect(trunk.instanceId, "skeleton", branches.instanceId, "skeleton");
        g.connect(branches.instanceId, "skeleton", vox.instanceId, "skeleton");
        g.connect(vox.instanceId, "blocks", cluster.instanceId, "blocks");
        g.connect(branches.instanceId, "skeleton", cluster.instanceId, "skeleton");
        return g;
    }

    /** Willow: curved trunk + WP wide branches + large noisy clusters. */
    private static PipelineGraph buildWillow() {
        PipelineGraph g = new PipelineGraph("Willow");
        PipelineGraph.NodeInstance trunk = g.addNode(CurvedPathNode.ID, 0, 40);
        trunk.params.set("curved.height", 14);
        trunk.params.set("curved.baseRadius", 1.2f);
        trunk.params.set("curved.taper", 0.5f);
        trunk.params.set("curved.leanAngle", 12.0f);
        PipelineGraph.NodeInstance branches = g.addNode(WeberPennBranchesNode.ID, 260, 40);
        branches.params.set("wp.levels", 3);
        branches.params.set("wp.branchCount", 5);
        branches.params.set("wp.branchAngleSpread", 65.0f);
        branches.params.set("wp.initialLength", 6);
        branches.params.set("wp.lengthRatio", 0.65f);
        branches.params.set("wp.radiusFactor", 0.5f);
        PipelineGraph.NodeInstance vox = g.addNode(SkeletonVoxelizerNode.ID, 520, 40);
        vox.params.set("vox.palette", Arrays.asList(new int[] {17, 1}));
        PipelineGraph.NodeInstance cluster = g.addNode(TipClusterFillNode.ID, 780, 40);
        cluster.params.set("cluster.palette", Arrays.asList(new int[] {18, 5}));
        cluster.params.set("cluster.radius", 5.0f);
        cluster.params.set("cluster.noisiness", 0.7f);
        g.connect(trunk.instanceId, "skeleton", branches.instanceId, "skeleton");
        g.connect(branches.instanceId, "skeleton", vox.instanceId, "skeleton");
        g.connect(vox.instanceId, "blocks", cluster.instanceId, "blocks");
        g.connect(branches.instanceId, "skeleton", cluster.instanceId, "skeleton");
        return g;
    }

    /** Dead tree: line trunk + simple recursive branches + voxelize + branch-depth painter (no leaves). */
    private static PipelineGraph buildDeadTree() {
        PipelineGraph g = new PipelineGraph("Dead tree");
        PipelineGraph.NodeInstance trunk = g.addNode(LinePathNode.ID, 0, 40);
        trunk.params.set("line.height", 10);
        trunk.params.set("line.baseRadius", 1.2f);
        trunk.params.set("line.taper", 0.45f);
        PipelineGraph.NodeInstance branches = g.addNode(SimpleRecursiveBranchesNode.ID, 260, 40);
        branches.params.set("srb.levels", 3);
        branches.params.set("srb.branchCount", 3);
        branches.params.set("srb.branchAngle", 40.0f);
        branches.params.set("srb.initialLength", 5);
        branches.params.set("srb.lengthDecay", 0.55f);
        branches.params.set("srb.radiusDecay", 0.4f);
        PipelineGraph.NodeInstance vox = g.addNode(SkeletonVoxelizerNode.ID, 520, 40);
        vox.params.set("vox.palette", Arrays.asList(new int[] {17, 0}));
        PipelineGraph.NodeInstance painter = g.addNode(BranchDepthPainterNode.ID, 780, 40);
        painter.params.set("branch.minDepth", 2);
        painter.params.set("branch.palette", Arrays.asList(new int[] {85, 0}));
        g.connect(trunk.instanceId, "skeleton", branches.instanceId, "skeleton");
        g.connect(branches.instanceId, "skeleton", vox.instanceId, "skeleton");
        g.connect(vox.instanceId, "blocks", painter.instanceId, "blocks");
        g.connect(branches.instanceId, "skeleton", painter.instanceId, "skeleton");
        return g;
    }

    /** Baobab: curved trunk + WP branches (wide, high start) + clusters + ellipsoid mask + depth repaint. */
    private static PipelineGraph buildBaobab() {
        PipelineGraph g = new PipelineGraph("Baobab");
        PipelineGraph.NodeInstance trunk = g.addNode(CurvedPathNode.ID, 0, 40);
        trunk.params.set("curved.height", 12);
        trunk.params.set("curved.baseRadius", 2.5f);
        trunk.params.set("curved.taper", 0.35f);
        trunk.params.set("curved.leanAngle", 5.0f);
        PipelineGraph.NodeInstance branches = g.addNode(WeberPennBranchesNode.ID, 260, 40);
        branches.params.set("wp.levels", 2);
        branches.params.set("wp.branchCount", 5);
        branches.params.set("wp.branchAngleSpread", 80.0f);
        branches.params.set("wp.initialLength", 6);
        branches.params.set("wp.lengthRatio", 0.55f);
        branches.params.set("wp.radiusFactor", 0.5f);
        PipelineGraph.NodeInstance vox = g.addNode(SkeletonVoxelizerNode.ID, 520, 40);
        vox.params.set("vox.palette", Arrays.asList(new int[] {17, 3}));
        PipelineGraph.NodeInstance cluster = g.addNode(TipClusterFillNode.ID, 780, 40);
        cluster.params.set("cluster.palette", Arrays.asList(new int[] {18, 4}));
        cluster.params.set("cluster.radius", 3.0f);
        cluster.params.set("cluster.noisiness", 0.75f);
        PipelineGraph.NodeInstance mask = g.addNode(EllipsoidMaskNode.ID, 1040, 40);
        mask.params.set("mask.radiusX", 12.0f);
        mask.params.set("mask.radiusY", 4.0f);
        mask.params.set("mask.radiusZ", 12.0f);
        mask.params.set("mask.centerOffsetY", 13.0f);
        g.connect(trunk.instanceId, "skeleton", branches.instanceId, "skeleton");
        g.connect(branches.instanceId, "skeleton", vox.instanceId, "skeleton");
        g.connect(vox.instanceId, "blocks", cluster.instanceId, "blocks");
        g.connect(branches.instanceId, "skeleton", cluster.instanceId, "skeleton");
        g.connect(cluster.instanceId, "blocks", mask.instanceId, "blocks");
        return g;
    }

    /** Giant fantasy: random walk trunk + space colonization crown + clusters + noise palette + depth repaint. */
    private static PipelineGraph buildGiantFantasy() {
        PipelineGraph g = new PipelineGraph("Giant fantasy");
        PipelineGraph.NodeInstance trunk = g.addNode(RandomWalkPathNode.ID, 0, 40);
        trunk.params.set("rwalk.steps", 14);
        trunk.params.set("rwalk.baseRadius", 1.4f);
        trunk.params.set("rwalk.taper", 0.45f);
        trunk.params.set("rwalk.deviationAngle", 15.0f);
        trunk.params.set("rwalk.upwardBias", 0.8f);
        PipelineGraph.NodeInstance crown = g.addNode(SpaceColonizationBranchesNode.ID, 260, 40);
        crown.params.set("sc.attractorCount", 300);
        crown.params.set("sc.crownRadiusX", 14.0f);
        crown.params.set("sc.crownRadiusY", 10.0f);
        crown.params.set("sc.crownRadiusZ", 14.0f);
        crown.params.set("sc.crownOffsetY", 4.0f);
        crown.params.set("sc.influenceRadius", 8.0f);
        crown.params.set("sc.killRadius", 2.5f);
        crown.params.set("sc.stepSize", 1.5f);
        crown.params.set("sc.maxIterations", 100);
        crown.params.set("sc.branchRadius", 1.2f);
        PipelineGraph.NodeInstance vox = g.addNode(SkeletonVoxelizerNode.ID, 520, 40);
        vox.params.set("vox.palette", Arrays.asList(new int[] {17, 0}, new int[] {17, 3}));
        PipelineGraph.NodeInstance cluster = g.addNode(TipClusterFillNode.ID, 780, 40);
        cluster.params.set("cluster.palette", Arrays.asList(new int[] {18, 4}, new int[] {18, 5}));
        cluster.params.set("cluster.radius", 6.0f);
        cluster.params.set("cluster.noisiness", 0.6f);
        g.connect(trunk.instanceId, "skeleton", crown.instanceId, "skeleton");
        g.connect(crown.instanceId, "skeleton", vox.instanceId, "skeleton");
        g.connect(vox.instanceId, "blocks", cluster.instanceId, "blocks");
        g.connect(crown.instanceId, "skeleton", cluster.instanceId, "skeleton");
        return g;
    }

    private PipelinePresets() {}
}
