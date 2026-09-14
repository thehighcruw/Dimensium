/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.noise;

import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseParams;
import imgui.ImGui;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class NoisePreviewRenderer {

    private static final int PREVIEW_TEX_SIZE = 64;

    public static int rerenderNoisePreview(NoiseParams p, int noisePreviewTex) {
        if (noisePreviewTex == -1) noisePreviewTex = GL11.glGenTextures();

        int sz = PREVIEW_TEX_SIZE;
        ByteBuffer buf = BufferUtils.createByteBuffer(sz * sz * 3);
        for (int py = 0; py < sz; py++) {
            for (int px = 0; px < sz; px++) {
                float wx = px * 50f / sz;
                float wy = py * 50f / sz;
                float v = NoiseSampler.sample2D(p, wx, wy);
                byte b = (byte) (int) (v * 255f);
                buf.put(b).put(b).put(b);
            }
        }

        buf.flip();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, noisePreviewTex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, sz, sz, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buf);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        float displaySize = ImGui.getContentRegionAvailX();
        ImGui.image(noisePreviewTex, displaySize, displaySize, 0, 0, 1, 1);

        return noisePreviewTex;
    }
}
