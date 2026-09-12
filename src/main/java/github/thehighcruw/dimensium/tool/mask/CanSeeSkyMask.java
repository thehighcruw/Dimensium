/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.mask;

import net.minecraft.world.World;

public class CanSeeSkyMask extends MaskNode {

    @Override
    public boolean test(World world, int x, int y, int z) {
        return world.canBlockSeeTheSky(x, y, z);
    }

    @Override
    public String displayName() {
        return "Can See Sky";
    }
}
