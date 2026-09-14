/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public class AdjacentMask extends MaskNode {

    public int blockId;
    public int meta;

    private static final int[][] OFFSETS = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};

    public AdjacentMask(int blockId, int meta) {
        this.blockId = blockId;
        this.meta = meta;
    }

    @Override
    public boolean test(World world, int x, int y, int z) {
        for (int[] o : OFFSETS) {
            Block b = world.getBlock(x + o[0], y, z + o[2]);
            if (Block.getIdFromBlock(b) != blockId) continue;
            if (meta >= 0 && world.getBlockMetadata(x + o[0], y, z + o[2]) != meta) continue;
            return true;
        }
        return false;
    }

    @Override
    public String displayName() {
        return "Adjacent: " + blockLabel(blockId, meta);
    }
}
