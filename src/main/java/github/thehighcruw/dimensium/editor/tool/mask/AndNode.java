/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import net.minecraft.world.World;

public class AndNode extends LogicNode {

    @Override
    public boolean test(World world, int x, int y, int z) {
        for (MaskNode child : children) {
            if (!child.test(world, x, y, z)) return false;
        }
        return true;
    }

    @Override
    public String displayName() {
        return "AND";
    }
}
