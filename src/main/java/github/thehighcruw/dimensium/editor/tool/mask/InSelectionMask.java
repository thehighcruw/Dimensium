/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.world.World;

public class InSelectionMask extends MaskNode {

    @Override
    public boolean test(World world, Vec3DInt coord) {
        return SelectionState.INSTANCE.contains(coord);
    }

    @Override
    public String displayName() {
        return "In Selection";
    }
}
