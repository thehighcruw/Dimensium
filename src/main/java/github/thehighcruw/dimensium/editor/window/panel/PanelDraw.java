/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.panel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeToolState;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeToolState.ShapeType;

/** Static drawing primitives and shared layout constants for all panel classes. */
public class PanelDraw {

    // ── Layout constants ──────────────────────────────────────────────────────
    public static final int PROPS_W = 200;
    public static final int TOTAL_W = PROPS_W;
    public static final int SELECTOR_H = 60;
    public static final int PAD = 8;
    public static final int SECTION_H = 12; // cy advance after sectionLabel()

    // Multi-palette row layout (relative to section cx)
    public static final int PAL_ROW_H = 20;
    public static final int PAL_NAME_X = 18;
    public static final int PAL_NAME_W = 46;
    public static final int PAL_SLIDER_X = 66;
    public static final int PAL_SLIDER_W = 52;
    public static final int PAL_PCT_X = 120;
    public static final int PAL_UP_X = 144;
    public static final int PAL_DN_X = 157;
    public static final int PAL_RM_X = 170;
    public static final int PAL_BTN_W = 11;
    public static final int PAL_RM_W = 14;

    // ── Colors ────────────────────────────────────────────────────────────────
    public static final float[] C_PANEL_TOP = { 0.075f, 0.075f, 0.100f, 0.97f };
    public static final float[] C_PANEL_BOT = { 0.055f, 0.055f, 0.078f, 0.97f };
    public static final float[] C_ACCENT = { 0.24f, 0.50f, 1.00f, 1.00f };
    public static final float[] C_BTN = { 0.13f, 0.13f, 0.18f, 0.95f };
    public static final float[] C_BTN_HOV = { 0.20f, 0.20f, 0.27f, 0.95f };
    public static final float[] C_BTN_ACT = { 0.24f, 0.50f, 1.00f, 0.24f };
    public static final float[] C_SEP = { 1.00f, 1.00f, 1.00f, 0.07f };
    public static final float[] C_SLIDER_BG = { 0.10f, 0.10f, 0.16f, 0.95f };
    public static final float[] C_SLIDER_FG = { 0.24f, 0.50f, 1.00f, 0.70f };
    public static final float TEXT_SCALE = 0.8f;

    private PanelDraw() {}

    // ── Text ──────────────────────────────────────────────────────────────────

    public static void drawSmall(FontRenderer fr, String text, int x, int y, int color) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glScalef(TEXT_SCALE, TEXT_SCALE, 1f);
        fr.drawString(text, Math.round(x / TEXT_SCALE), Math.round(y / TEXT_SCALE), color);
        GL11.glPopMatrix();
    }

    public static void drawSmallShadow(FontRenderer fr, String text, int x, int y, int color) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glScalef(TEXT_SCALE, TEXT_SCALE, 1f);
        fr.drawStringWithShadow(text, Math.round(x / TEXT_SCALE), Math.round(y / TEXT_SCALE), color);
        GL11.glPopMatrix();
    }

    public static int smallWidth(FontRenderer fr, String text) {
        return Math.round(fr.getStringWidth(text) * TEXT_SCALE);
    }

    public static String truncate(FontRenderer fr, String text, int maxPx) {
        if (smallWidth(fr, text) <= maxPx) return text;
        while (!text.isEmpty() && smallWidth(fr, text + "…") > maxPx) text = text.substring(0, text.length() - 1);
        return text + "…";
    }

    public static String formatLarge(int v) {
        if (v >= 1000000) return (v / 1000000) + "M";
        if (v >= 1000) return (v / 1000) + "k";
        return String.valueOf(v);
    }

    // ── Widgets ───────────────────────────────────────────────────────────────

    public static void sectionLabel(Tessellator t, FontRenderer fr, String text, int cx, int cy) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        color(t, C_ACCENT);
        rect(t, cx, cy + 2, cx + 4, cy + 5);
        drawSmall(fr, text, cx + 7, cy, 0x6699CC);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        color(t, C_SEP);
        rect(t, 4, cy + 8, TOTAL_W - 4, cy + 9);
    }

    public static void styledButton(Tessellator t, FontRenderer fr, int x, int y, int w, int h, String label,
        boolean active, boolean hov) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (active) {
            color(t, C_BTN_ACT);
            rect(t, x, y, x + w, y + h);
            color(t, C_ACCENT);
            rect(t, x, y, x + w, y + 1);
        } else {
            color(t, hov ? C_BTN_HOV : C_BTN);
            rect(t, x, y, x + w, y + h);
        }
        int tw = smallWidth(fr, label);
        drawSmall(fr, label, x + (w - tw) / 2, y + (h - 7) / 2, active ? 0xFFFFFF : hov ? 0xBBBBCC : 0x778899);
    }

    public static void miniBtn(Tessellator t, FontRenderer fr, int x, int y, int w, String label, boolean enabled,
        int mx, int my) {
        boolean hov = enabled && mx >= x && mx < x + w && my >= y && my < y + 11;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (enabled) {
            color(t, hov ? C_BTN_HOV : C_BTN);
        } else {
            GL11.glColor4f(0.08f, 0.08f, 0.11f, 0.50f);
        }
        rect(t, x, y, x + w, y + 11);
        int lw = smallWidth(fr, label);
        drawSmall(fr, label, x + (w - lw) / 2, y + 2, enabled ? (hov ? 0xFFFFFF : 0xBBBBCC) : 0x334444);
    }

    public static int hollowToggle(Tessellator t, FontRenderer fr, int cx, int cy, int mx, int my) {
        boolean hollow = ShapeToolState.INSTANCE.shapeHollow;
        boolean hov = mx >= cx && mx < cx + 72 && my >= cy && my < cy + 14;
        styledButton(
            t,
            fr,
            cx,
            cy,
            72,
            14,
            (hollow ? "\2476✓\247r " : "  ") + I18n.format("dimensium.ui.label.hollow"),
            hollow,
            hov);
        return cy + 18;
    }

    public static void renderItemIcon(Minecraft mc, ItemStack stack, int x, int y) {
        if (stack == null || stack.getItem() == null) return;
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        new RenderItem().renderItemAndEffectIntoGUI(mc.fontRenderer, mc.renderEngine, stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    // ── GL primitives ─────────────────────────────────────────────────────────

    public static void color(Tessellator t, float[] c) {
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
    }

    public static void rect(Tessellator t, int x1, int y1, int x2, int y2) {
        t.startDrawingQuads();
        t.addVertex(x1, y2, 0);
        t.addVertex(x2, y2, 0);
        t.addVertex(x2, y1, 0);
        t.addVertex(x1, y1, 0);
        t.draw();
    }

    public static void gradRect(int x1, int y1, int x2, int y2, float[] top, float[] bot) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(bot[0], bot[1], bot[2], bot[3]);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glColor4f(top[0], top[1], top[2], top[3]);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glEnd();
    }

    public static void gradRectH(int x1, int y1, int x2, int y2, float[] left, float[] right) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(left[0], left[1], left[2], left[3]);
        GL11.glVertex2f(x1, y2);
        GL11.glColor4f(right[0], right[1], right[2], right[3]);
        GL11.glVertex2f(x2, y2);
        GL11.glColor4f(right[0], right[1], right[2], right[3]);
        GL11.glVertex2f(x2, y1);
        GL11.glColor4f(left[0], left[1], left[2], left[3]);
        GL11.glVertex2f(x1, y1);
        GL11.glEnd();
    }

    // ── Shape helpers (shared by sections and click layout) ───────────────────

    public static int shapeCategory() {
        return switch (ShapeToolState.INSTANCE.shapeType) {
            case DISK, PLANE, SUPERELLIPSE, REGULAR_POLYGON, ARCHIMEDEAN_SPIRAL -> 1;
            default -> 0;
        };
    }

    public static ShapeType[] shapesForCategory(int cat) {
        if (cat == 1) return new ShapeType[] { ShapeType.DISK, ShapeType.PLANE, ShapeType.SUPERELLIPSE,
            ShapeType.REGULAR_POLYGON, ShapeType.ARCHIMEDEAN_SPIRAL };
        return new ShapeType[] { ShapeType.CUBOID, ShapeType.SPHERE, ShapeType.CYLINDER, ShapeType.PYRAMID,
            ShapeType.CONE, ShapeType.TORUS, ShapeType.OCTAHEDRON, ShapeType.SUPERSPHERE, ShapeType.TUBE,
            ShapeType.DODECAHEDRON, ShapeType.ICOSAHEDRON };
    }
}
