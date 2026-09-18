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
    protected String applyActionKey() {
        return "dimensium.action.op.replace";
    }

    @Override
    protected int[] buildBlockOp(
            int x, int y, int z, Block worldBlock, int worldMeta, Block findBlock, Block replaceBlock) {
        if (worldBlock != findBlock) return null;
        if (flag && worldMeta != block1.getItemDamage()) return null;
        return new int[] {x, y, z, Block.getIdFromBlock(replaceBlock), block2.getItemDamage()};
    }
}
