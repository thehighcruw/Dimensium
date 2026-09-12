/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.panel.sections;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.OverlayRenderer;
import github.thehighcruw.dimensium.render.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.render.imgui.ImGuiManager;
import github.thehighcruw.dimensium.tool.state.PaletteState;
import imgui.ImGui;

@SideOnly(Side.CLIENT)
public class MultiPaletteSection {

    private final PaletteState ps;
    private final int[] weightBuf = new int[1];

    public MultiPaletteSection(PaletteState ps) {
        this.ps = ps;
    }

    public void render() {
        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.palette"));

        int total = ps.totalPaletteWeight();

        for (int i = 0; i < ps.palette.size(); i++) {
            ItemStack stack = ps.palette.get(i);

            if (DeferredItemRender.placeButton("##pal_item_" + i, stack, 16f * ImGuiManager.INSTANCE.getUIScale())) {
                final int idx = i;
                OverlayRenderer.picker.open(picked -> ps.palette.set(idx, picked));
            }
            ImGui.sameLine();

            weightBuf[0] = ps.getWeight(i);
            ImGui.setNextItemWidth(80f);
            if (ImGui.sliderInt("##pal_w_" + i, weightBuf, 1, 100)) {
                ps.setWeight(i, weightBuf[0]);
            }
            ImGui.sameLine();

            int pct = total > 0 ? (int) Math.round(ps.getWeight(i) * 100.0 / total) : 0;
            ImGui.textDisabled(pct + "%");
            ImGui.sameLine();

            if (ImGui.button("^##pal_up_" + i) && i > 0) {
                ItemStack tmpS = ps.palette.remove(i);
                ps.palette.add(i - 1, tmpS);
                int tmpW = ps.paletteWeights.remove(i);
                ps.paletteWeights.add(i - 1, tmpW);
            }
            ImGui.sameLine();

            if (ImGui.button("v##pal_dn_" + i) && i < ps.palette.size() - 1) {
                ItemStack tmpS = ps.palette.remove(i);
                ps.palette.add(i + 1, tmpS);
                int tmpW = ps.paletteWeights.remove(i);
                ps.paletteWeights.add(i + 1, tmpW);
            }
            ImGui.sameLine();

            if (ImGui.button("x##pal_rm_" + i)) {
                ps.palette.remove(i);
                if (i < ps.paletteWeights.size()) ps.paletteWeights.remove(i);
                i--;
            }
        }

        if (ImGui.button(I18n.format("dimensium.ui.palette.add") + "##pal_add")) {
            OverlayRenderer.picker.open(picked -> {
                ps.palette.add(picked);
                ps.setWeight(ps.palette.size() - 1, 50);
            });
        }
    }
}
