/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.panel.sections;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.panel.ToolSection;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.RockToolState;
import imgui.ImGui;

@SideOnly(Side.CLIENT)
public class RockSection implements ToolSection {

    private final RockToolState state;
    private final BrushSection brushSection;
    private final float[] noiseRadius = new float[1];
    private final float[] noisiness = new float[1];
    private final float[] smoothingStdDev = new float[1];
    private final float[] meldStrength = new float[1];

    public RockSection(RockToolState state, BrushState bs) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.rock"));

        noiseRadius[0] = state.noiseRadius;
        if (ImGui.sliderFloat(I18n.format("dimensium.ui.rock.noise_radius") + "##rock_nr", noiseRadius, 0.5f, 20f)) {
            state.noiseRadius = noiseRadius[0];
        }

        noisiness[0] = state.noisiness;
        if (ImGui.sliderFloat(I18n.format("dimensium.ui.rock.noisiness") + "##rock_noise", noisiness, 0.0f, 1.0f)) {
            state.noisiness = noisiness[0];
        }

        smoothingStdDev[0] = state.smoothingStdDev;
        if (ImGui
            .sliderFloat(I18n.format("dimensium.ui.rock.smoothing") + "##rock_smooth", smoothingStdDev, 0.0f, 10f)) {
            state.smoothingStdDev = smoothingStdDev[0];
        }

        meldStrength[0] = state.meldStrength;
        if (ImGui.sliderFloat(I18n.format("dimensium.ui.rock.meld") + "##rock_meld", meldStrength, 0.0f, 5f)) {
            state.meldStrength = meldStrength[0];
        }

        if (ImGui.button(I18n.format("dimensium.ui.rock.randomize_seed") + "##rock_seed")) {
            state.noiseSeed = ThreadLocalRandom.current()
                .nextLong();
        }
    }
}
