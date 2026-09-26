/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Fake {@link IBlockAccess} backed by a normalized (0-based) int[] map of (blockId, meta) pairs.
 * Keys use the same bit-pack scheme as {@link github.thehighcruw.dimensium.tool.ChangeProposal}.
 */
class BlockMapBlockAccess implements IBlockAccess {

    private final Map<Long, int[]> data;
    private final Vec3DInt dims;

    BlockMapBlockAccess(Map<Long, int[]> data, Vec3DInt dims) {
        this.data = data;
        this.dims = dims;
    }

    static long packKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF)) | (((long) (y & 0x3FFFFFF)) << 26) | (((long) (z & 0x3FFFFFF)) << 52);
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= dims.x() || y >= dims.y() || z >= dims.z()) return Blocks.air;
        int[] bd = data.get(packKey(x, y, z));
        if (bd == null) return Blocks.air;
        Block block = Block.getBlockById(bd[0]);
        return block == null ? Blocks.air : block;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= dims.x() || y >= dims.y() || z >= dims.z()) return 0;
        int[] bd = data.get(packKey(x, y, z));
        return bd == null ? 0 : bd[1];
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

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int min) {
        return 0xF000F0;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean def) {
        Block block = getBlock(x, y, z);
        if (block == Blocks.air) return false;
        return block.isSideSolid(this, x, y, z, side);
    }
}
