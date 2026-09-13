/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.imgui;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import imgui.ImDrawData;
import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImVec4;
import imgui.type.ImInt;

@SideOnly(Side.CLIENT)
public class DimensiumImGuiGlRenderer {

    private static final Logger LOG = LogManager.getLogger("DimensiumImGui");

    private int program;
    private int uniformTex, uniformProjMtx;
    private int attribPos, attribUV, attribColor;
    private int vboHandle, iboHandle;
    private int vaoHandle; // 0 if VAOs unavailable (Compatibility Profile)
    private boolean useVao;
    private int fontTexture;

    private static final String VERT_SRC = """
        #version 120
        uniform mat4 ProjMtx;
        attribute vec2 Position;
        attribute vec2 UV;
        attribute vec4 Color;
        varying vec2 Frag_UV;
        varying vec4 Frag_Color;
        void main() {
          Frag_UV = UV;
          Frag_Color = Color;
          gl_Position = ProjMtx * vec4(Position.xy, 0, 1);
        }
        """;

    private static final String FRAG_SRC = """
        #version 120
        uniform sampler2D Texture;
        varying vec2 Frag_UV;
        varying vec4 Frag_Color;
        void main() {
          gl_FragColor = Frag_Color * texture2D(Texture, Frag_UV.st);
        }
        """;

    public void init() {
        createDeviceObjects();
        createFontsTexture();
    }

    private int compileShader(int type, String src) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            LOG.error("ImGui shader compile failed: {}", GL20.glGetShaderInfoLog(id, 512));
        }
        return id;
    }

    private void createDeviceObjects() {
        int vert = compileShader(GL20.GL_VERTEX_SHADER, VERT_SRC);
        int frag = compileShader(GL20.GL_FRAGMENT_SHADER, FRAG_SRC);

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vert);
        GL20.glAttachShader(program, frag);
        // Force predictable locations: Position=0 avoids gl_Vertex aliasing issues
        // in GL 2.1 Compatibility Profile when attrib 0 defaults to Color.
        GL20.glBindAttribLocation(program, 0, "Position");
        GL20.glBindAttribLocation(program, 1, "UV");
        GL20.glBindAttribLocation(program, 2, "Color");
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            LOG.error("ImGui shader link failed: {}", GL20.glGetProgramInfoLog(program, 512));
        }
        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);

        uniformTex = GL20.glGetUniformLocation(program, "Texture");
        uniformProjMtx = GL20.glGetUniformLocation(program, "ProjMtx");
        attribPos = GL20.glGetAttribLocation(program, "Position");
        attribUV = GL20.glGetAttribLocation(program, "UV");
        attribColor = GL20.glGetAttribLocation(program, "Color");

        vboHandle = GL15.glGenBuffers();
        iboHandle = GL15.glGenBuffers();

        // VAO disabled: APPLE_vertex_array_object on LWJGL2 macOS may not track
        // ELEMENT_ARRAY_BUFFER or behave correctly with VBOs. Compatibility Profile
        // does not require VAOs — plain VBO + attrib pointer state works fine.
        useVao = false;

        // Try VAO paths: ARB extension → APPLE extension (macOS) → GL30 core → none.
        // LWJGL2's GL30 shim checks capability flags that may be unset even if the context
        // supports VAOs; ARB/APPLE paths bypass that check.
        try {
            vaoHandle = ARBVertexArrayObject.glGenVertexArrays();
            ARBVertexArrayObject.glBindVertexArray(vaoHandle);
            ARBVertexArrayObject.glBindVertexArray(0);
            useVao = true;
        } catch (Exception | Error e1) {
            try {
                vaoHandle = APPLEVertexArrayObject.glGenVertexArraysAPPLE();
                APPLEVertexArrayObject.glBindVertexArrayAPPLE(vaoHandle);
                APPLEVertexArrayObject.glBindVertexArrayAPPLE(0);
                useVao = true;
            } catch (Exception | Error e2) {
                try {
                    vaoHandle = GL30.glGenVertexArrays();
                    GL30.glBindVertexArray(vaoHandle);
                    GL30.glBindVertexArray(0);
                    useVao = true;
                } catch (Exception | Error e3) {
                    LOG.warn("ImGui: VAO unavailable ({}), rendering may fail on Core Profile", e3.getMessage());
                }
            }
        }
    }

    private void bindVao() {
        if (useVao) bindVaoId(vaoHandle);
    }

    private void unbindVao() {
        if (useVao) bindVaoId(0);
    }

    private static void bindVaoId(int id) {
        try {
            ARBVertexArrayObject.glBindVertexArray(id);
            return;
        } catch (Exception | Error ignored) {}
        try {
            APPLEVertexArrayObject.glBindVertexArrayAPPLE(id);
            return;
        } catch (Exception | Error ignored) {}
        try {
            GL30.glBindVertexArray(id);
        } catch (Exception | Error ignored) {}
    }

    public void rebuildFontTexture() {
        if (fontTexture != 0) {
            GL11.glDeleteTextures(fontTexture);
            fontTexture = 0;
        }
        createFontsTexture();
    }

    private void createFontsTexture() {
        ImFontAtlas atlas = ImGui.getIO()
            .getFonts();
        ImInt w = new ImInt(), h = new ImInt();
        ByteBuffer pixels = atlas.getTexDataAsRGBA32(w, h);

        // GL_UNPACK_ROW_LENGTH is client state — not saved/restored by glPushAttrib.
        // MC's texture uploads can leave it non-zero, corrupting the font atlas upload.
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        fontTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA,
            w.get(),
            h.get(),
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            pixels);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        atlas.setTexID(fontTexture);
    }

    private final FloatBuffer projBuf = BufferUtils.createFloatBuffer(16);
    // imgui-java reuses ONE native ByteBuffer object for all getCmdList*BufferData calls.
    // getCmdListIdxBufferData(n) overwrites the same Java object returned by getCmdListVtxBufferData(n).
    // We copy vtx data into a private scratch buffer before fetching the idx buffer.
    private ByteBuffer vtxScratch = BufferUtils.createByteBuffer(1024);

    public void renderDrawData(ImDrawData drawData) {
        if (drawData == null || !drawData.getValid() || drawData.getCmdListsCount() <= 0) return;

        float dispX = drawData.getDisplayPosX();
        float dispY = drawData.getDisplayPosY();
        float dispW = drawData.getDisplaySizeX();
        float dispH = drawData.getDisplaySizeY();
        float scaleX = drawData.getFramebufferScaleX();
        float scaleY = drawData.getFramebufferScaleY();

        int fbW = (int) (dispW * scaleX);
        int fbH = (int) (dispH * scaleY);
        if (fbW <= 0 || fbH <= 0) return;

        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL14.glBlendFuncSeparate(
            GL11.GL_SRC_ALPHA,
            GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE,
            GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST); // MC may leave alpha-test on; it applies post-shader in compat profile
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D); // GL 2.1 compat: sampler2D still respects the texture-unit enable bit
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        GL11.glColorMask(true, true, true, true);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        GL11.glViewport(0, 0, fbW, fbH);

        float r = dispX + dispW;
        float b = dispY + dispH;
        projBuf.clear();
        projBuf.put(
            new float[] { 2f / (r - dispX), 0, 0, 0, 0, 2f / (dispY - b), 0, 0, 0, 0, -1, 0, (r + dispX) / (dispX - r),
                (dispY + b) / (b - dispY), 0, 1 });
        projBuf.flip();

        GL20.glUseProgram(program);
        GL20.glUniform1i(uniformTex, 0);
        GL20.glUniformMatrix4(uniformProjMtx, false, projBuf);

        bindVao();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboHandle);
        GL20.glEnableVertexAttribArray(attribPos);
        GL20.glEnableVertexAttribArray(attribUV);
        GL20.glEnableVertexAttribArray(attribColor);

        int vtxSize = ImDrawData.sizeOfImDrawVert();
        int idxSize = ImDrawData.sizeOfImDrawIdx();
        int idxType = (idxSize == 2) ? GL11.GL_UNSIGNED_SHORT : GL11.GL_UNSIGNED_INT;

        for (int n = 0; n < drawData.getCmdListsCount(); n++) {
            // imgui-java reuses a single JNI ByteBuffer: getCmdListIdxBufferData clobbers
            // the reference returned by getCmdListVtxBufferData. Copy vtx into scratch first.
            ByteBuffer rawVtx = drawData.getCmdListVtxBufferData(n);
            rawVtx.rewind();
            int vtxBytes = rawVtx.limit();
            if (vtxScratch.capacity() < vtxBytes) {
                vtxScratch = BufferUtils.createByteBuffer(vtxBytes * 2);
            }
            vtxScratch.clear();
            vtxScratch.put(rawVtx);
            vtxScratch.flip();
            ByteBuffer vtxBuf = vtxScratch;

            ByteBuffer idxBuf = drawData.getCmdListIdxBufferData(n);
            idxBuf.rewind();

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboHandle);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vtxBuf, GL15.GL_STREAM_DRAW);

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboHandle);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, idxBuf, GL15.GL_STREAM_DRAW);

            GL20.glVertexAttribPointer(attribPos, 2, GL11.GL_FLOAT, false, vtxSize, 0L);
            GL20.glVertexAttribPointer(attribUV, 2, GL11.GL_FLOAT, false, vtxSize, 8L);
            GL20.glVertexAttribPointer(attribColor, 4, GL11.GL_UNSIGNED_BYTE, true, vtxSize, 16L);

            for (int cmd = 0; cmd < drawData.getCmdListCmdBufferSize(n); cmd++) {
                int elemCount = drawData.getCmdListCmdBufferElemCount(n, cmd);
                if (elemCount == 0) continue;

                ImVec4 cr = drawData.getCmdListCmdBufferClipRect(n, cmd);
                float cx = (cr.x - dispX) * scaleX;
                float cy = (cr.y - dispY) * scaleY;
                float cz = (cr.z - dispX) * scaleX;
                float cw = (cr.w - dispY) * scaleY;
                if (cx >= fbW || cy >= fbH || cz < 0 || cw < 0) continue;

                GL11.glScissor((int) cx, (int) (fbH - cw), (int) (cz - cx), (int) (cw - cy));

                long texId = drawData.getCmdListCmdBufferTextureId(n, cmd);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, (int) texId);

                int idxOffset = drawData.getCmdListCmdBufferIdxOffset(n, cmd);
                int vtxOffset = drawData.getCmdListCmdBufferVtxOffset(n, cmd);

                if (vtxOffset != 0) {
                    long base = (long) vtxOffset * vtxSize;
                    GL20.glVertexAttribPointer(attribPos, 2, GL11.GL_FLOAT, false, vtxSize, base);
                    GL20.glVertexAttribPointer(attribUV, 2, GL11.GL_FLOAT, false, vtxSize, base + 8);
                    GL20.glVertexAttribPointer(attribColor, 4, GL11.GL_UNSIGNED_BYTE, true, vtxSize, base + 16);
                }

                GL11.glDrawElements(GL11.GL_TRIANGLES, elemCount, idxType, (long) idxOffset * idxSize);

                if (vtxOffset != 0) {
                    GL20.glVertexAttribPointer(attribPos, 2, GL11.GL_FLOAT, false, vtxSize, 0L);
                    GL20.glVertexAttribPointer(attribUV, 2, GL11.GL_FLOAT, false, vtxSize, 8L);
                    GL20.glVertexAttribPointer(attribColor, 4, GL11.GL_UNSIGNED_BYTE, true, vtxSize, 16L);
                }
            }
        }

        GL20.glDisableVertexAttribArray(attribPos);
        GL20.glDisableVertexAttribArray(attribUV);
        GL20.glDisableVertexAttribArray(attribColor);
        unbindVao();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL20.glUseProgram(0);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

}
