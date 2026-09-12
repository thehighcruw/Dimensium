/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.popup;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.render.imgui.ImGuiManager;
import github.thehighcruw.dimensium.render.imgui.ImGuiWindow;
import github.thehighcruw.dimensium.util.RenderUtils;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

@SideOnly(Side.CLIENT)
public class BlockInfoWindow extends ImGuiWindow {

    public static final BlockInfoWindow INSTANCE = new BlockInfoWindow();

    private static final String WINDOW_ID = "###block_info_window";
    private static final int FLAGS = ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar
        | ImGuiWindowFlags.NoScrollWithMouse;

    private boolean open = false;

    private BlockInfoWindow() {}

    @Override
    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowBlockInfoOpen(value);
    }

    public void renderImGui() {
        if (!open) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        MovingObjectPosition mop = RenderUtils.raycastAtCursor();

        float scale = ImGuiManager.INSTANCE.getUIScale();
        ImGui.setNextWindowSizeConstraints(180 * scale, 0, 400 * scale, Float.MAX_VALUE);
        ImGui.setNextWindowPos(10 * scale, 30 * scale, ImGuiCond.FirstUseEver);

        ImBoolean openRef = new ImBoolean(true);
        ImGui.begin(I18n.format("dimensium.block_info.title") + WINDOW_ID, openRef, FLAGS);
        captureBounds();

        if (!openRef.get()) {
            ImGui.end();
            setOpen(false);
            return;
        }

        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            Block block = mc.theWorld.getBlock(mop.blockX, mop.blockY, mop.blockZ);
            int meta = mc.theWorld.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);

            if (block != null && block != Blocks.air) {
                String displayName;
                try {
                    displayName = new ItemStack(block, 1, meta).getDisplayName();
                } catch (Exception e) {
                    displayName = block.getLocalizedName();
                }
                ImGui.text(displayName);

                String unloc = block.getUnlocalizedName();
                if (unloc != null) {
                    float dim = 0.55f;
                    ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, dim, dim, dim, 1f);
                    ImGui.text(unloc);
                    ImGui.popStyleColor();
                }

                ImGui.separator();

                if (meta != 0) {
                    ImGui.text(I18n.format("dimensium.block_info.meta") + " " + meta);
                }

                ImGui.text(
                    I18n.format("dimensium.block_info.pos") + " " + mop.blockX + ", " + mop.blockY + ", " + mop.blockZ);

                EntityLivingBase eye = mc.renderViewEntity;
                if (eye != null) {
                    double dx = mop.blockX + 0.5 - eye.posX;
                    double dy = mop.blockY + 0.5 - (eye.posY + eye.getEyeHeight());
                    double dz = mop.blockZ + 0.5 - eye.posZ;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    ImGui.text(I18n.format("dimensium.block_info.distance") + " " + String.format("%.1f", dist) + " m");
                }
            } else {
                float dim = 0.55f;
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, dim, dim, dim, 1f);
                ImGui.text(I18n.format("dimensium.block_info.air"));
                ImGui.popStyleColor();
            }
        } else {
            float dim = 0.55f;
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, dim, dim, dim, 1f);
            ImGui.text(I18n.format("dimensium.block_info.no_target"));
            ImGui.popStyleColor();
        }

        ImGui.end();
    }
}
