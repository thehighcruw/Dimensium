/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.lasso;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportRegistry;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportState;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;

@SideOnly(Side.CLIENT)
public final class LassoComputer {

    private static final double RAY_MAX = 128.0;
    private static final int MIN_WORLD_Y = 0;
    private static final int MAX_WORLD_Y = 255;

    /**
     * Compute the set of blocks inside the screen-space lasso polygon.
     *
     * Projects each candidate block's center into screen space and tests it
     * against the polygon. Depth is counted in discrete blocks per view column
     * (screen pixel), so lassoDepth=N always means the surface block plus N
     * blocks behind it, regardless of the ray angle.
     */
    public static Set<Long> compute(
            Minecraft mc, List<float[]> polygon, int lassoDepth, boolean includeNonSolid, int sw, int sh) {
        Set<Long> result = new HashSet<>();
        if (mc.theWorld == null || mc.renderViewEntity == null || polygon.size() < 3) return result;

        EntityLivingBase eye = mc.renderViewEntity;
        Vec3DDouble eyePos = Vec3DDouble.from(eye.posX, eye.posY + eye.getEyeHeight(), eye.posZ);

        double yaw = Math.toRadians(eye.rotationYaw);
        double pitch = Math.toRadians(eye.rotationPitch);

        Vec3DDouble lookDir =
                Vec3DDouble.from(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        Vec3DDouble upDir =
                Vec3DDouble.from(-Math.sin(yaw) * Math.sin(pitch), Math.cos(pitch), Math.cos(yaw) * Math.sin(pitch));

        FreecamState fs = FreecamState.INSTANCE;
        double tanHX = fs.projTanHX;
        double tanHY = fs.projTanHY;

        // Determine viewport content center for the NDC → cursor mapping.
        // Mirrors the inverse of ViewportState.cursorToNdcX/Y, matching how
        // GuiDimensiumOverlay/TickHandler cast rays through the cursor.
        ViewportState vp = ViewportRegistry.INSTANCE.active();
        int sf = RenderUtils.scaleFactor();
        final double ndcCenterX, ndcCenterY;
        if (vp != null && vp.contentW > 1 && vp.contentH > 1) {
            ndcCenterX = (vp.contentX + vp.contentW * 0.5) / sf;
            ndcCenterY = (vp.contentY + vp.contentH * 0.5) / sf;
        } else {
            ndcCenterX = sw * 0.5;
            ndcCenterY = sh * 0.5;
        }

        int minBX = (int) Math.floor(eyePos.x() - RAY_MAX);
        int maxBX = (int) Math.ceil(eyePos.x() + RAY_MAX);
        int minBY = Math.max(MIN_WORLD_Y, (int) Math.floor(eyePos.y() - RAY_MAX));
        int maxBY = Math.min(MAX_WORLD_Y, (int) Math.ceil(eyePos.y() + RAY_MAX));
        int minBZ = (int) Math.floor(eyePos.z() - RAY_MAX);
        int maxBZ = (int) Math.ceil(eyePos.z() + RAY_MAX);

        // Map each screen-pixel column to the sorted list of (blockKey, fwd) candidates.
        // Column key = isx * sh + isy — a bijection for valid on-screen coords.
        Map<Long, List<long[]>> colCandidates = new HashMap<>();

        for (int bx = minBX; bx <= maxBX; bx++) {
            for (int bz = minBZ; bz <= maxBZ; bz++) {
                for (int by = minBY; by <= maxBY; by++) {
                    Block block = mc.theWorld.getBlock(bx, by, bz);
                    if (block == Blocks.air) continue;
                    if (!includeNonSolid && !block.isOpaqueCube()) continue;

                    Vec3DDouble delta =
                            Vec3DDouble.from(bx + 0.5 - eyePos.x(), by + 0.5 - eyePos.y(), bz + 0.5 - eyePos.z());

                    double fwd = delta.dot(lookDir);
                    if (fwd <= 0.0 || fwd > RAY_MAX) continue;

                    double right = delta.x() * rightX + delta.z() * rightZ;
                    double up = delta.dot(upDir);

                    // Inverse of cursorToNdcX/Y: ndcX = -right/(fwd*tanHX),
                    // cursorX = ndcCenterX - ndcX * sw/2
                    double ndcX = right / (fwd * tanHX);
                    double ndcY = up / (fwd * tanHY);
                    float sx = (float) (ndcCenterX - ndcX * sw * 0.5);
                    float sy = (float) (ndcCenterY - ndcY * sh * 0.5);

                    int isx = Math.round(sx);
                    int isy = Math.round(sy);
                    if (isx < 0 || isy < 0 || isx >= sw || isy >= sh) continue;

                    if (!pointInPolygon(sx, sy, polygon)) continue;

                    long colKey = (long) isx * sh + isy;
                    long blockKey = SelectionState.pack(Vec3DInt.from(bx, by, bz));

                    colCandidates
                            .computeIfAbsent(colKey, k -> new ArrayList<>())
                            .add(new long[] {blockKey, Double.doubleToRawLongBits(fwd)});
                }
            }
        }

        // Per column: sort front-to-back, keep first (lassoDepth + 1) blocks.
        for (List<long[]> list : colCandidates.values()) {
            list.sort(Comparator.comparingDouble(a -> Double.longBitsToDouble(a[1])));
            int limit = Math.min(list.size(), lassoDepth + 1);
            for (int i = 0; i < limit; i++) {
                result.add(list.get(i)[0]);
            }
        }

        return result;
    }

    private static boolean pointInPolygon(double px, double py, List<float[]> polygon) {
        int n = polygon.size();
        boolean inside = false;
        int j = n - 1;
        for (int i = 0; i < n; i++) {
            float xi = polygon.get(i)[0], yi = polygon.get(i)[1];
            float xj = polygon.get(j)[0], yj = polygon.get(j)[1];
            if (((yi > py) != (yj > py)) && (px < (double) (xj - xi) * (py - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
            j = i;
        }
        return inside;
    }

    private LassoComputer() {}
}
