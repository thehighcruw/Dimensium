/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.brushes;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.math.ShapeMath;
import github.thehighcruw.dimensium.tool.state.PaletteState;
import github.thehighcruw.dimensium.tool.state.ShapeToolState;

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
            && (s.shapeType == ShapeToolState.ShapeType.CYLINDER || s.shapeType == ShapeToolState.ShapeType.CONE
                || s.shapeType == ShapeToolState.ShapeType.TUBE)) {
                    d = w;
                }

        // Center on hit block: offset so dx=0..w-1 is symmetric around mop.blockX.
        int x = mop.blockX - (w - 1) / 2;
        int y = mop.blockY - (h - 1) / 2;
        int z = mop.blockZ - (d - 1) / 2;

        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                for (int dz = 0; dz < d; dz++) {
                    if (!inShapeGeom(s, dx, dy, dz, w, h, d)) continue;
                    if (s.shapeKeepExisting && world.getBlock(x + dx, y + dy, z + dz) != Blocks.air) continue;
                    ItemStack item = ps.samplePalette(rand);
                    if (item == null) continue;
                    Block blk = Block.getBlockFromItem(item.getItem());
                    int meta = item.getItemDamage();
                    if (blk != null && blk != Blocks.air)
                        ChangeProposal.write(world, x + dx, y + dy, z + dz, blk, meta);
                }
            }
        }
    }

    private static boolean inShapeGeom(ShapeToolState s, int dx, int dy, int dz, int w, int h, int d) {
        return ShapeMath.inShapeGeom(
            s.shapeType,
            dx,
            dy,
            dz,
            w,
            h,
            d,
            s.shapeHollow,
            s.shapeExponent,
            s.torusRingRadius,
            s.torusRingRadiusZ,
            s.torusTubeRadius,
            s.tubeWallThickness,
            s.shapeSupersphereExp,
            s.shapePolygonSides,
            s.shapeSpiralSpacing,
            s.shapeSpiralTurns);
    }
}
