/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.brushes;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public interface BrushStrategy {

    void apply(World world, MovingObjectPosition mop);
}
