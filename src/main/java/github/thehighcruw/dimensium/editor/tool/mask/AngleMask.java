/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.BlockUtils;
import net.minecraft.world.World;

public class AngleMask extends MaskNode {

    public float angle;
    public float range;

    public AngleMask(float angle, float range) {
        this.angle = angle;
        this.range = range;
    }

    @Override
    public boolean test(World world, Vec3DInt coord) {
        int maxDiff = 0;
        int h0 = topSolidY(world, coord);
        for (Vec3DInt offset : BlockUtils.ADJACENT_OFFSETS) {
            Vec3DInt neighbour = coord.plus(offset);
            int h1 = topSolidY(world, neighbour);
            int diff = Math.abs(h1 - h0);
            if (diff > maxDiff) maxDiff = diff;
        }
        double blockAngle = Math.toDegrees(Math.atan2(maxDiff, 1));
        return Math.abs(blockAngle - angle) <= range;
    }

    private int topSolidY(World world, Vec3DInt coord) {
        for (int y = coord.y() + 2; y >= coord.y() - 2; y--) {
            if (!world.isAirBlock(coord.x(), y, coord.z())) return y;
        }
        return coord.y();
    }

    @Override
    public String displayName() {
        return "Angle: " + (int) angle + " ±" + (int) range;
    }
}
