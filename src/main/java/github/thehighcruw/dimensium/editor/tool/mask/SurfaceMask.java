/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.world.World;

public class SurfaceMask extends MaskNode {

    private static final int[][] OFFSETS = {{0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};

    @Override
    public boolean test(World world, int x, int y, int z) {
        for (int[] o : OFFSETS) {
            Block b = world.getBlock(x + o[0], y + o[1], z + o[2]);
            if (world.isAirBlock(x + o[0], y + o[1], z + o[2])) return true;
            if (b instanceof BlockLiquid) return true;
        }
        return false;
    }

    @Override
    public String displayName() {
        return "Surface";
    }
}
