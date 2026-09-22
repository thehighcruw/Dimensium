/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.imgui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.Tool;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Loads tool icon PNGs from assets/dimensium/textures/tools/<name>.png and caches
 * them as GL textures for use with ImGui's draw list. Returns 0 for any tool
 * whose icon file is missing or fails to load.
 *
 * UV coordinates are 0,0 → 1,1 (top-left origin), unlike FBO-baked item icons
 * in ItemIconCache which require a vertical flip.
 */
@SideOnly(Side.CLIENT)
public final class ToolIconCache {

    public static final ToolIconCache INSTANCE = new ToolIconCache();

    private ToolIconCache() {}

    private static final int BYTES_PER_PIXEL = 4;

    private final Map<Tool, Integer> cache = new EnumMap<>(Tool.class);

    /** Returns the GL texture ID for the given tool's icon, or 0 if absent/failed. */
    public int getTexture(Tool tool) {
        if (cache.isEmpty()) loadAll();
        return cache.getOrDefault(tool, 0);
    }

    private void loadAll() {
        for (Tool tool : Tool.values()) {
            cache.put(tool, loadIcon(tool.name().toLowerCase()));
        }
    }

    private static int loadIcon(String name) {
        try {
            ResourceLocation location = new ResourceLocation("dimensium", "textures/tools/" + name + ".png");
            InputStream stream = Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(location)
                    .getInputStream();
            BufferedImage img = ImageIO.read(stream);
            if (img == null) return 0;
            return upload(img);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int upload(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int[] pixels = img.getRGB(0, 0, width, height, null, 0, width);
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * BYTES_PER_PIXEL);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();

        int texId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texId;
    }
}
