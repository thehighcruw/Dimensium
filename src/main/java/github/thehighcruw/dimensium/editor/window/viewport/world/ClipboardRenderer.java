/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;

import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.glu.GLU;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.shared.SelectionState;

@SideOnly(Side.CLIENT)
public class ClipboardRenderer {

    public static final int TEX_SIZE = 512;

    private int fboId = -1;
    private int texId = -1;
    private int depthId = -1;
    private int whiteLightmap = -1; // 1x1 white texture bound on lightmap unit
    private int lastVersion = -2;
    public boolean fboFailed = false;
    public String failReason = null;

    private float previewAzim = 225f;
    private float previewElev = 28f;
    private float previewZoom = 1.0f;

    public void setCamera(float azim, float elev, float zoom) {
        this.previewAzim = azim;
        this.previewElev = elev;
        this.previewZoom = zoom;
        lastVersion = -2; // force rebake on next maybeRebake()
    }

    public void resetCamera() {
        this.previewAzim = 225f;
        this.previewElev = 28f;
        this.previewZoom = 1.0f;
    }

    /** Call once per frame BEFORE 2D rendering begins, outside any glPushAttrib. */
    public void maybeRebake(SelectionState sel) {
        if (sel.clipboard == null || fboFailed) return;
        if (sel.clipboardVersion != lastVersion) {
            rebake(sel);
            lastVersion = sel.clipboardVersion;
        }
    }

    /** Returns the GL texture ID for the current clipboard, or -1 if unavailable. */
    public int getTexture(SelectionState sel) {
        if (sel.clipboard == null || fboFailed || texId == -1) return -1;
        return texId;
    }

    private void ensureFbo() {
        if (fboId != -1) return;
        try {
            // 1x1 all-white texture used as a stand-in lightmap (texture unit 1).
            // Without it, tessellator brightness coords sample black → blocks render black.
            whiteLightmap = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, whiteLightmap);
            ByteBuffer white = ByteBuffer.allocateDirect(4);
            white.put((byte) 0xFF)
                .put((byte) 0xFF)
                .put((byte) 0xFF)
                .put((byte) 0xFF)
                .flip();
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA8,
                1,
                1,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                white);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            fboId = EXTFramebufferObject.glGenFramebuffersEXT();
            texId = GL11.glGenTextures();
            depthId = EXTFramebufferObject.glGenRenderbuffersEXT();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA8,
                TEX_SIZE,
                TEX_SIZE,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, depthId);
            EXTFramebufferObject.glRenderbufferStorageEXT(
                EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                GL14.GL_DEPTH_COMPONENT24,
                TEX_SIZE,
                TEX_SIZE);
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, 0);

            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId);
            EXTFramebufferObject.glFramebufferTexture2DEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,
                GL11.GL_TEXTURE_2D,
                texId,
                0);
            EXTFramebufferObject.glFramebufferRenderbufferEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                depthId);

            int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT);
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);

            if (status != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT) {
                fboFailed = true;
                failReason = "FBO incomplete: 0x" + Integer.toHexString(status);
                System.err.println("[ClipboardRenderer] " + failReason);
            }
        } catch (Exception e) {
            fboFailed = true;
            failReason = e.getClass()
                .getSimpleName() + ": "
                + e.getMessage();
            System.err.println("[ClipboardRenderer] FBO creation failed: " + failReason);
        }
    }

    private void rebake(SelectionState sel) {
        ensureFbo();
        if (fboFailed || fboId == -1) return;
        System.out.println(
            "[ClipboardRenderer] rebaking " + sel.clipW
                + "x"
                + sel.clipH
                + "x"
                + sel.clipD
                + " into fbo="
                + fboId
                + " tex="
                + texId);

        int W = sel.clipW, H = sel.clipH, D = sel.clipD;

        // Save full GL state before touching anything
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        Tessellator tess = Tessellator.instance;
        boolean tessStarted = false;
        Minecraft mc = Minecraft.getMinecraft();
        int savedAO = mc.gameSettings.ambientOcclusion;

        try {
            // Bind FBO and set viewport
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId);
            GL11.glViewport(0, 0, TEX_SIZE, TEX_SIZE);

            GL11.glClearColor(0.05f, 0.05f, 0.07f, 1f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            // Perspective projection
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GLU.gluPerspective(45f, 1f, 0.1f, 1000f);

            // Camera: 30° elevation, looking at structure center from +x+z corner
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();

            float cx = W / 2f, cy = H / 2f, cz = D / 2f;
            float span = Math.max(Math.max(W, D), H);
            float dist = (span * 1.7f + 2f) / previewZoom;
            double elevRad = Math.toRadians(previewElev);
            double azimRad = Math.toRadians(previewAzim);

            float ex = cx + dist * (float) (Math.cos(elevRad) * Math.cos(azimRad));
            float ey = cy + dist * (float) Math.sin(elevRad);
            float ez = cz + dist * (float) (Math.cos(elevRad) * Math.sin(azimRad));

            GLU.gluLookAt(ex, ey, ez, cx, cy, cz, 0f, 1f, 0f);

            // Render blocks
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1f, 1f, 1f, 1f);

            // Bind block texture atlas on unit 0
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            mc.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);

            // Bind white 1x1 on unit 1 (lightmap unit).
            // Tessellator stores brightness as UV coords for unit 1; without a texture
            // bound here, the lightmap lookup returns black and blocks render invisible.
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, whiteLightmap);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

            ClipboardBlockAccess bAccess = new ClipboardBlockAccess(sel.clipboard, W, H, D);
            RenderBlocks rb = new RenderBlocks(bAccess);
            rb.useInventoryTint = false;
            rb.renderAllFaces = true; // ensure exterior faces always render

            mc.gameSettings.ambientOcclusion = 0;

            // RenderBlocks expects the tessellator to already be drawing —
            // the world renderer normally handles this, so we must do it here.
            tess.startDrawingQuads();
            tessStarted = true;

            for (java.util.Map.Entry<Long, SelectionState.BlockData> entry : sel.clipboard.entrySet()) {
                long key = entry.getKey();
                int x = (int) (key >> 20) & 0xFFFFF;
                int y = (int) (key >> 10) & 0x3FF;
                int z = (int) key & 0x3FF;
                SelectionState.BlockData bd = entry.getValue();
                if (bd != null && bd.block != Blocks.air) {
                    rb.renderBlockByRenderType(bd.block, x, y, z);
                }
            }

            tess.draw();
            tessStarted = false;
        } catch (Exception e) {
            System.err.println("[ClipboardRenderer] rebake failed: " + e);
        } finally {
            // Always clean up — FBO must be unbound and state restored even if a block renderer throws.
            if (tessStarted) {
                try {
                    tess.draw();
                } catch (Exception ignored) {}
            }
            mc.gameSettings.ambientOcclusion = savedAO;
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    /** PNG bytes of the last baked clipboard render. Populated on demand via {@link #capturePng()}. */
    private byte[] latestPng = null;

    public byte[] getLatestPng() {
        return latestPng;
    }

    /**
     * Synchronously reads the current FBO contents into {@link #latestPng}.
     * Causes a GPU pipeline stall — call only when the PNG is actually needed
     * (e.g. blueprint creation), never every frame or on every copy.
     */
    public void capturePng() {
        if (fboId == -1 || fboFailed) return;
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId);
        latestPng = readPixelsAsPng();
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
    }

    private byte[] readPixelsAsPng() {
        try {
            ByteBuffer buf = ByteBuffer.allocateDirect(TEX_SIZE * TEX_SIZE * 4);
            GL11.glReadPixels(0, 0, TEX_SIZE, TEX_SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
            int[] pixels = new int[TEX_SIZE * TEX_SIZE];
            for (int y = 0; y < TEX_SIZE; y++) {
                for (int x = 0; x < TEX_SIZE; x++) {
                    int r = buf.get() & 0xFF;
                    int g = buf.get() & 0xFF;
                    int b = buf.get() & 0xFF;
                    int a = buf.get() & 0xFF;
                    pixels[(TEX_SIZE - 1 - y) * TEX_SIZE + x] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
            BufferedImage img = new BufferedImage(TEX_SIZE, TEX_SIZE, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, TEX_SIZE, TEX_SIZE, pixels, 0, TEX_SIZE);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("[ClipboardRenderer] PNG capture failed: " + e.getMessage());
            return null;
        }
    }

}
