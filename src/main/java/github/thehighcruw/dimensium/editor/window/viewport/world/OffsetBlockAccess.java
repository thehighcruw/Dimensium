/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * IBlockAccess wrapper that adds an offset to all coordinates before forwarding to a delegate.
 * Used when rendering blocks at local (origin-relative) coordinates while the delegate
 * (e.g. the real world) is indexed by absolute world coordinates.
 */
class OffsetBlockAccess implements IBlockAccess {

    private final IBlockAccess delegate;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    OffsetBlockAccess(IBlockAccess delegate, Vec3DInt offset) {
        this.delegate = delegate;
        this.offsetX = offset.x();
        this.offsetY = offset.y();
        this.offsetZ = offset.z();
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return delegate.getBlock(x + offsetX, y + offsetY, z + offsetZ);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return delegate.getBlockMetadata(x + offsetX, y + offsetY, z + offsetZ);
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return delegate.isAirBlock(x + offsetX, y + offsetY, z + offsetZ);
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return delegate.getTileEntity(x + offsetX, y + offsetY, z + offsetZ);
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        return delegate.isBlockProvidingPowerTo(x + offsetX, y + offsetY, z + offsetZ, side);
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return delegate.getBiomeGenForCoords(x + offsetX, z + offsetZ);
    }

    @Override
    public int getHeight() {
        return delegate.getHeight();
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return delegate.extendedLevelsInChunkCache();
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int min) {
        return delegate.getLightBrightnessForSkyBlocks(x + offsetX, y + offsetY, z + offsetZ, min);
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean def) {
        return delegate.isSideSolid(x + offsetX, y + offsetY, z + offsetZ, side, def);
    }
}
