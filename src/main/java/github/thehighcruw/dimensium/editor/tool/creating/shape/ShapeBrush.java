/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.shape;

import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.state.PaletteState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class ShapeBrush implements BrushStrategy {

    private static final Random rand = new Random();

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        ShapeToolState s = ShapeToolState.INSTANCE;
        PaletteState ps = PaletteState.INSTANCE;
        if (ps.palette.isEmpty()) return;
        Vec3DInt dims = s.effectiveDimensions(s.shapeWidth, s.shapeHeight, s.shapeDepth);

        // Center on hit block: offset so dx=0..dims.x()-1 is symmetric around mop.blockX.
        Vec3DInt origin = Vec3DInt.from(
                mop.blockX - (dims.x() - 1) / 2, mop.blockY - (dims.y() - 1) / 2, mop.blockZ - (dims.z() - 1) / 2);

        Vec3DInt.forEachInclusive(Vec3DInt.ZERO, dims.minus(1), offset -> {
            if (!inShapeGeom(s, offset, dims)) return;
            Vec3DInt pos = origin.plus(offset);
            if (s.shapeKeepExisting && WorldUtils.getBlock(world, pos) != Blocks.air) return;
            BrushUtil.writeFromItem(world, pos, ps.samplePalette(rand));
        });
    }

    private static boolean inShapeGeom(ShapeToolState s, Vec3DInt offset, Vec3DInt dims) {
        return ShapeMath.inShapeGeom(
                s.shapeType,
                offset,
                dims,
                s.shapeHollow,
                s.shapeExponent,
                s.torusRingRadius,
                s.torusRingRadiusZ,
                s.torusTubeRadius,
                s.tubeWallThickness,
                s.shapeSupersphereExp,
                s.shapePolygonSides,
                s.shapeSpiralSpacing,
                s.shapeSpiralTurns,
                DimensiumConfig.shapeThreshold);
    }
}
