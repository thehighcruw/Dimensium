/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.blueprint;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;

import com.github.bsideup.jabel.Desugar;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Uploads blueprint thumbnail PNGs as GL textures, evicting the oldest when full.
 *
 * Sidecar PNG files are read and decoded on a background thread pool so the render
 * thread never blocks. Returns -1 (placeholder) until decoding finishes, then the
 * texture is uploaded on the next render-thread call to get().
 */
@SideOnly(Side.CLIENT)
public class BlueprintThumbnailCache {

    private static final int MAX_ENTRIES = 256;

    private static final ExecutorService DECODE_POOL = Executors.newFixedThreadPool(2, new ThreadFactory() {

        private int n = 0;

        public Thread newThread(@Nonnull Runnable r) {
            Thread t = new Thread(r, "bp-thumb-decode-" + n++);
            t.setDaemon(true);
            return t;
        }
    });

    @Desugar
    private record DecodedImage(int w, int h, ByteBuffer buf) {}

    private final Map<File, Integer> cache = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {

        protected boolean removeEldestEntry(Map.Entry<File, Integer> eldest) {
            if (size() > MAX_ENTRIES) {
                GL11.glDeleteTextures(eldest.getValue());
                return true;
            }
            return false;
        }
    };

    private final Set<File> pending = ConcurrentHashMap.newKeySet();
    private final Map<File, DecodedImage> ready = new ConcurrentHashMap<>();

    /**
     * Returns GL texture ID for the blueprint file, or -1 while loading / unavailable.
     * Reads the sidecar PNG (blueprintFile + ".png") lazily on a background thread.
     */
    public int get(File blueprintFile) {
        Integer cached = cache.get(blueprintFile);
        if (cached != null) return cached;

        DecodedImage decoded = ready.remove(blueprintFile);
        if (decoded != null) {
            int id = upload(decoded);
            if (id != -1) cache.put(blueprintFile, id);
            return id;
        }

        if (!pending.add(blueprintFile)) return -1;

        DECODE_POOL.submit(() -> {
            DecodedImage img = decode(BlueprintIO.sidecarFor(blueprintFile));
            if (img != null) ready.put(blueprintFile, img);
            pending.remove(blueprintFile);
        });
        return -1;
    }

    private static DecodedImage decode(File sidecar) {
        if (!sidecar.exists()) return null;
        try {
            byte[] bytes = Files.readAllBytes(sidecar.toPath());
            BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if (img == null) return null;
            int w = img.getWidth(), h = img.getHeight();
            int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);
            ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4);
            for (int pixel : pixels) {
                buf.put((byte) ((pixel >> 16) & 0xFF));
                buf.put((byte) ((pixel >> 8) & 0xFF));
                buf.put((byte) (pixel & 0xFF));
                buf.put((byte) ((pixel >> 24) & 0xFF));
            }
            buf.flip();
            return new DecodedImage(w, h, buf);
        } catch (Exception e) {
            return null;
        }
    }

    private static int upload(DecodedImage img) {
        try {
            int id = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA8,
                img.w,
                img.h,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                img.buf);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            return id;
        } catch (Exception e) {
            return -1;
        }
    }
}
