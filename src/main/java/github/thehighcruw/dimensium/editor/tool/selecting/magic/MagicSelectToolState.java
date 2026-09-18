/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.magic;

import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.Set;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MagicSelectToolState {

    public static final MagicSelectToolState INSTANCE = new MagicSelectToolState();

    public enum MagicCompareType {
        BLOCK_STATE("dimensium.magic_compare.block_state"),
        BLOCK("dimensium.magic_compare.block"),
        SOLID("dimensium.magic_compare.solid"),
        ANY("dimensium.magic_compare.any");

        public final String label;

        MagicCompareType(String label) {
            this.label = label;
        }
    }

    public enum MagicDirection {
        BOTH("dimensium.magic_dir.both"),
        UP_ONLY("dimensium.magic_dir.up_only"),
        DOWN_ONLY("dimensium.magic_dir.down_only");

        public final String label;

        MagicDirection(String label) {
            this.label = label;
        }
    }

    public int magicSelectLimit = 10000;
    public int magicSelectRange = 1;
    public boolean magicSelectSurface = false;
    public boolean magicSelectCorners = false;
    public MagicCompareType magicCompareType = MagicCompareType.BLOCK_STATE;
    public MagicDirection magicDirection = MagicDirection.BOTH;

    public Set<Long> floodFillFrom(World world, MovingObjectPosition mop) {
        return SelectionState.floodFill(
                world,
                Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ),
                magicSelectLimit,
                magicSelectRange,
                magicSelectSurface,
                magicSelectCorners,
                magicCompareType,
                magicDirection);
    }
}
