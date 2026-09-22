/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.world.World;

public class BlockMap {

    private final Map<Long, int[]> blocks = new HashMap<>();

    public void put(Vec3DInt pos, Block block, int meta) {
        blocks.put(ChangeProposal.packKey(pos), new int[] {Block.getIdFromBlock(block), meta});
    }

    public boolean contains(Vec3DInt pos) {
        return blocks.containsKey(ChangeProposal.packKey(pos));
    }

    public void merge(BlockMap other) {
        blocks.putAll(other.blocks);
    }

    public Map<Long, int[]> entries() {
        return blocks;
    }

    public void applyToWorld(World world) {
        for (Map.Entry<Long, int[]> entry : blocks.entrySet()) {
            Vec3DInt pos = ChangeProposal.unpackKey(entry.getKey());
            Block block = Block.getBlockById(entry.getValue()[0]);
            int meta = entry.getValue()[1];
            ChangeProposal.write(world, pos, block, meta);
        }
    }

    public int size() {
        return blocks.size();
    }
}
