/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.manipulating.distort.DistortToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.SelectionTransforms;
import imgui.ImGui;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class DistortSelectionWindow extends AbstractSelectionOpWindow {

    public static final DistortSelectionWindow INSTANCE = new DistortSelectionWindow();

    private final float[] scale = {10f};
    private final float[] distX = {3f};
    private final float[] distY = {3f};
    private final float[] distZ = {3f};
    private long seed = 0L;

    private DistortSelectionWindow() {}

    public void open() {
        DistortToolState s = DistortToolState.INSTANCE;
        scale[0] = s.distortScale;
        distX[0] = s.distortDistanceX;
        distY[0] = s.distortDistanceY;
        distZ[0] = s.distortDistanceZ;
        seed = s.distortSeed;
        open = true;
    }

    @Override
    protected String titleKey() {
        return "dimensium.select.distort.title";
    }

    @Override
    protected String windowId() {
        return "###distort_selection_window";
    }

    @Override
    protected float windowHeight() {
        return 240f;
    }

    @Override
    protected String bodyId() {
        return "##distort_body";
    }

    @Override
    protected String applyId() {
        return "dsel";
    }

    @Override
    protected void renderBody(float sliderW) {
        ImGui.setNextItemWidth(sliderW);
        ImGui.sliderFloat(I18n.format("dimensium.ui.distort.scale") + "##dsel_scale", scale, 1f, 100f);
        ImGui.setNextItemWidth(sliderW);
        ImGui.sliderFloat(I18n.format("dimensium.ui.distort.distance_x") + "##dsel_distx", distX, 0f, 20f);
        ImGui.setNextItemWidth(sliderW);
        ImGui.sliderFloat(I18n.format("dimensium.ui.distort.distance_y") + "##dsel_disty", distY, 0f, 20f);
        ImGui.setNextItemWidth(sliderW);
        ImGui.sliderFloat(I18n.format("dimensium.ui.distort.distance_z") + "##dsel_distz", distZ, 0f, 20f);

        ImGui.spacing();
        if (ImGui.button(I18n.format("dimensium.ui.distort.randomize_seed") + "##dsel_rnd")) {
            seed = ThreadLocalRandom.current().nextLong();
        }
        ImGui.sameLine();
        ImGui.textDisabled(Long.toHexString(seed).toUpperCase());
    }

    @Override
    protected void applyOp(SelectionState sel) {
        sel.applyOp(
                SelectionTransforms.distort(sel.getSelectedBlocks(), scale[0], seed, distX[0], distY[0], distZ[0]),
                BooleanOp.REPLACE);
    }
}
