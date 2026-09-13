/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.brushes;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class BrushSection {

    private final BrushState bs;
    private final ImInt shapeIdx = new ImInt();
    private final int[] radius = new int[1];
    private final int[] height = new int[1];
    private final ImBoolean hollow = new ImBoolean();

    public BrushSection(BrushState bs) {
        this.bs = bs;
    }

    public void render(boolean showHollow) {
        ImGui.separator();
        ImGui.text(I18n.format("dimensium.ui.section.brush"));

        BrushShape[] shapes = BrushShape.values();
        String[] shapeLabels = new String[shapes.length];
        for (int i = 0; i < shapes.length; i++) shapeLabels[i] = I18n.format(shapes[i].label);
        shapeIdx.set(bs.brushShape.ordinal());
        if (ImGui.combo(I18n.format("dimensium.ui.brush.shape") + "##brush_shape", shapeIdx, shapeLabels)) {
            bs.brushShape = shapes[shapeIdx.get()];
        }

        radius[0] = bs.brushRadius;
        if (ImGui.sliderInt(I18n.format("dimensium.ui.brush.radius") + "##brush_radius", radius, 0, 32)) {
            bs.brushRadius = radius[0];
        }

        if (bs.brushShape.hasHeight) {
            height[0] = bs.brushHeight;
            if (ImGui.sliderInt(I18n.format("dimensium.ui.brush.height") + "##brush_height", height, 0, 32)) {
                bs.brushHeight = height[0];
            }
        }

        if (showHollow) {
            hollow.set(bs.hollow);
            if (ImGui.checkbox(I18n.format("dimensium.ui.brush.hollow") + "##brush_hollow", hollow)) {
                bs.hollow = hollow.get();
            }
        }
    }
}
