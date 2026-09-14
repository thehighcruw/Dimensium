/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;

public final class WorldUtils {

    private WorldUtils() {}

    public static @Nullable Block getWorldBlock(Vec3DInt coord) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return null;

        return world.getBlock(coord.x(), coord.y(), coord.z());
    }

    public static int getWorldBlockMeta(Vec3DInt coord) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return -1;

        return world.getBlockMetadata(coord.x(), coord.y(), coord.z());
    }
}
