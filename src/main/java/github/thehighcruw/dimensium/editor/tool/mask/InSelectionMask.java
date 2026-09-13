/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import net.minecraft.world.World;

import github.thehighcruw.dimensium.shared.SelectionState;

public class InSelectionMask extends MaskNode {

    @Override
    public boolean test(World world, int x, int y, int z) {
        return SelectionState.INSTANCE.contains(x, y, z);
    }

    @Override
    public String displayName() {
        return "In Selection";
    }
}
