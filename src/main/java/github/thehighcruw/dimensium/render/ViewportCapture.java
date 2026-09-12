package github.thehighcruw.dimensium.render;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ViewportCapture {

    private ViewportCapture() {}

    /**
     * Copy the current GL read framebuffer into the given viewport's texture.
     * Must be called before ImGui renders anything this frame.
     * The texture has GL origin (bottom-left), so callers must flip V UVs when displaying.
     * Only the active viewport is captured each frame — inactive viewports keep their last frame.
     */
    public static void capture(ViewportState vp, int w, int h) {
        if (vp.texId == -1) {
            vp.texId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, vp.texId);
            // NEAREST: the UV maps 1:1 texel-to-pixel; LINEAR would blur at sub-texel UV boundaries.
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, vp.texId);
        }

        if (w != vp.texW || h != vp.texH) {
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGB,
                w,
                h,
                0,
                GL11.GL_RGB,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null);
            vp.texW = w;
            vp.texH = h;
        }

        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
}
