/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.mask;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public class BlockMask extends MaskNode {

    public int blockId;
    public int meta;

    public BlockMask(int blockId, int meta) {
        this.blockId = blockId;
        this.meta = meta;
    }

    @Override
    public boolean test(World world, int x, int y, int z) {
        Block b = world.getBlock(x, y, z);
        if (Block.getIdFromBlock(b) != blockId) return false;
        return meta < 0 || world.getBlockMetadata(x, y, z) == meta;
    }

    @Override
    public String displayName() {
        return "Block: " + blockLabel(blockId, meta);
    }
}
