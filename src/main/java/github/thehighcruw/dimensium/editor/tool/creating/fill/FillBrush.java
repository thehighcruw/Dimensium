/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.fill;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.BlockUtils;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
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
        Vec3DInt start = WorldUtils.mopToCoord(mop);

        Block target = WorldUtils.getBlock(world, start);
        Block paint = s.getPaintBlock();
        int meta = s.getPaintMeta();
        if (target == paint) return;

        Queue<Vec3DInt> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        int count = 0;

        queue.add(start);
        visited.add(start.asCommaString());

        while (!queue.isEmpty() && count < FloodfillToolState.FILL_MAX) {
            Vec3DInt pos = queue.poll();
            if (WorldUtils.getBlock(world, pos) != target) continue;

            ChangeProposal.write(world, pos, paint, meta);
            count++;

            for (Vec3DInt offset : BlockUtils.NEIGHBOUR_OFFSETS) {
                Vec3DInt neighbour = pos.plus(offset);
                String key = neighbour.asCommaString();

                if (!visited.contains(key) && WorldUtils.getBlock(world, neighbour) == target) {
                    visited.add(key);
                    queue.add(neighbour);
                }
            }
        }
    }
}
