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
public class ReplaceSelectionWindow extends ToggleableWindow {

    public static final ReplaceSelectionWindow INSTANCE = new ReplaceSelectionWindow();

    private static final String WINDOW_ID = "###replace_selection_window";

    private ItemStack findBlock = null;
    private ItemStack replaceBlock = null;
    private boolean exactMeta = false;

    private ReplaceSelectionWindow() {}

    public void open() {
        findBlock = null;
        replaceBlock = null;
        exactMeta = false;
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
        boolean visible = ImGui.begin(I18n.format("dimensium.op.replace.title") + WINDOW_ID, pOpen);
        captureBounds();

        if (visible) {
            boolean hasSel = SelectionState.INSTANCE.hasSelection();
            boolean canApply = findBlock != null && replaceBlock != null && hasSel;

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
            ImGui.beginChild("##replace_body", 0f, childH);

            if (!hasSel) {
                ImGui.textDisabled(I18n.format("dimensium.ui.hint.no_selection"));
                ImGui.separator();
            }

            float cellSize = 20f * scale;

            ImGui.text(I18n.format("dimensium.op.replace.find") + ":");
            ImGui.sameLine();
            if (findBlock != null) {
                if (DeferredItemRender.placeButton("##rep_find", findBlock, cellSize * 0.5f, false)) {
                    OverlayRenderer.picker.open(stack -> findBlock = stack);
                }
                ImGui.sameLine();
                ImGui.text(findBlock.getDisplayName());
            } else {
                if (ImGui.button(
                    I18n.format("dimensium.op.replace.no_block") + "##rep_find_pick",
                    cellSize * 2f,
                    cellSize)) {
                    OverlayRenderer.picker.open(stack -> findBlock = stack);
                }
            }

            ImGui.text(I18n.format("dimensium.op.replace.replace_with") + ":");
            ImGui.sameLine();
            if (replaceBlock != null) {
                if (DeferredItemRender.placeButton("##rep_repl", replaceBlock, cellSize * 0.5f, false)) {
                    OverlayRenderer.picker.open(stack -> replaceBlock = stack);
                }
                ImGui.sameLine();
                ImGui.text(replaceBlock.getDisplayName());
            } else {
                if (ImGui.button(
                    I18n.format("dimensium.op.replace.no_block") + "##rep_repl_pick",
                    cellSize * 2f,
                    cellSize)) {
                    OverlayRenderer.picker.open(stack -> replaceBlock = stack);
                }
            }

            ImGui.spacing();
            ImBoolean cbExact = new ImBoolean(exactMeta);
            if (ImGui.checkbox(I18n.format("dimensium.op.replace.exact_meta") + "##rep_exact", cbExact)) {
                exactMeta = cbExact.get();
            }

            ImGui.endChild();

            ImGui.separator();
            ImGui.setCursorPosX(
                windowW - ImGui.getStyle()
                    .getWindowPaddingX() - btnW);
            if (!canApply) ImGui.beginDisabled();
            if (ImGui.button(I18n.format("dimensium.op.apply") + "##rep_apply", btnW, 0)) {
                applyReplace();
                close();
            }
            if (!canApply) ImGui.endDisabled();
        }

        ImGui.end();
        if (!pOpen.get()) close();
    }

    private void applyReplace() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection() || findBlock == null || replaceBlock == null) return;
        Block findB = Block.getBlockFromItem(findBlock.getItem());
        Block replaceB = Block.getBlockFromItem(replaceBlock.getItem());
        if (findB == null || findB == Blocks.air || replaceB == null) return;

        int findMeta = findBlock.getItemDamage();
        int replaceId = Block.getIdFromBlock(replaceB);
        int replaceMeta = replaceBlock.getItemDamage();

        net.minecraft.world.World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;

        List<int[]> ops = new ArrayList<>();
        for (long key : sel.getSelectedBlocks()) {
            Vec3DInt cv = SelectionState.unpack(key);
            int x = cv.x(), y = cv.y(), z = cv.z();
            Block worldBlock = world.getBlock(x, y, z);
            if (worldBlock != findB) continue;
            if (exactMeta && world.getBlockMetadata(x, y, z) != findMeta) continue;
            ops.add(new int[] { x, y, z, replaceId, replaceMeta });
        }
        BlockSender.sendChunked(ops, I18n.format("dimensium.action.op.replace"));
    }
}
