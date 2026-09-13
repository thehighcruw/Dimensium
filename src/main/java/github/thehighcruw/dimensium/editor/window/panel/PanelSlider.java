/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.panel;

import java.util.function.Consumer;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/**
 * Standalone slider widget. Supports linear, quadratic, and exponential scaling.
 *
 * Usage:
 * - Call render() each frame to draw; pass current value from your state.
 * - Call onClick() on LMB; pass a setter lambda — drag is then tracked statically.
 * - Call PanelSlider.updateDrag(mouseX) each render frame while LMB is held.
 * - Call PanelSlider.endDrag() on LMB release.
 * - Call PanelSlider.handleKey(key, ch) from KeyHandler to handle ctrl+click text input.
 *
 * Special case: pass a non-null displayText to render() to override the value label
 * (e.g. show "75%" instead of the raw weight for palette entries).
 */
public class PanelSlider {

    // ── Scale enum ────────────────────────────────────────────────────────────

    public enum Scale {
        /** value = min + t * (max − min) */
        LINEAR,
        /** value = min + t² * (max − min) — small values more precise */
        QUADRATIC,
        /** value = min * (max/min)^t — logarithmic feel */
        EXPONENTIAL
    }

    // ── Instance config ───────────────────────────────────────────────────────

    final float min, max, defaultValue;
    final Scale scale;

    // ── Shared singleton state (at most one slider dragging/editing at a time) ──

    public static final class SharedState {

        public static final SharedState INSTANCE = new SharedState();

        private SharedState() {}

        // drag
        public PanelSlider activeDrag = null;
        public int dragBarX, dragBarW;
        public Consumer<Float> dragSetter;
        public float dragMin, dragMax;
        public Scale dragScale;

        // text-edit
        public PanelSlider activeEdit = null;
        public String editBuffer = "";
        public Consumer<Float> editSetter;
        public float editMin, editMax;
    }

    // ── Layout constants (relative to cx / PROPS_W) ───────────────────────────

    private static final int LABEL_W = 55;
    private static final int VAL_W = 38;
    private static final int GAP = 4;
    private static final int ROW_H = 14;
    private static final int BAR_H = 8;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PanelSlider(float min, float max, float defaultValue, Scale scale) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.scale = scale;
    }

    // ── Scale math ────────────────────────────────────────────────────────────

    private static float valueToT(float v, float mn, float mx, Scale sc) {
        float clamped = Math.max(mn, Math.min(mx, v));
        float linear = (clamped - mn) / (mx - mn);
        return switch (sc) {
            case QUADRATIC -> (float) Math.sqrt(Math.max(0, linear));
            case EXPONENTIAL -> (mn <= 0 || mx <= 0) ? linear : (float) (Math.log(clamped / mn) / Math.log(mx / mn));
            default -> linear;
        };
    }

    static float tToValue(float t, float mn, float mx, Scale sc) {
        float c = Math.max(0f, Math.min(1f, t));
        return switch (sc) {
            case QUADRATIC -> mn + c * c * (mx - mn);
            case EXPONENTIAL -> (mn <= 0 || mx <= 0) ? mn + c * (mx - mn) : mn * (float) Math.pow(mx / mn, c);
            case LINEAR -> mn + c * (mx - mn);
        };
    }

    // ── Geometry helpers ──────────────────────────────────────────────────────

    private static int barX(int cx) {
        return cx + LABEL_W;
    }

    private static int barW(int cx) {
        return (PanelDraw.PROPS_W - PanelDraw.PAD) - barX(cx) - VAL_W - GAP;
    }

    private static int barTop(int cy) {
        return cy + (ROW_H - BAR_H) / 2;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    /**
     * Draws a slider row and returns the next cy (cy + ROW_H + 2).
     *
     * @param displayText if non-null, shown as value label instead of the formatted number
     *                    (use for palette % display)
     * @param value       current value from caller's state
     */
    public int render(Tessellator t, FontRenderer fr, String label, String displayText, float value, int cx, int cy,
        int mx, int my) {
        int bx = barX(cx);
        int bw = barW(cx);
        int by = barTop(cy);
        boolean hov = mx >= bx && mx < bx + bw && my >= cy && my < cy + ROW_H;

        PanelDraw.drawSmall(fr, label, cx, cy + 3, SharedState.INSTANCE.activeEdit == this ? 0xFFFFFF : 0x888899);

        if (SharedState.INSTANCE.activeEdit == this) {
            // Text input: bright distinct background, thick border, white text
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            // Background: dark teal, covers bar+value area
            GL11.glColor4f(0.05f, 0.20f, 0.25f, 0.98f);
            PanelDraw.rect(t, bx, cy, bx + bw + GAP + VAL_W, cy + ROW_H);
            // 2px accent border top + bottom
            PanelDraw.color(t, PanelDraw.C_ACCENT);
            PanelDraw.rect(t, bx, cy, bx + bw + GAP + VAL_W, cy + 2);
            PanelDraw.rect(t, bx, cy + ROW_H - 2, bx + bw + GAP + VAL_W, cy + ROW_H);
            // Typed text + blinking cursor
            boolean cursorOn = (System.currentTimeMillis() / 500) % 2 == 0;
            String display = SharedState.INSTANCE.editBuffer + (cursorOn ? "|" : " ");
            PanelDraw.drawSmall(fr, display, bx + 3, cy + 4, 0xFFFFFF);
        } else {
            // Bar
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            PanelDraw.color(t, PanelDraw.C_SLIDER_BG);
            PanelDraw.rect(t, bx, by, bx + bw, by + BAR_H);

            float tv = valueToT(value, min, max, scale);
            int fill = Math.round(tv * bw);
            if (fill > 0) {
                PanelDraw.color(t, hov ? PanelDraw.C_ACCENT : PanelDraw.C_SLIDER_FG);
                PanelDraw.rect(t, bx, by, bx + fill, by + BAR_H);
            }
            // Handle marker
            int hx = bx + Math.max(0, Math.min(bw - 2, fill - 1));
            GL11.glColor4f(1f, 1f, 1f, 0.45f);
            PanelDraw.rect(t, hx, by, hx + 2, by + BAR_H);

            // Value text
            String valStr = displayText != null ? displayText : formatValue(value);
            PanelDraw.drawSmall(fr, valStr, bx + bw + GAP, cy + 3, hov ? 0xFFFFFF : 0xBBBBCC);
        }

        return cy + ROW_H + 2;
    }

    // ── Click / drag entry ────────────────────────────────────────────────────

    /**
     * Call on LMB click. Returns true if this slider consumed the click.
     *
     * @param ctrl   whether Ctrl is held (triggers text-input mode)
     * @param setter lambda to write the new value back into caller's state
     */
    public boolean onClick(int ax, int ay, int cx, int cy, float currentValue, boolean ctrl, Consumer<Float> setter) {
        int bx = barX(cx);
        int bw = barW(cx);
        if (ay < cy || ay >= cy + ROW_H) return false;
        // Ctrl+click: full row width; drag: bar only
        if (ctrl && ax < cx) return false;
        if (!ctrl && (ax < bx || ax >= bx + bw)) return false;

        if (ctrl) {
            SharedState.INSTANCE.activeEdit = this;
            SharedState.INSTANCE.editBuffer = formatValue(currentValue);
            SharedState.INSTANCE.editSetter = setter;
            SharedState.INSTANCE.editMin = min;
            SharedState.INSTANCE.editMax = max;
            SharedState.INSTANCE.activeDrag = null;
        } else {
            SharedState.INSTANCE.activeEdit = null;
            SharedState.INSTANCE.activeDrag = this;
            SharedState.INSTANCE.dragBarX = bx;
            SharedState.INSTANCE.dragBarW = bw;
            SharedState.INSTANCE.dragSetter = setter;
            SharedState.INSTANCE.dragMin = min;
            SharedState.INSTANCE.dragMax = max;
            SharedState.INSTANCE.dragScale = scale;
            float tv = (ax - bx) / (float) bw;
            setter.accept(tToValue(tv, min, max, scale));
        }
        return true;
    }

    // ── Static drag update / release ──────────────────────────────────────────

    static void updateDrag(int mouseX) {
        if (SharedState.INSTANCE.activeDrag == null) return;
        float tv = (mouseX - SharedState.INSTANCE.dragBarX) / (float) SharedState.INSTANCE.dragBarW;
        SharedState.INSTANCE.dragSetter.accept(
            tToValue(tv, SharedState.INSTANCE.dragMin, SharedState.INSTANCE.dragMax, SharedState.INSTANCE.dragScale));
    }

    static void endDrag() {
        SharedState.INSTANCE.activeDrag = null;
    }

    // ── Static key handler (call from KeyHandler when overlay is active) ──────

    /**
     * Returns true if a text-edit was active and consumed the key (caller should
     * not process the key further).
     */
    public static boolean handleKey(int key, char ch) {
        if (SharedState.INSTANCE.activeEdit == null) return false;

        if (key == Keyboard.KEY_ESCAPE) {
            SharedState.INSTANCE.activeEdit = null;
            return true;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            commitEdit();
            return true;
        }
        if (key == Keyboard.KEY_BACK) {
            if (!SharedState.INSTANCE.editBuffer.isEmpty())
                SharedState.INSTANCE.editBuffer = SharedState.INSTANCE.editBuffer
                    .substring(0, SharedState.INSTANCE.editBuffer.length() - 1);
            return true;
        }
        // Accept digits, decimal point, minus sign
        if (ch >= '0' && ch <= '9' || ch == '.' || (ch == '-' && SharedState.INSTANCE.editBuffer.isEmpty())) {
            SharedState.INSTANCE.editBuffer += ch;
            return true;
        }
        // Consume other keys while editing to prevent accidental tool-switches
        return true;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static void commitEdit() {
        if (SharedState.INSTANCE.editSetter == null) {
            SharedState.INSTANCE.activeEdit = null;
            return;
        }
        try {
            float v = Float.parseFloat(SharedState.INSTANCE.editBuffer);
            v = Math.max(SharedState.INSTANCE.editMin, Math.min(SharedState.INSTANCE.editMax, v));
            SharedState.INSTANCE.editSetter.accept(v);
        } catch (NumberFormatException ignored) {
            // leave value unchanged on bad input
        }
        SharedState.INSTANCE.activeEdit = null;
    }

    static String formatValue(float v) {
        if (v == Math.floor(v) && !Float.isInfinite(v) && Math.abs(v) < 1e6f) return String.valueOf((int) v);
        return String.format("%.2f", v);
    }
}
