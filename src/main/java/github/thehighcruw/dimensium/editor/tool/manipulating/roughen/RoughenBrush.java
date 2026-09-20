/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.roughen;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.BlockUtils;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class RoughenBrush implements BrushStrategy {

    private static final Random RNG = new Random();

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        RoughenToolState s = RoughenToolState.INSTANCE;
        Vec3DInt coord = WorldUtils.mopToCoord(mop);

        List<Vec3DInt> candidates = new ArrayList<>();
        BrushUtil.forBrush(bs, offset -> {
            Vec3DInt pos = coord.plus(offset);
            if (WorldUtils.getBlock(world, pos) == Blocks.air) return;
            int exposed = countAirFaces(world, pos);
            if (exposed >= s.faces) {
                candidates.add(pos);
            }
        });

        Collections.shuffle(candidates, RNG);
        int count = Math.round(candidates.size() * s.rougheningRatio);
        for (int i = 0; i < count; i++) {
            ChangeProposal.write(world, candidates.get(i), Blocks.air, 0);
        }
    }

    private static int countAirFaces(World world, Vec3DInt pos) {
        int count = 0;
        for (Vec3DInt offset : BlockUtils.NEIGHBOUR_OFFSETS) {
            if (WorldUtils.getBlock(world, pos.plus(offset)) == Blocks.air) count++;
        }
        return count;
    }
}
