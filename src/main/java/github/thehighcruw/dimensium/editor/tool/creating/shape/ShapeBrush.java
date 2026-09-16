/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.shape;

import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.state.PaletteState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class ShapeBrush implements BrushStrategy {

    private static final Random rand = new Random();

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        ShapeToolState s = ShapeToolState.INSTANCE;
        PaletteState ps = PaletteState.INSTANCE;
        if (ps.palette.isEmpty()) return;
        int w = s.shapeWidth, h = s.shapeHeight, d = s.shapeDepth;

        if (s.shapeType == ShapeToolState.ShapeType.TORUS) {
            int outer = s.torusRingRadius + s.torusTubeRadius;
            w = outer * 2 + 1;
            h = s.torusTubeRadius * 2 + 1;
            d = outer * 2 + 1;
        } else if (s.shapeType == ShapeToolState.ShapeType.ARCHIMEDEAN_SPIRAL) {
            int r = (int) Math.ceil(s.shapeSpiralSpacing * s.shapeSpiralTurns);
            w = r * 2 + 1;
            h = 1;
            d = r * 2 + 1;
        } else if (!s.shapeSeparateAxes
                && (s.shapeType == ShapeToolState.ShapeType.CYLINDER
                        || s.shapeType == ShapeToolState.ShapeType.CONE
                        || s.shapeType == ShapeToolState.ShapeType.TUBE)) {
            d = w;
        }

        // Center on hit block: offset so dx=0..w-1 is symmetric around mop.blockX.
        final int fw = w, fh = h, fd = d;
        Vec3DInt origin =
                Vec3DInt.from(mop.blockX - (fw - 1) / 2, mop.blockY - (fh - 1) / 2, mop.blockZ - (fd - 1) / 2);

        Vec3DInt shapeDims = Vec3DInt.from(fw, fh, fd);
        Vec3DInt.forEachInclusive(Vec3DInt.ZERO, shapeDims.minus(1), (dx, dy, dz) -> {
            if (!inShapeGeom(s, Vec3DInt.from(dx, dy, dz), shapeDims)) return;
            Vec3DInt pos = origin.plus(dx, dy, dz);
            if (s.shapeKeepExisting && WorldUtils.getBlock(world, pos) != Blocks.air) return;
            ItemStack item = ps.samplePalette(rand);
            if (item == null) return;
            Block blk = Block.getBlockFromItem(item.getItem());
            int meta = item.getItemDamage();
            if (blk != null && blk != Blocks.air) ChangeProposal.write(world, pos, blk, meta);
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
