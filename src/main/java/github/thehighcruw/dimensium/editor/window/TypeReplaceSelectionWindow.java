/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
public class TypeReplaceSelectionWindow extends AbstractReplaceWindow {

    public static final TypeReplaceSelectionWindow INSTANCE = new TypeReplaceSelectionWindow();

    private TypeReplaceSelectionWindow() {}

    @Override
    protected boolean flagDefault() {
        return true;
    }

    @Override
    protected String windowId() {
        return "###type_replace_selection_window";
    }

    @Override
    protected String titleKey() {
        return "dimensium.op.type_replace.title";
    }

    @Override
    protected String prefix() {
        return "trepl";
    }

    @Override
    protected String block1LabelKey() {
        return "dimensium.op.type_replace.source";
    }

    @Override
    protected String block2LabelKey() {
        return "dimensium.op.type_replace.target";
    }

    @Override
    protected String checkboxKey() {
        return "dimensium.op.type_replace.preserve";
    }

    @Override
    protected void applyOp() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection() || block1 == null || block2 == null) return;
        Block srcBlock = Block.getBlockFromItem(block1.getItem());
        Block tgtBlock = Block.getBlockFromItem(block2.getItem());
        if (srcBlock == null || srcBlock == Blocks.air || tgtBlock == null) return;

        int tgtId = Block.getIdFromBlock(tgtBlock);
        int tgtMeta = block2.getItemDamage();

        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;

        List<int[]> ops = new ArrayList<>();
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt cv = SelectionState.unpack(key);
            int x = cv.x(), y = cv.y(), z = cv.z();
            Block worldBlock = world.getBlock(x, y, z);
            if (worldBlock != srcBlock) continue;
            int outMeta = flag ? world.getBlockMetadata(x, y, z) : tgtMeta;
            ops.add(new int[] {x, y, z, tgtId, outMeta});
        }
        BlockSender.sendChunked(ops, I18n.format("dimensium.action.op.type_replace"));
    }
}
