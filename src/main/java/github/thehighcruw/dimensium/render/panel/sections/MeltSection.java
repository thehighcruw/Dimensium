package github.thehighcruw.dimensium.render.panel.sections;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.panel.ToolSection;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.MeltToolState;
import imgui.ImGui;

@SideOnly(Side.CLIENT)
public class MeltSection implements ToolSection {

    private final MeltToolState state;
    private final BrushSection brushSection;
    private final int[] smoothStrength = new int[1];
    private final float[] threshold = new float[1];

    public MeltSection(MeltToolState state, BrushState bs) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.melt"));

        smoothStrength[0] = state.meltSmoothStrength;
        if (ImGui.sliderInt(I18n.format("dimensium.ui.melt.smooth_strength") + "##melt_str", smoothStrength, 1, 10)) {
            state.meltSmoothStrength = smoothStrength[0];
        }

        threshold[0] = state.meltThreshold;
        if (ImGui.sliderFloat(I18n.format("dimensium.ui.melt.threshold") + "##melt_thresh", threshold, 0.0f, 1.0f)) {
            state.meltThreshold = threshold[0];
        }
    }
}
