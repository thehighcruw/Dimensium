/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.shared.BlockColorCache;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.SelectionState.BlockData;
import github.thehighcruw.dimensium.shared.util.PerfTrace;
import github.thehighcruw.dimensium.tool.BuilderTool;
import github.thehighcruw.dimensium.tool.BuilderToolState;

@SideOnly(Side.CLIENT)
class HologramRenderer {

    private static final int PER_BLOCK_MAX = 2048;

    private int cachedClipVersion = -1;
    private float[] cachedClipWire = null;

    void render(Minecraft mc, SelectionState sel, BuilderToolState bts, double rx, double ry, double rz) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 400.0);
        boolean isMove = bts.activeTool == BuilderTool.MOVE;
        boolean isStack = bts.activeTool == BuilderTool.STACK;
        boolean isSmear = bts.activeTool == BuilderTool.SMEAR;

        int volume = sel.clipW * sel.clipH * sel.clipD;
        boolean perBlock = volume <= PER_BLOCK_MAX;

        if (isMove) {
            PerfTrace.push("renderSourceDim");
            renderSourceDim(sel, rx, ry, rz, pulse);
            PerfTrace.pop();
        }

        if (isStack) {
            int w = sel.width(), h = sel.height(), d = sel.depth();
            int x0 = Math.min(bts.stackX, 0), x1 = Math.max(bts.stackX, 0);
            int y0 = Math.min(bts.stackY, 0), y1 = Math.max(bts.stackY, 0);
            int z0 = Math.min(bts.stackZ, 0), z1 = Math.max(bts.stackZ, 0);
            int total = (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1) - 1;
            int copyIdx = 0;
            PerfTrace.push("renderStack copies=" + total);
            for (int ix = x0; ix <= x1; ix++) {
                for (int iy = y0; iy <= y1; iy++) {
                    for (int iz = z0; iz <= z1; iz++) {
                        if (ix == 0 && iy == 0 && iz == 0) continue;
                        renderDestination(
                            mc,
                            sel,
                            ix * w,
                            iy * h,
                            iz * d,
                            rx,
                            ry,
                            rz,
                            pulse,
                            perBlock,
                            ++copyIdx,
                            total);
                    }
                }
            }
            PerfTrace.pop();
        } else if (isSmear) {
            PerfTrace.push("renderSmearVolume");
            renderSmearVolume(sel, bts, rx, ry, rz, pulse);
            PerfTrace.pop();
        } else {
            PerfTrace.push("renderDestination perBlock=" + perBlock + " vol=" + volume);
            renderDestination(mc, sel, bts.offsetX, bts.offsetY, bts.offsetZ, rx, ry, rz, pulse, perBlock, 1, 1);
            PerfTrace.pop();
        }

        if (bts.axisLock != BuilderToolState.AxisLock.NONE && !isStack) {
            drawAxisLine(sel, bts, rx, ry, rz);
        }
    }

    private void renderSourceDim(SelectionState sel, double rx, double ry, double rz, float pulse) {
        GL11.glPushMatrix();
        GL11.glTranslated(sel.minX() - rx, sel.minY() - ry, sel.minZ() - rz);
        GL11.glColor4f(1.0f, 0.1f, 0.1f, 0.12f + pulse * 0.06f);
        SelectionRenderer.drawFilledBox(0, 0, 0, sel.width(), sel.height(), sel.depth());
        GL11.glColor4f(1.0f, 0.2f, 0.2f, 0.7f);
        GL11.glLineWidth(1.5f);
        SelectionRenderer.drawBox(0, 0, 0, sel.width(), sel.height(), sel.depth());
        GL11.glPopMatrix();
    }

    private void renderSmearVolume(SelectionState sel, BuilderToolState bts, double rx, double ry, double rz,
        float pulse) {
        int dx = bts.offsetX, dy = bts.offsetY, dz = bts.offsetZ;

        int sweptMinX = sel.minX() + Math.min(0, dx);
        int sweptMinY = sel.minY() + Math.min(0, dy);
        int sweptMinZ = sel.minZ() + Math.min(0, dz);
        int sweptW = sel.width() + Math.abs(dx);
        int sweptH = sel.height() + Math.abs(dy);
        int sweptD = sel.depth() + Math.abs(dz);

        GL11.glPushMatrix();
        GL11.glTranslated(sweptMinX - rx, sweptMinY - ry, sweptMinZ - rz);

        GL11.glColor4f(0.0f, 0.8f, 0.9f, 0.08f + pulse * 0.04f);
        SelectionRenderer.drawFilledBox(0, 0, 0, sweptW, sweptH, sweptD);

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

    private void renderDestination(Minecraft mc, SelectionState sel, int ox, int oy, int oz, double rx, double ry,
        double rz, float pulse, boolean perBlock, int copyIndex, int totalCopies) {
        double hx = sel.minX() + ox - rx;
        double hy = sel.minY() + oy - ry;
        double hz = sel.minZ() + oz - rz;
        int w = sel.clipW, h = sel.clipH, d = sel.clipD;

        if (perBlock && sel.clipboard != null) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            mc.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
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
                        BlockData bd = sel.clipboardGet(x, y, z);
                        if (bd.block == Blocks.air || bd.block.getRenderType() != 0) continue;
                        for (int face = 0; face < 6; face++) {
                            int nx = x + GhostRenderer.NX[face], ny = y + GhostRenderer.NY[face],
                                nz = z + GhostRenderer.NZ[face];
                            boolean occ = nx >= 0 && nx < w
                                && ny >= 0
                                && ny < h
                                && nz >= 0
                                && nz < d
                                && sel.clipboardGet(nx, ny, nz).block != Blocks.air;
                            if (!occ) {
                                GhostRenderer.addTexturedFace(t, x, y, z, bd.block, bd.meta, face);
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
                        BlockData bd = sel.clipboardGet(x, y, z);
                        if (bd.block == Blocks.air || bd.block.getRenderType() == 0) continue;
                        int blockId = Block.getIdFromBlock(bd.block);
                        int rgb = BlockColorCache.INSTANCE.blockColor(blockId, bd.meta);
                        if (rgb < 0) rgb = 0x888888;
                        float r = ((rgb >> 16) & 0xFF) / 255f;
                        float g = ((rgb >> 8) & 0xFF) / 255f;
                        float b = (rgb & 0xFF) / 255f;
                        GL11.glColor4f(r, g, b, 1.0f);
                        for (int face = 0; face < 6; face++) {
                            int nx = x + GhostRenderer.NX[face], ny = y + GhostRenderer.NY[face],
                                nz = z + GhostRenderer.NZ[face];
                            boolean occ = nx >= 0 && nx < w
                                && ny >= 0
                                && ny < h
                                && nz >= 0
                                && nz < d
                                && sel.clipboardGet(nx, ny, nz).block != Blocks.air;
                            if (!occ) {
                                GhostRenderer.addSingleFace(t, x, y, z, face);
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
                        BlockData bd = sel.clipboardGet(x, y, z);
                        if (bd.block == Blocks.air) continue;
                        for (int face = 0; face < 6; face++) {
                            int nx = x + GhostRenderer.NX[face];
                            int ny = y + GhostRenderer.NY[face];
                            int nz = z + GhostRenderer.NZ[face];
                            boolean neighborOccupied = nx >= 0 && nx < w
                                && ny >= 0
                                && ny < h
                                && nz >= 0
                                && nz < d
                                && sel.clipboardGet(nx, ny, nz).block != Blocks.air;
                            if (!neighborOccupied) {
                                GhostRenderer.addSingleFace(t, x, y, z, face, 0.02f);
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
            GL11.glDepthMask(true);
            GL11.glDepthFunc(GL11.GL_LESS);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(0.0f, 0.0f);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);

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
            SelectionRenderer.drawFilledBox(0, 0, 0, w, h, d);
            GL11.glColor4f(0.2f, 1.0f, 0.4f, 0.9f - (float) (copyIndex - 1) / Math.max(1, totalCopies) * 0.4f);
            GL11.glLineWidth(2.0f);
            SelectionRenderer.drawBox(0, 0, 0, w, h, d);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, pulse * 0.3f);
            GL11.glLineWidth(1.0f);
            SelectionRenderer.drawBox(-0.02f, -0.02f, -0.02f, w + 0.02f, h + 0.02f, d + 0.02f);
            GL11.glPopMatrix();
        }
    }

    private void ensureClipWireframeCache(SelectionState sel) {
        if (sel.clipboardVersion == cachedClipVersion && cachedClipWire != null) return;
        cachedClipWire = computeClipWireframe(sel);
        cachedClipVersion = sel.clipboardVersion;
    }

    private static float[] computeClipWireframe(SelectionState sel) {
        if (sel.clipboard == null) return new float[0];
        int w = sel.clipW, h = sel.clipH, d = sel.clipD;

        java.util.HashSet<Long> set = new java.util.HashSet<>(w * h * d);
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) for (int z = 0; z < d; z++)
            if (sel.clipboardGet(x, y, z).block != Blocks.air) set.add(SelectionRenderer.lPack(x, y, z));

        return GhostRenderer.creaseWireframeFromSet(set);
    }

    private static void drawAxisLine(SelectionState sel, BuilderToolState bts, double rx, double ry, double rz) {
        int w = sel.clipW, h = sel.clipH, d = sel.clipD;
        double srcCx = sel.minX() + w / 2.0 - rx;
        double srcCy = sel.minY() + h / 2.0 - ry;
        double srcCz = sel.minZ() + d / 2.0 - rz;
        double dstCx = srcCx + bts.offsetX;
        double dstCy = srcCy + bts.offsetY;
        double dstCz = srcCz + bts.offsetZ;

        float lr = bts.axisLock == BuilderToolState.AxisLock.X ? 1.0f : 0.3f;
        float lg = bts.axisLock == BuilderToolState.AxisLock.Y ? 1.0f : 0.3f;
        float lb = bts.axisLock == BuilderToolState.AxisLock.Z ? 1.0f : 0.3f;

        Tessellator t = Tessellator.instance;
        GL11.glColor4f(lr, lg, lb, 0.8f);
        WorldLines.setEye(0, 0, 0); // vertices already camera-relative
        t.startDrawingQuads();
        WorldLines.addSegment(t, srcCx, srcCy, srcCz, dstCx, dstCy, dstCz, WorldLines.W_SEL);
        t.draw();
    }
}
