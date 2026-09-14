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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class BrushPreviewRenderer {

    public static final BrushPreviewRenderer INSTANCE = new BrushPreviewRenderer();

    private static final int BRUSH_VOXEL_MAX = 100_000;

    private BrushShape cachedBrushShape = null;
    private int cachedBrushSize = -1;
    private int cachedBrushSizeY = -1;
    private int cachedBrushSizeZ = -1;
    private float cachedThreshold = -1f;
    private float[] cachedBrushWire = null;
    private HashSet<Long> cachedBrushSet = null;

    public void render(ToolRenderer renderer, Minecraft mc, Vec3DDouble camPos) {
        MovingObjectPosition mop = RenderUtils.raycastAtCursor();
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        int bx = mop.blockX, by = mop.blockY, bz = mop.blockZ;
        double rx = camPos.x(), ry = camPos.y(), rz = camPos.z();
        Tool activeTool = DimensiumEditorMode.INSTANCE.selectedTool;
        BrushState bs = BrushState.INSTANCE;
        int sx = bs.brushRadius;
        int sy = bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius;
        BrushShape shape = bs.brushShape;

        if (renderer.renderHover(mop, camPos)) return;

        boolean buttonHeld = BrushInputRegistry.usesDragLoop(activeTool) && Mouse.isButtonDown(KeyConstants.RMB);

        if (buttonHeld) {
            if (activeTool == Tool.SMOOTH) {
                // Accumulate solid blocks from all drag positions visited so far + current cursor.
                // Uses absolute world-coord packing (20 bits/axis, offset 524288).
                HashSet<Long> absSet = new HashSet<>();
                collectSolidAbsolute(mc, shape, bx, by, bz, sx, sy, sx, absSet);
                for (long pk : TickHandler.INSTANCE.getSmoothDragPositions()) {
                    int cx2 = (int) ((pk >> 42) & 0x1FFFFF) - 1048576;
                    int cy2 = (int) ((pk >> 21) & 0x1FFFFF) - 1048576;
                    int cz2 = (int) (pk & 0x1FFFFF) - 1048576;
                    collectSolidAbsolute(mc, shape, cx2, cy2, cz2, sx, sy, sx, absSet);
                }
                float[] wire = creaseWireframeAbsolute(absSet);
                if (wire != null && wire.length > 0) {
                    GL11.glColor4f(0.50f, 0.85f, 1.0f, 0.9f);
                    GL11.glPushMatrix();
                    GL11.glTranslated(-rx, -ry, -rz);
                    // glTranslated(-rx,-ry,-rz) → eye in local (world) space = (rx,ry,rz)
                    WorldLines.setEye(Vec3DDouble.from(rx, ry, rz));
                    GhostRenderer.drawWireframeCache(Tessellator.instance, wire);
                    GL11.glPopMatrix();
                }
            } else {
                // Non-smooth tools: pulsating fill + crease using relative coords.
                HashSet<Long> affectedSet = new HashSet<>();
                for (int dx = -sx; dx <= sx; dx++)
                    for (int dy = -sy; dy <= sy; dy++)
                        for (int dz = -sx; dz <= sx; dz++) {
                            if (!inBrushShape(shape, dx, dy, dz, sx, sy, sx)) continue;
                            if (renderer.isBlockAffected(mc, bx + dx, by + dy, bz + dz))
                                affectedSet.add(SelectionRenderer.lPack(dx + sx, dy + sy, dz + sx));
                        }

                if (!affectedSet.isEmpty()) {
                    float pulse = 0.22f + 0.13f * (float) Math.sin(System.currentTimeMillis() / 180.0);
                    GL11.glPushMatrix();
                    GL11.glTranslated(-rx, -ry, -rz);
                    GL11.glColor4f(0.35f, 0.75f, 1.0f, pulse);
                    Tessellator t = Tessellator.instance;
                    t.startDrawingQuads();
                    int batched = 0;
                    for (long pk : affectedSet) {
                        int lx = (int) ((pk >> 26) & 0x1FFF) - 4096;
                        int ly = (int) ((pk >> 13) & 0x1FFF) - 4096;
                        int lz = (int) (pk & 0x1FFF) - 4096;
                        Vec3DInt world = Vec3DInt.from(lx + bx - sx, ly + by - sy, lz + bz - sx);
                        for (int face = 0; face < 6; face++) {
                            long nk = SelectionRenderer.lPack(
                                    lx + GhostRenderer.NX[face],
                                    ly + GhostRenderer.NY[face],
                                    lz + GhostRenderer.NZ[face]);
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

                    float[] wire = creaseWireframeFromSet(affectedSet);
                    if (wire != null && wire.length > 0) {
                        GL11.glColor4f(0.50f, 0.85f, 1.0f, 0.9f);
                        GL11.glPushMatrix();
                        Vec3DDouble brushTrans = Vec3DDouble.from(bx - sx - rx, by - sy - ry, bz - sx - rz);
                        GL11.glTranslated(brushTrans.x(), brushTrans.y(), brushTrans.z());
                        WorldLines.setEyeForTranslation(brushTrans);
                        GhostRenderer.drawWireframeCache(Tessellator.instance, wire);
                        GL11.glPopMatrix();
                    }
                }
            }
        } else {
            // Static brush-shape preview: transparent white faces + white crease edges
            float[] wire = getBrushWireframe(shape, sx, sy, sx);
            GL11.glPushMatrix();
            Vec3DDouble shapeTrans = Vec3DDouble.from(bx - sx - rx, by - sy - ry, bz - sx - rz);
            GL11.glTranslated(shapeTrans.x(), shapeTrans.y(), shapeTrans.z());
            WorldLines.setEyeForTranslation(shapeTrans);

            // Outer faces — view-shaded transparent white.
            // Depth write enabled so overlapping faces don't accumulate (fixes corner glow).
            if (cachedBrushSet != null && !cachedBrushSet.isEmpty()) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(true);

                // View direction from brush center → normalized
                Vec3DDouble viewDir = Vec3DDouble.from(rx - bx, ry - by, rz - bz);
                if (viewDir.length() > 0.001) viewDir = viewDir.normalize();
                double ecx = viewDir.x(), ecy = viewDir.y(), ecz = viewDir.z();

                Tessellator tf = Tessellator.instance;
                for (int faceDir = 0; faceDir < 6; faceDir++) {
                    // dot(faceNormal, eyeDir): faces toward player are bright, away are dim
                    float dot = (float) (ecx * GhostRenderer.NX[faceDir]
                            + ecy * GhostRenderer.NY[faceDir]
                            + ecz * GhostRenderer.NZ[faceDir]);
                    float brightness = 0.25f + 0.75f * Math.max(0f, dot);
                    GL11.glColor4f(brightness, brightness, brightness, 0.12f);
                    tf.startDrawingQuads();
                    int batched = 0;
                    for (long pk : cachedBrushSet) {
                        int lx = (int) ((pk >> 26) & 0x1FFF) - 4096;
                        int ly = (int) ((pk >> 13) & 0x1FFF) - 4096;
                        int lz = (int) (pk & 0x1FFF) - 4096;
                        long nk = SelectionRenderer.lPack(
                                lx + GhostRenderer.NX[faceDir],
                                ly + GhostRenderer.NY[faceDir],
                                lz + GhostRenderer.NZ[faceDir]);
                        if (!cachedBrushSet.contains(nk)) {
                            GhostRenderer.addSingleFace(tf, Vec3DInt.from(lx, ly, lz), faceDir);
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

            // Crease edges — white, softer
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.35f);
            if (wire != null && wire.length > 0) {
                GhostRenderer.drawWireframeCache(Tessellator.instance, wire);
            } else {
                SelectionRenderer.drawBox(0, 0, 0, sx * 2 + 1, sy * 2 + 1, sx * 2 + 1);
            }
            GL11.glPopMatrix();
        }
    }

    private float[] getBrushWireframe(BrushShape shape, int sx, int sy, int sz) {
        float thr = DimensiumConfig.shapeThreshold;
        if (shape == cachedBrushShape
                && sx == cachedBrushSize
                && sy == cachedBrushSizeY
                && sz == cachedBrushSizeZ
                && thr == cachedThreshold) return cachedBrushWire;
        cachedBrushShape = shape;
        cachedBrushSize = sx;
        cachedBrushSizeY = sy;
        cachedBrushSizeZ = sz;
        cachedThreshold = thr;
        cachedBrushSet = buildBrushSet(shape, sx, sy, sz);
        cachedBrushWire = cachedBrushSet != null ? creaseWireframeFromSet(cachedBrushSet) : null;
        return cachedBrushWire;
    }

    private static HashSet<Long> buildBrushSet(BrushShape shape, int sx, int sy, int sz) {
        HashSet<Long> set = new HashSet<>();
        for (int dx = -sx; dx <= sx; dx++)
            for (int dy = -sy; dy <= sy; dy++)
                for (int dz = -sz; dz <= sz; dz++)
                    if (inBrushShape(shape, dx, dy, dz, sx, sy, sz))
                        set.add(SelectionRenderer.lPack(dx + sx, dy + sy, dz + sz));
        return set.size() > BRUSH_VOXEL_MAX ? null : set;
    }

    /**
     * Computes a crease-edge wireframe for an arbitrary set of voxels packed via
     * SelectionRenderer.lPack. Returns interleaved line segment vertices (x0,y0,z0,x1,y1,z1,...).
     * A crease edge is one shared by faces on different axes (i.e. a corner edge of the silhouette).
     */
    private static float[] creaseWireframeFromSet(HashSet<Long> set) {
        if (set.size() > BRUSH_VOXEL_MAX) return null;
        return GhostRenderer.creaseWireframeFromSet(set);
    }

    // ── Absolute-coord helpers for SMOOTH accumulated crease ──────────────────

    private static final int ABS_OFFSET = 524288; // 2^19 — supports ±524287 world coords

    private static long wPack(int x, int y, int z) {
        return ((long) (x + ABS_OFFSET) << 40) | ((long) (y + ABS_OFFSET) << 20) | (z + ABS_OFFSET);
    }

    private static void collectSolidAbsolute(
            Minecraft mc, BrushShape shape, int cx, int cy, int cz, int sx, int sy, int sz, HashSet<Long> out) {
        for (int dx = -sx; dx <= sx; dx++)
            for (int dy = -sy; dy <= sy; dy++)
                for (int dz = -sz; dz <= sz; dz++) {
                    if (!inBrushShape(shape, dx, dy, dz, sx, sy, sz)) continue;
                    if (mc.theWorld.getBlock(cx + dx, cy + dy, cz + dz) != net.minecraft.init.Blocks.air)
                        out.add(wPack(cx + dx, cy + dy, cz + dz));
                }
    }

    private static float[] creaseWireframeAbsolute(HashSet<Long> set) {
        if (set.isEmpty() || set.size() > BRUSH_VOXEL_MAX) return null;

        HashMap<Long, Integer> edgeMask = getEdgeMask(set);

        int creaseCount = 0;
        for (int mask : edgeMask.values()) if (Integer.bitCount(mask) > 1) creaseCount++;
        float[] verts = new float[creaseCount * 6];
        int vi = 0;
        for (Map.Entry<Long, Integer> entry : edgeMask.entrySet()) {
            if (Integer.bitCount(entry.getValue()) <= 1) continue;
            long ek = entry.getKey();
            int axis = (int) (ek >>> 62) & 3;
            long pos = ek & 0x3FFFFFFFFFFFFFFFL;
            int ex = (int) ((pos >> 40) & 0xFFFFF) - ABS_OFFSET;
            int ey = (int) ((pos >> 20) & 0xFFFFF) - ABS_OFFSET;
            int ez = (int) (pos & 0xFFFFF) - ABS_OFFSET;
            verts[vi++] = ex;
            verts[vi++] = ey;
            verts[vi++] = ez;
            verts[vi++] = ex + (axis == 0 ? 1 : 0);
            verts[vi++] = ey + (axis == 1 ? 1 : 0);
            verts[vi++] = ez + (axis == 2 ? 1 : 0);
        }
        return verts;
    }

    @Nonnull
    private static HashMap<Long, Integer> getEdgeMask(HashSet<Long> set) {
        HashMap<Long, Integer> edgeMask = new HashMap<>(set.size() * 4);
        for (long pk : set) {
            int bx = (int) ((pk >> 40) & 0xFFFFF) - ABS_OFFSET;
            int by = (int) ((pk >> 20) & 0xFFFFF) - ABS_OFFSET;
            int bz = (int) (pk & 0xFFFFF) - ABS_OFFSET;
            for (int face = 0; face < 6; face++) {
                long nb = wPack(bx + GhostRenderer.NX[face], by + GhostRenderer.NY[face], bz + GhostRenderer.NZ[face]);
                if (set.contains(nb)) continue;
                int axisBit = GhostRenderer.FACE_AXIS_BIT[face];
                for (int[] e : GhostRenderer.FACE_EDGES[face]) {
                    // Edge key: pack edge axis (2 bits) + absolute start corner (wPack)
                    long ek = ((long) e[0] << 62) | wPack(bx + e[1], by + e[2], bz + e[3]);
                    edgeMask.compute(ek, (k, prev) -> prev == null ? axisBit : prev | axisBit);
                }
            }
        }
        return edgeMask;
    }

    static boolean inBrushShape(BrushShape shape, int dx, int dy, int dz, int sx, int sy, int sz) {
        return BrushUtil.inShape(shape, dx, dy, dz, sx, sy, sz);
    }
}
