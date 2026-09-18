/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;

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
    protected String block1HeaderKey() {
        return "dimensium.op.replace.find";
    }

    @Override
    protected String block2HeaderKey() {
        return "dimensium.op.replace.replace_with";
    }

    @Override
    protected String checkboxKey() {
        return "dimensium.op.replace.exact_meta";
    }

    @Override
    protected String applyActionKey() {
        return "dimensium.action.op.replace";
    }

    @Override
    protected int[] buildBlockOp(int x, int y, int z, Block worldBlock, int worldMeta, BlockMapping mapping) {
        Block findBlock = Block.getBlockFromItem(mapping.source().getItem());
        if (worldBlock != findBlock) return null;
        if (flag && worldMeta != mapping.source().getItemDamage()) return null;
        Block replaceBlock = Block.getBlockFromItem(mapping.target().getItem());
        return new int[] {
            x, y, z, Block.getIdFromBlock(replaceBlock), mapping.target().getItemDamage()
        };
    }
}
