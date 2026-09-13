/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.inventory.colorPicker;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ColourPickerSlider extends Gui {

    private static final int TRACK_H = 10;
    private static final int HANDLE_W = 3;

    /** Gradient stops: packed ARGB pairs (left → right). */
    private int leftColor;
    private int rightColor;
    private boolean rainbowHue;

    private float value;
    private final float min;
    private final float max;

    private int trackX;
    private int trackY;
    private int trackW;
    private boolean dragging;

    private final Runnable onChange;

    public ColourPickerSlider(float min, float max, float value, Runnable onChange) {
        this.min = min;
        this.max = max;
        this.value = value;
        this.onChange = onChange;
    }

    public void setGradient(int leftArgb, int rightArgb) {
        this.leftColor = leftArgb;
        this.rightColor = rightArgb;
        this.rainbowHue = false;
    }

    public void setRainbowHue() {
        this.rainbowHue = true;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float v) {
        this.value = clamp(v);
    }

    public void draw(int x, int y, int w) {
        this.trackX = x;
        this.trackY = y;
        this.trackW = w;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        Tessellator tess = Tessellator.instance;

        if (rainbowHue) {
            drawRainbow(tess, x, y, w);
        } else {
            drawGradient(tess, x, y, w, leftColor, rightColor);
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        // Handle: 3px wide (white border, 1px black centre).
        int hx = x + (int) ((value - min) / (max - min) * (w - HANDLE_W));
        drawRect(hx, y - 1, hx + HANDLE_W, y + TRACK_H + 1, 0xFFFFFFFF);
        drawRect(hx + 1, y, hx + 2, y + TRACK_H, 0xFF000000);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private static void drawGradient(Tessellator tess, int x, int y, int w, int left, int right) {
        float lr = ((left >> 16) & 0xFF) / 255f;
        float lg = ((left >> 8) & 0xFF) / 255f;
        float lb = (left & 0xFF) / 255f;
        float la = ((left >> 24) & 0xFF) / 255f;
        float rr = ((right >> 16) & 0xFF) / 255f;
        float rg = ((right >> 8) & 0xFF) / 255f;
        float rb = (right & 0xFF) / 255f;
        float ra = ((right >> 24) & 0xFF) / 255f;

        tess.startDrawingQuads();
        tess.setColorRGBA_F(rr, rg, rb, ra);
        tess.addVertex(x + w, y + TRACK_H, 0);
        tess.addVertex(x + w, y, 0);
        tess.setColorRGBA_F(lr, lg, lb, la);
        tess.addVertex(x, y, 0);
        tess.addVertex(x, y + TRACK_H, 0);
        tess.draw();
    }

    private static void drawRainbow(Tessellator tess, int x, int y, int w) {
        // 6 hue segments: 0°→60°→120°→180°→240°→300°→360°
        float[][] stops = { { 1f, 0f, 0f }, // red
            { 1f, 1f, 0f }, // yellow
            { 0f, 1f, 0f }, // green
            { 0f, 1f, 1f }, // cyan
            { 0f, 0f, 1f }, // blue
            { 1f, 0f, 1f }, // magenta
            { 1f, 0f, 0f }, // red again
        };
        int segments = stops.length - 1;
        tess.startDrawingQuads();
        for (int i = 0; i < segments; i++) {
            int x0 = x + i * w / segments;
            int x1 = x + (i + 1) * w / segments;
            float[] l = stops[i];
            float[] r = stops[i + 1];
            tess.setColorRGBA_F(r[0], r[1], r[2], 1f);
            tess.addVertex(x1, y + TRACK_H, 0);
            tess.addVertex(x1, y, 0);
            tess.setColorRGBA_F(l[0], l[1], l[2], 1f);
            tess.addVertex(x0, y, 0);
            tess.addVertex(x0, y + TRACK_H, 0);
        }
        tess.draw();
    }

    public boolean mousePressed(int mx, int my) {
        if (mx >= trackX && mx <= trackX + trackW && my >= trackY - 1 && my <= trackY + TRACK_H + 1) {
            dragging = true;
            updateFromMouse(mx);
            return true;
        }
        return false;
    }

    public void mouseReleased() {
        dragging = false;
    }

    public void mouseDragged(int mx) {
        if (!dragging) return;
        updateFromMouse(mx);
    }

    private void updateFromMouse(int mx) {
        float t = (float) (mx - trackX) / trackW;
        value = clamp(min + t * (max - min));
        if (onChange != null) onChange.run();
    }

    private float clamp(float v) {
        return Math.max(min, Math.min(max, v));
    }
}
