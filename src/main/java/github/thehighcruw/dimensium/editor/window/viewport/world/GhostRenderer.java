/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

/**
 * Static utility for rendering a translucent ghost preview of a block list.
 * Reusable by any tool that needs a placement preview.
 */
public class GhostRenderer {

    static final int BATCH_SIZE = 2048;

    // ── Exterior wireframe constants ──────────────────────────────────────────

    // Face neighbor offsets: +X,-X,+Y,-Y,+Z,-Z
    static final int[] NX = {1, -1, 0, 0, 0, 0};
    static final int[] NY = {0, 0, 1, -1, 0, 0};
    static final int[] NZ = {0, 0, 0, 0, 1, -1};

    // For each of 6 faces, 4 edges; each edge = {axis, dx, dy, dz}
    // axis: 0=X-axis edge, 1=Y-axis edge, 2=Z-axis edge
    // corner of edge = block corner at (bx+dx, by+dy, bz+dz)
    static final int[][][] FACE_EDGES = {
        {{2, 1, 0, 0}, {1, 1, 0, 1}, {2, 1, 1, 0}, {1, 1, 0, 0}}, // +X face
        {{2, 0, 0, 0}, {1, 0, 0, 1}, {2, 0, 1, 0}, {1, 0, 0, 0}}, // -X face
        {{0, 0, 1, 0}, {2, 1, 1, 0}, {0, 0, 1, 1}, {2, 0, 1, 0}}, // +Y face
        {{0, 0, 0, 0}, {2, 1, 0, 0}, {0, 0, 0, 1}, {2, 0, 0, 0}}, // -Y face
        {{0, 0, 0, 1}, {1, 1, 0, 1}, {0, 0, 1, 1}, {1, 0, 0, 1}}, // +Z face
        {{0, 0, 0, 0}, {1, 1, 0, 0}, {0, 0, 1, 0}, {1, 0, 0, 0}}, // -Z face
    };
    // One bit per face direction: +X=1, -X=2, +Y=4, -Y=8, +Z=16, -Z=32
    static final int[] FACE_BIT = {1, 2, 4, 8, 16, 32};

    public static final GhostRenderer INSTANCE = new GhostRenderer();

    // ── Exterior wireframe for ghost block list ────────────────────────────────

    HashMap<Long, Integer> computeLocalWireframe(List<Vec3DInt> blocks) {
        Set<Long> set = new HashSet<>(blocks.size() * 2);
        for (Vec3DInt p : blocks) set.add(SelectionRenderer.lPack(p.x(), p.y(), p.z()));
        return getEdgeMask(blocks, set);
    }

    @Nonnull
    private HashMap<Long, Integer> getEdgeMask(List<Vec3DInt> blocks, Set<Long> set) {
        HashMap<Long, Integer> edgeMask = new HashMap<>(blocks.size() * 4);
        for (Vec3DInt p : blocks) {
            for (int face = 0; face < 6; face++) {
                if (set.contains(SelectionRenderer.lPack(p.plus(NX[face], NY[face], NZ[face])))) continue;
                int faceBit = FACE_BIT[face];
                for (int[] e : FACE_EDGES[face]) {
                    Vec3DInt corner = p.plus(e[1], e[2], e[3]);
                    long ek = lEdgeKey(e[0], corner.x(), corner.y(), corner.z());
                    edgeMask.compute(ek, (k, prev) -> prev == null ? faceBit : prev | faceBit);
                }
            }
        }
        return edgeMask;
    }

    // ── Geometry helpers ──────────────────────────────────────────────────────

    /** Emit one textured quad using the block's icon for the given side. Skips if icon is null. */
    static void addFace(
            Tessellator t,
            Block block,
            int meta,
            int side,
            float tr,
            float tg,
            float tb,
            Vec3DFloat a,
            Vec3DFloat b,
            Vec3DFloat c,
            Vec3DFloat d) {
        IIcon icon;
        try {
            icon = block.getIcon(side, meta);
        } catch (Exception e) {
            return;
        }
        if (icon == null) return;

        double u1 = icon.getMinU(), u2 = icon.getMaxU();
        double v1 = icon.getMinV(), v2 = icon.getMaxV();
        addColoredVertex(t, tr, tg, tb, a.x(), a.y(), a.z(), u1, v1);
        addColoredVertex(t, tr, tg, tb, b.x(), b.y(), b.z(), u2, v1);
        addColoredVertex(t, tr, tg, tb, c.x(), c.y(), c.z(), u2, v2);
        addColoredVertex(t, tr, tg, tb, d.x(), d.y(), d.z(), u1, v2);
    }

    private static void addColoredVertex(
            Tessellator t, float r, float g, float b, double x, double y, double z, double u, double v) {
        t.setColorRGBA_F(r, g, b, 1.0f);
        t.addVertexWithUV(x, y, z, u, v);
    }

    static HashMap<Long, Integer> outlineWireframeFromSet(HashSet<Long> set) {
        HashMap<Long, Integer> edgeMask = new HashMap<>(set.size() * 4);
        for (long pk : set) {
            Vec3DInt b = SelectionRenderer.lUnpack(pk);
            for (int face = 0; face < 6; face++) {
                if (set.contains(SelectionRenderer.lPack(b.plus(NX[face], NY[face], NZ[face])))) continue;
                int faceBit = FACE_BIT[face];
                for (int[] e : FACE_EDGES[face]) {
                    long ek = ((long) e[0] << 39) | SelectionRenderer.lPack(b.plus(e[1], e[2], e[3]));
                    edgeMask.compute(ek, (k, prev) -> prev == null ? faceBit : prev | faceBit);
                }
            }
        }
        return edgeMask;
    }

    /**
     * Builds billboard-quad vertices for the silhouette edges of a precomputed edge mask.
     * Only fold edges (bitCount &gt; 1: edge borders two differently-oriented exterior faces) are
     * considered. Among those, only edges where one adjacent face is front-facing and one is
     * back-facing relative to the camera are drawn — the true view-dependent silhouette contour.
     */
    static float[] buildSilhouetteVerts(HashMap<Long, Integer> edgeMask, Vec3DDouble eye) {
        if (edgeMask == null || edgeMask.isEmpty()) return new float[0];

        float[] verts = new float[edgeMask.size() * 6];
        int vi = 0;
        for (Map.Entry<Long, Integer> entry : edgeMask.entrySet()) {
            int mask = entry.getValue();
            long ek = entry.getKey();
            int axis = (int) (ek >> 39) & 3;
            Vec3DInt coord = Vec3DInt.from(
                    (int) ((ek >> 26) & 0x1FFF) - 4096, (int) ((ek >> 13) & 0x1FFF) - 4096, (int) (ek & 0x1FFF) - 4096);
            Vec3DInt end = coord.plus(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0);

            // Edge midpoint
            double mx = (coord.x() + end.x()) * 0.5;
            double my = (coord.y() + end.y()) * 0.5;
            double mz = (coord.z() + end.z()) * 0.5;

            // Which face normals point toward the camera from this edge's midpoint
            int frontFaceBits = (eye.x() > mx ? FACE_BIT[0] : FACE_BIT[1])
                    | (eye.y() > my ? FACE_BIT[2] : FACE_BIT[3])
                    | (eye.z() > mz ? FACE_BIT[4] : FACE_BIT[5]);

            boolean isSilhouette =
                    Integer.bitCount(mask) > 1 && (mask & frontFaceBits) != 0 && (mask & ~frontFaceBits) != 0;
            if (!isSilhouette) continue;

            verts[vi++] = coord.x();
            verts[vi++] = coord.y();
            verts[vi++] = coord.z();
            verts[vi++] = end.x();
            verts[vi++] = end.y();
            verts[vi++] = end.z();
        }
        return vi == verts.length ? verts : Arrays.copyOf(verts, vi);
    }

    static void addBoxFaces(Tessellator t, float x2, float y2, float z2) {
        t.addVertex(0f, 0f, 0f);
        t.addVertex(0f, 0f, z2);
        t.addVertex(x2, 0f, z2);
        t.addVertex(x2, 0f, 0f);
        t.addVertex(0f, y2, 0f);
        t.addVertex(x2, y2, 0f);
        t.addVertex(x2, y2, z2);
        t.addVertex(0f, y2, z2);
        t.addVertex(0f, 0f, 0f);
        t.addVertex(x2, 0f, 0f);
        t.addVertex(x2, y2, 0f);
        t.addVertex(0f, y2, 0f);
        t.addVertex(0f, 0f, z2);
        t.addVertex(0f, y2, z2);
        t.addVertex(x2, y2, z2);
        t.addVertex(x2, 0f, z2);
        t.addVertex(0f, 0f, 0f);
        t.addVertex(0f, y2, 0f);
        t.addVertex(0f, y2, z2);
        t.addVertex(0f, 0f, z2);
        t.addVertex(x2, 0f, 0f);
        t.addVertex(x2, 0f, z2);
        t.addVertex(x2, y2, z2);
        t.addVertex(x2, y2, 0f);
    }

    /**
     * Add one face of the unit cube at (x,y,z) to t. Face index matches NX/NY/NZ:
     * 0=+X, 1=-X, 2=+Y, 3=-Y, 4=+Z, 5=-Z.
     *
     * expand: push the face outward along its normal AND expand edges laterally by
     * the same amount. Use a small value (~0.02f) for glow passes to fill the
     * 1-pixel seam that otherwise appears at exterior crease edges where adjacent
     * exterior faces meet but don't overlap.
     */
    static void addSingleFace(Tessellator t, Vec3DInt pos, int face, float expand) {
        float x1 = pos.x() - expand, y1 = pos.y() - expand, z1 = pos.z() - expand;
        float x2 = pos.x() + 1 + expand, y2 = pos.y() + 1 + expand, z2 = pos.z() + 1 + expand;
        switch (face) {
            case 0: {
                float fx = pos.x() + 1 + expand;
                t.addVertex(fx, y1, z1);
                t.addVertex(fx, y1, z2);
                t.addVertex(fx, y2, z2);
                t.addVertex(fx, y2, z1);
                break;
            }
            case 1: {
                float fx = pos.x() - expand;
                t.addVertex(fx, y1, z1);
                t.addVertex(fx, y2, z1);
                t.addVertex(fx, y2, z2);
                t.addVertex(fx, y1, z2);
                break;
            }
            case 2: {
                float fy = pos.y() + 1 + expand;
                t.addVertex(x1, fy, z1);
                t.addVertex(x2, fy, z1);
                t.addVertex(x2, fy, z2);
                t.addVertex(x1, fy, z2);
                break;
            }
            case 3: {
                float fy = pos.y() - expand;
                t.addVertex(x1, fy, z1);
                t.addVertex(x1, fy, z2);
                t.addVertex(x2, fy, z2);
                t.addVertex(x2, fy, z1);
                break;
            }
            case 4: {
                float fz = pos.z() + 1 + expand;
                t.addVertex(x1, y1, fz);
                t.addVertex(x1, y2, fz);
                t.addVertex(x2, y2, fz);
                t.addVertex(x2, y1, fz);
                break;
            }
            case 5: {
                float fz = pos.z() - expand;
                t.addVertex(x1, y1, fz);
                t.addVertex(x2, y1, fz);
                t.addVertex(x2, y2, fz);
                t.addVertex(x1, y2, fz);
                break;
            }
        }
    }

    static void addSingleFace(Tessellator t, Vec3DInt pos, int face) {
        addSingleFace(t, pos, face, 0f);
    }

    private long lEdgeKey(int axis, int x, int y, int z) {
        return ((long) axis << 39) | ((long) (x + 4096) << 26) | ((long) (y + 4096) << 13) | (z + 4096);
    }

    @FunctionalInterface
    public interface BlockFilter {

        boolean accept(int[] bm);
    }

    /**
     * Renders blocks using Minecraft's RenderBlocks so non-full blocks (slabs, stairs, etc.)
     * display with their correct geometry instead of a full cube. Caller must set up GL state
     * (texture atlas bound, cull face, depth test, color tint) before calling.
     * Ambient occlusion is temporarily disabled to avoid artifacts in the ghost render context.
     */
    static void renderBlocksPass(Tessellator t, IBlockAccess world, Iterable<Vec3DInt> positions) {
        Minecraft mc = Minecraft.getMinecraft();
        int savedAO = mc.gameSettings.ambientOcclusion;
        mc.gameSettings.ambientOcclusion = 0;
        // RenderBlocks emits CCW winding (standard Minecraft); callers set GL_CW for manual emission.
        GL11.glFrontFace(GL11.GL_CCW);
        RenderBlocks rb = new RenderBlocks(world);
        rb.useInventoryTint = false;
        t.startDrawingQuads();
        for (Vec3DInt pos : positions) {
            Block block = world.getBlock(pos.x(), pos.y(), pos.z());
            if (block != null && block != Blocks.air) {
                rb.renderBlockByRenderType(block, pos.x(), pos.y(), pos.z());
            }
        }
        t.draw();
        GL11.glFrontFace(GL11.GL_CW);
        mc.gameSettings.ambientOcclusion = savedAO;
    }

    /**
     * Renders exterior faces (untextured, batched) for entries matching filter.
     * Caller sets GL color first. origin is the world-space reference point for the
     * current GL matrix; vertices are emitted as (blockPos - origin) so they are
     * camera-relative small values with full float precision.
     */
    static void drawExteriorFacesSingleColor(
            Tessellator t, Map<Long, int[]> proposed, BlockFilter filter, Vec3DInt origin) {
        int batched = 0;
        t.startDrawingQuads();
        for (Map.Entry<Long, int[]> e : proposed.entrySet()) {
            if (filter != null && !filter.accept(e.getValue())) continue;
            Vec3DInt b = ChangeProposal.unpackKey(e.getKey());
            Vec3DInt local = b.minus(origin);
            for (int face = 0; face < 6; face++) {
                long nk = ChangeProposal.packKey(b.plus(NX[face], NY[face], NZ[face]));
                if (!proposed.containsKey(nk)) {
                    addSingleFace(t, local, face);
                    if (++batched % BATCH_SIZE == 0) {
                        t.draw();
                        t.startDrawingQuads();
                    }
                }
            }
        }
        t.draw();
    }
}
