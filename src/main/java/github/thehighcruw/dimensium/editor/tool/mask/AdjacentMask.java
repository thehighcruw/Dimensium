/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.BlockUtils;
import net.minecraft.world.World;

public class AdjacentMask extends MaskNode {

    public int blockId;
    public int meta;

    public AdjacentMask(int blockId, int meta) {
        this.blockId = blockId;
        this.meta = meta;
    }

    @Override
    public boolean test(World world, Vec3DInt coord) {
        return MaskUtils.testAnyAtOffsets(world, BlockUtils.ADJACENT_OFFSETS, coord, blockId, meta);
    }

    @Override
    public String displayName() {
        return "Adjacent: " + blockLabel(blockId, meta);
    }
}
