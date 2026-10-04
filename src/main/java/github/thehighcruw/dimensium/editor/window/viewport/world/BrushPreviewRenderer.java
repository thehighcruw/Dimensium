/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.handler.TickHandler;
import github.thehighcruw.dimensium.editor.tool.BrushInputRegistry;
import github.thehighcruw.dimensium.editor.tool.Tool;
import github.thehighcruw.dimensium.editor.tool.ToolRenderer;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushShape;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.HashMap;
import java.util.HashSet;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class BrushPreviewRenderer {

    public static final BrushPreviewRenderer INSTANCE = new BrushPreviewRenderer();

    private static final int BRUSH_VOXEL_MAX = 100_000;

    private BrushShape cachedBrushShape = null;
    private Vec3DInt cachedBrushSize = Vec3DInt.from(-1);
    private float cachedThreshold = -1f;
    private HashMap<Long, Integer> cachedBrushEdgeMask = null;
    private HashSet<Long> cachedBrushSet = null;

    public void render(ToolRenderer renderer, Minecraft mc, Vec3DDouble camPos) {
        MovingObjectPosition mop = RenderUtils.raycastAtCursor();
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        Vec3DInt cursor = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        Tool activeTool = DimensiumEditorMode.INSTANCE.getSelectedTool();
        BrushState bs = BrushState.INSTANCE;
        int sx = bs.brushRadius;
        int sy = bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius;
        BrushShape shape = bs.brushShape;
        Vec3DInt brushSize = Vec3DInt.from(sx, sy, sx);

        if (renderer.renderHover(mop, camPos)) return;

        boolean buttonHeld = BrushInputRegistry.usesDragLoop(activeTool) && Mouse.isButtonDown(KeyConstants.RMB);

        if (buttonHeld) {
            if (activeTool == Tool.SMOOTH) {
                // Accumulate solid blocks from all drag positions visited so far + current cursor.
                // Uses absolute world-coord packing (20 bits/axis, offset 524288).
                HashSet<Long> absSet = new HashSet<>();
                collectSolidAbsolute(mc, shape, cursor, brushSize, absSet);
                for (Vec3DInt dragPos : TickHandler.INSTANCE.getSmoothDragPositions()) {
                    collectSolidAbsolute(mc, shape, dragPos, brushSize, absSet);
                }
                HashMap<Long, Integer> edgeMask = outlineWireframeAbsolute(absSet);
                if (edgeMask != null && !edgeMask.isEmpty()) {
                    GL11.glColor4f(1.0f, 1.0f, 0.0f, 0.9f);
                    GL11.glPushMatrix();
                    GL11.glTranslated(-camPos.x(), -camPos.y(), -camPos.z());
                    // glTranslated(-camPos) → eye in local (world) space = camPos
                    WorldLines.setEye(camPos);
                    WorldLines.drawWireframeCache(
                            Tessellator.instance,
                            GhostRenderer.buildSilhouetteVerts(edgeMask, camPos),
                            WorldLines.W_SILHOUETTE);
                    GL11.glPopMatrix();
                }
            } else {
                // Non-smooth tools: pulsating fill + silhouette outline using relative coords.
                HashSet<Long> affectedSet = new HashSet<>();
                Vec3DInt.forEachInclusive(brushSize.negate(), brushSize, offset -> {
                    if (!BrushUtil.inShape(shape, offset, brushSize)) return;
                    Vec3DInt worldPos = cursor.plus(offset);
                    if (!renderer.isBlockAffected(mc, worldPos)) return;
                    if (!ChangeProposal.testDestination(mc.theWorld, worldPos)) return;
                    affectedSet.add(SelectionRenderer.lPack(offset.plus(brushSize)));
                });

                if (!affectedSet.isEmpty()) {
                    float pulse = 0.22f + 0.13f * (float) Math.sin(System.currentTimeMillis() / 180.0);
                    GL11.glPushMatrix();
                    GL11.glTranslated(-camPos.x(), -camPos.y(), -camPos.z());
                    GL11.glColor4f(0.35f, 0.75f, 1.0f, pulse);
                    Tessellator t = Tessellator.instance;
                    t.startDrawingQuads();
                    int batched = 0;
                    for (long pk : affectedSet) {
                        Vec3DInt local = SelectionRenderer.lUnpack(pk);
                        Vec3DInt world = cursor.plus(local.minus(brushSize));
                        for (int face = 0; face < 6; face++) {
                            long nk = SelectionRenderer.lPack(
                                    local.plus(GhostRenderer.NX[face], GhostRenderer.NY[face], GhostRenderer.NZ[face]));
                            if (!affectedSet.contains(nk)) {
                                GhostRenderer.addSingleFace(t, world, face);
                                if (++batched % 2048 == 0) {
                                    t.draw();
                                    t.startDrawingQuads();
                                }
                            }
                        }
                    }
                    t.draw();
                    GL11.glPopMatrix();

                    HashMap<Long, Integer> affectedEdgeMask = outlineWireframeFromSet(affectedSet);
                    if (affectedEdgeMask != null && !affectedEdgeMask.isEmpty()) {
                        GL11.glColor4f(1.0f, 1.0f, 0.0f, 0.9f);
                        GL11.glPushMatrix();
                        Vec3DDouble brushTrans =
                                cursor.toDouble().minus(brushSize.toDouble()).minus(camPos);
                        GL11.glTranslated(brushTrans.x(), brushTrans.y(), brushTrans.z());
                        WorldLines.setEyeForTranslation(brushTrans);
                        WorldLines.drawWireframeCache(
                                Tessellator.instance,
                                GhostRenderer.buildSilhouetteVerts(affectedEdgeMask, WorldLines.eye),
                                WorldLines.W_SILHOUETTE);
                        GL11.glPopMatrix();
                    }
                }
            }
        } else {
            // Static brush-shape preview: transparent white faces + yellow outline edges
            HashMap<Long, Integer> brushEdgeMask = getBrushWireframe(shape, brushSize);
            GL11.glPushMatrix();
            Vec3DDouble shapeTrans =
                    cursor.toDouble().minus(brushSize.toDouble()).minus(camPos);
            GL11.glTranslated(shapeTrans.x(), shapeTrans.y(), shapeTrans.z());
            WorldLines.setEyeForTranslation(shapeTrans);

            // Outer faces — view-shaded transparent white.
            // Depth write enabled so overlapping faces don't accumulate (fixes corner glow).
            if (cachedBrushSet != null && !cachedBrushSet.isEmpty()) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(true);

                // View direction from brush center → normalized
                Vec3DDouble viewDir = camPos.minus(cursor.toDouble());
                if (viewDir.length() > 0.001) viewDir = viewDir.normalize();

                Tessellator tf = Tessellator.instance;
                for (int faceDir = 0; faceDir < 6; faceDir++) {
                    // dot(faceNormal, eyeDir): faces toward player are bright, away are dim
                    float dot = (float) viewDir.dot(Vec3DDouble.from(
                            GhostRenderer.NX[faceDir], GhostRenderer.NY[faceDir], GhostRenderer.NZ[faceDir]));
                    float brightness = 0.25f + 0.75f * Math.max(0f, dot);
                    GL11.glColor4f(brightness, brightness, brightness, 0.12f);
                    tf.startDrawingQuads();
                    int batched = 0;
                    for (long pk : cachedBrushSet) {
                        Vec3DInt local = SelectionRenderer.lUnpack(pk);
                        long nk = SelectionRenderer.lPack(local.plus(
                                GhostRenderer.NX[faceDir], GhostRenderer.NY[faceDir], GhostRenderer.NZ[faceDir]));
                        if (!cachedBrushSet.contains(nk)) {
                            GhostRenderer.addSingleFace(tf, local, faceDir);
                            if (++batched % 2048 == 0) {
                                tf.draw();
                                tf.startDrawingQuads();
                            }
                        }
                    }
                    tf.draw();
                }

                GL11.glDepthMask(false);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }

            // Outline edges — yellow, depth-tested so inner contours hidden by the face pass are culled.
            GL11.glColor4f(1.0f, 1.0f, 0.0f, 0.35f);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-1.0f, -1.0f);
            if (brushEdgeMask != null && !brushEdgeMask.isEmpty()) {
                WorldLines.drawWireframeCache(
                        Tessellator.instance,
                        GhostRenderer.buildSilhouetteVerts(brushEdgeMask, WorldLines.eye),
                        WorldLines.W_SILHOUETTE);
            } else {
                SelectionRenderer.drawBox(0, 0, 0, sx * 2 + 1, sy * 2 + 1, sx * 2 + 1);
            }
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(0.0f, 0.0f);
            GL11.glDepthFunc(GL11.GL_LESS);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glPopMatrix();
        }
    }

    private HashMap<Long, Integer> getBrushWireframe(BrushShape shape, Vec3DInt brushSize) {
        float thr = DimensiumConfig.shapeThreshold;
        if (shape == cachedBrushShape && brushSize.equals(cachedBrushSize) && thr == cachedThreshold)
            return cachedBrushEdgeMask;
        cachedBrushShape = shape;
        cachedBrushSize = brushSize;
        cachedThreshold = thr;
        cachedBrushSet = buildBrushSet(shape, brushSize);
        cachedBrushEdgeMask = cachedBrushSet != null ? outlineWireframeFromSet(cachedBrushSet) : null;
        return cachedBrushEdgeMask;
    }

    private static HashSet<Long> buildBrushSet(BrushShape shape, Vec3DInt brushSize) {
        HashSet<Long> set = new HashSet<>();
        Vec3DInt.forEachInclusive(brushSize.negate(), brushSize, offset -> {
            if (BrushUtil.inShape(shape, offset, brushSize)) set.add(SelectionRenderer.lPack(offset.plus(brushSize)));
        });
        return set.size() > BRUSH_VOXEL_MAX ? null : set;
    }

    private static HashMap<Long, Integer> outlineWireframeFromSet(HashSet<Long> set) {
        if (set.size() > BRUSH_VOXEL_MAX) return null;
        return GhostRenderer.outlineWireframeFromSet(set);
    }

    // ── Absolute-coord helpers for SMOOTH accumulated silhouette ─────────────

    private static final int ABS_OFFSET = 524288; // 2^19 — supports ±524287 world coords

    private static long wPack(int x, int y, int z) {
        return ((long) (x + ABS_OFFSET) << 40) | ((long) (y + ABS_OFFSET) << 20) | (z + ABS_OFFSET);
    }

    private static long wPack(Vec3DInt p) {
        return wPack(p.x(), p.y(), p.z());
    }

    private static Vec3DInt wUnpack(long pk) {
        return Vec3DInt.from(
                (int) ((pk >> 40) & 0xFFFFF) - ABS_OFFSET,
                (int) ((pk >> 20) & 0xFFFFF) - ABS_OFFSET,
                (int) (pk & 0xFFFFF) - ABS_OFFSET);
    }

    private static void collectSolidAbsolute(
            Minecraft mc, BrushShape shape, Vec3DInt center, Vec3DInt brushSize, HashSet<Long> out) {
        Vec3DInt.forEachInclusive(brushSize.negate(), brushSize, offset -> {
            if (!BrushUtil.inShape(shape, offset, brushSize)) return;
            Vec3DInt worldPos = center.plus(offset);
            if (WorldUtils.getBlock(mc.theWorld, worldPos) == Blocks.air) return;
            if (!ChangeProposal.testDestination(mc.theWorld, worldPos)) return;
            out.add(wPack(worldPos));
        });
    }

    private static HashMap<Long, Integer> outlineWireframeAbsolute(HashSet<Long> set) {
        if (set.isEmpty() || set.size() > BRUSH_VOXEL_MAX) return null;
        return getEdgeMask(set);
    }

    @Nonnull
    private static HashMap<Long, Integer> getEdgeMask(HashSet<Long> set) {
        HashMap<Long, Integer> edgeMask = new HashMap<>(set.size() * 4);
        for (long pk : set) {
            Vec3DInt b = wUnpack(pk);
            for (int face = 0; face < 6; face++) {
                long nb = wPack(b.plus(GhostRenderer.NX[face], GhostRenderer.NY[face], GhostRenderer.NZ[face]));
                if (set.contains(nb)) continue;
                int faceBit = GhostRenderer.FACE_BIT[face];
                for (int[] e : GhostRenderer.FACE_EDGES[face]) {
                    // Edge key: pack edge axis (2 bits) + absolute start corner (wPack)
                    long ek = ((long) e[0] << 62) | wPack(b.plus(e[1], e[2], e[3]));
                    edgeMask.compute(ek, (k, prev) -> prev == null ? faceBit : prev | faceBit);
                }
            }
        }
        return edgeMask;
    }
}
