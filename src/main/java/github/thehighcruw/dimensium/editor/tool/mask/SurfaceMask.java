/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.BlockUtils;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.world.World;

public class SurfaceMask extends MaskNode {

    @Override
    public boolean test(World world, Vec3DInt coord) {
        for (Vec3DInt offset : BlockUtils.NEIGHBOUR_OFFSETS) {
            Vec3DInt neighbour = coord.plus(offset);
            Block b = WorldUtils.getBlock(neighbour);
            if (world.isAirBlock(neighbour.x(), neighbour.y(), neighbour.z())) return true;
            if (b instanceof BlockLiquid) return true;
        }
        return false;
    }

    @Override
    public String displayName() {
        return "Surface";
    }
}
