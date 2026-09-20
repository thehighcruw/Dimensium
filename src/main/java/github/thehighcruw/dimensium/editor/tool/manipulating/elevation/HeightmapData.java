/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.elevation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

public class HeightmapData {

    private final String name;
    private final float[][] pixels; // [row=z][col=x], normalized 0..1
    private final int width;
    private final int height;

    private HeightmapData(String name, float[][] pixels, int width, int height) {
        this.name = name;
        this.pixels = pixels;
        this.width = width;
        this.height = height;
    }

    public String name() {
        return name;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public static HeightmapData fromStream(String name, InputStream stream) throws IOException {
        BufferedImage img = ImageIO.read(stream);
        if (img == null) throw new IOException("Failed to decode image: " + name);
        return fromImage(name, img);
    }

    public static HeightmapData fromFile(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new IOException("Failed to decode: " + file.getName());
        return fromImage(stripExtension(file.getName()), img);
    }

    private static HeightmapData fromImage(String name, BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        float[][] pixels = new float[h][w];
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                int argb = img.getRGB(col, row);
                float r = ((argb >> 16) & 0xFF) / 255f;
                float g = ((argb >> 8) & 0xFF) / 255f;
                float b = (argb & 0xFF) / 255f;
                pixels[row][col] = (r + g + b) / 3f;
            }
        }
        return new HeightmapData(name, pixels, w, h);
    }

    /**
     * Samples the heightmap at normalized brush coordinates.
     *
     * @param nx normalized x in [-1, 1]
     * @param nz normalized z in [-1, 1]
     * @return interpolated value in [0, 1]
     */
    public float sample(float nx, float nz) {
        float px = (nx * 0.5f + 0.5f) * (width - 1);
        float pz = (nz * 0.5f + 0.5f) * (height - 1);
        int x0 = Math.max(0, Math.min(width - 1, (int) px));
        int z0 = Math.max(0, Math.min(height - 1, (int) pz));
        int x1 = Math.min(width - 1, x0 + 1);
        int z1 = Math.min(height - 1, z0 + 1);
        float tx = px - x0;
        float tz = pz - z0;
        float v00 = pixels[z0][x0];
        float v10 = pixels[z0][x1];
        float v01 = pixels[z1][x0];
        float v11 = pixels[z1][x1];
        return (v00 * (1 - tx) + v10 * tx) * (1 - tz) + (v01 * (1 - tx) + v11 * tx) * tz;
    }

    /** Returns a direct RGBA ByteBuffer suitable for GL texture upload. */
    public ByteBuffer toRgbaBuffer() {
        ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                byte v = (byte) Math.round(pixels[row][col] * 255f);
                buf.put(v);
                buf.put(v);
                buf.put(v);
                buf.put((byte) 0xFF);
            }
        }
        buf.flip();
        return buf;
    }

    static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
