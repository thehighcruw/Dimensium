/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.selecting.box.BoxSelectToolState;
import imgui.ImGui;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class BooleanOpSection {

    private final ImInt comboIdx = new ImInt();

    public void render(BoxSelectToolState state) {
        BooleanOp[] ops = BooleanOp.values();
        String[] labels = new String[ops.length];
        for (int i = 0; i < ops.length; i++) labels[i] = I18n.format(ops[i].label);
        comboIdx.set(state.booleanOp.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.boolean_op") + "##boolop", comboIdx, labels)) {
            state.booleanOp = ops[comboIdx.get()];
        }
    }
}
