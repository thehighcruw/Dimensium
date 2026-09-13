/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState;
import github.thehighcruw.dimensium.tool.ChangeProposal;

public class PathMath {

    public static List<int[]> computePathBlocks(PathToolState state, ItemStack activeBlock) {
        List<PathToolState.PathPoint> pts = state.points;
        if (pts.size() < 2) return new ArrayList<>();

        Map<Long, int[]> out = new HashMap<>();

        if (state.curveType == PathToolState.CurveType.CATMULL_ROM) {
            List<double[]> all = densify(catmullRomAll(pts, state.looped));
            applySplinePositions(state, activeBlock, out, all, pts);
        } else if (state.curveType == PathToolState.CurveType.BEZIER) {
            List<double[]> all = densify(bezierAll(pts, state.looped));
            applySplinePositions(state, activeBlock, out, all, pts);
        } else {
            int segCount = state.looped ? pts.size() : pts.size() - 1;
            for (int seg = 0; seg < segCount; seg++) {
                PathToolState.PathPoint a = pts.get(seg);
                PathToolState.PathPoint b = pts.get((seg + 1) % pts.size());

                List<double[]> centerline = switch (state.curveType) {
                    case BRESENHAM, CATMULL_ROM, BEZIER -> bresenhamSegment(a, b);
                    case DDA -> densify(ddaSegment(a, b));
                    case CATENARY -> densify(catenarySegment(a, b, state.catenarySlack));
                };

                for (double[] pos : centerline) {
                    int cx = blockCoord(pos[0], state.curveType);
                    int cy = blockCoord(pos[1], state.curveType);
                    int cz = blockCoord(pos[2], state.curveType);
                    double t = pos[3];
                    int r = Math.round((float) (a.radius * (1 - t) + b.radius * t));
                    int[] bm = resolveBlock(state, activeBlock, seg, t, cx, cy, cz, a, b);
                    if (bm != null) addSphere(out, cx, cy, cz, r, bm[0], bm[1]);
                }
            }
        }

        List<int[]> result = new ArrayList<>(out.size());
        for (Map.Entry<Long, int[]> e : out.entrySet()) {
            long key = e.getKey();
            int[] bm = e.getValue();
            result.add(
                new int[] { ChangeProposal.unpackX(key), ChangeProposal.unpackY(key), ChangeProposal.unpackZ(key),
                    bm[0], bm[1] });
        }
        return result;
    }

    private static void applySplinePositions(PathToolState state, ItemStack activeBlock, Map<Long, int[]> out,
        List<double[]> positions, List<PathToolState.PathPoint> pts) {
        if (positions.isEmpty()) return;
        double totalArc = 0;
        for (int i = 1; i < positions.size(); i++) {
            double[] a = positions.get(i - 1), b = positions.get(i);
            double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
            totalArc += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        double arcSoFar = 0;
        for (int i = 0; i < positions.size(); i++) {
            double[] pos = positions.get(i);
            if (i > 0) {
                double[] prev = positions.get(i - 1);
                double dx = pos[0] - prev[0], dy = pos[1] - prev[1], dz = pos[2] - prev[2];
                arcSoFar += Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
            double globalT = totalArc > 0 ? arcSoFar / totalArc : 0;

            // Find segment
            int segIdx = 0;
            double segT = 0;
            if (pts.size() > 1) {
                double segLen = 1.0 / (pts.size() - 1);
                segIdx = Math.min((int) (globalT / segLen), pts.size() - 2);
                segT = (globalT - segIdx * segLen) / segLen;
                segT = Math.max(0, Math.min(1, segT));
            }

            PathToolState.PathPoint pa = pts.get(segIdx);
            PathToolState.PathPoint pb = pts.get(Math.min(segIdx + 1, pts.size() - 1));
            int wpx = (int) Math.round(pos[0]), wpy = (int) Math.round(pos[1]), wpz = (int) Math.round(pos[2]);
            int[] bm = resolveBlock(state, activeBlock, segIdx, segT, wpx, wpy, wpz, pa, pb);
            int r = Math.round((float) (pa.radius * (1 - segT) + pb.radius * segT));
            if (bm != null) addSphere(out, wpx, wpy, wpz, r, bm[0], bm[1]);
        }
    }

    /** Returns {blockId, meta} or null if no valid block is available (skip placement). */
    private static int[] resolveBlock(PathToolState state, ItemStack activeBlock, int segIdx, double t, int wx, int wy,
        int wz, PathToolState.PathPoint a, PathToolState.PathPoint b) {
        if (!state.hasMultipleBlocks()) {
            return blockToIdMeta(a.block != null ? a.block : activeBlock);
        }
        double adjT = 0.0;
        switch (state.interp) {
            case NEAREST:
                adjT = t;
                break;
            case LINEAR:
                adjT = t + voxelHash(wx, wy, wz, state.interpSeed) * 0.25;
                break;
            case BEZIER: {
                // Seeded per-segment bezier S-curve, then dither
                Random rnd = new Random(state.interpSeed ^ (segIdx * 6364136223846793005L));
                float bp1 = rnd.nextFloat(), bp2 = rnd.nextFloat();
                float tc = (float) Math.max(0, Math.min(1, t));
                float inv = 1f - tc;
                float remapped = 3f * inv * inv * tc * bp1 + 3f * inv * tc * tc * bp2 + tc * tc * tc;
                adjT = remapped + voxelHash(wx, wy, wz, state.interpSeed) * 0.25;
                break;
            }
        }
        return blockToIdMeta(
            adjT < 0.5 ? (a.block != null ? a.block : activeBlock) : (b.block != null ? b.block : activeBlock));
    }

    public static double voxelHash(int x, int y, int z, long seed) {
        long h = seed ^ (x * 2654435761L) ^ (y * 805459861L) ^ (z * 3266489917L);
        h = (h ^ (h >>> 30)) * 0xbf58476d1ce4e5b9L;
        h = (h ^ (h >>> 27)) * 0x94d049bb133111ebL;
        h = h ^ (h >>> 31);
        return ((h & 0xFFFFFFL) / (double) 0x1000000L) - 0.5;
    }

    /** Returns {blockId, meta} or null if stack is null or not a placeable block. */
    private static int[] blockToIdMeta(ItemStack stack) {
        if (stack == null) return null;
        Block blk = Block.getBlockFromItem(stack.getItem());
        if (blk == null || blk == net.minecraft.init.Blocks.air) return null;
        return new int[] { Block.getIdFromBlock(blk), stack.getItemDamage() };
    }

    static List<double[]> bresenhamSegment(PathToolState.PathPoint a, PathToolState.PathPoint b) {
        List<double[]> result = new ArrayList<>();
        int x0 = a.x, y0 = a.y, z0 = a.z;
        int x1 = b.x, y1 = b.y, z1 = b.z;
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int totalSteps = Math.max(dx, Math.max(dy, dz));
        if (totalSteps == 0) {
            result.add(new double[] { x0, y0, z0, 0 });
            return result;
        }

        int err1, err2;
        int x = x0, y = y0, z = z0;
        int step = 0;
        result.add(new double[] { x, y, z, 0.0 });

        if (dx >= dy && dx >= dz) {
            err1 = 2 * dy - dx;
            err2 = 2 * dz - dx;
            for (int i = 0; i < dx; i++) {
                x += sx;
                if (err1 > 0) {
                    y += sy;
                    err1 -= 2 * dx;
                }
                if (err2 > 0) {
                    z += sz;
                    err2 -= 2 * dx;
                }
                err1 += 2 * dy;
                err2 += 2 * dz;
                step++;
                result.add(new double[] { x, y, z, (double) step / totalSteps });
            }
        } else if (dy >= dx && dy >= dz) {
            err1 = 2 * dx - dy;
            err2 = 2 * dz - dy;
            for (int i = 0; i < dy; i++) {
                y += sy;
                if (err1 > 0) {
                    x += sx;
                    err1 -= 2 * dy;
                }
                if (err2 > 0) {
                    z += sz;
                    err2 -= 2 * dy;
                }
                err1 += 2 * dx;
                err2 += 2 * dz;
                step++;
                result.add(new double[] { x, y, z, (double) step / totalSteps });
            }
        } else {
            err1 = 2 * dx - dz;
            err2 = 2 * dy - dz;
            for (int i = 0; i < dz; i++) {
                z += sz;
                if (err1 > 0) {
                    x += sx;
                    err1 -= 2 * dz;
                }
                if (err2 > 0) {
                    y += sy;
                    err2 -= 2 * dz;
                }
                err1 += 2 * dx;
                err2 += 2 * dy;
                step++;
                result.add(new double[] { x, y, z, (double) step / totalSteps });
            }
        }
        return result;
    }

    static List<double[]> ddaSegment(PathToolState.PathPoint a, PathToolState.PathPoint b) {
        List<double[]> result = new ArrayList<>();
        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        int steps = (int) Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps == 0) {
            result.add(new double[] { a.x, a.y, a.z, 0 });
            return result;
        }
        double ix = dx / steps, iy = dy / steps, iz = dz / steps;
        for (int i = 0; i <= steps; i++) {
            result.add(new double[] { a.x + ix * i, a.y + iy * i, a.z + iz * i, (double) i / steps });
        }
        return result;
    }

    static List<double[]> catenarySegment(PathToolState.PathPoint a, PathToolState.PathPoint b, float slack) {
        double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        double dHoriz = Math.sqrt(dx * dx + dz * dz);
        // Estimate arc length by sampling densely first
        int preSamples = 64;
        double arcLen = 0;
        double[] prev = null;
        for (int i = 0; i <= preSamples; i++) {
            double t = (double) i / preSamples;
            double[] p = new double[] { a.x + dx * t, a.y + dy * t - 4.0 * slack * dHoriz * t * (1 - t), a.z + dz * t,
                t };
            if (prev != null) {
                double ex = p[0] - prev[0], ey = p[1] - prev[1], ez = p[2] - prev[2];
                arcLen += Math.sqrt(ex * ex + ey * ey + ez * ez);
            }
            prev = p;
        }
        int steps = Math.max(1, (int) Math.ceil(arcLen));
        List<double[]> result = new ArrayList<>(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double sagY = -4.0 * slack * dHoriz * t * (1 - t);
            result.add(new double[] { a.x + dx * t, a.y + dy * t + sagY, a.z + dz * t, t });
        }
        return result;
    }

    static List<double[]> catmullRomAll(List<PathToolState.PathPoint> pts, boolean looped) {
        List<double[]> result = new ArrayList<>();
        int n = pts.size();
        if (n < 2) return result;

        int segCount = looped ? n : n - 1;
        for (int seg = 0; seg < segCount; seg++) {
            PathToolState.PathPoint p0, p1, p2, p3;
            if (looped) {
                p0 = pts.get((seg - 1 + n) % n);
                p1 = pts.get(seg % n);
                p2 = pts.get((seg + 1) % n);
                p3 = pts.get((seg + 2) % n);
            } else {
                p0 = seg == 0 ? pts.get(0) : pts.get(seg - 1);
                p1 = pts.get(seg);
                p2 = pts.get(seg + 1);
                p3 = seg + 2 >= n ? pts.get(n - 1) : pts.get(seg + 2);
            }
            double dx = p2.x - p1.x, dy = p2.y - p1.y, dz = p2.z - p1.z;
            double segLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int samples = Math.max(2, (int) (segLen * 2 + 1));
            for (int i = 0; i <= samples; i++) {
                double t = (double) i / samples;
                double t2 = t * t, t3 = t2 * t;
                double bx = 0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t
                    + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2
                    + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);
                double by = 0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t
                    + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2
                    + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);
                double bz = 0.5 * ((2 * p1.z) + (-p0.z + p2.z) * t
                    + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2
                    + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3);
                if (i > 0 || seg == 0) result.add(new double[] { bx, by, bz, 0 });
            }
        }
        return result;
    }

    static List<double[]> bezierAll(List<PathToolState.PathPoint> pts, boolean looped) {
        // All points are control points for a single degree-(n-1) Bezier curve.
        // Evaluate using de Casteljau (O(n²) per sample, stable for reasonable n).
        int n = pts.size();
        if (n < 2) return new ArrayList<>();

        List<PathToolState.PathPoint> ctrl = new ArrayList<>(pts);
        if (looped) ctrl.add(pts.get(0)); // close the curve
        int m = ctrl.size();

        // Estimate control-polygon length to determine sample density
        double polyLen = 0;
        for (int i = 1; i < m; i++) {
            double ex = ctrl.get(i).x - ctrl.get(i - 1).x;
            double ey = ctrl.get(i).y - ctrl.get(i - 1).y;
            double ez = ctrl.get(i).z - ctrl.get(i - 1).z;
            polyLen += Math.sqrt(ex * ex + ey * ey + ez * ez);
        }
        int samples = Math.max(m, (int) (polyLen * 2 + 1));

        double[] work = new double[m * 3];
        List<double[]> result = new ArrayList<>(samples);
        for (int si = 0; si < samples; si++) {
            double t = (double) si / (samples - 1);
            for (int j = 0; j < m; j++) {
                work[j * 3] = ctrl.get(j).x;
                work[j * 3 + 1] = ctrl.get(j).y;
                work[j * 3 + 2] = ctrl.get(j).z;
            }
            for (int r = 1; r < m; r++) {
                for (int j = 0; j < m - r; j++) {
                    work[j * 3] = (1 - t) * work[j * 3] + t * work[(j + 1) * 3];
                    work[j * 3 + 1] = (1 - t) * work[j * 3 + 1] + t * work[(j + 1) * 3 + 1];
                    work[j * 3 + 2] = (1 - t) * work[j * 3 + 2] + t * work[(j + 1) * 3 + 2];
                }
            }
            result.add(new double[] { work[0], work[1], work[2], t });
        }
        return result;
    }

    /** Bresenham generates exact integers → floor. All other (smooth) curves → round to nearest block. */
    private static int blockCoord(double v, PathToolState.CurveType type) {
        return type == PathToolState.CurveType.BRESENHAM ? (int) Math.floor(v) : (int) Math.round(v);
    }

    /** Inserts linear interpolants so no consecutive pair is more than 1 block apart in 3D. */
    static List<double[]> densify(List<double[]> raw) {
        if (raw.size() < 2) return raw;
        List<double[]> result = new ArrayList<>(raw.size() * 2);
        result.add(raw.get(0));
        for (int i = 1; i < raw.size(); i++) {
            double[] a = raw.get(i - 1), b = raw.get(i);
            double ex = b[0] - a[0], ey = b[1] - a[1], ez = b[2] - a[2];
            double dist = Math.sqrt(ex * ex + ey * ey + ez * ez);
            if (dist > 1.0) {
                int extra = (int) Math.ceil(dist);
                for (int j = 1; j <= extra; j++) {
                    double ft = (double) j / extra;
                    result.add(
                        new double[] { a[0] + ex * ft, a[1] + ey * ft, a[2] + ez * ft, a[3] + (b[3] - a[3]) * ft });
                }
            } else {
                result.add(b);
            }
        }
        return result;
    }

    static void addSphere(Map<Long, int[]> out, int cx, int cy, int cz, int radius, int blockId, int meta) {
        if (radius <= 0) {
            out.put(ChangeProposal.packKey(cx, cy, cz), new int[] { blockId, meta });
            return;
        }
        int[] bm = new int[] { blockId, meta };
        for (int ox = -radius; ox <= radius; ox++) {
            for (int oy = -radius; oy <= radius; oy++) {
                for (int oz = -radius; oz <= radius; oz++) {
                    if (ox * ox + oy * oy + oz * oz <= radius * radius) {
                        out.put(ChangeProposal.packKey(cx + ox, cy + oy, cz + oz), bm);
                    }
                }
            }
        }
    }

    private PathMath() {}
}
