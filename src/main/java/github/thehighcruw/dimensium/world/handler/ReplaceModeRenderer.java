/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.handler;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.window.viewport.world.WorldLines;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class ReplaceModeRenderer {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        if (!ReplaceModeState.INSTANCE.active) return;
        if (OverlayRenderer.isNotCreative()) return;
        if (DimensiumEditorMode.INSTANCE.isActive()) return;
        if (event.target == null || event.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        // Suppress vanilla selection box — we draw our own below in RenderWorldLastEvent.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!ReplaceModeState.INSTANCE.active) return;
        if (OverlayRenderer.isNotCreative()) return;
        if (DimensiumEditorMode.INSTANCE.isActive()) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;

        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        float pt = event.partialTicks;
        Entity cam = mc.renderViewEntity != null ? mc.renderViewEntity : player;
        Vec3DDouble camPos = Vec3DDouble.from(
                cam.lastTickPosX + (cam.posX - cam.lastTickPosX) * pt,
                cam.lastTickPosY + (cam.posY - cam.lastTickPosY) * pt,
                cam.lastTickPosZ + (cam.posZ - cam.lastTickPosZ) * pt);

        Vec3DDouble blockTrans =
                Vec3DDouble.from(mop.blockX - camPos.x(), mop.blockY - camPos.y(), mop.blockZ - camPos.z());

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glPushMatrix();
        GL11.glTranslated(blockTrans.x(), blockTrans.y(), blockTrans.z());
        WorldLines.setEyeForTranslation(blockTrans);

        GL11.glColor4f(1.0f, 0.3f, 0.1f, 1.0f);
        WorldLines.drawBox(-0.002f, -0.002f, -0.002f, 1.002f, 1.002f, 1.002f);

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}
