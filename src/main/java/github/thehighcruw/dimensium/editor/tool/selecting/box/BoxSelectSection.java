/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.box;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOpSection;
import github.thehighcruw.dimensium.editor.window.panel.ToolSection;

@SideOnly(Side.CLIENT)
public class BoxSelectSection implements ToolSection {

    private final BoxSelectToolState state;
    private final BooleanOpSection booleanOpSection = new BooleanOpSection();

    public BoxSelectSection(BoxSelectToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        booleanOpSection.render(state);
    }
}
