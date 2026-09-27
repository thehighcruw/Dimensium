/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import github.thehighcruw.dimensium.editor.pipeline.NodeRegistry.NodeGroup;
import github.thehighcruw.dimensium.editor.pipeline.nodes.ArcCurveNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.BlueprintBlocksNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.BranchDepthPainterNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.CircleCurveNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.ConstantFloatNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.CurveFillNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.CurveScatterNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.CurvedPathNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.DensityPaletteNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.DepthPaletteNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.EllipseCurveNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.EllipsoidMaskNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.GaussianBlurNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.GridBlocksNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.HelixCurveNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.IntersectBlocksNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.LinePathNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.MapRangeNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.MathNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.MergeBlocksNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.MirrorBlocksNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.NoiseErodeNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.NoisePaletteNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.RandomFloatNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.RandomWalkPathNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.RotateBlocksNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.ScatterBlocksNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.ShapeMaskNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.SimpleRecursiveBranchesNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.SkeletonVoxelizerNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.SpaceColonizationBranchesNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.SubtractBlocksNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.TipClusterFillNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.TranslateBlocksNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.WeberPennBranchesNode;
import github.thehighcruw.dimensium.editor.pipeline.nodes.WhorlBranchesNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PipelinePresets {

    public static void registerNodes() {
        // Generate — creates data from nothing
        final String sgShape = "dimensium.ui.pipeline.subgroup.shape";
        final String sgSkeleton = "dimensium.ui.pipeline.subgroup.skeleton";
        final String sgCurve = "dimensium.ui.pipeline.subgroup.curve";
        final String sgValue = "dimensium.ui.pipeline.subgroup.value";
        NodeRegistry.register(ShapeMaskNode.ID, NodeGroup.GENERATE, sgShape, ShapeMaskNode::new);
        NodeRegistry.register(LinePathNode.ID, NodeGroup.GENERATE, sgSkeleton, LinePathNode::new);
        NodeRegistry.register(CurvedPathNode.ID, NodeGroup.GENERATE, null, CurvedPathNode::new);
        NodeRegistry.register(RandomWalkPathNode.ID, NodeGroup.GENERATE, null, RandomWalkPathNode::new);
        NodeRegistry.register(ArcCurveNode.ID, NodeGroup.GENERATE, sgCurve, ArcCurveNode::new);
        NodeRegistry.register(CircleCurveNode.ID, NodeGroup.GENERATE, null, CircleCurveNode::new);
        NodeRegistry.register(EllipseCurveNode.ID, NodeGroup.GENERATE, null, EllipseCurveNode::new);
        NodeRegistry.register(HelixCurveNode.ID, NodeGroup.GENERATE, null, HelixCurveNode::new);
        NodeRegistry.register(ConstantFloatNode.ID, NodeGroup.GENERATE, sgValue, ConstantFloatNode::new);
        NodeRegistry.register(RandomFloatNode.ID, NodeGroup.GENERATE, null, RandomFloatNode::new);
        NodeRegistry.register(BlueprintBlocksNode.ID, NodeGroup.GENERATE, null, BlueprintBlocksNode::new);
        // Branch — grows a skeleton from another skeleton
        NodeRegistry.register(WhorlBranchesNode.ID, NodeGroup.BRANCH, WhorlBranchesNode::new);
        NodeRegistry.register(SimpleRecursiveBranchesNode.ID, NodeGroup.BRANCH, SimpleRecursiveBranchesNode::new);
        NodeRegistry.register(SpaceColonizationBranchesNode.ID, NodeGroup.BRANCH, SpaceColonizationBranchesNode::new);
        NodeRegistry.register(WeberPennBranchesNode.ID, NodeGroup.BRANCH, WeberPennBranchesNode::new);
        // Voxelize — converts skeleton or curve to blocks
        NodeRegistry.register(SkeletonVoxelizerNode.ID, NodeGroup.VOXELIZE, SkeletonVoxelizerNode::new);
        NodeRegistry.register(BranchDepthPainterNode.ID, NodeGroup.VOXELIZE, BranchDepthPainterNode::new);
        NodeRegistry.register(TipClusterFillNode.ID, NodeGroup.VOXELIZE, TipClusterFillNode::new);
        NodeRegistry.register(CurveFillNode.ID, NodeGroup.VOXELIZE, CurveFillNode::new);
        // Combine — boolean operations on block maps
        NodeRegistry.register(MergeBlocksNode.ID, NodeGroup.COMBINE, MergeBlocksNode::new);
        NodeRegistry.register(SubtractBlocksNode.ID, NodeGroup.COMBINE, SubtractBlocksNode::new);
        NodeRegistry.register(IntersectBlocksNode.ID, NodeGroup.COMBINE, IntersectBlocksNode::new);
        // Transform — reposition, orient, and repeat block maps
        final String sgMove = "dimensium.ui.pipeline.subgroup.move";
        final String sgRepeat = "dimensium.ui.pipeline.subgroup.repeat";
        NodeRegistry.register(TranslateBlocksNode.ID, NodeGroup.TRANSFORM, sgMove, TranslateBlocksNode::new);
        NodeRegistry.register(RotateBlocksNode.ID, NodeGroup.TRANSFORM, null, RotateBlocksNode::new);
        NodeRegistry.register(MirrorBlocksNode.ID, NodeGroup.TRANSFORM, null, MirrorBlocksNode::new);
        NodeRegistry.register(ScatterBlocksNode.ID, NodeGroup.TRANSFORM, sgRepeat, ScatterBlocksNode::new);
        NodeRegistry.register(GridBlocksNode.ID, NodeGroup.TRANSFORM, null, GridBlocksNode::new);
        NodeRegistry.register(CurveScatterNode.ID, NodeGroup.TRANSFORM, null, CurveScatterNode::new);
        // Filter — modify the shape of existing blocks
        NodeRegistry.register(EllipsoidMaskNode.ID, NodeGroup.FILTER, EllipsoidMaskNode::new);
        NodeRegistry.register(NoiseErodeNode.ID, NodeGroup.FILTER, NoiseErodeNode::new);
        NodeRegistry.register(GaussianBlurNode.ID, NodeGroup.FILTER, GaussianBlurNode::new);
        // Paint — recolor blocks without moving them
        NodeRegistry.register(DepthPaletteNode.ID, NodeGroup.PAINT, DepthPaletteNode::new);
        NodeRegistry.register(DensityPaletteNode.ID, NodeGroup.PAINT, DensityPaletteNode::new);
        NodeRegistry.register(NoisePaletteNode.ID, NodeGroup.PAINT, NoisePaletteNode::new);
        // Math — float arithmetic and remapping
        NodeRegistry.register(MapRangeNode.ID, NodeGroup.MATH, MapRangeNode::new);
        NodeRegistry.register(MathNode.ID, NodeGroup.MATH, MathNode::new);

        if (!PipelineLibrary.INSTANCE.hasDefaults()) {
            PipelineLibrary.INSTANCE.loadDefaults(buildDefaultGraphs());
        }
    }

    public static final String TREES_FOLDER = "Presets/Trees";
    public static final String STRUCTURES_FOLDER = "Presets/Structures";

    public static List<PipelineGraph> buildDefaultGraphs() {
        List<PipelineGraph> graphs = new ArrayList<>();

        List<PipelineGraph> trees = new ArrayList<>();
        trees.add(buildOak());
        trees.add(buildPine());
        trees.add(buildWillow());
        trees.add(buildDeadTree());
        trees.add(buildBaobab());
        trees.add(buildGiantFantasy());
        for (PipelineGraph graph : trees) graph.folder = TREES_FOLDER;
        graphs.addAll(trees);

        List<PipelineGraph> structures = new ArrayList<>();
        structures.add(buildArchBridge());
        for (PipelineGraph graph : structures) graph.folder = STRUCTURES_FOLDER;
        graphs.addAll(structures);

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

    /**
     * Arch bridge: a semicircular stone-brick arch in the YZ plane with a matching flat deck.
     *
     * <p>
     * Graph layout:
     *
     * <pre>
     *  [Arc Curve] → [Curve Fill] ──────────────┐
     *                                     [Merge Blocks]
     *  [Shape (box/deck)] ──────────────────────┘
     * </pre>
     *
     * <p>
     * The arch radius and deck dimensions are matched so the deck sits flush with the arch feet.
     * Stone brick (id=98) is used throughout.
     */
    private static PipelineGraph buildArchBridge() {
        final int stoneBrick = 98;
        final int archRadius = 10;

        PipelineGraph g = new PipelineGraph("Arch Bridge");

        // Arch — semicircle in the YZ plane (vertical, east–west span)
        PipelineGraph.NodeInstance arc = g.addNode(ArcCurveNode.ID, 0, 40);
        arc.params.set("arc.radius", (float) archRadius);
        arc.params.set("arc.startAngle", 0.0f);
        arc.params.set("arc.sweepAngle", 180.0f);
        arc.params.set("arc.plane", 2); // YZ vertical

        PipelineGraph.NodeInstance fill = g.addNode(CurveFillNode.ID, 260, 40);
        fill.params.set("curveFill.radius", 1.5f);
        fill.params.set("curveFill.palette", Arrays.asList(new int[] {stoneBrick, 0}));

        // Deck — thin box spanning the bridge, raised to the top of the arch
        PipelineGraph.NodeInstance deck = g.addNode(ShapeMaskNode.ID, 0, 220);
        deck.params.set("shape.type", ShapeMaskNode.SHAPE_BOX);
        deck.params.set("box.width", 5);
        deck.params.set("box.height", 2);
        deck.params.set("box.depth", 25);
        deck.params.set("shape.palette", Arrays.asList(new int[] {stoneBrick, 0}));

        PipelineGraph.NodeInstance deckRaise = g.addNode(TranslateBlocksNode.ID, 260, 220);
        deckRaise.params.set("translate.y", 10);

        PipelineGraph.NodeInstance merge = g.addNode(MergeBlocksNode.ID, 520, 130);

        g.connect(arc.instanceId, "curve", fill.instanceId, "curve");
        g.connect(deck.instanceId, "blocks", deckRaise.instanceId, "blocks");
        g.connect(fill.instanceId, "blocks", merge.instanceId, "blocksA");
        g.connect(deckRaise.instanceId, "blocks", merge.instanceId, "blocksB");

        return g;
    }

    private PipelinePresets() {}
}
