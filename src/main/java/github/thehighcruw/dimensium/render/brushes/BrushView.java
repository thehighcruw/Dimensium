/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.brushes;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface BrushView {

    /** Which world-space voxels should glow in the active-paint preview. */
    boolean isBlockAffected(Minecraft mc, int wx, int wy, int wz);

    /**
     * Optional extra in-world render (gradient axis, elevation circle, etc.).
     * Return true to suppress the standard brush wireframe entirely.
     */
    default boolean renderCustomHover(Minecraft mc, MovingObjectPosition mop, double rx, double ry, double rz) {
        return false;
    }
}
