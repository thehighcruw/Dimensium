/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.path;

import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.util.BlockUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.item.ItemStack;

public class PathMath {

    public static List<int[]> computePathBlocks(PathToolState state, ItemStack activeBlock) {
        List<PathToolState.PathPoint> pts = state.points;
        if (pts.size() < 2) return new ArrayList<>();

        Map<Long, int[]> out = new HashMap<>();

        if (state.curveType == PathToolState.CurveType.CATMULL_ROM) {
            List<SplinePoint> all = densify(catmullRomAll(pts, state.looped));
            applySplinePositions(state, activeBlock, out, all, pts);
        } else if (state.curveType == PathToolState.CurveType.BEZIER) {
            List<SplinePoint> all = densify(bezierAll(pts, state.looped));
            applySplinePositions(state, activeBlock, out, all, pts);
        } else {
            int segCount = state.looped ? pts.size() : pts.size() - 1;
            for (int seg = 0; seg < segCount; seg++) {
                PathToolState.PathPoint a = pts.get(seg);
                PathToolState.PathPoint b = pts.get((seg + 1) % pts.size());

                List<SplinePoint> centerline =
                        switch (state.curveType) {
                            case BRESENHAM, CATMULL_ROM, BEZIER -> bresenhamSegment(a, b);
                            case DDA -> densify(ddaSegment(a, b));
                            case CATENARY -> densify(catenarySegment(a, b, state.catenarySlack));
                        };

                for (SplinePoint pos : centerline) {
                    int cx = blockCoord(pos.x(), state.curveType);
                    int cy = blockCoord(pos.y(), state.curveType);
                    int cz = blockCoord(pos.z(), state.curveType);
                    int r = Math.round((float) (a.radius * (1 - pos.t()) + b.radius * pos.t()));
                    int[] bm = resolveBlock(state, activeBlock, seg, pos.t(), cx, cy, cz, a, b);
                    if (bm != null) addSphere(out, cx, cy, cz, r, bm[0], bm[1]);
                }
            }
        }

        List<int[]> result = new ArrayList<>(out.size());
        for (Map.Entry<Long, int[]> e : out.entrySet()) {
            long key = e.getKey();
            int[] bm = e.getValue();
            result.add(new int[] {
                ChangeProposal.unpackX(key), ChangeProposal.unpackY(key), ChangeProposal.unpackZ(key), bm[0], bm[1]
            });
        }
        return result;
    }

    private static void applySplinePositions(
            PathToolState state,
            ItemStack activeBlock,
            Map<Long, int[]> out,
            List<SplinePoint> positions,
            List<PathToolState.PathPoint> pts) {
        if (positions.isEmpty()) return;
        double totalArc = 0;
        for (int i = 1; i < positions.size(); i++) {
            totalArc += positions.get(i - 1).pos().minus(positions.get(i).pos()).length();
        }

        double arcSoFar = 0;
        for (int i = 0; i < positions.size(); i++) {
            SplinePoint pos = positions.get(i);
            if (i > 0) arcSoFar += positions.get(i - 1).pos().minus(pos.pos()).length();
            double globalT = totalArc > 0 ? arcSoFar / totalArc : 0;

            // Find segment
            int segIdx = 0;
            double segT = 0;
            if (pts.size() > 1) {
                double segLen = 1.0 / (pts.size() - 1);
                segIdx = Math.min((int) (globalT / segLen), pts.size() - 2);
                segT = Math.max(0, Math.min(1, (globalT - segIdx * segLen) / segLen));
            }

            PathToolState.PathPoint pa = pts.get(segIdx);
            PathToolState.PathPoint pb = pts.get(Math.min(segIdx + 1, pts.size() - 1));
            int wpx = (int) Math.round(pos.x()), wpy = (int) Math.round(pos.y()), wpz = (int) Math.round(pos.z());
            int[] bm = resolveBlock(state, activeBlock, segIdx, segT, wpx, wpy, wpz, pa, pb);
            int r = Math.round((float) (pa.radius * (1 - segT) + pb.radius * segT));
            if (bm != null) addSphere(out, wpx, wpy, wpz, r, bm[0], bm[1]);
        }
    }

    /** Returns {blockId, meta} or null if no valid block is available (skip placement). */
    private static int[] resolveBlock(
            PathToolState state,
            ItemStack activeBlock,
            int segIdx,
            double t,
            int wx,
            int wy,
            int wz,
            PathToolState.PathPoint a,
            PathToolState.PathPoint b) {
        if (!state.hasMultipleBlocks()) {
            return BlockUtils.blockToIdMeta(a.block != null ? a.block : activeBlock);
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
        return BlockUtils.blockToIdMeta(
                adjT < 0.5 ? (a.block != null ? a.block : activeBlock) : (b.block != null ? b.block : activeBlock));
    }

    public static double voxelHash(int x, int y, int z, long seed) {
        long h = seed ^ (x * 2654435761L) ^ (y * 805459861L) ^ (z * 3266489917L);
        h = (h ^ (h >>> 30)) * 0xbf58476d1ce4e5b9L;
        h = (h ^ (h >>> 27)) * 0x94d049bb133111ebL;
        h = h ^ (h >>> 31);
        return ((h & 0xFFFFFFL) / (double) 0x1000000L) - 0.5;
    }

    static List<SplinePoint> bresenhamSegment(PathToolState.PathPoint a, PathToolState.PathPoint b) {
        List<SplinePoint> result = new ArrayList<>();
        int x0 = a.pos.x(), y0 = a.pos.y(), z0 = a.pos.z();
        int x1 = b.pos.x(), y1 = b.pos.y(), z1 = b.pos.z();
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int totalSteps = Math.max(dx, Math.max(dy, dz));
        if (totalSteps == 0) {
            result.add(SplinePoint.of(x0, y0, z0, 0));
            return result;
        }

        int err1, err2;
        int x = x0, y = y0, z = z0;
        int step = 0;
        result.add(SplinePoint.of(x, y, z, 0.0));

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
                result.add(SplinePoint.of(x, y, z, (double) ++step / totalSteps));
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
                result.add(SplinePoint.of(x, y, z, (double) ++step / totalSteps));
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
                result.add(SplinePoint.of(x, y, z, (double) ++step / totalSteps));
            }
        }
        return result;
    }

    static List<SplinePoint> ddaSegment(PathToolState.PathPoint a, PathToolState.PathPoint b) {
        List<SplinePoint> result = new ArrayList<>();
        Vec3DDouble delta = b.pos.minus(a.pos).toDouble();
        int steps = (int) Math.max(Math.abs(delta.x()), Math.max(Math.abs(delta.y()), Math.abs(delta.z())));
        if (steps == 0) {
            result.add(SplinePoint.of(a.pos.x(), a.pos.y(), a.pos.z(), 0));
            return result;
        }
        Vec3DDouble origin = a.pos.toDouble();
        Vec3DDouble inc = delta.divide(steps);
        for (int i = 0; i <= steps; i++) {
            Vec3DDouble p = origin.plus(inc.times(i));
            result.add(SplinePoint.of(p.x(), p.y(), p.z(), (double) i / steps));
        }
        return result;
    }

    static List<SplinePoint> catenarySegment(PathToolState.PathPoint a, PathToolState.PathPoint b, float slack) {
        Vec3DDouble delta = b.pos.minus(a.pos).toDouble();
        Vec3DDouble origin = a.pos.toDouble();
        double dHoriz = Vec3DDouble.from(delta.x(), 0, delta.z()).length();
        // Estimate arc length by sampling densely first
        int preSamples = 64;
        double arcLen = 0;
        SplinePoint prev = null;
        for (int i = 0; i <= preSamples; i++) {
            double t = (double) i / preSamples;
            Vec3DDouble base = origin.plus(delta.times(t));
            SplinePoint p = SplinePoint.of(base.x(), base.y() - 4.0 * slack * dHoriz * t * (1 - t), base.z(), t);
            if (prev != null) arcLen += prev.pos().minus(p.pos()).length();
            prev = p;
        }
        int steps = Math.max(1, (int) Math.ceil(arcLen));
        List<SplinePoint> result = new ArrayList<>(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3DDouble base = origin.plus(delta.times(t));
            double sagY = -4.0 * slack * dHoriz * t * (1 - t);
            result.add(SplinePoint.of(base.x(), base.y() + sagY, base.z(), t));
        }
        return result;
    }

    static List<SplinePoint> catmullRomAll(List<PathToolState.PathPoint> pts, boolean looped) {
        List<SplinePoint> result = new ArrayList<>();
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
            Vec3DDouble v0 = p0.pos.toDouble(), v1 = p1.pos.toDouble(), v2 = p2.pos.toDouble(), v3 = p3.pos.toDouble();
            double segLen = v2.minus(v1).length();
            int samples = Math.max(2, (int) (segLen * 2 + 1));
            for (int i = 0; i <= samples; i++) {
                double t = (double) i / samples;
                double t2 = t * t, t3 = t2 * t;
                Vec3DDouble b = v1.times(2)
                        .plus(v2.minus(v0).times(t))
                        .plus(v0.times(2)
                                .minus(v1.times(5))
                                .plus(v2.times(4))
                                .minus(v3)
                                .times(t2))
                        .plus(v0.negate()
                                .plus(v1.times(3))
                                .minus(v2.times(3))
                                .plus(v3)
                                .times(t3))
                        .times(0.5);
                if (i > 0 || seg == 0) result.add(SplinePoint.of(b.x(), b.y(), b.z(), 0));
            }
        }
        return result;
    }

    static List<SplinePoint> bezierAll(List<PathToolState.PathPoint> pts, boolean looped) {
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
            polyLen += ctrl.get(i).pos.minus(ctrl.get(i - 1).pos).toDouble().length();
        }
        int samples = Math.max(m, (int) (polyLen * 2 + 1));

        // de Casteljau work buffer
        Vec3DDouble[] work = new Vec3DDouble[m];
        List<SplinePoint> result = new ArrayList<>(samples);
        for (int si = 0; si < samples; si++) {
            double t = (double) si / (samples - 1);
            for (int j = 0; j < m; j++) {
                work[j] = ctrl.get(j).pos.toDouble();
            }
            for (int r = 1; r < m; r++) {
                for (int j = 0; j < m - r; j++) {
                    work[j] = work[j].lerp(work[j + 1], t);
                }
            }
            result.add(SplinePoint.of(work[0].x(), work[0].y(), work[0].z(), t));
        }
        return result;
    }

    /** Bresenham generates exact integers → floor. All other (smooth) curves → round to nearest block. */
    private static int blockCoord(double v, PathToolState.CurveType type) {
        return type == PathToolState.CurveType.BRESENHAM ? (int) Math.floor(v) : (int) Math.round(v);
    }

    /** Inserts linear interpolants so no consecutive pair is more than 1 block apart in 3D. */
    static List<SplinePoint> densify(List<SplinePoint> raw) {
        if (raw.size() < 2) return raw;
        List<SplinePoint> result = new ArrayList<>(raw.size() * 2);
        result.add(raw.get(0));
        for (int i = 1; i < raw.size(); i++) {
            SplinePoint a = raw.get(i - 1), b = raw.get(i);
            Vec3DDouble delta = b.pos().minus(a.pos());
            double dist = delta.length();
            if (dist > 1.0) {
                int extra = (int) Math.ceil(dist);
                for (int j = 1; j <= extra; j++) {
                    double ft = (double) j / extra;
                    result.add(SplinePoint.of(
                            a.x() + delta.x() * ft,
                            a.y() + delta.y() * ft,
                            a.z() + delta.z() * ft,
                            a.t() + (b.t() - a.t()) * ft));
                }
            } else {
                result.add(b);
            }
        }
        return result;
    }

    static void addSphere(Map<Long, int[]> out, int cx, int cy, int cz, int radius, int blockId, int meta) {
        if (radius <= 0) {
            out.put(ChangeProposal.packKey(cx, cy, cz), new int[] {blockId, meta});
            return;
        }
        int[] bm = new int[] {blockId, meta};
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
