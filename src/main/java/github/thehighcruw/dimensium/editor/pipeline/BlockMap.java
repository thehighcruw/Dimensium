/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.world.World;

public class BlockMap {

    private final Map<Long, int[]> blocks = new HashMap<>();

    /**
     * Packs (x, y, z) into a long key using 26 signed bits per component.
     * Supports coordinates in [-33554432, 33554431] — handles negative Y from trunk base spheres.
     */
    public static long packKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF)) | (((long) (y & 0x3FFFFFF)) << 26) | (((long) (z & 0x3FFFFFF)) << 52);
    }

    public static long packKey(Vec3DInt v) {
        return packKey(v.x(), v.y(), v.z());
    }

    public static Vec3DInt unpackKey(long k) {
        return Vec3DInt.from(
                signExtend26((int) (k & 0x3FFFFFF)), signExtend26((int) ((k >> 26) & 0x3FFFFFF)), signExtend26((int)
                        ((k >> 52) & 0x3FFFFFF)));
    }

    private static int signExtend26(int v) {
        return (v << 6) >> 6;
    }

    public void put(Vec3DInt pos, Block block, int meta) {
        blocks.put(packKey(pos), new int[] {Block.getIdFromBlock(block), meta});
    }

    public boolean contains(Vec3DInt pos) {
        return blocks.containsKey(packKey(pos));
    }

    public void merge(BlockMap other) {
        blocks.putAll(other.blocks);
    }

    public Map<Long, int[]> entries() {
        return blocks;
    }

    public void applyToWorld(World world) {
        for (Map.Entry<Long, int[]> entry : blocks.entrySet()) {
            Vec3DInt pos = unpackKey(entry.getKey());
            Block block = Block.getBlockById(entry.getValue()[0]);
            int meta = entry.getValue()[1];
            ChangeProposal.write(world, pos, block, meta);
        }
    }

    public int size() {
        return blocks.size();
    }
}
