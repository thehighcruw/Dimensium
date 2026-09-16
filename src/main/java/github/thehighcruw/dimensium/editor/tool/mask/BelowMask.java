/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.world.World;

public class BelowMask extends MaskNode {

    public int blockId;
    public int meta;

    public BelowMask(int blockId, int meta) {
        this.blockId = blockId;
        this.meta = meta;
    }

    @Override
    public boolean test(World world, Vec3DInt coord) {
        Vec3DInt below = coord.plus(0, -1, 0);
        Block b = WorldUtils.getBlock(world, below);
        if (Block.getIdFromBlock(b) != blockId) return false;
        return meta < 0 || WorldUtils.getBlockMetadata(world, below) == meta;
    }

    @Override
    public String displayName() {
        return "Below: " + blockLabel(blockId, meta);
    }
}
