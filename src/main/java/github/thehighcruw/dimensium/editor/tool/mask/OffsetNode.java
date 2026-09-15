/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.world.World;

public class OffsetNode extends LogicNode {

    public Vec3DInt offset;

    public OffsetNode(Vec3DInt offset) {
        this.offset = offset;
    }

    @Override
    public boolean test(World world, Vec3DInt coord) {
        Vec3DInt atOffset = coord.plus(offset);
        for (MaskNode child : children) {
            if (!child.test(world, atOffset)) return false;
        }
        return true;
    }

    @Override
    public String displayName() {
        return "OFFSET(" + offset.x() + "," + offset.y() + "," + offset.z() + ")";
    }
}
