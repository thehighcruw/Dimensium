/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
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
    public boolean test(World world, Vec3DInt coord) {
        return Vec3DInt.anyInclusive(Vec3DInt.from(-radius), Vec3DInt.from(radius), offset -> {
            if (offset.equals(Vec3DInt.ZERO)) return false;
            return offset.lengthSq() <= (long) radius * radius
                    && MaskUtils.testAt(world, coord.plus(offset), blockId, meta);
        });
    }

    @Override
    public String displayName() {
        return "Near(" + radius + "): " + blockLabel(blockId, meta);
    }
}
