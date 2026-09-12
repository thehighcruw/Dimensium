/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.mask;

import net.minecraft.world.World;

public class OffsetNode extends LogicNode {

    public int dx, dy, dz;

    public OffsetNode(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    @Override
    public boolean test(World world, int x, int y, int z) {
        int ox = x + dx, oy = y + dy, oz = z + dz;
        for (MaskNode child : children) {
            if (!child.test(world, ox, oy, oz)) return false;
        }
        return true;
    }

    @Override
    public String displayName() {
        return "OFFSET(" + dx + "," + dy + "," + dz + ")";
    }
}
