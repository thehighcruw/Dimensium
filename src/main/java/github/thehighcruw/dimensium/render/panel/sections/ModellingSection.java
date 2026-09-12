package github.thehighcruw.dimensium.render.panel.sections;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.render.panel.ToolSection;
import github.thehighcruw.dimensium.tool.state.ModellingToolState;
import github.thehighcruw.dimensium.tool.state.ModellingToolState.Mode;
import github.thehighcruw.dimensium.tool.state.ModellingToolState.PasteMode;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class ModellingSection implements ToolSection {

    private final ModellingToolState state;
    private final ImInt modeIdx = new ImInt();
    private final ImInt pasteModeIdx = new ImInt();
    private final ImBoolean offsetTarget = new ImBoolean();

    public ModellingSection(ModellingToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        ImGui.dummy(0f, 3f);
        ImGui.separator();
        ImGui.dummy(0f, 2f);
        ImGui.text(I18n.format("dimensium.ui.section.modelling"));

        Mode[] modes = Mode.values();
        String[] modeLabels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) modeLabels[i] = I18n.format(modes[i].label);
        modeIdx.set(state.mode.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.modelling.mode") + "##mod_mode", modeIdx, modeLabels)) {
            state.mode = modes[modeIdx.get()];
        }

        PasteMode[] pasteModes = PasteMode.values();
        String[] pasteModeLabels = new String[pasteModes.length];
        for (int i = 0; i < pasteModes.length; i++) pasteModeLabels[i] = I18n.format(pasteModes[i].label);
        pasteModeIdx.set(state.pasteMode.ordinal());
        if (ImGui
            .combo(I18n.format("dimensium.ui.modelling.paste_mode") + "##mod_paste", pasteModeIdx, pasteModeLabels)) {
            state.pasteMode = pasteModes[pasteModeIdx.get()];
        }

        offsetTarget.set(state.offsetTargetPoint);
        if (ImGui.checkbox(I18n.format("dimensium.ui.modelling.offset_target") + "##mod_offset", offsetTarget)) {
            state.offsetTargetPoint = offsetTarget.get();
        }

        ModellingToolState.ModelPoint selPt = state.selectedPointObj();
        if (selPt != null) {
            ImGui.dummy(0f, 3f);
            ImGui.separator();
            ImGui.dummy(0f, 2f);
            ImGui.text(I18n.format("dimensium.ui.modelling.selected_point"));
            ImGui.textDisabled(selPt.x + ", " + selPt.y + ", " + selPt.z);
            ImGui.pushStyleColor(ImGuiCol.Button, 0.65f, 0.10f, 0.10f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.80f, 0.20f, 0.20f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.50f, 0.05f, 0.05f, 1.0f);
            if (ImGui.button(I18n.format("dimensium.ui.modelling.remove_selected") + "##mod_rm_sel")) {
                state.removeSelectedPoint();
            }
            ImGui.popStyleColor(3);
        }
    }
}
