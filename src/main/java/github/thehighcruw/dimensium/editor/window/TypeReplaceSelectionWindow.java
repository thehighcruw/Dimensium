/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.window.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ToggleableWindow;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.Vec3DInt;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class TypeReplaceSelectionWindow extends ToggleableWindow {

    public static final TypeReplaceSelectionWindow INSTANCE = new TypeReplaceSelectionWindow();

    private static final String WINDOW_ID = "###type_replace_selection_window";

    private ItemStack sourceType = null;
    private ItemStack targetBlock = null;
    private boolean preserveProperties = true;

    private TypeReplaceSelectionWindow() {}

    public void open() {
        sourceType = null;
        targetBlock = null;
        preserveProperties = true;
        open = true;
    }

    public void close() {
        open = false;
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        float w = 340f * scale;
        float vpW = ImGui.getIO()
            .getDisplaySizeX(),
            vpH = ImGui.getIO()
                .getDisplaySizeY();
        ImGui.setNextWindowPos((vpW - w) * 0.5f, vpH * 0.3f, ImGuiCond.Appearing);
        ImGui.setNextWindowSize(w, 280f * scale, ImGuiCond.Appearing);

        ImBoolean pOpen = new ImBoolean(true);
        boolean visible = ImGui.begin(I18n.format("dimensium.op.type_replace.title") + WINDOW_ID, pOpen);
        captureBounds();

        if (visible) {
            boolean hasSel = SelectionState.INSTANCE.hasSelection();
            boolean canApply = sourceType != null && targetBlock != null && hasSel;

            float windowW = ImGui.getWindowWidth();
            float btnW = 70f * scale;
            float footerH = ImGui.getStyle()
                .getItemSpacingY() + 1f
                + ImGui.getStyle()
                    .getItemSpacingY()
                + ImGui.getFrameHeight()
                + ImGui.getStyle()
                    .getWindowPaddingY();
            float childH = Math.max(0f, ImGui.getContentRegionAvailY() - footerH);
            ImGui.beginChild("##trepl_body", 0f, childH);

            if (!hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.separator();
            }

            float cellSize = 20f * scale;

            ImGui.text(I18n.format("dimensium.op.type_replace.source") + ":");
            ImGui.sameLine();
            if (sourceType != null) {
                if (DeferredItemRender.placeButton("##trepl_src", sourceType, cellSize * 0.5f, false)) {
                    OverlayRenderer.picker.open(stack -> sourceType = stack);
                }
                ImGui.sameLine();
                ImGui.text(sourceType.getDisplayName());
            } else {
                if (ImGui.button(
                    I18n.format("dimensium.op.replace.no_block") + "##trepl_src_pick",
                    cellSize * 2f,
                    cellSize)) {
                    OverlayRenderer.picker.open(stack -> sourceType = stack);
                }
            }

            ImGui.text(I18n.format("dimensium.op.type_replace.target") + ":");
            ImGui.sameLine();
            if (targetBlock != null) {
                if (DeferredItemRender.placeButton("##trepl_tgt", targetBlock, cellSize * 0.5f, false)) {
                    OverlayRenderer.picker.open(stack -> targetBlock = stack);
                }
                ImGui.sameLine();
                ImGui.text(targetBlock.getDisplayName());
            } else {
                if (ImGui.button(
                    I18n.format("dimensium.op.replace.no_block") + "##trepl_tgt_pick",
                    cellSize * 2f,
                    cellSize)) {
                    OverlayRenderer.picker.open(stack -> targetBlock = stack);
                }
            }

            ImGui.spacing();
            ImBoolean cbPreserve = new ImBoolean(preserveProperties);
            if (ImGui.checkbox(I18n.format("dimensium.op.type_replace.preserve") + "##trepl_pres", cbPreserve)) {
                preserveProperties = cbPreserve.get();
            }

            ImGui.endChild();

            ImGui.separator();
            ImGui.setCursorPosX(
                windowW - ImGui.getStyle()
                    .getWindowPaddingX() - btnW);
            if (!canApply) ImGui.beginDisabled();
            if (ImGui.button(I18n.format("dimensium.op.apply") + "##trepl_apply", btnW, 0)) {
                applyTypeReplace();
                close();
            }
            if (!canApply) ImGui.endDisabled();
        }

        ImGui.end();
        if (!pOpen.get()) close();
    }

    private void applyTypeReplace() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection() || sourceType == null || targetBlock == null) return;
        Block srcBlock = Block.getBlockFromItem(sourceType.getItem());
        Block tgtBlock = Block.getBlockFromItem(targetBlock.getItem());
        if (srcBlock == null || srcBlock == Blocks.air || tgtBlock == null) return;

        int tgtId = Block.getIdFromBlock(tgtBlock);
        int tgtMeta = targetBlock.getItemDamage();

        net.minecraft.world.World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;

        List<int[]> ops = new ArrayList<>();
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt cv = SelectionState.unpack(key);
            int x = cv.x(), y = cv.y(), z = cv.z();
            Block worldBlock = world.getBlock(x, y, z);
            if (worldBlock != srcBlock) continue;
            int outMeta = preserveProperties ? world.getBlockMetadata(x, y, z) : tgtMeta;
            ops.add(new int[] { x, y, z, tgtId, outMeta });
        }
        BlockSender.sendChunked(ops, I18n.format("dimensium.action.op.type_replace"));
    }
}
