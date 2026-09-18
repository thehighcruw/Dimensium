/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.manipulating.smooth.SmoothToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.SelectionTransforms;
import imgui.ImGui;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class SmoothSelectionWindow extends AbstractSelectionOpWindow {

    public static final SmoothSelectionWindow INSTANCE = new SmoothSelectionWindow();

    private final int[] strength = {2};
    private final float[] threshold = {0.5f};

    private SmoothSelectionWindow() {}

    public void open() {
        strength[0] = SmoothToolState.INSTANCE.smoothStrength;
        threshold[0] = 0.5f;
        open = true;
    }

    @Override
    protected String titleKey() {
        return "dimensium.select.smooth.title";
    }

    @Override
    protected String windowId() {
        return "###smooth_selection_window";
    }

    @Override
    protected float windowHeight() {
        return 200f;
    }

    @Override
    protected String bodyId() {
        return "##smooth_body";
    }

    @Override
    protected String applyId() {
        return "ssel";
    }

    @Override
    protected void renderBody(float sliderW) {
        ImGui.setNextItemWidth(sliderW);
        ImGui.sliderInt(I18n.format("dimensium.ui.smooth.strength") + "##ssel_strength", strength, 1, 8);
        ImGui.setNextItemWidth(sliderW);
        ImGui.sliderFloat(I18n.format("dimensium.select.smooth.threshold") + "##ssel_threshold", threshold, 0.01f, 1f);
    }

    @Override
    protected void applyOp(SelectionState sel) {
        sel.applyOp(SelectionTransforms.smooth(sel.getSelectedBlocks(), strength[0], threshold[0]), BooleanOp.REPLACE);
    }
}
