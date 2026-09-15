/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
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
    public boolean test(World world, Vec3DInt coord) {
        Block b = WorldUtils.getBlock(world, coord);
        if (Block.getIdFromBlock(b) != blockId) return false;
        return meta < 0 || WorldUtils.getBlockMetadata(coord) == meta;
    }

    @Override
    public String displayName() {
        return "Block: " + blockLabel(blockId, meta);
    }
}
