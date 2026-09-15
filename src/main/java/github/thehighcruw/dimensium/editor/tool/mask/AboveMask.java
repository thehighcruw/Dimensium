/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.block.Block;
import net.minecraft.world.World;

public class AboveMask extends MaskNode {

    public int blockId;
    public int meta;

    public AboveMask(int blockId, int meta) {
        this.blockId = blockId;
        this.meta = meta;
    }

    @Override
    public boolean test(World world, Vec3DInt coord) {
        Block b = world.getBlock(coord.x(), coord.y() + 1, coord.z());
        if (Block.getIdFromBlock(b) != blockId) return false;
        return meta < 0 || world.getBlockMetadata(coord.x(), coord.y() + 1, coord.z()) == meta;
    }

    @Override
    public String displayName() {
        return "Above: " + blockLabel(blockId, meta);
    }
}
