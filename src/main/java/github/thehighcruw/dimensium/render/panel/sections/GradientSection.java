package github.thehighcruw.dimensium.render.panel.sections;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.panel.ToolSection;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.GradientToolState;
import github.thehighcruw.dimensium.tool.state.GradientToolState.GradientInterp;
import github.thehighcruw.dimensium.tool.state.GradientToolState.GradientShape;
import github.thehighcruw.dimensium.tool.state.PaletteState;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class GradientSection implements ToolSection {

    private final GradientToolState state;
    private final MultiPaletteSection paletteSection;
    private final BrushSection brushSection;
    private final ImInt shapeIdx = new ImInt();
    private final ImInt interpIdx = new ImInt();
    private final ImBoolean maskSurface = new ImBoolean();
    private final ImBoolean clampToEdge = new ImBoolean();

    public GradientSection(GradientToolState state, BrushState bs, PaletteState ps) {
        this.state = state;
        this.paletteSection = new MultiPaletteSection(ps);
        this.brushSection = new BrushSection(bs);
    }

    @Override
    public void render() {
        paletteSection.render();
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.gradient"));

        GradientShape[] shapes = GradientShape.values();
        String[] shapeLabels = new String[shapes.length];
        for (int i = 0; i < shapes.length; i++) shapeLabels[i] = I18n.format(shapes[i].label);
        shapeIdx.set(state.gradientShape.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.gradient.shape") + "##grad_shape", shapeIdx, shapeLabels)) {
            state.gradientShape = shapes[shapeIdx.get()];
        }

        GradientInterp[] interps = GradientInterp.values();
        String[] interpLabels = new String[interps.length];
        for (int i = 0; i < interps.length; i++) interpLabels[i] = I18n.format(interps[i].label);
        interpIdx.set(state.gradientInterp.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.gradient.interp") + "##grad_interp", interpIdx, interpLabels)) {
            state.gradientInterp = interps[interpIdx.get()];
        }

        maskSurface.set(state.gradientMaskSurface);
        if (ImGui.checkbox(I18n.format("dimensium.ui.gradient.mask_surface") + "##grad_mask", maskSurface)) {
            state.gradientMaskSurface = maskSurface.get();
        }

        clampToEdge.set(state.gradientClampToEdge);
        if (ImGui.checkbox(I18n.format("dimensium.ui.gradient.clamp_edge") + "##grad_clamp", clampToEdge)) {
            state.gradientClampToEdge = clampToEdge.get();
        }

        if (ImGui.button(I18n.format("dimensium.ui.gradient.randomize_seed") + "##grad_seed")) {
            state.gradientSeed = ThreadLocalRandom.current()
                .nextLong();
        }
    }
}
