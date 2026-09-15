/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.world.World;

public class MaskUtils {

    private MaskUtils() {}

    public static boolean testAnyAtOffsets(World world, Vec3DInt[] offsets, Vec3DInt coord, int blockId, int meta) {
        for (Vec3DInt offset : offsets) {
            Vec3DInt neighbour = coord.plus(offset);
            if (testAt(world, neighbour, blockId, meta)) return true;
        }
        return false;
    }

    public static boolean testAt(World world, Vec3DInt coord, int blockId, int meta) {
        Block b = WorldUtils.getBlock(world, coord);
        if (Block.getIdFromBlock(b) != blockId) return false;
        return meta < 0 || WorldUtils.getBlockMetadata(world, coord) == meta;
    }
}
