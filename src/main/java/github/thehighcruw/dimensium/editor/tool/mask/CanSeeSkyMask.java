/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.world.World;

public class CanSeeSkyMask extends MaskNode {

    @Override
    public boolean test(World world, Vec3DInt coord) {
        return world.canBlockSeeTheSky(coord.x(), coord.y(), coord.z());
    }

    @Override
    public String displayName() {
        return "Can See Sky";
    }
}
