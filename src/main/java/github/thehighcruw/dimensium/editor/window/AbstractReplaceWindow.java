/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import com.github.bsideup.jabel.Desugar;
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
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
abstract class AbstractReplaceWindow extends ToggleableWindow {

    @Desugar
    record BlockMapping(ItemStack source, ItemStack target) {}

    protected final List<BlockMapping> mappings = new ArrayList<>();
    protected boolean flag;

    protected abstract boolean flagDefault();

    protected abstract String windowId();

    protected abstract String titleKey();

    /** Short prefix for all ImGui widget IDs in this window, e.g. "rep" or "trepl". */
    protected abstract String prefix();

    protected abstract String block1HeaderKey();

    protected abstract String block2HeaderKey();

    protected abstract String checkboxKey();

    /** I18n key for the undo/history action name. */
    protected abstract String applyActionKey();

    /**
     * Called per selected block per mapping. Return an {@code {x, y, z, blockId, meta}} op to
     * apply, or {@code null} to skip.
     */
    protected abstract int[] buildBlockOp(int x, int y, int z, Block worldBlock, int worldMeta, BlockMapping mapping);

    protected final void applyOp() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection()) return;
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;

        List<BlockMapping> completeMappings = mappings.stream()
                .filter(m -> m.source() != null && m.target() != null)
                .filter(m -> {
                    Block src = Block.getBlockFromItem(m.source().getItem());
                    return src != null && src != Blocks.air;
                })
                .collect(Collectors.toList());
        if (completeMappings.isEmpty()) return;

        List<int[]> ops = new ArrayList<>();
        outer:
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt cv = SelectionState.unpack(key);
            int x = cv.x(), y = cv.y(), z = cv.z();
            Block worldBlock = world.getBlock(x, y, z);
            int worldMeta = world.getBlockMetadata(x, y, z);
            for (BlockMapping mapping : completeMappings) {
                int[] op = buildBlockOp(x, y, z, worldBlock, worldMeta, mapping);
                if (op != null) {
                    ops.add(op);
                    continue outer;
                }
            }
        }
        BlockSender.sendChunked(ops, I18n.format(applyActionKey()));
    }

    public void open() {
        mappings.clear();
        mappings.add(new BlockMapping(null, null));
        flag = flagDefault();
        open = true;
    }

    public void close() {
        open = false;
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        float w = 400f * scale;
        float vpW = ImGui.getIO().getDisplaySizeX(), vpH = ImGui.getIO().getDisplaySizeY();
        ImGui.setNextWindowPos((vpW - w) * 0.5f, vpH * 0.3f, ImGuiCond.Appearing);
        ImGui.setNextWindowSize(w, 340f * scale, ImGuiCond.Appearing);

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui.begin(I18n.format(titleKey()) + windowId(), pOpen);
        captureBounds();

        if (visible) {
            boolean hasSel = SelectionState.INSTANCE.hasSelection();
            boolean hasCompleteMapping = mappings.stream().anyMatch(m -> m.source() != null && m.target() != null);
            boolean canApply = hasSel && hasCompleteMapping;

            float windowW = ImGui.getWindowWidth();
            float btnW = 70f * scale;
            float childH = Math.max(0f, ImGui.getContentRegionAvailY() - footerButtonHeight());
            ImGui.beginChild("##" + prefix() + "_body", 0f, childH);

            if (!hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.separator();
            }

            float cellSize = 20f * scale;
            float itemBtnH = cellSize
                    + DeferredItemRender.ITEM_PAD * 2f
                    + ImGui.getStyle().getFramePaddingY() * 2f;
            float removeW = ImGui.calcTextSize("X").x + ImGui.getStyle().getFramePaddingX() * 2f;

            ImGui.text(I18n.format(block1HeaderKey()));
            ImGui.sameLine(windowW * 0.5f - ImGui.getStyle().getWindowPaddingX());
            ImGui.text(I18n.format(block2HeaderKey()));

            int removeIndex = -1;
            for (int i = 0; i < mappings.size(); i++) {
                int removed = renderMappingRow(i, mappings.get(i), cellSize, itemBtnH, removeW);
                if (removed >= 0) {
                    removeIndex = removed;
                    break;
                }
            }
            if (removeIndex >= 0 && mappings.size() > 1) {
                mappings.remove(removeIndex);
            }

            ImGui.newLine();
            if (ImGui.button(I18n.format("dimensium.op.replace.add_mapping") + "##" + prefix() + "_add")) {
                mappings.add(new BlockMapping(null, null));
            }

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

    /** Returns the index to remove, or -1. */
    private int renderMappingRow(int index, BlockMapping mapping, float cellSize, float itemBtnH, float removeW) {
        String pfx = prefix() + "_m" + index;

        if (mapping.source() != null) {
            if (DeferredItemRender.placeButton("##" + pfx + "_src", mapping.source(), cellSize * 0.5f, false)) {
                OverlayRenderer.picker.open(stack -> mappings.set(
                        index, new BlockMapping(stack, mappings.get(index).target())));
            }
        } else {
            if (ImGui.button(I18n.format("dimensium.op.replace.no_block") + "##" + pfx + "_srcpick", 0, itemBtnH)) {
                OverlayRenderer.picker.open(stack -> mappings.set(
                        index, new BlockMapping(stack, mappings.get(index).target())));
            }
        }

        ImGui.sameLine();
        ImGui.text("->");
        ImGui.sameLine();

        if (mapping.target() != null) {
            if (DeferredItemRender.placeButton("##" + pfx + "_dst", mapping.target(), cellSize * 0.5f, false)) {
                OverlayRenderer.picker.open(stack ->
                        mappings.set(index, new BlockMapping(mappings.get(index).source(), stack)));
            }
        } else {
            if (ImGui.button(I18n.format("dimensium.op.replace.no_block") + "##" + pfx + "_dstpick", 0, itemBtnH)) {
                OverlayRenderer.picker.open(stack ->
                        mappings.set(index, new BlockMapping(mappings.get(index).source(), stack)));
            }
        }

        ImGui.sameLine();
        if (mappings.size() > 1) {
            if (ImGui.button("X##" + pfx + "_rem", removeW, itemBtnH)) {
                return index;
            }
        }
        return -1;
    }
}
