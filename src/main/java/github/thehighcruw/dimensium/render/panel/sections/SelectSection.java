/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.panel.sections;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.panel.ToolSection;
import github.thehighcruw.dimensium.tool.state.SelectToolState;

@SideOnly(Side.CLIENT)
public class SelectSection implements ToolSection {

    private final SelectToolState state;
    private final BooleanOpSection booleanOpSection = new BooleanOpSection();

    public SelectSection(SelectToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        booleanOpSection.render(state);
    }
}
