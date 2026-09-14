/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.lasso;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import imgui.ImGui;
import imgui.type.ImBoolean;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class LassoSelectSection implements ToolSection {

    private final LassoSelectToolState state;
    private final int[] depth = new int[1];
    private final ImBoolean nonSolid = new ImBoolean();

    public LassoSelectSection(LassoSelectToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.lasso"));

        depth[0] = state.lassoDepth;
        if (ImGui.sliderInt(I18n.format("dimensium.ui.lasso.depth") + "##lasso_depth", depth, 1, 64)) {
            state.lassoDepth = depth[0];
        }

        nonSolid.set(state.lassoIncludeNonSolid);
        if (ImGui.checkbox(I18n.format("dimensium.ui.lasso.non_solid") + "##lasso_nonsolid", nonSolid)) {
            state.lassoIncludeNonSolid = nonSolid.get();
        }
    }
}
