/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.window.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.BlockUtils;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

@SideOnly(Side.CLIENT)
public class FillSelectionWindow extends ToggleableWindow {

    public static final FillSelectionWindow INSTANCE = new FillSelectionWindow();

    private static final String WINDOW_ID = "###fill_selection_window";

    private static final int MODE_FILL_ALL = 0;
    private static final int MODE_FILL_OUTLINE = 1;
    private static final int MODE_FILL_WALLS = 2;
    private static final int MODE_FILL_TOP = 3;
    private static final int MODE_FILL_BOTTOM = 4;

    private ItemStack selectedBlock = null;
    private final ImInt fillMode = new ImInt(MODE_FILL_ALL);

    private FillSelectionWindow() {}

    public void open() {
        selectedBlock = null;
        fillMode.set(MODE_FILL_ALL);
        open = true;
    }

    public void close() {
        open = false;
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        float vpW = ImGui.getIO().getDisplaySizeX(), vpH = ImGui.getIO().getDisplaySizeY();
        float w = 320f * scale;
        ImGui.setNextWindowPos((vpW - w) * 0.5f, vpH * 0.3f, ImGuiCond.Appearing);
        ImGui.setNextWindowSize(w, 240f * scale, ImGuiCond.Appearing);

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui.begin(I18n.format("dimensium.op.fill.title") + WINDOW_ID, pOpen);
        captureBounds();

        if (visible) {
            boolean hasSel = SelectionState.INSTANCE.hasSelection();
            boolean canApply = selectedBlock != null && hasSel;

            float windowW = ImGui.getWindowWidth();
            float btnW = 70f * scale;
            float childH = Math.max(0f, ImGui.getContentRegionAvailY() - footerButtonHeight());
            ImGui.beginChild("##fill_body", 0f, childH);

            if (!hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.separator();
            }

            float cellSize = 20f * scale;

            if (selectedBlock != null) {
                if (DeferredItemRender.placeButton("##fill_block", selectedBlock, cellSize * 0.5f, false)) {
                    openPicker();
                }
                ImGui.sameLine();
                ImGui.text(selectedBlock.getDisplayName());
            } else {
                if (ImGui.button(
                        I18n.format("dimensium.op.replace.no_block") + "##fill_pick",
                        0,
                        cellSize
                                + DeferredItemRender.ITEM_PAD * 2f
                                + ImGui.getStyle().getFramePaddingY() * 2f)) {
                    openPicker();
                }
            }

            ImGui.spacing();

            String[] modeLabels = {
                I18n.format("dimensium.op.fill.mode.fill"),
                I18n.format("dimensium.op.fill.mode.outline"),
                I18n.format("dimensium.op.fill.mode.walls"),
                I18n.format("dimensium.op.fill.mode.top"),
                I18n.format("dimensium.op.fill.mode.bottom")
            };
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX() * 0.7f);
            ImGui.combo("##fill_mode", fillMode, modeLabels, modeLabels.length);

            ImGui.endChild();

            ImGui.separator();
            ImGui.setCursorPosX(windowW - ImGui.getStyle().getWindowPaddingX() - btnW);
            if (!canApply) ImGui.beginDisabled();
            if (ImGui.button(I18n.format("dimensium.op.apply") + "##fill_apply", btnW, 0)) {
                applyFill();
                close();
            }
            if (!canApply) ImGui.endDisabled();
        }

        ImGui.end();
        if (!pOpen.get()) close();
    }

    private void openPicker() {
        OverlayRenderer.picker.open(stack -> selectedBlock = stack);
    }

    private void applyFill() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection() || selectedBlock == null) return;
        Block block = Block.getBlockFromItem(selectedBlock.getItem());
        if (block == null) return;
        int blockId = Block.getIdFromBlock(block);
        int blockMeta = selectedBlock.getItemDamage();
        Set<Long> selected = sel.getSelectedBlocks();
        List<int[]> ops = new ArrayList<>();

        for (long key : selected) {
            Vec3DInt cv = SelectionState.unpack(key);
            if (matchesFillMode(selected, cv, fillMode.get())) {
                ops.add(new int[] {cv.x(), cv.y(), cv.z(), blockId, blockMeta});
            }
        }

        String modeName = getModeKey(fillMode.get());
        BlockSender.sendChunked(ops, I18n.format("dimensium.action.op.fill", I18n.format(modeName)));
    }

    private boolean matchesFillMode(Set<Long> selected, Vec3DInt cv, int mode) {
        return switch (mode) {
            case MODE_FILL_OUTLINE -> {
                for (Vec3DInt d : BlockUtils.NEIGHBOUR_OFFSETS) {
                    if (!selected.contains(SelectionState.pack(cv.plus(d)))) yield true;
                }
                yield false;
            }
            case MODE_FILL_WALLS ->
                !selected.contains(SelectionState.pack(cv.plus(1, 0, 0)))
                        || !selected.contains(SelectionState.pack(cv.plus(-1, 0, 0)))
                        || !selected.contains(SelectionState.pack(cv.plus(0, 0, 1)))
                        || !selected.contains(SelectionState.pack(cv.plus(0, 0, -1)));
            case MODE_FILL_TOP -> !selected.contains(SelectionState.pack(cv.plus(0, 1, 0)));
            case MODE_FILL_BOTTOM -> !selected.contains(SelectionState.pack(cv.plus(0, -1, 0)));
            default -> true;
        };
    }

    private String getModeKey(int mode) {
        return switch (mode) {
            case MODE_FILL_OUTLINE -> "dimensium.op.fill.mode.outline";
            case MODE_FILL_WALLS -> "dimensium.op.fill.mode.walls";
            case MODE_FILL_TOP -> "dimensium.op.fill.mode.top";
            case MODE_FILL_BOTTOM -> "dimensium.op.fill.mode.bottom";
            default -> "dimensium.op.fill.mode.fill";
        };
    }
}
