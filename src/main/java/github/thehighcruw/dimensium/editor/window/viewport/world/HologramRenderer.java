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

        int volume = sel.clipDim.product();
        boolean perBlock = volume <= PER_BLOCK_MAX;

        if (isMove) {
            PerfTrace.push("renderSourceDim");
            renderSourceDim(sel, camPos, pulse);
            PerfTrace.pop();
        }

        if (isStack) {
            int w = sel.width(), h = sel.height(), d = sel.depth();
            Vec3DInt s0 = bts.stack.min(Vec3DInt.ZERO);
            Vec3DInt s1 = bts.stack.max(Vec3DInt.ZERO);
            int total = s1.minus(s0).plus(1).product() - 1;
            int[] copyIdx = {0};
            PerfTrace.push("renderStack copies=" + total);
            Vec3DInt.forEachInclusive(s0, s1, (ix, iy, iz) -> {
                if (ix == 0 && iy == 0 && iz == 0) return;
                renderDestination(
                        mc, sel, Vec3DInt.from(ix * w, iy * h, iz * d), camPos, pulse, perBlock, ++copyIdx[0], total);
            });
            PerfTrace.pop();
        } else if (isSmear) {
            PerfTrace.push("renderSmearVolume");
            renderSmearVolume(sel, bts, camPos, pulse);
            PerfTrace.pop();
        } else {
            PerfTrace.push("renderDestination perBlock=" + perBlock + " vol=" + volume);
            renderDestination(mc, sel, bts.offset, camPos, pulse, perBlock, 1, 1);
            PerfTrace.pop();
        }

        if (bts.axisLock != BuilderToolState.AxisLock.NONE && !isStack) {
            drawAxisLine(sel, bts, camPos);
        }
    }

    private void renderSourceDim(SelectionState sel, Vec3DDouble camPos, float pulse) {
        GL11.glPushMatrix();
        Vec3DDouble selTrans = sel.min().toDouble().minus(camPos);
        GL11.glTranslated(selTrans.x(), selTrans.y(), selTrans.z());
        GL11.glColor4f(1.0f, 0.1f, 0.1f, 0.12f + pulse * 0.06f);
        SelectionRenderer.drawFilledBox(sel.width(), sel.height(), sel.depth());
        GL11.glColor4f(1.0f, 0.2f, 0.2f, 0.7f);
        GL11.glLineWidth(1.5f);
        SelectionRenderer.drawBox(0, 0, 0, sel.width(), sel.height(), sel.depth());
        GL11.glPopMatrix();
    }

    private void renderSmearVolume(SelectionState sel, BuilderToolState bts, Vec3DDouble camPos, float pulse) {
        Vec3DInt offset = bts.offset;
        Vec3DInt selMin = sel.min();
        Vec3DInt sweptMin = selMin.plus(offset.min(Vec3DInt.ZERO));
        Vec3DInt sweptDims =
                Vec3DInt.from(sel.width(), sel.height(), sel.depth()).plus(offset.abs());
        Vec3DInt selOff = selMin.minus(sweptMin);
        Vec3DInt destOff = selOff.plus(offset);

        GL11.glPushMatrix();
        GL11.glTranslated(sweptMin.x() - camPos.x(), sweptMin.y() - camPos.y(), sweptMin.z() - camPos.z());

        GL11.glColor4f(0.0f, 0.8f, 0.9f, 0.08f + pulse * 0.04f);
        SelectionRenderer.drawFilledBox(sweptDims.x(), sweptDims.y(), sweptDims.z());

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.6f);
        GL11.glLineWidth(1.5f);
        SelectionRenderer.drawBox(selOff.x(), selOff.y(), selOff.z(), sel.width(), sel.height(), sel.depth());

        GL11.glColor4f(0.0f, 0.9f, 1.0f, 0.5f + pulse * 0.3f);
        GL11.glLineWidth(2.0f);
        SelectionRenderer.drawBox(destOff.x(), destOff.y(), destOff.z(), sel.width(), sel.height(), sel.depth());

        GL11.glPopMatrix();
    }

    private void renderDestination(
            Minecraft mc,
            SelectionState sel,
            Vec3DInt offset,
            Vec3DDouble camPos,
            float pulse,
            boolean perBlock,
            int copyIndex,
            int totalCopies) {
        Vec3DDouble hPos = sel.min().toDouble().plus(offset.toDouble()).minus(camPos);
        Vec3DInt clipDims = sel.clipDim;

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
            GL11.glTranslated(hPos.x(), hPos.y(), hPos.z());

            Tessellator t = Tessellator.instance;
            PerfTrace.push("texturedPass " + clipDims);
            t.startDrawingQuads();
            int[] batched = {0};
            Vec3DInt.forEachInclusive(Vec3DInt.ZERO, clipDims.minus(1), pos -> {
                BlockData bd = sel.clipboardGet(pos);
                if (bd.block() == Blocks.air || bd.block().getRenderType() != 0) return;
                for (int face = 0; face < 6; face++) {
                    if (isFacingAir(sel, face, pos, clipDims)) {
                        GhostRenderer.addTexturedFace(t, pos, bd.block(), bd.meta(), face);
                        if (++batched[0] % 2048 == 0) {
                            t.draw();
                            t.startDrawingQuads();
                        }
                    }
                }
            });
            t.draw();
            PerfTrace.pop();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            PerfTrace.push("colorPass");
            t.startDrawingQuads();
            batched[0] = 0;
            Vec3DInt.forEachInclusive(Vec3DInt.ZERO, clipDims.minus(1), pos -> {
                BlockData bd = sel.clipboardGet(pos);
                if (bd.block() == Blocks.air || bd.block().getRenderType() == 0) return;
                int blockId = Block.getIdFromBlock(bd.block());
                int rgb = BlockColorCache.INSTANCE.blockColor(blockId, bd.meta());
                if (rgb < 0) rgb = 0x888888;
                GL11.glColor4f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f, 1.0f);
                for (int face = 0; face < 6; face++) {
                    if (isFacingAir(sel, face, pos, clipDims)) {
                        GhostRenderer.addSingleFace(t, pos, face);
                        if (++batched[0] % 2048 == 0) {
                            t.draw();
                            t.startDrawingQuads();
                        }
                    }
                }
            });
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
            batched[0] = 0;
            Vec3DInt.forEachInclusive(Vec3DInt.ZERO, clipDims.minus(1), pos -> {
                BlockData bd = sel.clipboardGet(pos);
                if (bd.block() == Blocks.air) return;
                for (int face = 0; face < 6; face++) {
                    Vec3DInt neighbor =
                            pos.plus(GhostRenderer.NX[face], GhostRenderer.NY[face], GhostRenderer.NZ[face]);
                    boolean neighborOccupied = neighbor.inBounds(Vec3DInt.ZERO, clipDims.minus(1))
                            && sel.clipboardGet(neighbor).block() != Blocks.air;
                    if (!neighborOccupied) {
                        GhostRenderer.addSingleFace(t, pos, face, 0.02f);
                        if (++batched[0] % 2048 == 0) {
                            t.draw();
                            t.startDrawingQuads();
                        }
                    }
                }
            });
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
            GL11.glTranslated(hPos.x(), hPos.y(), hPos.z());
            GL11.glColor4f(0.2f, 1.0f, 0.4f, 0.08f + pulse * 0.04f);
            SelectionRenderer.drawFilledBox(clipDims.x(), clipDims.y(), clipDims.z());
            GL11.glColor4f(0.2f, 1.0f, 0.4f, 0.9f - (float) (copyIndex - 1) / Math.max(1, totalCopies) * 0.4f);
            GL11.glLineWidth(2.0f);
            SelectionRenderer.drawBox(0, 0, 0, clipDims.x(), clipDims.y(), clipDims.z());
            GL11.glColor4f(1.0f, 1.0f, 1.0f, pulse * 0.3f);
            GL11.glLineWidth(1.0f);
            SelectionRenderer.drawBox(
                    -0.02f, -0.02f, -0.02f, clipDims.x() + 0.02f, clipDims.y() + 0.02f, clipDims.z() + 0.02f);
            GL11.glPopMatrix();
        }
    }

    private static boolean isFacingAir(SelectionState sel, int face, Vec3DInt pos, Vec3DInt dims) {
        Vec3DInt neighbor = pos.plus(GhostRenderer.NX[face], GhostRenderer.NY[face], GhostRenderer.NZ[face]);
        return !neighbor.inBounds(Vec3DInt.ZERO, dims.minus(1))
                || sel.clipboardGet(neighbor).block() == Blocks.air;
    }

    private void ensureClipWireframeCache(SelectionState sel) {
        if (sel.clipboardVersion == cachedClipVersion && cachedClipWire != null) return;
        cachedClipWire = computeClipWireframe(sel);
        cachedClipVersion = sel.clipboardVersion;
    }

    private static float[] computeClipWireframe(SelectionState sel) {
        if (sel.clipboard == null) return new float[0];
        Vec3DInt clipDims = sel.clipDim;

        HashSet<Long> set = new HashSet<>(clipDims.product());
        Vec3DInt.forEachInclusive(Vec3DInt.ZERO, clipDims.minus(1), pos -> {
            if (sel.clipboardGet(pos).block() != Blocks.air) set.add(SelectionRenderer.lPack(pos));
        });

        return GhostRenderer.creaseWireframeFromSet(set);
    }

    private static void drawAxisLine(SelectionState sel, BuilderToolState bts, Vec3DDouble camPos) {
        Vec3DDouble src =
                sel.min().toDouble().plus(sel.clipDim.toDouble().times(0.5)).minus(camPos);
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
