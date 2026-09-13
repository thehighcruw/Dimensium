/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.imgui;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Bakes MC item icons into cached GL textures so they can be drawn via
 * ImGui's draw list (addImage), which gives correct z-ordering with modals.
 *
 * Each unique (item id, damage) pair is rendered once into a shared FBO at
 * FBO_SIZE × FBO_SIZE. The resulting texture is stored and reused every frame.
 *
 * UV coordinates are flipped vertically (uv0Y=1, uv1Y=0) when calling
 * addImage because OpenGL FBO row 0 is at the bottom while ImGui UV (0,0)
 * is at the top-left.
 */
@SideOnly(Side.CLIENT)
public final class ItemIconCache {

    public static final ItemIconCache INSTANCE = new ItemIconCache();

    private ItemIconCache() {}

    static final int FBO_SIZE = 32;
    private static final RenderItem RENDER_ITEM = new RenderItem();

    // key = item numeric id * 65536 + damage value
    private final Map<Integer, Integer> cache = new HashMap<>();
    private int fboId = 0;
    private int depthRboId = 0;

    private static int cacheKey(ItemStack stack) {
        return Item.getIdFromItem(stack.getItem()) * 65536 + stack.getItemDamage();
    }

    /** Returns the GL texture id for this stack, baking it on first access. */
    public int getTexture(ItemStack stack) {
        if (stack == null) return 0;
        int key = cacheKey(stack);
        Integer cached = cache.get(key);
        if (cached != null) return cached;
        return bake(stack, key);
    }

    private void ensureFBO() {
        if (fboId != 0) return;
        fboId = GL30.glGenFramebuffers();
        depthRboId = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRboId);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, FBO_SIZE, FBO_SIZE);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthRboId);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private int bake(ItemStack stack, int key) {
        ensureFBO();
        Minecraft mc = Minecraft.getMinecraft();

        int texId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA8,
            FBO_SIZE,
            FBO_SIZE,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        int prevFBO = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texId, 0);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        GL11.glViewport(0, 0, FBO_SIZE, FBO_SIZE);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        // Y-down to match MC GUI convention. FBO row 0 ends up at the bottom,
        // so callers must pass uv0Y=1 / uv1Y=0 to addImage to un-flip.
        GL11.glOrtho(0, 16, 16, 0, -2000, 2000);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef(0f, 0f, -2000f);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        RENDER_ITEM.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.renderEngine, stack, 0, 0);
        RENDER_ITEM.renderItemOverlayIntoGUI(mc.fontRenderer, mc.renderEngine, stack, 0, 0);

        RenderHelper.disableStandardItemLighting();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFBO);

        cache.put(key, texId);
        return texId;
    }

    /** Delete all cached textures and the shared FBO. Call on resource reload or shutdown. */
    public void clear() {
        for (int texId : cache.values()) {
            GL11.glDeleteTextures(texId);
        }
        cache.clear();
        if (fboId != 0) {
            GL30.glDeleteFramebuffers(fboId);
            GL30.glDeleteRenderbuffers(depthRboId);
            fboId = 0;
            depthRboId = 0;
        }
    }
}
