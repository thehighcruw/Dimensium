/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.world.World;

public class AndNode extends LogicNode {

    @Override
    public boolean test(World world, Vec3DInt coord) {
        for (MaskNode child : children) {
            if (!child.test(world, coord)) return false;
        }
        return true;
    }

    @Override
    public String displayName() {
        return "AND";
    }
}
