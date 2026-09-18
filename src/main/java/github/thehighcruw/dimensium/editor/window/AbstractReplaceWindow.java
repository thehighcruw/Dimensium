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
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
abstract class AbstractReplaceWindow extends ToggleableWindow {

    protected ItemStack block1;
    protected ItemStack block2;
    protected boolean flag;

    protected abstract boolean flagDefault();

    protected abstract String windowId();

    protected abstract String titleKey();

    /** Short prefix for all ImGui widget IDs in this window, e.g. "rep" or "trepl". */
    protected abstract String prefix();

    protected abstract String block1LabelKey();

    protected abstract String block2LabelKey();

    protected abstract String checkboxKey();

    /** I18n key for the undo/history action name, e.g. "dimensium.action.op.replace". */
    protected abstract String applyActionKey();

    /**
     * Called per selected block. Return an {@code {x, y, z, blockId, meta}} op to apply, or
     * {@code null} to skip this block.
     */
    protected abstract int[] buildBlockOp(
            int x, int y, int z, Block worldBlock, int worldMeta, Block findBlock, Block replaceBlock);

    protected final void applyOp() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection() || block1 == null || block2 == null) return;
        Block findBlock = Block.getBlockFromItem(block1.getItem());
        Block replaceBlock = Block.getBlockFromItem(block2.getItem());
        if (findBlock == null || findBlock == Blocks.air || replaceBlock == null) return;
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;
        List<int[]> ops = new ArrayList<>();
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt cv = SelectionState.unpack(key);
            int x = cv.x(), y = cv.y(), z = cv.z();
            int[] op = buildBlockOp(
                    x, y, z, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z), findBlock, replaceBlock);
            if (op != null) ops.add(op);
        }
        BlockSender.sendChunked(ops, I18n.format(applyActionKey()));
    }

    public void open() {
        block1 = null;
        block2 = null;
        flag = flagDefault();
        open = true;
    }

    public void close() {
        open = false;
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        float w = 340f * scale;
        float vpW = ImGui.getIO().getDisplaySizeX(), vpH = ImGui.getIO().getDisplaySizeY();
        ImGui.setNextWindowPos((vpW - w) * 0.5f, vpH * 0.3f, ImGuiCond.Appearing);
        ImGui.setNextWindowSize(w, 280f * scale, ImGuiCond.Appearing);

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui.begin(I18n.format(titleKey()) + windowId(), pOpen);
        captureBounds();

        if (visible) {
            boolean hasSel = SelectionState.INSTANCE.hasSelection();
            boolean canApply = block1 != null && block2 != null && hasSel;

            float windowW = ImGui.getWindowWidth();
            float btnW = 70f * scale;
            float childH = Math.max(0f, ImGui.getContentRegionAvailY() - footerButtonHeight());
            ImGui.beginChild("##" + prefix() + "_body", 0f, childH);

            if (!hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.separator();
            }

            float cellSize = 20f * scale;
            renderBlockPicker(block1LabelKey(), prefix() + "_b1", cellSize, b -> block1 = b, block1);
            renderBlockPicker(block2LabelKey(), prefix() + "_b2", cellSize, b -> block2 = b, block2);

            ImGui.spacing();
            ImBoolean cb = new ImBoolean(flag);
            if (ImGui.checkbox(I18n.format(checkboxKey()) + "##" + prefix() + "_flag", cb)) {
                flag = cb.get();
            }

            ImGui.endChild();

            ImGui.separator();
            ImGui.setCursorPosX(windowW - ImGui.getStyle().getWindowPaddingX() - btnW);
            if (!canApply) ImGui.beginDisabled();
            if (ImGui.button(I18n.format("dimensium.op.apply") + "##" + prefix() + "_apply", btnW, 0)) {
                applyOp();
                close();
            }
            if (!canApply) ImGui.endDisabled();
        }

        ImGui.end();
        if (!pOpen.get()) close();
    }

    private static void renderBlockPicker(
            String labelKey, String id, float cellSize, Consumer<ItemStack> setter, ItemStack current) {
        ImGui.text(I18n.format(labelKey) + ":");
        ImGui.sameLine();
        if (current != null) {
            if (DeferredItemRender.placeButton("##" + id, current, cellSize * 0.5f, false)) {
                OverlayRenderer.picker.open(setter);
            }
            ImGui.sameLine();
            ImGui.text(current.getDisplayName());
        } else {
            if (ImGui.button(
                    I18n.format("dimensium.op.replace.no_block") + "##" + id + "_pick", cellSize * 2f, cellSize)) {
                OverlayRenderer.picker.open(setter);
            }
        }
    }
}
