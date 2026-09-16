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
public class ReplaceSelectionWindow extends AbstractReplaceWindow {

    public static final ReplaceSelectionWindow INSTANCE = new ReplaceSelectionWindow();

    private ReplaceSelectionWindow() {}

    @Override
    protected boolean flagDefault() {
        return false;
    }

    @Override
    protected String windowId() {
        return "###replace_selection_window";
    }

    @Override
    protected String titleKey() {
        return "dimensium.op.replace.title";
    }

    @Override
    protected String prefix() {
        return "rep";
    }

    @Override
    protected String block1LabelKey() {
        return "dimensium.op.replace.find";
    }

    @Override
    protected String block2LabelKey() {
        return "dimensium.op.replace.replace_with";
    }

    @Override
    protected String checkboxKey() {
        return "dimensium.op.replace.exact_meta";
    }

    @Override
    protected void applyOp() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection() || block1 == null || block2 == null) return;
        Block findB = Block.getBlockFromItem(block1.getItem());
        Block replaceB = Block.getBlockFromItem(block2.getItem());
        if (findB == null || findB == Blocks.air || replaceB == null) return;

        int findMeta = block1.getItemDamage();
        int replaceId = Block.getIdFromBlock(replaceB);
        int replaceMeta = block2.getItemDamage();

        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;

        List<int[]> ops = new ArrayList<>();
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt cv = SelectionState.unpack(key);
            int x = cv.x(), y = cv.y(), z = cv.z();
            Block worldBlock = world.getBlock(x, y, z);
            if (worldBlock != findB) continue;
            if (flag && world.getBlockMetadata(x, y, z) != findMeta) continue;
            ops.add(new int[] {x, y, z, replaceId, replaceMeta});
        }
        BlockSender.sendChunked(ops, I18n.format("dimensium.action.op.replace"));
    }
}
