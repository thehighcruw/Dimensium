/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

public class ClipboardBlockAccess implements IBlockAccess {

    private final Map<Long, SelectionState.BlockData> data;
    private final Vec3DInt dims;

    public ClipboardBlockAccess(Map<Long, SelectionState.BlockData> data, Vec3DInt dims) {
        this.data = data;
        this.dims = dims;
    }

    private SelectionState.BlockData at(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= dims.x() || y >= dims.y() || z >= dims.z()) return null;
        return data.get(SelectionState.clipboardKey(x, y, z));
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        SelectionState.BlockData bd = at(x, y, z);
        return (bd == null) ? Blocks.air : bd.block();
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        SelectionState.BlockData bd = at(x, y, z);
        return (bd == null) ? 0 : bd.meta();
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getBlock(x, y, z) == Blocks.air;
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return null;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        return 0;
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return BiomeGenBase.plains;
    }

    @Override
    public int getHeight() {
        return Math.max(dims.y(), 1);
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    // Max packed brightness: sky=15, block=15
    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int min) {
        return 0xF000F0;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean def) {
        SelectionState.BlockData bd = at(x, y, z);
        if (bd == null || bd.block() == Blocks.air) return false;
        return bd.block().isSideSolid(this, x, y, z, side);
    }
}
