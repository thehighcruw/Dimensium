/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.panel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

/** Standalone drop-down widget. mx/my must be passed explicitly (no outer-class capture). */
public class PanelComboBox {

    boolean open = false;
    int screenY = 0;

    public int renderHeader(Tessellator t, FontRenderer fr, String current, int cx, int cy, int w, int mx, int my) {
        screenY = cy;
        boolean hov = mx >= cx && mx < cx + w && my >= cy && my < cy + 16;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        PanelDraw.color(t, hov ? PanelDraw.C_BTN_HOV : PanelDraw.C_BTN);
        PanelDraw.rect(t, cx, cy, cx + w, cy + 16);
        if (open) {
            PanelDraw.color(t, PanelDraw.C_ACCENT);
            PanelDraw.rect(t, cx, cy + 15, cx + w, cy + 16);
        }
        PanelDraw.drawSmall(fr, current, cx + 4, cy + 4, 0xFFFFFF);
        PanelDraw.drawSmall(fr, open ? "^" : "v", cx + w - 12, cy + 4, 0x8899AA);
        return cy + 20;
    }

    public void renderPopup(Tessellator t, FontRenderer fr, String[] labels, int selectedIdx, int cx, int w, int sh,
        int mx, int my) {
        if (!open) return;
        int popupH = labels.length * 14 + 2;
        int dropY = screenY + 17;
        if (dropY + popupH > sh - 8) dropY = screenY - popupH;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.10f, 0.10f, 0.14f, 0.98f);
        Tessellator tt = Tessellator.instance;
        tt.startDrawingQuads();
        tt.addVertex(cx, dropY + popupH, 0);
        tt.addVertex(cx + w, dropY + popupH, 0);
        tt.addVertex(cx + w, dropY, 0);
        tt.addVertex(cx, dropY, 0);
        tt.draw();
        PanelDraw.color(t, PanelDraw.C_ACCENT);
        PanelDraw.rect(t, cx, dropY, cx + w, dropY + 1);
        PanelDraw.rect(t, cx, dropY + popupH - 1, cx + w, dropY + popupH);

        int iy = dropY + 1;
        for (int i = 0; i < labels.length; i++) {
            boolean sel = i == selectedIdx;
            boolean hov = mx >= cx && mx < cx + w && my >= iy && my < iy + 14;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            if (sel) {
                PanelDraw.color(t, PanelDraw.C_BTN_ACT);
                PanelDraw.rect(t, cx, iy, cx + w, iy + 14);
                PanelDraw.color(t, PanelDraw.C_ACCENT);
                PanelDraw.rect(t, cx, iy, cx + 2, iy + 14);
            } else if (hov) {
                PanelDraw.color(t, PanelDraw.C_BTN_HOV);
                PanelDraw.rect(t, cx, iy, cx + w, iy + 14);
            }
            PanelDraw.drawSmall(fr, labels[i], cx + 6, iy + 3, sel ? 0xFFFFFF : 0xBBBBCC);
            iy += 14;
        }
    }

    public int handleClick(int ax, int ay, String[] labels, int cx, int w) {
        int guiScale = Minecraft.getMinecraft().gameSettings.guiScale;
        if (guiScale < 1) guiScale = 2;
        int sh = Minecraft.getMinecraft().displayHeight / guiScale;
        if (open) {
            int popupH = labels.length * 14 + 2;
            int dropY = screenY + 17;
            if (dropY + popupH > sh - 8) dropY = screenY - popupH;
            int iy = dropY + 1;
            int result = -1;
            for (int i = 0; i < labels.length; i++) {
                if (ay >= iy && ay < iy + 14 && ax >= cx && ax < cx + w) result = i;
                iy += 14;
            }
            open = false;
            return result;
        }
        if (ay >= screenY && ay < screenY + 16 && ax >= cx && ax < cx + w) {
            open = true;
            return -2;
        }
        return -3;
    }

    public void close() {
        open = false;
    }
}
