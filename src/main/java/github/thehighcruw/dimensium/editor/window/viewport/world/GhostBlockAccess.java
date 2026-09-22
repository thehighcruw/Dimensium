/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Fake IBlockAccess backed by a ChangeProposal block map (world-space coords,
 * ChangeProposal.packKey format, value = {blockId, meta}).
 * Returns max brightness so ghost blocks render fully lit without world lighting.
 * Entries with blockId == 0 are treated as air (removals in a proposal).
 */
class GhostBlockAccess implements IBlockAccess {

    private final Map<Long, int[]> blocks;

    GhostBlockAccess(Map<Long, int[]> blocks) {
        this.blocks = blocks;
    }

    private int[] at(int x, int y, int z) {
        return blocks.get(ChangeProposal.packKey(x, y, z));
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        int[] bm = at(x, y, z);
        if (bm == null || bm[0] == 0) return Blocks.air;
        Block b = Block.getBlockById(bm[0]);
        return b == null ? Blocks.air : b;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        int[] bm = at(x, y, z);
        return (bm == null) ? 0 : bm[1];
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
        return 256;
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
        Block b = getBlock(x, y, z);
        if (b == Blocks.air) return false;
        return b.isSideSolid(this, x, y, z, side);
    }
}
