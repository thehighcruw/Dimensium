/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.mask;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public class NearMask extends MaskNode {

    public int blockId;
    public int meta;
    public int radius;

    public NearMask(int blockId, int meta, int radius) {
        this.blockId = blockId;
        this.meta = meta;
        this.radius = Math.max(1, radius);
    }

    @Override
    public boolean test(World world, int x, int y, int z) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
                    Block b = world.getBlock(x + dx, y + dy, z + dz);
                    if (Block.getIdFromBlock(b) != blockId) continue;
                    if (meta >= 0 && world.getBlockMetadata(x + dx, y + dy, z + dz) != meta) continue;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String displayName() {
        return "Near(" + radius + "): " + blockLabel(blockId, meta);
    }
}
