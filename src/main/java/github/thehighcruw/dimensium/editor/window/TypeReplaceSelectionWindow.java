/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;

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
    protected String applyActionKey() {
        return "dimensium.action.op.type_replace";
    }

    @Override
    protected int[] buildBlockOp(
            int x, int y, int z, Block worldBlock, int worldMeta, Block findBlock, Block replaceBlock) {
        if (worldBlock != findBlock) return null;
        int outMeta = flag ? worldMeta : block2.getItemDamage();
        return new int[] {x, y, z, Block.getIdFromBlock(replaceBlock), outMeta};
    }
}
