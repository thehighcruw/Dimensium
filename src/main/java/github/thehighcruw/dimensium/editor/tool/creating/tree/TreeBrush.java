/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.Pipeline;
import github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline.PipelineContext;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class TreeBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        Vec3DInt hitPos = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        Vec3DInt origin = hitPos.plus(BrushUtil.faceNormal(mop.sideHit));

        Pipeline pipeline = TreeToolState.INSTANCE.pipeline();
        PipelineContext context = new PipelineContext(origin, TreeToolState.INSTANCE.seed);
        BlockMap result = pipeline.execute(context);
        result.applyToWorld(world);
    }
}
