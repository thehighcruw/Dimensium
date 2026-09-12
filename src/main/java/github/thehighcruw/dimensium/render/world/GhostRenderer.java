package github.thehighcruw.dimensium.render.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import github.thehighcruw.dimensium.tool.ChangeProposal;

/**
 * Static utility for rendering a translucent ghost preview of a block list.
 * Reusable by any tool that needs a placement preview.
 */
public class GhostRenderer {

    private static final int BATCH_SIZE = 2048;

    // ── Exterior wireframe constants ──────────────────────────────────────────

    // Face neighbor offsets: +X,-X,+Y,-Y,+Z,-Z
    static final int[] NX = { 1, -1, 0, 0, 0, 0 };
    static final int[] NY = { 0, 0, 1, -1, 0, 0 };
    static final int[] NZ = { 0, 0, 0, 0, 1, -1 };

    // For each of 6 faces, 4 edges; each edge = {axis, dx, dy, dz}
    // axis: 0=X-axis edge, 1=Y-axis edge, 2=Z-axis edge
    // corner of edge = block corner at (bx+dx, by+dy, bz+dz)
    static final int[][][] FACE_EDGES = { { { 2, 1, 0, 0 }, { 1, 1, 0, 1 }, { 2, 1, 1, 0 }, { 1, 1, 0, 0 } }, // +X face
        { { 2, 0, 0, 0 }, { 1, 0, 0, 1 }, { 2, 0, 1, 0 }, { 1, 0, 0, 0 } }, // -X face
        { { 0, 0, 1, 0 }, { 2, 1, 1, 0 }, { 0, 0, 1, 1 }, { 2, 0, 1, 0 } }, // +Y face
        { { 0, 0, 0, 0 }, { 2, 1, 0, 0 }, { 0, 0, 0, 1 }, { 2, 0, 0, 0 } }, // -Y face
        { { 0, 0, 0, 1 }, { 1, 1, 0, 1 }, { 0, 0, 1, 1 }, { 1, 0, 0, 1 } }, // +Z face
        { { 0, 0, 0, 0 }, { 1, 1, 0, 0 }, { 0, 0, 1, 0 }, { 1, 0, 0, 0 } }, // -Z face
    };
    // Which "plane group" each face belongs to: X-faces=1, Y-faces=2, Z-faces=4
    static final int[] FACE_AXIS_BIT = { 1, 1, 2, 2, 4, 4 };

    public static final GhostRenderer INSTANCE = new GhostRenderer();

    // Wireframe cache for ghost shape (invalidated when block list reference changes)
    private List<int[]> wireCachedBlocks = null;
    private float[] wireCache = null; // x1,y1,z1,x2,y2,z2,...

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Render a ghost shape preview in world space.
     *
     * @param blocks    Local [dx,dy,dz] positions, or null for bbox-only fallback
     * @param bboxW/H/D Effective bounding box
     * @param cr,cg,cb  Fallback RGB colour used when block is null or non-standard
     * @param pulse     Pulsing intensity 0–1 for animated alpha
     * @param wx,wy,wz  World anchor (anchorX/Y/Z)
     * @param rx,ry,rz  Interpolated player eye position
     * @param block     Block whose texture to use, or null for solid-colour fallback
     * @param meta      Block metadata for texture lookup
     */
    public void render(List<int[]> blocks, int bboxW, int bboxH, int bboxD, float cr, float cg, float cb, float pulse,
        double wx, double wy, double wz, double rx, double ry, double rz, Block block, int meta) {
        GL11.glPushMatrix();
        GL11.glTranslated(wx - rx, wy - ry, wz - rz);
        WorldLines.setEye(rx - wx, ry - wy, rz - wz);

        if (blocks != null) {
            renderBlockGhost(blocks, bboxW, bboxH, bboxD, cr, cg, cb, pulse, block, meta);
        } else {
            renderBBoxFallback(bboxW, bboxH, bboxD, cr, cg, cb, pulse);
        }

        GL11.glPopMatrix();
    }

    // ── Internal rendering ────────────────────────────────────────────────────

    private void renderBlockGhost(List<int[]> blocks, int bboxW, int bboxH, int bboxD, float cr, float cg, float cb,
        float pulse, Block block, int meta) {
        Tessellator t = Tessellator.instance;

        // Build occupancy set once — reused for exterior face culling and wireframe.
        Set<Long> set = new HashSet<>(blocks.size() * 2);
        for (int[] p : blocks) set.add(lPack(p[0], p[1], p[2]));

        boolean textured = block != null && block.getRenderType() == 0;

        if (textured) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glFrontFace(GL11.GL_CW);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.80f + 0.20f * pulse);

            t.startDrawingQuads();
            int count = 0;
            for (int[] p : blocks) {
                for (int face = 0; face < 6; face++) {
                    if (!set.contains(lPack(p[0] + NX[face], p[1] + NY[face], p[2] + NZ[face]))) {
                        addTexturedFace(t, p[0], p[1], p[2], block, meta, face);
                        if (++count % BATCH_SIZE == 0) {
                            t.draw();
                            t.startDrawingQuads();
                        }
                    }
                }
            }
            t.draw();

            GL11.glDisable(GL11.GL_TEXTURE_2D);
        } else {
            GL11.glColor4f(cr, cg, cb, 0.80f + 0.20f * pulse);
            t.startDrawingQuads();
            int count = 0;
            for (int[] p : blocks) {
                for (int face = 0; face < 6; face++) {
                    if (!set.contains(lPack(p[0] + NX[face], p[1] + NY[face], p[2] + NZ[face]))) {
                        addSingleFace(t, p[0], p[1], p[2], face);
                        if (++count % BATCH_SIZE == 0) {
                            t.draw();
                            t.startDrawingQuads();
                        }
                    }
                }
            }
            t.draw();
        }

        // Exterior wireframe — edges where the surface has a crease (not flat)
        GL11.glColor4f(
            textured ? 1.0f : cr * 0.9f,
            textured ? 1.0f : cg * 0.9f,
            textured ? 1.0f : cb * 0.9f,
            0.75f + pulse * 0.15f);
        drawGhostExteriorWireframe(t, blocks);
    }

    private void renderBBoxFallback(int w, int h, int d, float cr, float cg, float cb, float pulse) {
        Tessellator t = Tessellator.instance;

        GL11.glColor4f(cr, cg, cb, 0.5f + 0.4f * pulse);
        t.startDrawingQuads();
        addBoxFaces(t, 0, 0, 0, w, h, d);
        t.draw();

        GL11.glColor4f(cr, cg, cb, 0.80f + pulse * 0.12f);
        WorldLines.drawBox(0, 0, 0, w, h, d, WorldLines.W_SEL);
    }

    // ── Exterior wireframe for ghost block list ────────────────────────────────

    /**
     * Draws a wireframe that envelops the exterior surface of the given block list.
     * Only crease edges (where two non-coplanar exterior faces meet) are drawn —
     * flat interior surface edges are omitted, matching Axiom's outline style.
     * Caches the computed edge list by list reference; recomputes when the list changes.
     */
    void drawGhostExteriorWireframe(Tessellator t, List<int[]> blocks) {
        if (blocks == null || blocks.isEmpty()) return;

        if (blocks != wireCachedBlocks) {
            wireCache = computeLocalWireframe(blocks);
            wireCachedBlocks = blocks;
        }

        drawWireframeCache(t, wireCache);
    }

    float[] computeLocalWireframe(List<int[]> blocks) {
        // Build block lookup set using local coord packing (coords assumed < ±4096)
        Set<Long> set = new HashSet<>(blocks.size() * 2);
        for (int[] p : blocks) set.add(lPack(p[0], p[1], p[2]));

        // For each exterior face, register its 4 edges with the face's plane-group bit.
        // An edge is a "crease" (should be drawn) iff it borders faces from 2+ plane groups.
        HashMap<Long, Integer> edgeMask = new HashMap<>(blocks.size() * 4);
        for (int[] p : blocks) {
            int bx = p[0], by = p[1], bz = p[2];
            for (int face = 0; face < 6; face++) {
                if (set.contains(lPack(bx + NX[face], by + NY[face], bz + NZ[face]))) continue;
                int axisBit = FACE_AXIS_BIT[face];
                for (int[] e : FACE_EDGES[face]) {
                    long ek = lEdgeKey(e[0], bx + e[1], by + e[2], bz + e[3]);
                    Integer prev = edgeMask.get(ek);
                    edgeMask.put(ek, prev == null ? axisBit : prev | axisBit);
                }
            }
        }

        return buildVertsFromEdgeMask(edgeMask);
    }

    /** Draws pre-computed wireframe vertices (x1,y1,z1,x2,y2,z2,...) as billboard quads. */
    static void drawWireframeCache(Tessellator t, float[] verts) {
        WorldLines.drawWireframeCache(t, verts, WorldLines.W_THIN);
    }

    // ── Geometry helpers ──────────────────────────────────────────────────────

    /** Render one textured block cube. Package-private so SelectionRenderer can reuse. */
    static void addTexturedCubeFaces(Tessellator t, int x, int y, int z, Block block, int meta) {
        addTexturedCubeFaces(t, x, y, z, block, meta, 0xFFFFFF);
    }

    /** World-aware variant: applies block.colorMultiplier for biome-tinted blocks (e.g. grass, leaves). */
    static void addTexturedCubeFaces(Tessellator t, int x, int y, int z, Block block, int meta,
        net.minecraft.world.IBlockAccess world) {
        int tint = 0xFFFFFF;
        try {
            tint = block.colorMultiplier(world, x, y, z);
        } catch (Exception ignored) {}
        addTexturedCubeFaces(t, x, y, z, block, meta, tint);
    }

    private static void addTexturedCubeFaces(Tessellator t, int x, int y, int z, Block block, int meta, int tintRGB) {
        float x1 = x, y1 = y, z1 = z, x2 = x + 1f, y2 = y + 1f, z2 = z + 1f;
        float tr = ((tintRGB >> 16) & 0xFF) / 255f;
        float tg = ((tintRGB >> 8) & 0xFF) / 255f;
        float tb = (tintRGB & 0xFF) / 255f;

        // -Y bottom
        addFace(t, block, meta, 0, tr, tg, tb, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1);
        // +Y top
        addFace(t, block, meta, 1, tr, tg, tb, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2);
        // -Z north
        addFace(t, block, meta, 2, tr, tg, tb, x2, y2, z1, x1, y2, z1, x1, y1, z1, x2, y1, z1);
        // +Z south
        addFace(t, block, meta, 3, tr, tg, tb, x1, y2, z2, x2, y2, z2, x2, y1, z2, x1, y1, z2);
        // -X west
        addFace(t, block, meta, 4, tr, tg, tb, x1, y2, z1, x1, y2, z2, x1, y1, z2, x1, y1, z1);
        // +X east
        addFace(t, block, meta, 5, tr, tg, tb, x2, y2, z2, x2, y2, z1, x2, y1, z1, x2, y1, z2);
    }

    /**
     * Add one textured face (face index matches NX/NY/NZ: 0=+X,1=-X,2=+Y,3=-Y,4=+Z,5=-Z)
     * of the block at (x,y,z). Uses white tint (1,1,1). For exterior-face-only textured passes.
     */
    static void addTexturedFace(Tessellator t, int x, int y, int z, Block block, int meta, int face) {
        addTexturedFace(t, x, y, z, block, meta, face, 0xFFFFFF);
    }

    static void addTexturedFace(Tessellator t, int x, int y, int z, Block block, int meta, int face, int tintRGB) {
        float tr = ((tintRGB >> 16) & 0xFF) / 255f;
        float tg = ((tintRGB >> 8) & 0xFF) / 255f;
        float tb = (tintRGB & 0xFF) / 255f;
        float x1 = x, y1 = y, z1 = z, x2 = x + 1f, y2 = y + 1f, z2 = z + 1f;
        switch (face) {
            case 0:
                addFace(t, block, meta, 5, tr, tg, tb, x2, y2, z2, x2, y2, z1, x2, y1, z1, x2, y1, z2);
                break;
            case 1:
                addFace(t, block, meta, 4, tr, tg, tb, x1, y2, z1, x1, y2, z2, x1, y1, z2, x1, y1, z1);
                break;
            case 2:
                addFace(t, block, meta, 1, tr, tg, tb, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2);
                break;
            case 3:
                addFace(t, block, meta, 0, tr, tg, tb, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1);
                break;
            case 4:
                addFace(t, block, meta, 3, tr, tg, tb, x1, y2, z2, x2, y2, z2, x2, y1, z2, x1, y1, z2);
                break;
            case 5:
                addFace(t, block, meta, 2, tr, tg, tb, x2, y2, z1, x1, y2, z1, x1, y1, z1, x2, y1, z1);
                break;
            default:
                break;
        }
    }

    /** Emit one textured quad using the block's icon for the given side. Skips if icon is null. */
    static void addFace(Tessellator t, Block block, int meta, int side, float tr, float tg, float tb, float ax,
        float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
        IIcon icon;
        try {
            icon = block.getIcon(side, meta);
        } catch (Exception e) {
            return;
        }
        if (icon == null) return;

        double u1 = icon.getMinU(), u2 = icon.getMaxU();
        double v1 = icon.getMinV(), v2 = icon.getMaxV();
        // Per-vertex color tint — tessellator encodes it in the vertex buffer.
        t.setColorRGBA_F(tr, tg, tb, 1.0f);
        t.addVertexWithUV(ax, ay, az, u1, v1);
        t.setColorRGBA_F(tr, tg, tb, 1.0f);
        t.addVertexWithUV(bx, by, bz, u2, v1);
        t.setColorRGBA_F(tr, tg, tb, 1.0f);
        t.addVertexWithUV(cx, cy, cz, u2, v2);
        t.setColorRGBA_F(tr, tg, tb, 1.0f);
        t.addVertexWithUV(dx, dy, dz, u1, v2);
    }

    private void addCubeFaces(Tessellator t, int x, int y, int z) {
        addBoxFaces(t, x, y, z, x + 1, y + 1, z + 1);
    }

    static float[] creaseWireframeFromSet(HashSet<Long> set) {
        HashMap<Long, Integer> edgeMask = new HashMap<>(set.size() * 4);
        for (long pk : set) {
            int bx = (int) ((pk >> 26) & 0x1FFF) - 4096;
            int by = (int) ((pk >> 13) & 0x1FFF) - 4096;
            int bz = (int) (pk & 0x1FFF) - 4096;
            for (int face = 0; face < 6; face++) {
                if (set.contains(SelectionRenderer.lPack(bx + NX[face], by + NY[face], bz + NZ[face]))) continue;
                int axisBit = FACE_AXIS_BIT[face];
                for (int[] e : FACE_EDGES[face]) {
                    long ek = ((long) e[0] << 39) | SelectionRenderer.lPack(bx + e[1], by + e[2], bz + e[3]);
                    Integer prev = edgeMask.get(ek);
                    edgeMask.put(ek, prev == null ? axisBit : prev | axisBit);
                }
            }
        }
        return buildVertsFromEdgeMask(edgeMask);
    }

    private static float[] buildVertsFromEdgeMask(HashMap<Long, Integer> edgeMask) {
        int creaseCount = 0;
        for (int mask : edgeMask.values()) if (Integer.bitCount(mask) > 1) creaseCount++;
        if (creaseCount == 0) return new float[0];
        float[] verts = new float[creaseCount * 6];
        int vi = 0;
        for (Map.Entry<Long, Integer> entry : edgeMask.entrySet()) {
            if (Integer.bitCount(entry.getValue()) <= 1) continue;
            long ek = entry.getKey();
            int axis = (int) (ek >> 39) & 3;
            int ex = (int) ((ek >> 26) & 0x1FFF) - 4096;
            int ey = (int) ((ek >> 13) & 0x1FFF) - 4096;
            int ez = (int) (ek & 0x1FFF) - 4096;
            verts[vi++] = ex;
            verts[vi++] = ey;
            verts[vi++] = ez;
            verts[vi++] = ex + (axis == 0 ? 1 : 0);
            verts[vi++] = ey + (axis == 1 ? 1 : 0);
            verts[vi++] = ez + (axis == 2 ? 1 : 0);
        }
        return verts;
    }

    static void addBoxFaces(Tessellator t, float x1, float y1, float z1, float x2, float y2, float z2) {
        t.addVertex(x1, y1, z1);
        t.addVertex(x1, y1, z2);
        t.addVertex(x2, y1, z2);
        t.addVertex(x2, y1, z1);
        t.addVertex(x1, y2, z1);
        t.addVertex(x2, y2, z1);
        t.addVertex(x2, y2, z2);
        t.addVertex(x1, y2, z2);
        t.addVertex(x1, y1, z1);
        t.addVertex(x2, y1, z1);
        t.addVertex(x2, y2, z1);
        t.addVertex(x1, y2, z1);
        t.addVertex(x1, y1, z2);
        t.addVertex(x1, y2, z2);
        t.addVertex(x2, y2, z2);
        t.addVertex(x2, y1, z2);
        t.addVertex(x1, y1, z1);
        t.addVertex(x1, y2, z1);
        t.addVertex(x1, y2, z2);
        t.addVertex(x1, y1, z2);
        t.addVertex(x2, y1, z1);
        t.addVertex(x2, y1, z2);
        t.addVertex(x2, y2, z2);
        t.addVertex(x2, y2, z1);
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
    static void addSingleFace(Tessellator t, int x, int y, int z, int face, float expand) {
        float x1 = x - expand, y1 = y - expand, z1 = z - expand;
        float x2 = x + 1 + expand, y2 = y + 1 + expand, z2 = z + 1 + expand;
        switch (face) {
            case 0: {
                float fx = x + 1 + expand;
                t.addVertex(fx, y1, z1);
                t.addVertex(fx, y1, z2);
                t.addVertex(fx, y2, z2);
                t.addVertex(fx, y2, z1);
                break;
            }
            case 1: {
                float fx = x - expand;
                t.addVertex(fx, y1, z1);
                t.addVertex(fx, y2, z1);
                t.addVertex(fx, y2, z2);
                t.addVertex(fx, y1, z2);
                break;
            }
            case 2: {
                float fy = y + 1 + expand;
                t.addVertex(x1, fy, z1);
                t.addVertex(x2, fy, z1);
                t.addVertex(x2, fy, z2);
                t.addVertex(x1, fy, z2);
                break;
            }
            case 3: {
                float fy = y - expand;
                t.addVertex(x1, fy, z1);
                t.addVertex(x1, fy, z2);
                t.addVertex(x2, fy, z2);
                t.addVertex(x2, fy, z1);
                break;
            }
            case 4: {
                float fz = z + 1 + expand;
                t.addVertex(x1, y1, fz);
                t.addVertex(x1, y2, fz);
                t.addVertex(x2, y2, fz);
                t.addVertex(x2, y1, fz);
                break;
            }
            case 5: {
                float fz = z - expand;
                t.addVertex(x1, y1, fz);
                t.addVertex(x2, y1, fz);
                t.addVertex(x2, y2, fz);
                t.addVertex(x1, y2, fz);
                break;
            }
        }
    }

    static void addSingleFace(Tessellator t, int x, int y, int z, int face) {
        addSingleFace(t, x, y, z, face, 0f);
    }

    static void addBoxEdges(Tessellator t, float x1, float y1, float z1, float x2, float y2, float z2) {
        t.addVertex(x1, y1, z1);
        t.addVertex(x2, y1, z1);
        t.addVertex(x2, y1, z1);
        t.addVertex(x2, y1, z2);
        t.addVertex(x2, y1, z2);
        t.addVertex(x1, y1, z2);
        t.addVertex(x1, y1, z2);
        t.addVertex(x1, y1, z1);
        t.addVertex(x1, y2, z1);
        t.addVertex(x2, y2, z1);
        t.addVertex(x2, y2, z1);
        t.addVertex(x2, y2, z2);
        t.addVertex(x2, y2, z2);
        t.addVertex(x1, y2, z2);
        t.addVertex(x1, y2, z2);
        t.addVertex(x1, y2, z1);
        t.addVertex(x1, y1, z1);
        t.addVertex(x1, y2, z1);
        t.addVertex(x2, y1, z1);
        t.addVertex(x2, y2, z1);
        t.addVertex(x2, y1, z2);
        t.addVertex(x2, y2, z2);
        t.addVertex(x1, y1, z2);
        t.addVertex(x1, y2, z2);
    }

    // ── Edge/block packing (local small-coord space, ±4096) ───────────────────

    private long lPack(int x, int y, int z) {
        return ((long) (x + 4096) << 26) | ((long) (y + 4096) << 13) | (z + 4096);
    }

    private long lEdgeKey(int axis, int x, int y, int z) {
        return ((long) axis << 39) | ((long) (x + 4096) << 26) | ((long) (y + 4096) << 13) | (z + 4096);
    }

    @FunctionalInterface
    public interface BlockFilter {

        boolean accept(int[] bm);
    }

    /** Renders exterior faces (untextured, batched) for entries matching filter. Caller sets GL color first. */
    static void drawExteriorFacesSingleColor(Tessellator t, Map<Long, int[]> proposed, BlockFilter filter) {
        int batched = 0;
        t.startDrawingQuads();
        for (Map.Entry<Long, int[]> e : proposed.entrySet()) {
            if (filter != null && !filter.accept(e.getValue())) continue;
            long key = e.getKey();
            int bx = ChangeProposal.unpackX(key), by = ChangeProposal.unpackY(key), bz = ChangeProposal.unpackZ(key);
            for (int face = 0; face < 6; face++) {
                long nk = ChangeProposal.packKey(bx + NX[face], by + NY[face], bz + NZ[face]);
                if (!proposed.containsKey(nk)) {
                    addSingleFace(t, bx, by, bz, face);
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
