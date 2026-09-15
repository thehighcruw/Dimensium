/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.shared.BlockColorCache;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.SelectionState.BlockData;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.PerfTrace;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import github.thehighcruw.dimensium.tool.BuilderTool;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import java.util.HashSet;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
class HologramRenderer {

    private static final int PER_BLOCK_MAX = 2048;

    private int cachedClipVersion = -1;
    private float[] cachedClipWire = null;

    void render(Minecraft mc, SelectionState sel, BuilderToolState bts, Vec3DDouble camPos) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 400.0);
        boolean isMove = bts.activeTool == BuilderTool.MOVE;
        boolean isStack = bts.activeTool == BuilderTool.STACK;
        boolean isSmear = bts.activeTool == BuilderTool.SMEAR;

        int volume = sel.clipDim.x() * sel.clipDim.y() * sel.clipDim.z();
        boolean perBlock = volume <= PER_BLOCK_MAX;

        if (isMove) {
            PerfTrace.push("renderSourceDim");
            renderSourceDim(sel, camPos, pulse);
            PerfTrace.pop();
        }

        if (isStack) {
            int w = sel.width(), h = sel.height(), d = sel.depth();
            int x0 = Math.min(bts.stack.x(), 0), x1 = Math.max(bts.stack.x(), 0);
            int y0 = Math.min(bts.stack.y(), 0), y1 = Math.max(bts.stack.y(), 0);
            int z0 = Math.min(bts.stack.z(), 0), z1 = Math.max(bts.stack.z(), 0);
            int total = (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1) - 1;
            int copyIdx = 0;
            PerfTrace.push("renderStack copies=" + total);
            for (int ix = x0; ix <= x1; ix++) {
                for (int iy = y0; iy <= y1; iy++) {
                    for (int iz = z0; iz <= z1; iz++) {
                        if (ix == 0 && iy == 0 && iz == 0) continue;
                        renderDestination(mc, sel, ix * w, iy * h, iz * d, camPos, pulse, perBlock, ++copyIdx, total);
                    }
                }
            }
            PerfTrace.pop();
        } else if (isSmear) {
            PerfTrace.push("renderSmearVolume");
            renderSmearVolume(sel, bts, camPos, pulse);
            PerfTrace.pop();
        } else {
            PerfTrace.push("renderDestination perBlock=" + perBlock + " vol=" + volume);
            renderDestination(mc, sel, bts.offset.x(), bts.offset.y(), bts.offset.z(), camPos, pulse, perBlock, 1, 1);
            PerfTrace.pop();
        }

        if (bts.axisLock != BuilderToolState.AxisLock.NONE && !isStack) {
            drawAxisLine(sel, bts, camPos);
        }
    }

    private void renderSourceDim(SelectionState sel, Vec3DDouble camPos, float pulse) {
        GL11.glPushMatrix();
        GL11.glTranslated(sel.minX() - camPos.x(), sel.minY() - camPos.y(), sel.minZ() - camPos.z());
        GL11.glColor4f(1.0f, 0.1f, 0.1f, 0.12f + pulse * 0.06f);
        SelectionRenderer.drawFilledBox(sel.width(), sel.height(), sel.depth());
        GL11.glColor4f(1.0f, 0.2f, 0.2f, 0.7f);
        GL11.glLineWidth(1.5f);
        SelectionRenderer.drawBox(0, 0, 0, sel.width(), sel.height(), sel.depth());
        GL11.glPopMatrix();
    }

    private void renderSmearVolume(SelectionState sel, BuilderToolState bts, Vec3DDouble camPos, float pulse) {
        int dx = bts.offset.x(), dy = bts.offset.y(), dz = bts.offset.z();

        int sweptMinX = sel.minX() + Math.min(0, dx);
        int sweptMinY = sel.minY() + Math.min(0, dy);
        int sweptMinZ = sel.minZ() + Math.min(0, dz);
        int sweptW = sel.width() + Math.abs(dx);
        int sweptH = sel.height() + Math.abs(dy);
        int sweptD = sel.depth() + Math.abs(dz);

        GL11.glPushMatrix();
        GL11.glTranslated(sweptMinX - camPos.x(), sweptMinY - camPos.y(), sweptMinZ - camPos.z());

        GL11.glColor4f(0.0f, 0.8f, 0.9f, 0.08f + pulse * 0.04f);
        SelectionRenderer.drawFilledBox(sweptW, sweptH, sweptD);

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.6f);
        GL11.glLineWidth(1.5f);
        SelectionRenderer.drawBox(
                sel.minX() - sweptMinX,
                sel.minY() - sweptMinY,
                sel.minZ() - sweptMinZ,
                sel.width(),
                sel.height(),
                sel.depth());

        GL11.glColor4f(0.0f, 0.9f, 1.0f, 0.5f + pulse * 0.3f);
        GL11.glLineWidth(2.0f);
        SelectionRenderer.drawBox(
                sel.minX() + dx - sweptMinX,
                sel.minY() + dy - sweptMinY,
                sel.minZ() + dz - sweptMinZ,
                sel.width(),
                sel.height(),
                sel.depth());

        GL11.glPopMatrix();
    }

    private void renderDestination(
            Minecraft mc,
            SelectionState sel,
            int ox,
            int oy,
            int oz,
            Vec3DDouble camPos,
            float pulse,
            boolean perBlock,
            int copyIndex,
            int totalCopies) {
        double hx = sel.minX() + ox - camPos.x();
        double hy = sel.minY() + oy - camPos.y();
        double hz = sel.minZ() + oz - camPos.z();
        int w = sel.clipDim.x(), h = sel.clipDim.y(), d = sel.clipDim.z();

        if (perBlock && sel.clipboard != null) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glFrontFace(GL11.GL_CW);
            // Fully opaque textured pass; additive glow below provides the animation.
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            GL11.glPushMatrix();
            GL11.glTranslated(hx, hy, hz);

            Tessellator t = Tessellator.instance;
            PerfTrace.push("texturedPass w=" + w + " h=" + h + " d=" + d);
            t.startDrawingQuads();
            int batched = 0;
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    for (int z = 0; z < d; z++) {
                        BlockData bd = sel.clipboardGet(Vec3DInt.from(x, y, z));
                        if (bd.block() == Blocks.air || bd.block().getRenderType() != 0) continue;
                        for (int face = 0; face < 6; face++) {
                            if (isFacingAir(sel, face, x, y, z, w, h, d)) {
                                GhostRenderer.addTexturedFace(t, Vec3DInt.from(x, y, z), bd.block(), bd.meta(), face);
                                if (++batched % 2048 == 0) {
                                    t.draw();
                                    t.startDrawingQuads();
                                }
                            }
                        }
                    }
                }
            }
            t.draw();
            PerfTrace.pop();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            PerfTrace.push("colorPass");
            t.startDrawingQuads();
            batched = 0;
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    for (int z = 0; z < d; z++) {
                        BlockData bd = sel.clipboardGet(Vec3DInt.from(x, y, z));
                        if (bd.block() == Blocks.air || bd.block().getRenderType() == 0) continue;
                        int blockId = Block.getIdFromBlock(bd.block());
                        int rgb = BlockColorCache.INSTANCE.blockColor(blockId, bd.meta());
                        if (rgb < 0) rgb = 0x888888;
                        float r = ((rgb >> 16) & 0xFF) / 255f;
                        float g = ((rgb >> 8) & 0xFF) / 255f;
                        float b = (rgb & 0xFF) / 255f;
                        GL11.glColor4f(r, g, b, 1.0f);
                        for (int face = 0; face < 6; face++) {
                            if (isFacingAir(sel, face, x, y, z, w, h, d)) {
                                GhostRenderer.addSingleFace(t, Vec3DInt.from(x, y, z), face);
                                if (++batched % 2048 == 0) {
                                    t.draw();
                                    t.startDrawingQuads();
                                }
                            }
                        }
                    }
                }
            }
            t.draw();
            PerfTrace.pop();

            // Glow — slightly more negative offset so no z-fighting with opaque pass.
            // glDepthMask(false): glow quads never occlude each other at crease edges.
            GL11.glPolygonOffset(-2.0f, -2.0f);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glDepthMask(false);
            GL11.glColor4f(0.20f, 1.0f, 0.45f, 0.05f + 0.07f * pulse);
            PerfTrace.push("glowPass");
            t.startDrawingQuads();
            batched = 0;
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    for (int z = 0; z < d; z++) {
                        BlockData bd = sel.clipboardGet(Vec3DInt.from(x, y, z));
                        if (bd.block() == Blocks.air) continue;
                        for (int face = 0; face < 6; face++) {
                            int nx = x + GhostRenderer.NX[face];
                            int ny = y + GhostRenderer.NY[face];
                            int nz = z + GhostRenderer.NZ[face];
                            boolean neighborOccupied = nx >= 0
                                    && nx < w
                                    && ny >= 0
                                    && ny < h
                                    && nz >= 0
                                    && nz < d
                                    && sel.clipboardGet(Vec3DInt.from(nx, ny, nz))
                                                    .block()
                                            != Blocks.air;
                            if (!neighborOccupied) {
                                GhostRenderer.addSingleFace(t, Vec3DInt.from(x, y, z), face, 0.02f);
                                if (++batched % 2048 == 0) {
                                    t.draw();
                                    t.startDrawingQuads();
                                }
                            }
                        }
                    }
                }
            }
            t.draw();
            PerfTrace.pop();
            RenderUtils.unsetGhostRendering();

            float alpha = 0.9f - (float) (copyIndex - 1) / Math.max(1, totalCopies) * 0.4f;
            GL11.glColor4f(0.2f, 1.0f, 0.4f, alpha);
            GL11.glLineWidth(2.0f);
            ensureClipWireframeCache(sel);
            GhostRenderer.drawWireframeCache(t, cachedClipWire);

            GL11.glPopMatrix();
        } else {
            GL11.glPushMatrix();
            GL11.glTranslated(hx, hy, hz);
            GL11.glColor4f(0.2f, 1.0f, 0.4f, 0.08f + pulse * 0.04f);
            SelectionRenderer.drawFilledBox(w, h, d);
            GL11.glColor4f(0.2f, 1.0f, 0.4f, 0.9f - (float) (copyIndex - 1) / Math.max(1, totalCopies) * 0.4f);
            GL11.glLineWidth(2.0f);
            SelectionRenderer.drawBox(0, 0, 0, w, h, d);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, pulse * 0.3f);
            GL11.glLineWidth(1.0f);
            SelectionRenderer.drawBox(-0.02f, -0.02f, -0.02f, w + 0.02f, h + 0.02f, d + 0.02f);
            GL11.glPopMatrix();
        }
    }

    private static boolean isFacingAir(SelectionState sel, int face, int x, int y, int z, int w, int h, int d) {
        int nx = x + GhostRenderer.NX[face], ny = y + GhostRenderer.NY[face], nz = z + GhostRenderer.NZ[face];
        return nx < 0
                || nx >= w
                || ny < 0
                || ny >= h
                || nz < 0
                || nz >= d
                || sel.clipboardGet(Vec3DInt.from(nx, ny, nz)).block() == Blocks.air;
    }

    private void ensureClipWireframeCache(SelectionState sel) {
        if (sel.clipboardVersion == cachedClipVersion && cachedClipWire != null) return;
        cachedClipWire = computeClipWireframe(sel);
        cachedClipVersion = sel.clipboardVersion;
    }

    private static float[] computeClipWireframe(SelectionState sel) {
        if (sel.clipboard == null) return new float[0];
        int w = sel.clipDim.x(), h = sel.clipDim.y(), d = sel.clipDim.z();

        HashSet<Long> set = new HashSet<>(w * h * d);
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                for (int z = 0; z < d; z++)
                    if (sel.clipboardGet(Vec3DInt.from(x, y, z)).block() != Blocks.air)
                        set.add(SelectionRenderer.lPack(x, y, z));

        return GhostRenderer.creaseWireframeFromSet(set);
    }

    private static void drawAxisLine(SelectionState sel, BuilderToolState bts, Vec3DDouble camPos) {
        int w = sel.clipDim.x(), h = sel.clipDim.y(), d = sel.clipDim.z();
        Vec3DDouble src = Vec3DDouble.from(sel.minX() + w / 2.0, sel.minY() + h / 2.0, sel.minZ() + d / 2.0)
                .minus(camPos);
        Vec3DDouble dst = src.plus(bts.offset.toDouble());

        float lr = bts.axisLock == BuilderToolState.AxisLock.X ? 1.0f : 0.3f;
        float lg = bts.axisLock == BuilderToolState.AxisLock.Y ? 1.0f : 0.3f;
        float lb = bts.axisLock == BuilderToolState.AxisLock.Z ? 1.0f : 0.3f;

        Tessellator t = Tessellator.instance;
        GL11.glColor4f(lr, lg, lb, 0.8f);
        WorldLines.setEye(Vec3DDouble.ZERO); // vertices already camera-relative
        t.startDrawingQuads();
        WorldLines.addSegment(t, src, dst, WorldLines.W_SEL);
        t.draw();
    }
}
