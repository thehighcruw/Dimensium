/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.mask;

import net.minecraft.world.World;

public class AngleMask extends MaskNode {

    public float angle;
    public float range;

    public AngleMask(float angle, float range) {
        this.angle = angle;
        this.range = range;
    }

    @Override
    public boolean test(World world, int x, int y, int z) {
        int[] dxs = { 1, -1, 0, 0 };
        int[] dzs = { 0, 0, 1, -1 };
        int maxDiff = 0;
        for (int i = 0; i < 4; i++) {
            int nx = x + dxs[i], nz = z + dzs[i];
            int h0 = topSolidY(world, x, y, z);
            int h1 = topSolidY(world, nx, y, nz);
            int diff = Math.abs(h1 - h0);
            if (diff > maxDiff) maxDiff = diff;
        }
        double blockAngle = Math.toDegrees(Math.atan2(maxDiff, 1));
        return Math.abs(blockAngle - angle) <= range;
    }

    private int topSolidY(World world, int x, int startY, int z) {
        for (int y = startY + 2; y >= startY - 2; y--) {
            if (!world.isAirBlock(x, y, z)) return y;
        }
        return startY;
    }

    @Override
    public String displayName() {
        return "Angle: " + (int) angle + " ±" + (int) range;
    }
}
