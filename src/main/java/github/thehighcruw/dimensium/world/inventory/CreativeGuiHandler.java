/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.inventory;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.network.PacketHandler;
import github.thehighcruw.dimensium.network.PacketOpenGradientGui;
import github.thehighcruw.dimensium.world.inventory.colorPicker.GuiColourPicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

@SideOnly(Side.CLIENT)
public class CreativeGuiHandler {

    private static final int BUTTON_ID = 3712;
    private static final int BUTTON_ID_GRADIENT = 3713;
    private static final int BTN_SIZE = 20;
    // Vanilla 1.7.10 creative inventory dimensions — stable constants.
    private static final int CREATIVE_W = 195;
    private static final int CREATIVE_H = 136;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new CreativeGuiHandler());
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiContainerCreative)) return;
        int guiLeft = (event.gui.width - CREATIVE_W) / 2;
        int guiTop = (event.gui.height - CREATIVE_H) / 2;
        event.buttonList.removeIf(b -> ((GuiButton) b).id == BUTTON_ID || ((GuiButton) b).id == BUTTON_ID_GRADIENT);
        event.buttonList.add(new ColourPickerButton(guiLeft + CREATIVE_W + 4, guiTop + 40));
        event.buttonList.add(new GradientButton(guiLeft + CREATIVE_W + 4, guiTop + 62));
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.gui instanceof GuiContainerCreative)) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (event.button.id == BUTTON_ID) {
            mc.displayGuiScreen(new GuiColourPicker(mc.thePlayer));
        } else if (event.button.id == BUTTON_ID_GRADIENT) {
            PacketHandler.CHANNEL.sendToServer(new PacketOpenGradientGui());
        }
    }

    private static final int C_PANEL = 0xFFC6C6C6;
    private static final int C_PANEL_HI = 0xFFFFFFFF;
    private static final int C_PANEL_SH = 0xFF555555;

    @SideOnly(Side.CLIENT)
    private static void drawItemButton(
            GuiButton btn, Minecraft mc, int mouseX, int mouseY, ItemStack icon, RenderItem renderer) {
        if (!btn.visible) return;
        boolean hovered = mouseX >= btn.xPosition
                && mouseY >= btn.yPosition
                && mouseX < btn.xPosition + btn.width
                && mouseY < btn.yPosition + btn.height;
        int fill = hovered ? 0xFFD4D4D4 : C_PANEL;
        GuiButton.drawRect(btn.xPosition, btn.yPosition, btn.xPosition + btn.width, btn.yPosition + btn.height, fill);
        GuiButton.drawRect(btn.xPosition, btn.yPosition, btn.xPosition + btn.width, btn.yPosition + 1, C_PANEL_HI);
        GuiButton.drawRect(btn.xPosition, btn.yPosition, btn.xPosition + 1, btn.yPosition + btn.height, C_PANEL_HI);
        GuiButton.drawRect(
                btn.xPosition,
                btn.yPosition + btn.height - 1,
                btn.xPosition + btn.width,
                btn.yPosition + btn.height,
                C_PANEL_SH);
        GuiButton.drawRect(
                btn.xPosition + btn.width - 1,
                btn.yPosition,
                btn.xPosition + btn.width,
                btn.yPosition + btn.height,
                C_PANEL_SH);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        renderer.zLevel = 100f;
        renderer.renderItemAndEffectIntoGUI(
                mc.fontRenderer, mc.getTextureManager(), icon, btn.xPosition + 2, btn.yPosition + 2);
        renderer.zLevel = 0f;
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
    }

    @SideOnly(Side.CLIENT)
    private static class ColourPickerButton extends GuiButton {

        private static final ItemStack ICON = new ItemStack(Items.dye, 1, 11);
        private final RenderItem renderer = new RenderItem();

        ColourPickerButton(int x, int y) {
            super(BUTTON_ID, x, y, BTN_SIZE, BTN_SIZE, "");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            drawItemButton(this, mc, mouseX, mouseY, ICON, renderer);
        }
    }

    @SideOnly(Side.CLIENT)
    private static class GradientButton extends GuiButton {

        private static final ItemStack ICON = new ItemStack(Items.blaze_powder, 1, 0);
        private final RenderItem renderer = new RenderItem();

        GradientButton(int x, int y) {
            super(BUTTON_ID_GRADIENT, x, y, BTN_SIZE, BTN_SIZE, "");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            drawItemButton(this, mc, mouseX, mouseY, ICON, renderer);
        }
    }
}
