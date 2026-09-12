package github.thehighcruw.dimensium.tool.state;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.render.world.RotationGizmo;
import github.thehighcruw.dimensium.render.world.ScaleGizmo;
import github.thehighcruw.dimensium.render.world.TranslationGizmo;
import github.thehighcruw.dimensium.render.world.ViewPlaneGizmo;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.math.ShapeMath;

/**
 * Client-side state for interactive shape placement.
 * Tracks anchor position, caches ghost block positions, and owns the gizmos.
 */
public class ShapePlacementState {

    public static final ShapePlacementState INSTANCE = new ShapePlacementState();

    public boolean active = false;
    public int anchorX, anchorY, anchorZ;
    /** Sub-block float anchor used for rendering and gizmo positioning. */
    public float anchorFX, anchorFY, anchorFZ;
    /** Pre-rotation bounding box (shape-type adjusted). */
    public int baseW, baseH, baseD;

    /** Rotation angles in degrees around each axis, applied X→Y→Z. Free-angle (not snapped). */
    public float rotX = 0f, rotY = 0f, rotZ = 0f;
    /** Rotation snapshot when a rotation drag began. */
    public float rotDragBaseX, rotDragBaseY, rotDragBaseZ;

    /**
     * Pre-computed block offset positions (already rotation-applied).
     * Offsets may be negative (blocks extend before anchor when rotated).
     * Null = exceeds maxGhostBlocks, use bbox fallback.
     */
    public List<int[]> ghostBlocks = null;
    public ChangeProposal preview = null;

    /** Per-axis scale multipliers applied on top of the tool-state dimensions. */
    public float scaleX = 1f, scaleY = 1f, scaleZ = 1f;

    public final TranslationGizmo gizmo = new TranslationGizmo();
    public final RotationGizmo rotGizmo = new RotationGizmo();
    public final ScaleGizmo scaleGizmo = new ScaleGizmo();
    public final ViewPlaneGizmo viewPlaneGizmo = new ViewPlaneGizmo();

    private String shapeKey = "";

    public void start(int x, int y, int z) {
        active = true;
        anchorX = x;
        anchorY = y;
        anchorZ = z;
        anchorFX = x;
        anchorFY = y;
        anchorFZ = z;
        rotX = 0f;
        rotY = 0f;
        rotZ = 0f;
        scaleX = 1f;
        scaleY = 1f;
        scaleZ = 1f;
        gizmo.reset();
        rotGizmo.reset();
        scaleGizmo.reset();
        viewPlaneGizmo.reset();
        shapeKey = "";
        ghostBlocks = null;
        rebuildIfNeeded();
    }

    public void cancel() {
        active = false;
        preview = null;
        gizmo.reset();
        rotGizmo.reset();
        scaleGizmo.reset();
        viewPlaneGizmo.reset();
    }

    /** Force ghost blocks to be rebuilt on next rebuildIfNeeded call. */
    public void invalidateGhost() {
        shapeKey = "";
        ghostBlocks = null;
    }

    /** Rotation center in world space — unchanged by rotation, always the base bbox center. */
    public double centerX() {
        return anchorFX + baseW / 2.0;
    }

    public double centerY() {
        return anchorFY + baseH / 2.0;
    }

    public double centerZ() {
        return anchorFZ + baseD / 2.0;
    }

    /**
     * Recomputes effectiveW/H/D and ghostBlocks when ToolState shape params or rotation change.
     * No-op if nothing changed since last call.
     */
    public void rebuildIfNeeded() {
        ShapeToolState s = ShapeToolState.INSTANCE;
        String key = buildKey(s);
        if (key.equals(shapeKey) && ghostBlocks != null) return;
        shapeKey = key;

        int w = Math.max(1, Math.round(s.shapeWidth * scaleX));
        int h = Math.max(1, Math.round(s.shapeHeight * scaleY));
        int d = Math.max(1, Math.round(s.shapeDepth * scaleZ));
        if (s.shapeType == ShapeToolState.ShapeType.TORUS) {
            int outerX = s.torusRingRadius + s.torusTubeRadius;
            int outerZ = s.torusRingRadiusZ + s.torusTubeRadius;
            w = outerX * 2 + 1;
            h = s.torusTubeRadius * 2 + 1;
            d = outerZ * 2 + 1;
        } else if (s.shapeType == ShapeToolState.ShapeType.ARCHIMEDEAN_SPIRAL) {
            int r = (int) Math.ceil(s.shapeSpiralSpacing * s.shapeSpiralTurns);
            w = r * 2 + 1;
            h = 1;
            d = r * 2 + 1;
        } else if (!s.shapeSeparateAxes
            && (s.shapeType == ShapeToolState.ShapeType.CYLINDER || s.shapeType == ShapeToolState.ShapeType.CONE
                || s.shapeType == ShapeToolState.ShapeType.TUBE)) {
                    d = w;
                }
        baseW = w;
        baseH = h;
        baseD = d;

        float[] R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);

        // Compute AABB of the rotated base bounding box corners.
        float ccx = w / 2f, ccy = h / 2f, ccz = d / 2f;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int mask = 0; mask < 8; mask++) {
            float hx = ((mask & 1) != 0 ? w : 0) - ccx;
            float hy = ((mask & 2) != 0 ? h : 0) - ccy;
            float hz = ((mask & 4) != 0 ? d : 0) - ccz;
            float wx = R[0] * hx + R[1] * hy + R[2] * hz + ccx;
            float wy = R[3] * hx + R[4] * hy + R[5] * hz + ccy;
            float wz = R[6] * hx + R[7] * hy + R[8] * hz + ccz;
            if (wx < minX) minX = wx;
            if (wx > maxX) maxX = wx;
            if (wy < minY) minY = wy;
            if (wy > maxY) maxY = wy;
            if (wz < minZ) minZ = wz;
            if (wz > maxZ) maxZ = wz;
        }
        int ix0 = (int) Math.floor(minX), iy0 = (int) Math.floor(minY), iz0 = (int) Math.floor(minZ);
        int ix1 = (int) Math.ceil(maxX), iy1 = (int) Math.ceil(maxY), iz1 = (int) Math.ceil(maxZ);

        ghostBlocks = buildGhostBlocks(s, w, h, d, R, ccx, ccy, ccz, ix0, iy0, iz0, ix1, iy1, iz1);
        rebuildShapeProposal(s);
    }

    private void rebuildShapeProposal(ShapeToolState s) {
        if (ghostBlocks == null || ghostBlocks.isEmpty()) {
            preview = null;
            return;
        }
        Block blk = null;
        int meta = 0;
        SelectedBlockState sbs = SelectedBlockState.INSTANCE;
        ItemStack sel = sbs.selectedBlock;
        if (sel != null) {
            Block b = Block.getBlockFromItem(sel.getItem());
            if (b != null && b != Blocks.air) {
                blk = b;
                meta = sel.getItemDamage();
            }
        }
        if (blk == null) blk = sbs.getPaintBlock();
        if (blk == null) {
            preview = null;
            return;
        }

        int blockId = Block.getIdFromBlock(blk);
        ChangeProposal p = ChangeProposal.forPreview();
        for (int[] offset : ghostBlocks) {
            long key = ChangeProposal.packKey(anchorX + offset[0], anchorY + offset[1], anchorZ + offset[2]);
            p.proposed.put(key, new int[] { blockId, meta });
        }
        preview = p;
    }

    private static List<int[]> buildGhostBlocks(ShapeToolState s, int w, int h, int d, float[] R, float ccx, float ccy,
        float ccz, int ix0, int iy0, int iz0, int ix1, int iy1, int iz1) {
        int maxGhost = DimensiumConfig.maxGhostBlocks;
        List<int[]> blocks = new ArrayList<>();

        for (int ox = ix0; ox <= ix1; ox++) {
            for (int oy = iy0; oy <= iy1; oy++) {
                for (int oz = iz0; oz <= iz1; oz++) {
                    // Sample at block center; inverse-transform (R^T) back to local space.
                    float dx0 = (ox + 0.5f) - ccx;
                    float dy0 = (oy + 0.5f) - ccy;
                    float dz0 = (oz + 0.5f) - ccz;
                    float ldx = R[0] * dx0 + R[3] * dy0 + R[6] * dz0 + ccx;
                    float ldy = R[1] * dx0 + R[4] * dy0 + R[7] * dz0 + ccy;
                    float ldz = R[2] * dx0 + R[5] * dy0 + R[8] * dz0 + ccz;

                    if (!ShapeMath.inShapeGeomF(
                        s.shapeType,
                        ldx,
                        ldy,
                        ldz,
                        w,
                        h,
                        d,
                        s.shapeHollow,
                        s.shapeExponent,
                        s.torusRingRadius,
                        s.torusRingRadiusZ,
                        s.torusTubeRadius,
                        s.tubeWallThickness,
                        s.shapeSupersphereExp,
                        s.shapePolygonSides,
                        s.shapeSpiralSpacing,
                        s.shapeSpiralTurns)) continue;

                    if (blocks.size() >= maxGhost) return null;
                    blocks.add(new int[] { ox, oy, oz });
                }
            }
        }
        return blocks;
    }

    private String buildKey(ShapeToolState s) {
        // Round angles to 0.5° to avoid rebuilding on float noise.
        int rx = Math.round(rotX * 2);
        int ry = Math.round(rotY * 2);
        int rz = Math.round(rotZ * 2);
        return s.shapeType.ordinal() + ","
            + s.shapeWidth
            + ","
            + s.shapeHeight
            + ","
            + s.shapeDepth
            + ","
            + s.shapeHollow
            + ","
            + s.torusRingRadius
            + ","
            + s.torusRingRadiusZ
            + ","
            + s.torusTubeRadius
            + ","
            + s.shapeSeparateAxes
            + ","
            + s.shapeExponent
            + ","
            + s.shapeSupersphereExp
            + ","
            + s.shapePolygonSides
            + ","
            + s.shapeSpiralSpacing
            + ","
            + s.shapeSpiralTurns
            + ","
            + s.tubeWallThickness
            + ","
            + rx
            + ","
            + ry
            + ","
            + rz
            + ","
            + anchorX
            + ","
            + anchorY
            + ","
            + anchorZ
            + ","
            + Math.round(scaleX * 100)
            + ","
            + Math.round(scaleY * 100)
            + ","
            + Math.round(scaleZ * 100);
    }
}
