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
        Vec3DInt.anyInclusive(Vec3DInt.from(-radius), Vec3DInt.from(radius), (dx, dy, dz) -> {
            if (dx == 0 && dy == 0 && dz == 0) return false;
            Vec3DInt offset = Vec3DInt.from(dx, dy, dz);
            if (offset.lengthSq() > (long) radius * radius) return false;
            return MaskUtils.testAt(world, coord.plus(offset), blockId, meta);
        });
        return false;
    }

    @Override
    public String displayName() {
        return "Near(" + radius + "): " + blockLabel(blockId, meta);
    }
}
