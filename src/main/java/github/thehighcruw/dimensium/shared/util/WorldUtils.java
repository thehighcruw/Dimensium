/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public final class WorldUtils {

    private WorldUtils() {}

    public static @Nullable Block getBlock(Vec3DInt coord) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return null;

        return getBlock(world, coord);
    }

    public static @Nullable Block getBlock(World world, Vec3DInt coord) {
        return world.getBlock(coord.x(), coord.y(), coord.z());
    }

    public static void setBlock(World world, Vec3DInt coord, Block block, int meta, int flags) {
        world.setBlock(coord.x(), coord.y(), coord.z(), block, meta, flags);
    }

    public static int getBlockMetadata(Vec3DInt coord) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return -1;

        return world.getBlockMetadata(coord.x(), coord.y(), coord.z());
    }

    public static int getBlockMetadata(World world, Vec3DInt coord) {
        return world.getBlockMetadata(coord.x(), coord.y(), coord.z());
    }

    public static Vec3DInt mopToCoord(MovingObjectPosition mop) {
        return Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
    }

    /** Returns an ItemStack for the block under mop, or null if mop is null/non-block/air. */
    public static @Nullable ItemStack blockStackFromMop(World world, MovingObjectPosition mop) {
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;
        Block block = world.getBlock(mop.blockX, mop.blockY, mop.blockZ);
        if (block == null || block == Blocks.air) return null;
        if (Item.getItemFromBlock(block) == null) return null;
        int meta = world.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);
        return new ItemStack(block, 1, meta);
    }
}
