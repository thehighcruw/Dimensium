/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public abstract class MaskNode {

    public abstract boolean test(World world, int x, int y, int z);

    public abstract String displayName();

    public List<MaskNode> children() {
        return Collections.emptyList();
    }

    public boolean isLogic() {
        return false;
    }

    protected static String blockLabel(int blockId, int meta) {
        if (blockId == 0) return "Air";
        Block b = Block.getBlockById(blockId);
        String name = b != null ? b.getLocalizedName() : "block:" + blockId;
        return name + (meta >= 0 ? ":" + meta : "");
    }
}
