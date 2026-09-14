/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.fill;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class FillBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        SelectedBlockState s = SelectedBlockState.INSTANCE;
        int sx = mop.blockX, sy = mop.blockY, sz = mop.blockZ;
        Block target = world.getBlock(sx, sy, sz);
        Block paint = s.getPaintBlock();
        int meta = s.getPaintMeta();
        if (target == paint) return;

        Queue<int[]> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        int count = 0;

        queue.add(new int[] {sx, sy, sz});
        visited.add(sx + "," + sy + "," + sz);

        while (!queue.isEmpty() && count < FloodfillToolState.FILL_MAX) {
            int[] pos = queue.poll();
            int x = pos[0], y = pos[1], z = pos[2];
            if (world.getBlock(x, y, z) != target) continue;
            ChangeProposal.write(world, x, y, z, paint, meta);
            count++;
            for (int[] dir : dirs) {
                int nx = x + dir[0], ny = y + dir[1], nz = z + dir[2];
                String key = nx + "," + ny + "," + nz;
                if (!visited.contains(key) && world.getBlock(nx, ny, nz) == target) {
                    visited.add(key);
                    queue.add(new int[] {nx, ny, nz});
                }
            }
        }
    }
}
