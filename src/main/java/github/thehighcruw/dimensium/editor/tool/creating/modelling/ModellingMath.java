/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.modelling;

import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingToolState.ModelPoint;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.BlockUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.item.ItemStack;

public class ModellingMath {

    public static List<int[]> computeBlocks(ModellingToolState state, ItemStack activeBlock) {
        int[] bm = BlockUtils.blockToIdMeta(activeBlock);
        if (bm == null) return Collections.emptyList();

        Map<Long, int[]> out = new HashMap<>();

        switch (state.mode) {
            case CONVEX_HULL -> computeConvexHull(out, state.allPoints(), bm);
            case SMART_SURFACE -> computeSmartSurface(out, state.allPoints(), bm);
            case TRIANGLE_STRIP -> computeTriangleStrip(out, state.allPoints(), bm);
            case TRIANGLE_FAN -> computeTriangleFan(out, state.allPoints(), bm);
            case FLAT -> computeLoftedFlat(out, state.rows, bm);
            case CATMULL_ROM -> computeLoftedCatmullRom(out, state.rows, bm);
            case BEZIER -> computeLoftedBezier(out, state.rows, bm);
        }

        List<int[]> result = new ArrayList<>(out.size());
        for (Map.Entry<Long, int[]> e : out.entrySet()) {
            long key = e.getKey();
            result.add(new int[] {
                ChangeProposal.unpackX(key),
                ChangeProposal.unpackY(key),
                ChangeProposal.unpackZ(key),
                e.getValue()[0],
                e.getValue()[1]
            });
        }
        return result;
    }

    // ── Shape modes ───────────────────────────────────────────────────────────

    private static boolean handleDegeneratePoints(Map<Long, int[]> out, List<ModelPoint> pts, int[] bm) {
        int n = pts.size();
        if (n == 0) return true;
        if (n == 1) {
            addPoint(out, pts.get(0), bm);
            return true;
        }
        if (n == 2) {
            bresenhamLine(out, pts.get(0), pts.get(1), bm);
            return true;
        }
        if (n == 3) {
            voxelizeTriangle(out, pts.get(0), pts.get(1), pts.get(2), bm);
            return true;
        }
        return false;
    }

    private static void computeConvexHull(Map<Long, int[]> out, List<ModelPoint> pts, int[] bm) {
        if (handleDegeneratePoints(out, pts, bm)) return;
        List<int[]> faces = convexHull3D(pts);
        if (faces.isEmpty()) {
            // Degenerate: fall back to all points connected as fan
            for (int i = 1; i + 1 < pts.size(); i++) voxelizeTriangle(out, pts.get(0), pts.get(i), pts.get(i + 1), bm);
        } else {
            for (int[] f : faces) voxelizeTriangle(out, pts.get(f[0]), pts.get(f[1]), pts.get(f[2]), bm);
        }
    }

    private static void computeSmartSurface(Map<Long, int[]> out, List<ModelPoint> pts, int[] bm) {
        if (handleDegeneratePoints(out, pts, bm)) return;
        int n = pts.size();

        Vec3DDouble centroid = Vec3DDouble.ZERO;
        for (ModelPoint p : pts) centroid = centroid.plus(p.pos().toDouble());
        centroid = centroid.divide(n);

        double[][] cov = new double[3][3];
        for (ModelPoint p : pts) {
            Vec3DDouble d = p.pos().toDouble().minus(centroid);
            double[] da = {d.x(), d.y(), d.z()};
            for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) cov[i][j] += da[i] * da[j];
        }

        double[][] evecs = new double[3][3];
        jacobiEigen3(cov, evecs);
        double[] u = evecs[0], v = evecs[1];

        double[][] proj = new double[n][2];
        for (int i = 0; i < n; i++) {
            Vec3DDouble delta = pts.get(i).pos().toDouble().minus(centroid);
            proj[i][0] = delta.x() * u[0] + delta.y() * u[1] + delta.z() * u[2];
            proj[i][1] = delta.x() * v[0] + delta.y() * v[1] + delta.z() * v[2];
        }

        List<int[]> tris = delaunay2D(proj, n);
        if (tris.isEmpty()) {
            computeConvexHull(out, pts, bm);
            return;
        }
        for (int[] tri : tris) voxelizeTriangle(out, pts.get(tri[0]), pts.get(tri[1]), pts.get(tri[2]), bm);
    }

    private static void jacobiEigen3(double[][] a, double[][] evecs) {
        double[][] m = new double[3][3];
        for (int i = 0; i < 3; i++) System.arraycopy(a[i], 0, m[i], 0, 3);
        double[][] v = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

        for (int iter = 0; iter < 100; iter++) {
            int p = 0, q = 1;
            double maxOff = Math.abs(m[0][1]);
            if (Math.abs(m[0][2]) > maxOff) {
                maxOff = Math.abs(m[0][2]);
                q = 2;
            }
            if (Math.abs(m[1][2]) > maxOff) {
                maxOff = Math.abs(m[1][2]);
                p = 1;
                q = 2;
            }
            if (maxOff < 1e-12) break;

            double theta = (m[q][q] - m[p][p]) / (2.0 * m[p][q]);
            double t = (theta >= 0 ? 1 : -1) / (Math.abs(theta) + Math.sqrt(1.0 + theta * theta));
            double c = 1.0 / Math.sqrt(1.0 + t * t), s = t * c;

            double mpq = m[p][q];
            m[p][p] -= t * mpq;
            m[q][q] += t * mpq;
            m[p][q] = m[q][p] = 0;
            for (int r = 0; r < 3; r++) {
                if (r == p || r == q) continue;
                double mrp = m[r][p], mrq = m[r][q];
                m[r][p] = m[p][r] = c * mrp - s * mrq;
                m[r][q] = m[q][r] = s * mrp + c * mrq;
            }
            for (int r = 0; r < 3; r++) {
                double vrp = v[r][p], vrq = v[r][q];
                v[r][p] = c * vrp - s * vrq;
                v[r][q] = s * vrp + c * vrq;
            }
        }

        double[] evals = {m[0][0], m[1][1], m[2][2]};
        int[] idx = {0, 1, 2};
        if (evals[idx[0]] < evals[idx[1]]) {
            int tmp = idx[0];
            idx[0] = idx[1];
            idx[1] = tmp;
        }
        if (evals[idx[1]] < evals[idx[2]]) {
            int tmp = idx[1];
            idx[1] = idx[2];
            idx[2] = tmp;
        }
        if (evals[idx[0]] < evals[idx[1]]) {
            int tmp = idx[0];
            idx[0] = idx[1];
            idx[1] = tmp;
        }

        for (int i = 0; i < 3; i++) for (int r = 0; r < 3; r++) evecs[i][r] = v[r][idx[i]];
    }

    private static List<int[]> delaunay2D(double[][] pts, int n) {
        double minX = pts[0][0], maxX = pts[0][0], minY = pts[0][1], maxY = pts[0][1];
        for (int i = 1; i < n; i++) {
            if (pts[i][0] < minX) minX = pts[i][0];
            if (pts[i][0] > maxX) maxX = pts[i][0];
            if (pts[i][1] < minY) minY = pts[i][1];
            if (pts[i][1] > maxY) maxY = pts[i][1];
        }
        double span = Math.max(maxX - minX, maxY - minY) * 10 + 1;
        double midX = (minX + maxX) / 2, midY = (minY + maxY) / 2;

        double[][] all = Arrays.copyOf(pts, n + 3);
        all[n] = new double[] {midX - span, midY - span};
        all[n + 1] = new double[] {midX, midY + span};
        all[n + 2] = new double[] {midX + span, midY - span};

        List<int[]> tris = new ArrayList<>();
        tris.add(new int[] {n, n + 1, n + 2});

        for (int i = 0; i < n; i++) {
            double px = all[i][0], py = all[i][1];
            List<int[]> bad = new ArrayList<>();
            for (int[] tri : tris) if (inCircumcircle(all, tri, px, py)) bad.add(tri);

            List<int[]> poly = new ArrayList<>();
            for (int[] tri : bad) {
                for (int e = 0; e < 3; e++) {
                    int v0 = tri[e], v1 = tri[(e + 1) % 3];
                    boolean shared = false;
                    outer:
                    for (int[] o : bad) {
                        if (o == tri) continue;
                        for (int f = 0; f < 3; f++) {
                            if (o[f] == v1 && o[(f + 1) % 3] == v0) {
                                shared = true;
                                break outer;
                            }
                        }
                    }
                    if (!shared) poly.add(new int[] {v0, v1});
                }
            }
            tris.removeAll(bad);
            for (int[] edge : poly) tris.add(new int[] {edge[0], edge[1], i});
        }

        List<int[]> result = new ArrayList<>();
        for (int[] tri : tris) if (tri[0] < n && tri[1] < n && tri[2] < n) result.add(tri);
        return result;
    }

    private static boolean inCircumcircle(double[][] pts, int[] tri, double px, double py) {
        double ax = pts[tri[0]][0] - px, ay = pts[tri[0]][1] - py;
        double bx = pts[tri[1]][0] - px, by = pts[tri[1]][1] - py;
        double cx = pts[tri[2]][0] - px, cy = pts[tri[2]][1] - py;
        double ar2 = ax * ax + ay * ay, br2 = bx * bx + by * by, cr2 = cx * cx + cy * cy;
        return ax * (by * cr2 - cy * br2) - ay * (bx * cr2 - cx * br2) + ar2 * (bx * cy - by * cx) > 0;
    }

    private static void computeTriangleStrip(Map<Long, int[]> out, List<ModelPoint> pts, int[] bm) {
        for (int i = 0; i + 2 < pts.size(); i++) {
            if (i % 2 == 0) {
                voxelizeTriangle(out, pts.get(i), pts.get(i + 1), pts.get(i + 2), bm);
            } else {
                voxelizeTriangle(out, pts.get(i + 1), pts.get(i), pts.get(i + 2), bm);
            }
        }
    }

    private static void computeTriangleFan(Map<Long, int[]> out, List<ModelPoint> pts, int[] bm) {
        if (pts.size() < 3) return;
        ModelPoint center = pts.get(0);
        for (int i = 1; i + 1 < pts.size(); i++) {
            voxelizeTriangle(out, center, pts.get(i), pts.get(i + 1), bm);
        }
    }

    // ── Surface modes (lofted) ────────────────────────────────────────────────

    private static void computeLoftedFlat(Map<Long, int[]> out, List<List<ModelPoint>> rows, int[] bm) {
        if (rows.size() < 2) {
            if (rows.size() == 1) {
                List<ModelPoint> row = rows.get(0);
                for (int i = 0; i + 1 < row.size(); i++) bresenhamLine(out, row.get(i), row.get(i + 1), bm);
            }
            return;
        }

        int maxCols = 0;
        for (List<ModelPoint> row : rows) maxCols = Math.max(maxCols, row.size());
        if (maxCols < 2) return;

        Vec3DDouble[] rowA = new Vec3DDouble[maxCols];
        Vec3DDouble[] rowB = new Vec3DDouble[maxCols];

        for (int r = 0; r + 1 < rows.size(); r++) {
            resampleRow(rows.get(r), maxCols, rowA);
            resampleRow(rows.get(r + 1), maxCols, rowB);
            for (int c = 0; c + 1 < maxCols; c++) {
                // Quad: (rowA[c], rowA[c+1], rowB[c+1], rowB[c]) → 2 triangles
                voxelizeTriangleD(out, rowA[c], rowA[c + 1], rowB[c], bm);
                voxelizeTriangleD(out, rowA[c + 1], rowB[c + 1], rowB[c], bm);
            }
        }
    }

    @FunctionalInterface
    private interface LoftedSampler {

        Vec3DDouble sample(Vec3DDouble[][] grid, int R, int C, int ui, int uSteps, int vi, int vSteps);
    }

    private static void computeLoftedSurface(
            Map<Long, int[]> out, List<List<ModelPoint>> rows, int[] bm, LoftedSampler sampler) {
        if (rows.size() < 2) {
            computeLoftedFlat(out, rows, bm);
            return;
        }

        int maxCols = 0;
        for (List<ModelPoint> row : rows) maxCols = Math.max(maxCols, row.size());
        if (maxCols < 1) return;

        int R = rows.size();
        Vec3DDouble[][] grid = new Vec3DDouble[R][maxCols];
        for (int r = 0; r < R; r++) resampleRow(rows.get(r), maxCols, grid[r]);

        int uSteps = Math.max(4, (R - 1) * 8);
        int vSteps = Math.max(4, (maxCols - 1) * 8);

        Vec3DDouble[][] cache = new Vec3DDouble[uSteps + 1][vSteps + 1];
        for (int ui = 0; ui <= uSteps; ui++)
            for (int vi = 0; vi <= vSteps; vi++)
                cache[ui][vi] = sampler.sample(grid, R, maxCols, ui, uSteps, vi, vSteps);

        for (int ui = 0; ui < uSteps; ui++) {
            for (int vi = 0; vi < vSteps; vi++) {
                voxelizeTriangleD(out, cache[ui][vi], cache[ui][vi + 1], cache[ui + 1][vi], bm);
                voxelizeTriangleD(out, cache[ui][vi + 1], cache[ui + 1][vi + 1], cache[ui + 1][vi], bm);
            }
        }
    }

    private static void computeLoftedCatmullRom(Map<Long, int[]> out, List<List<ModelPoint>> rows, int[] bm) {
        computeLoftedSurface(
                out,
                rows,
                bm,
                (grid, R, C, ui, uSteps, vi, vSteps) -> sampleGridCatmullRom(
                        grid, R, C, (double) ui / uSteps * (R - 1), (double) vi / vSteps * (C - 1)));
    }

    private static void computeLoftedBezier(Map<Long, int[]> out, List<List<ModelPoint>> rows, int[] bm) {
        computeLoftedSurface(
                out,
                rows,
                bm,
                (grid, R, C, ui, uSteps, vi, vSteps) ->
                        sampleGridBezier(grid, R, C, (double) ui / uSteps, (double) vi / vSteps));
    }

    // ── Surface sampling ──────────────────────────────────────────────────────

    private static Vec3DDouble sampleGridCatmullRom(Vec3DDouble[][] grid, int R, int C, double u, double v) {
        u = Math.max(0, Math.min(R - 1, u));
        v = Math.max(0, Math.min(C - 1, v));
        int ri = Math.min((int) u, R - 2);
        int ci = Math.min((int) v, C - 2);
        double ut = u - ri;
        double vt = v - ci;

        // Catmull-Rom across rows, linear across columns
        Vec3DDouble colA = grid[ri][ci].lerp(grid[ri][ci + 1], vt);
        Vec3DDouble colB =
                ri + 1 < R ? grid[ri + 1][ci].lerp(grid[ri + 1][ci + 1], vt) : grid[ri][ci].lerp(grid[ri][ci + 1], vt);
        Vec3DDouble colA0 = ri > 0 ? grid[ri - 1][ci].lerp(grid[ri - 1][ci + 1], vt) : colA;
        Vec3DDouble colB1 = ri + 2 < R ? grid[ri + 2][ci].lerp(grid[ri + 2][ci + 1], vt) : colB;

        return catmullRomInterp(colA0, colA, colB, colB1, ut);
    }

    private static Vec3DDouble sampleGridBezier(Vec3DDouble[][] grid, int R, int C, double u, double v) {
        // Tensor-product Bezier: evaluate along rows, then columns
        double[] rowWts = bezierBasis(R, u);
        double[] colWts = bezierBasis(C, v);
        Vec3DDouble sum = Vec3DDouble.ZERO;
        for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) sum = sum.plus(grid[r][c].times(rowWts[r] * colWts[c]));
        return sum;
    }

    private static double[] bezierBasis(int n, double t) {
        // Bernstein basis weights for degree n-1 at parameter t
        double[] w = new double[n];
        double[] c = new double[n];
        c[0] = 1;
        for (int i = 1; i < n; i++) {
            c[i] = 0;
            for (int j = i; j > 0; j--) c[j] = c[j] + c[j - 1];
        }
        double s = 1 - t;
        double tp = 1;
        for (int i = 0; i < n; i++) {
            w[i] = c[i] * tp * Math.pow(s, n - 1 - i);
            tp *= t;
        }
        return w;
    }

    public static Vec3DDouble catmullRomInterp(
            Vec3DDouble p0, Vec3DDouble p1, Vec3DDouble p2, Vec3DDouble p3, double t) {
        double t2 = t * t, t3 = t2 * t;
        return p1.times(2)
                .plus(p2.minus(p0).times(t))
                .plus(p0.times(2).minus(p1.times(5)).plus(p2.times(4)).minus(p3).times(t2))
                .plus(p0.negate().plus(p1.times(3)).minus(p2.times(3)).plus(p3).times(t3))
                .times(0.5);
    }

    /** Resample row to exactly n evenly-spaced points using linear interpolation. */
    static void resampleRow(List<ModelPoint> row, int n, Vec3DDouble[] out) {
        int m = row.size();
        if (m == 0) {
            Arrays.fill(out, Vec3DDouble.ZERO);
            return;
        }
        if (m == 1) {
            Arrays.fill(out, row.get(0).pos().toDouble());
            return;
        }
        // Build cumulative arc lengths
        double[] arc = new double[m];
        arc[0] = 0;
        for (int i = 1; i < m; i++) {
            arc[i] = arc[i - 1]
                    + row.get(i)
                            .pos()
                            .toDouble()
                            .minus(row.get(i - 1).pos().toDouble())
                            .length();
        }
        double totalLen = arc[m - 1];
        for (int k = 0; k < n; k++) {
            double t = (n == 1) ? 0 : (double) k / (n - 1) * totalLen;
            int seg = m - 2;
            for (int i = 0; i < m - 1; i++) {
                if (arc[i + 1] >= t) {
                    seg = i;
                    break;
                }
            }
            double segLen = arc[seg + 1] - arc[seg];
            double st = segLen > 0 ? (t - arc[seg]) / segLen : 0;
            Vec3DDouble pa = row.get(seg).pos().toDouble(),
                    pb = row.get(seg + 1).pos().toDouble();
            out[k] = pa.lerp(pb, st);
        }
    }

    // ── 3D Convex Hull (incremental) ──────────────────────────────────────────

    /** Public entry point for external callers (e.g. SelectionTransforms). */
    public static List<int[]> convexHull3DPublic(List<ModelPoint> pts) {
        return convexHull3D(pts);
    }

    static List<int[]> convexHull3D(List<ModelPoint> pts) {
        int n = pts.size();
        Vec3DDouble[] P = new Vec3DDouble[n];
        for (int i = 0; i < n; i++) P[i] = pts.get(i).pos().toDouble();

        int[] tet = findInitialTetrahedron(P, n);
        if (tet == null) return coplanarHull(n);

        int a = tet[0], b = tet[1], c = tet[2], d = tet[3];
        Vec3DDouble centroid = P[a].plus(P[b]).plus(P[c]).plus(P[d]).divide(4);

        List<int[]> faces = new ArrayList<>();
        addFaceOutward(faces, P, a, b, c, centroid);
        addFaceOutward(faces, P, a, b, d, centroid);
        addFaceOutward(faces, P, a, c, d, centroid);
        addFaceOutward(faces, P, b, c, d, centroid);

        Set<Integer> inHull = new HashSet<>(Arrays.asList(tet[0], tet[1], tet[2], tet[3]));

        for (int i = 0; i < n; i++) {
            if (inHull.contains(i)) continue;

            List<int[]> visible = new ArrayList<>();
            List<int[]> invisible = new ArrayList<>();
            for (int[] f : faces) {
                if (faceVisible(P, f, P[i])) visible.add(f);
                else invisible.add(f);
            }
            if (visible.isEmpty()) continue;

            Set<Long> invisEdges = new HashSet<>();
            for (int[] f : invisible) {
                invisEdges.add(edgeKey(f[0], f[1]));
                invisEdges.add(edgeKey(f[1], f[2]));
                invisEdges.add(edgeKey(f[2], f[0]));
            }

            List<int[]> horizon = new ArrayList<>();
            for (int[] f : visible) {
                for (int e = 0; e < 3; e++) {
                    int v0 = f[e], v1 = f[(e + 1) % 3];
                    if (invisEdges.contains(edgeKey(v1, v0))) horizon.add(new int[] {v0, v1});
                }
            }

            Vec3DDouble newCentroid = computeCentroid(P, invisible);
            faces = invisible;
            for (int[] edge : horizon) {
                addFaceOutward(faces, P, edge[0], edge[1], i, newCentroid);
            }
            inHull.add(i);
        }

        return faces;
    }

    private static List<int[]> coplanarHull(int n) {
        // Project to best 2D plane and return a fan
        List<int[]> result = new ArrayList<>();
        if (n < 3) return result;
        for (int i = 1; i + 1 < n; i++) result.add(new int[] {0, i, i + 1});
        return result;
    }

    private static int[] findInitialTetrahedron(Vec3DDouble[] P, int n) {
        int p0 = 0;
        double maxD = -1;
        int p1 = -1;
        for (int i = 1; i < n; i++) {
            double d = dist2(P[p0], P[i]);
            if (d > maxD) {
                maxD = d;
                p1 = i;
            }
        }
        if (p1 < 0 || maxD < 1e-9) return null;

        maxD = -1;
        int p2 = -1;
        for (int i = 0; i < n; i++) {
            if (i == p0 || i == p1) continue;
            double d = distToLine2(P[i], P[p0], P[p1]);
            if (d > maxD) {
                maxD = d;
                p2 = i;
            }
        }
        if (p2 < 0 || maxD < 1e-9) return null;

        maxD = -1;
        int p3 = -1;
        for (int i = 0; i < n; i++) {
            if (i == p0 || i == p1 || i == p2) continue;
            double d = Math.abs(distToPlane(P[i], P[p0], P[p1], P[p2]));
            if (d > maxD) {
                maxD = d;
                p3 = i;
            }
        }
        if (p3 < 0 || maxD < 1e-9) return null;

        return new int[] {p0, p1, p2, p3};
    }

    /** Returns the cross product (B-A) × (C-A). */
    private static Vec3DDouble triNormal(Vec3DDouble A, Vec3DDouble B, Vec3DDouble C) {
        return B.minus(A).cross(C.minus(A));
    }

    private static void addFaceOutward(List<int[]> faces, Vec3DDouble[] P, int a, int b, int c, Vec3DDouble inside) {
        Vec3DDouble A = P[a], B = P[b], C = P[c];
        if (triNormal(A, B, C).dot(A.minus(inside)) >= 0) {
            faces.add(new int[] {a, b, c});
        } else {
            faces.add(new int[] {a, c, b});
        }
    }

    private static boolean faceVisible(Vec3DDouble[] P, int[] face, Vec3DDouble p) {
        Vec3DDouble A = P[face[0]], B = P[face[1]], C = P[face[2]];
        return triNormal(A, B, C).dot(p.minus(A)) > 1e-9;
    }

    private static Vec3DDouble computeCentroid(Vec3DDouble[] P, List<int[]> faces) {
        if (faces.isEmpty()) return Vec3DDouble.ZERO;
        Vec3DDouble sum = Vec3DDouble.ZERO;
        int cnt = 0;
        for (int[] f : faces) {
            for (int i = 0; i < 3; i++) {
                sum = sum.plus(P[f[i]]);
                cnt++;
            }
        }
        return sum.divide(cnt);
    }

    private static long edgeKey(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }

    // ── Triangle voxelizer ────────────────────────────────────────────────────

    /** Public entry point for external callers (e.g. SelectionTransforms). */
    public static void voxelizeTriangleDPublic(
            Map<Long, int[]> out, Vec3DDouble A, Vec3DDouble B, Vec3DDouble C, int[] bm) {
        voxelizeTriangleD(out, A, B, C, bm);
    }

    static void voxelizeTriangle(Map<Long, int[]> out, ModelPoint A, ModelPoint B, ModelPoint C, int[] bm) {
        voxelizeTriangleD(out, A.pos().toDouble(), B.pos().toDouble(), C.pos().toDouble(), bm);
    }

    static void voxelizeTriangleD(Map<Long, int[]> out, Vec3DDouble A, Vec3DDouble B, Vec3DDouble C, int[] bm) {
        double maxEdge = Math.max(
                B.minus(A).length(), Math.max(C.minus(B).length(), A.minus(C).length()));
        int steps = Math.max(2, (int) Math.ceil(maxEdge * 2));

        for (int ui = 0; ui <= steps; ui++) {
            double u = (double) ui / steps;
            int viMax = steps - ui;
            for (int vi = 0; vi <= viMax; vi++) {
                double v = (double) vi / steps;
                double w = 1.0 - u - v;
                addPoint(
                        out,
                        A.times(u).plus(B.times(v)).plus(C.times(w)).plus(0.5).floor(),
                        bm);
            }
        }
    }

    // ── Line rasterizer ───────────────────────────────────────────────────────

    static void bresenhamLine(Map<Long, int[]> out, ModelPoint a, ModelPoint b, int[] bm) {
        Vec3DInt pa = a.pos(), pb = b.pos();
        int x = pa.x(), y = pa.y(), z = pa.z();
        int x1 = pb.x(), y1 = pb.y(), z1 = pb.z();
        int dx = Math.abs(x1 - x), dy = Math.abs(y1 - y), dz = Math.abs(z1 - z);
        int sx = x < x1 ? 1 : -1, sy = y < y1 ? 1 : -1, sz = z < z1 ? 1 : -1;
        addPoint(out, x, y, z, bm);
        int[] pos = {x, y, z};
        int[] step = {sx, sy, sz};
        int[] deltas = {dx, dy, dz};
        if (dx >= dy && dx >= dz) {
            bresenhamMajor(out, pos, step, deltas, 0, bm);
        } else if (dy >= dx && dy >= dz) {
            bresenhamMajor(out, pos, step, deltas, 1, bm);
        } else {
            bresenhamMajor(out, pos, step, deltas, 2, bm);
        }
    }

    private static void bresenhamMajor(Map<Long, int[]> out, int[] pos, int[] step, int[] deltas, int major, int[] bm) {
        int a = major == 0 ? 1 : 0;
        int b = major == 2 ? 1 : 2;
        int dm = deltas[major], da = deltas[a], db = deltas[b];
        int err1 = 2 * da - dm, err2 = 2 * db - dm;
        for (int i = 0; i < dm; i++) {
            pos[major] += step[major];
            if (err1 > 0) {
                pos[a] += step[a];
                err1 -= 2 * dm;
            }
            if (err2 > 0) {
                pos[b] += step[b];
                err2 -= 2 * dm;
            }
            err1 += 2 * da;
            err2 += 2 * db;
            addPoint(out, pos[0], pos[1], pos[2], bm);
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static void addPoint(Map<Long, int[]> out, ModelPoint p, int[] bm) {
        out.put(ChangeProposal.packKey(p.pos().x(), p.pos().y(), p.pos().z()), bm);
    }

    private static void addPoint(Map<Long, int[]> out, Vec3DInt p, int[] bm) {
        out.put(ChangeProposal.packKey(p.x(), p.y(), p.z()), bm);
    }

    private static void addPoint(Map<Long, int[]> out, int x, int y, int z, int[] bm) {
        out.put(ChangeProposal.packKey(x, y, z), bm);
    }

    private static double dist2(Vec3DDouble A, Vec3DDouble B) {
        return A.minus(B).lengthSq();
    }

    private static double distToLine2(Vec3DDouble P, Vec3DDouble A, Vec3DDouble B) {
        Vec3DDouble d = B.minus(A);
        double len2 = d.lengthSq();
        if (len2 < 1e-12) return dist2(P, A);
        double t = P.minus(A).dot(d) / len2;
        return P.minus(A.plus(d.times(t))).lengthSq();
    }

    private static double distToPlane(Vec3DDouble P, Vec3DDouble A, Vec3DDouble B, Vec3DDouble C) {
        Vec3DDouble n = triNormal(A, B, C);
        double len = n.length();
        if (len < 1e-12) return 0;
        return n.dot(P.minus(A)) / len;
    }

    private ModellingMath() {}
}
