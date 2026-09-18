/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.brushes;

import com.github.bsideup.jabel.Desugar;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

@Desugar
public record GaussianBrushContext(
        BrushState bs,
        Vec3DInt origin,
        Vec3DInt brushSize,
        GaussianKernel kernel,
        int snStX,
        int snStY,
        int margin,
        int[] snapId) {

    public static GaussianBrushContext build(World world, MovingObjectPosition mop, float smoothStrength) {
        BrushState bs = BrushState.INSTANCE;
        Vec3DInt origin = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        int sx = Math.min(bs.brushRadius, 12);
        int sy = Math.min(bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius, 12);
        GaussianKernel kernel = GaussianKernel.build(smoothStrength * 0.5f + 0.5f);
        int margin = kernel.kR;
        Vec3DInt brushSize = Vec3DInt.from(sx, sy, sx);
        int snStY = 2 * (sx + margin) + 1;
        int snStX = (2 * (sy + margin) + 1) * snStY;
        int[] snapId = BrushUtil.snapshotBlockIds(world, origin, brushSize, margin);
        return new GaussianBrushContext(bs, origin, brushSize, kernel, snStX, snStY, margin, snapId);
    }

    public Vec3DInt snapCoord(Vec3DInt offset) {
        int sx = brushSize.x(), sy = brushSize.y();
        return offset.plus(sx + margin, sy + margin, sx + margin);
    }
}
