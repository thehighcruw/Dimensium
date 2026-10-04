/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.painting.painter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushSection;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.shared.util.BlockFamilyRegistry;
import imgui.ImGui;
import imgui.type.ImBoolean;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class PainterSection implements ToolSection {

    private final PainterToolState state;
    private final BrushSection brushSection;
    private final ImBoolean maskSurface = new ImBoolean();
    private final ImBoolean typeReplace = new ImBoolean();

    public PainterSection(PainterToolState state, BrushState bs) {
        this.state = state;
        this.brushSection = new BrushSection(bs);
    }

    @Override
    public void render() {
        brushSection.render(false);

        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.options"));

        maskSurface.set(state.painterMaskSurface);
        if (ImGui.checkbox(I18n.format("dimensium.ui.painter.mask_surface") + "##paint_mask", maskSurface)) {
            state.painterMaskSurface = maskSurface.get();
        }

        Block paintBlock = SelectedBlockState.INSTANCE.getPaintBlock();
        int paintMeta = SelectedBlockState.INSTANCE.getPaintMeta();
        boolean canTypeReplace = BlockFamilyRegistry.hasFamily(Block.getIdFromBlock(paintBlock), paintMeta);
        if (canTypeReplace) {
            typeReplace.set(state.painterTypeReplace);
            if (ImGui.checkbox(I18n.format("dimensium.ui.paint.type_replace") + "##paint_type_replace", typeReplace)) {
                state.painterTypeReplace = typeReplace.get();
            }
        } else {
            state.painterTypeReplace = false;
        }
    }
}
