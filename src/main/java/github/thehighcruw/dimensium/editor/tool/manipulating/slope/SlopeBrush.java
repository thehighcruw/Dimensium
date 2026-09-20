/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.slope;

import github.thehighcruw.dimensium.editor.tool.ActiveDragState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.shared.math.Vec2DInt;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class SlopeBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        SlopeToolState state = SlopeToolState.INSTANCE;
        if (!state.hasPos1 || !state.hasPos2) return;

        Vec3DInt pos1 = state.pos1;
        Vec3DInt pos2 = state.pos2;
        Vec3DInt brushCenter = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        int radius = state.slopeRadius;

        double axisX = pos2.x() - pos1.x();
        double axisZ = pos2.z() - pos1.z();
        double axisLen2 = axisX * axisX + axisZ * axisZ;
        double axisLen = Math.sqrt(axisLen2);
        double heightDelta = pos2.y() - pos1.y();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double brushR = Math.sqrt((double) dx * dx + (double) dz * dz) / radius;
                if (brushR > 1.0) continue;

                double edgeWeight = edgeFalloff(brushR, state.slopeSmoothing);
                if (edgeWeight <= 0.0) continue;

                Vec2DInt column = Vec2DInt.from(brushCenter.x() + dx, brushCenter.z() + dz);

                double t = computeT(state, pos1, column, axisX, axisZ, axisLen2, axisLen);
                if (state.slopeClamp) {
                    t = Math.max(0.0, Math.min(1.0, t));
                }

                double targetY = pos1.y() + t * heightDelta;
                int topY = getTopSolidY(world, column);
                int flattenY = Math.max(0, Math.min(255, (int) Math.round(topY + edgeWeight * (targetY - topY))));

                applyFlatten(world, column, topY, flattenY, state.slopeApplyMode);
            }
        }
    }

    private static double computeT(
            SlopeToolState state,
            Vec3DInt pos1,
            Vec2DInt column,
            double axisX,
            double axisZ,
            double axisLen2,
            double axisLen) {
        if (state.slopeShape == SlopeToolState.SlopeShape.PLANE) {
            if (axisLen2 < 0.0001) return 0.0;
            return ((column.x() - pos1.x()) * axisX + (column.y() - pos1.z()) * axisZ) / axisLen2;
        } else {
            double colDx = column.x() - pos1.x();
            double colDz = column.y() - pos1.z();
            double columnRadius = Math.sqrt(colDx * colDx + colDz * colDz);
            if (axisLen < 0.0001) return 0.0;
            return columnRadius / axisLen;
        }
    }

    private static void applyFlatten(
            World world, Vec2DInt column, int topY, int flattenY, SlopeToolState.SlopeApplyMode mode) {
        int cx = column.x(), cz = column.y();
        if (topY < flattenY && mode != SlopeToolState.SlopeApplyMode.LOWER) {
            for (int y = topY + 1; y <= flattenY; y++) {
                Block source = world.getBlock(cx, topY, cz);
                int meta = world.getBlockMetadata(cx, topY, cz);
                if (source == null || source == Blocks.air) source = Blocks.dirt;
                ChangeProposal.write(world, Vec3DInt.from(cx, y, cz), source, meta);
            }
        } else if (topY > flattenY && mode != SlopeToolState.SlopeApplyMode.RAISE) {
            for (int y = topY; y > flattenY; y--) {
                ChangeProposal.write(world, Vec3DInt.from(cx, y, cz), Blocks.air, 0);
            }
        }
    }

    private static double edgeFalloff(double r, float smoothing) {
        if (r >= 1.0) return 0.0;
        if (smoothing <= 0f) return 1.0;
        double fadeStart = 1.0 - smoothing;
        if (r <= fadeStart) return 1.0;
        double t = (r - fadeStart) / smoothing;
        return 1.0 - t * t * (3.0 - 2.0 * t);
    }

    private static int getTopSolidY(World world, Vec2DInt column) {
        ChangeProposal drag = ActiveDragState.INSTANCE.activeDrag;
        for (int y = 255; y >= 0; y--) {
            if (drag != null) {
                long key = ChangeProposal.packKey(column.x(), y, column.y());
                int[] bm = drag.proposed.get(key);
                if (bm != null) {
                    if (bm[0] != 0) return y;
                    continue;
                }
            }
            if (world.getBlock(column.x(), y, column.y()) != Blocks.air) return y;
        }
        return 0;
    }
}
