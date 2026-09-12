/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.state.ModellingToolState;
import github.thehighcruw.dimensium.tool.state.ModellingToolState.ModelPoint;

public class ModellingMath {

    public static List<int[]> computeBlocks(ModellingToolState state, ItemStack activeBlock) {
        int[] bm = blockToIdMeta(activeBlock);
        if (bm == null) return Collections.emptyList();

        Map<Long, int[]> out = new HashMap<>();

        switch (state.mode) {
            case CONVEX_HULL:
                computeConvexHull(out, state.allPoints(), bm);
                break;
            case SMART_SURFACE:
                computeSmartSurface(out, state.allPoints(), bm);
                break;
            case TRIANGLE_STRIP:
                computeTriangleStrip(out, state.allPoints(), bm);
                break;
            case TRIANGLE_FAN:
                computeTriangleFan(out, state.allPoints(), bm);
                break;
            case FLAT:
                computeLoftedFlat(out, state.rows, bm);
                break;
            case CATMULL_ROM:
                computeLoftedCatmullRom(out, state.rows, bm);
                break;
            case BEZIER:
                computeLoftedBezier(out, state.rows, bm);
                break;
        }

        List<int[]> result = new ArrayList<>(out.size());
        for (Map.Entry<Long, int[]> e : out.entrySet()) {
            long key = e.getKey();
            result.add(
                new int[] { ChangeProposal.unpackX(key), ChangeProposal.unpackY(key), ChangeProposal.unpackZ(key),
                    e.getValue()[0], e.getValue()[1] });
        }
        return result;
    }

    // ── Shape modes ───────────────────────────────────────────────────────────

    private static void computeConvexHull(Map<Long, int[]> out, List<ModelPoint> pts, int[] bm) {
        int n = pts.size();
        if (n == 0) return;
        if (n == 1) {
            addPoint(out, pts.get(0).x, pts.get(0).y, pts.get(0).z, bm);
            return;
        }
        if (n == 2) {
            bresenhamLine(out, pts.get(0), pts.get(1), bm);
            return;
        }
        if (n == 3) {
            voxelizeTriangle(out, pts.get(0), pts.get(1), pts.get(2), bm);
            return;
        }
        List<int[]> faces = convexHull3D(pts);
        if (faces.isEmpty()) {
            // Degenerate: fall back to all points connected as fan
            for (int i = 1; i + 1 < n; i++) voxelizeTriangle(out, pts.get(0), pts.get(i), pts.get(i + 1), bm);
        } else {
            for (int[] f : faces) voxelizeTriangle(out, pts.get(f[0]), pts.get(f[1]), pts.get(f[2]), bm);
        }
    }

    private static void computeSmartSurface(Map<Long, int[]> out, List<ModelPoint> pts, int[] bm) {
        int n = pts.size();
        if (n == 0) return;
        if (n == 1) {
            addPoint(out, pts.get(0).x, pts.get(0).y, pts.get(0).z, bm);
            return;
        }
        if (n == 2) {
            bresenhamLine(out, pts.get(0), pts.get(1), bm);
            return;
        }
        if (n == 3) {
            voxelizeTriangle(out, pts.get(0), pts.get(1), pts.get(2), bm);
            return;
        }

        double cx = 0, cy = 0, cz = 0;
        for (ModelPoint p : pts) {
            cx += p.x;
            cy += p.y;
            cz += p.z;
        }
        cx /= n;
        cy /= n;
        cz /= n;

        double[][] cov = new double[3][3];
        for (ModelPoint p : pts) {
            double[] d = { p.x - cx, p.y - cy, p.z - cz };
            for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) cov[i][j] += d[i] * d[j];
        }

        double[][] evecs = new double[3][3];
        jacobiEigen3(cov, evecs);
        double[] u = evecs[0], v = evecs[1];

        double[][] proj = new double[n][2];
        for (int i = 0; i < n; i++) {
            double dx = pts.get(i).x - cx, dy = pts.get(i).y - cy, dz = pts.get(i).z - cz;
            proj[i][0] = dx * u[0] + dy * u[1] + dz * u[2];
            proj[i][1] = dx * v[0] + dy * v[1] + dz * v[2];
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
        double[][] v = { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };

        for (int iter = 0; iter < 100; iter++) {
            int p = 0, q = 1;
            double maxOff = Math.abs(m[0][1]);
            if (Math.abs(m[0][2]) > maxOff) {
                maxOff = Math.abs(m[0][2]);
                p = 0;
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

        double[] evals = { m[0][0], m[1][1], m[2][2] };
        int[] idx = { 0, 1, 2 };
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
        all[n] = new double[] { midX - span, midY - span };
        all[n + 1] = new double[] { midX, midY + span };
        all[n + 2] = new double[] { midX + span, midY - span };

        List<int[]> tris = new ArrayList<>();
        tris.add(new int[] { n, n + 1, n + 2 });

        for (int i = 0; i < n; i++) {
            double px = all[i][0], py = all[i][1];
            List<int[]> bad = new ArrayList<>();
            for (int[] tri : tris) if (inCircumcircle(all, tri, px, py)) bad.add(tri);

            List<int[]> poly = new ArrayList<>();
            for (int[] tri : bad) {
                for (int e = 0; e < 3; e++) {
                    int v0 = tri[e], v1 = tri[(e + 1) % 3];
                    boolean shared = false;
                    outer: for (int[] o : bad) {
                        if (o == tri) continue;
                        for (int f = 0; f < 3; f++) {
                            if (o[f] == v1 && o[(f + 1) % 3] == v0) {
                                shared = true;
                                break outer;
                            }
                        }
                    }
                    if (!shared) poly.add(new int[] { v0, v1 });
                }
            }
            tris.removeAll(bad);
            for (int[] edge : poly) tris.add(new int[] { edge[0], edge[1], i });
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

        double[][] rowA = new double[maxCols][3];
        double[][] rowB = new double[maxCols][3];

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

    private static void computeLoftedCatmullRom(Map<Long, int[]> out, List<List<ModelPoint>> rows, int[] bm) {
        if (rows.size() < 2) {
            computeLoftedFlat(out, rows, bm);
            return;
        }

        int maxCols = 0;
        for (List<ModelPoint> row : rows) maxCols = Math.max(maxCols, row.size());
        if (maxCols < 1) return;

        int R = rows.size();
        double[][][] grid = new double[R][maxCols][3];
        for (int r = 0; r < R; r++) resampleRow(rows.get(r), maxCols, grid[r]);

        int uSteps = Math.max(4, (R - 1) * 8);
        int vSteps = Math.max(4, (maxCols - 1) * 8);

        double[][][] cache = new double[uSteps + 1][vSteps + 1][3];
        for (int ui = 0; ui <= uSteps; ui++) {
            double u = (double) ui / uSteps * (R - 1);
            for (int vi = 0; vi <= vSteps; vi++) {
                double v = (double) vi / vSteps * (maxCols - 1);
                cache[ui][vi] = sampleGridCatmullRom(grid, R, maxCols, u, v);
            }
        }

        for (int ui = 0; ui < uSteps; ui++) {
            for (int vi = 0; vi < vSteps; vi++) {
                voxelizeTriangleD(out, cache[ui][vi], cache[ui][vi + 1], cache[ui + 1][vi], bm);
                voxelizeTriangleD(out, cache[ui][vi + 1], cache[ui + 1][vi + 1], cache[ui + 1][vi], bm);
            }
        }
    }

    private static void computeLoftedBezier(Map<Long, int[]> out, List<List<ModelPoint>> rows, int[] bm) {
        if (rows.size() < 2) {
            computeLoftedFlat(out, rows, bm);
            return;
        }

        int maxCols = 0;
        for (List<ModelPoint> row : rows) maxCols = Math.max(maxCols, row.size());
        if (maxCols < 1) return;

        int R = rows.size();
        double[][][] grid = new double[R][maxCols][3];
        for (int r = 0; r < R; r++) resampleRow(rows.get(r), maxCols, grid[r]);

        int uSteps = Math.max(4, (R - 1) * 8);
        int vSteps = Math.max(4, (maxCols - 1) * 8);

        double[][][] cache = new double[uSteps + 1][vSteps + 1][3];
        for (int ui = 0; ui <= uSteps; ui++) {
            double u = (double) ui / uSteps;
            for (int vi = 0; vi <= vSteps; vi++) {
                double v = (double) vi / vSteps;
                cache[ui][vi] = sampleGridBezier(grid, R, maxCols, u, v);
            }
        }

        for (int ui = 0; ui < uSteps; ui++) {
            for (int vi = 0; vi < vSteps; vi++) {
                voxelizeTriangleD(out, cache[ui][vi], cache[ui][vi + 1], cache[ui + 1][vi], bm);
                voxelizeTriangleD(out, cache[ui][vi + 1], cache[ui + 1][vi + 1], cache[ui + 1][vi], bm);
            }
        }
    }

    // ── Surface sampling ──────────────────────────────────────────────────────

    private static double[] sampleGridCatmullRom(double[][][] grid, int R, int C, double u, double v) {
        u = Math.max(0, Math.min(R - 1, u));
        v = Math.max(0, Math.min(C - 1, v));
        int ri = (int) Math.min((int) u, R - 2);
        int ci = (int) Math.min((int) v, C - 2);
        double ut = u - ri;
        double vt = v - ci;

        // Catmull-Rom across rows, linear across columns
        double[] colA = lerpD(grid[ri][ci], grid[ri][ci + 1], vt);
        double[] colB = ri + 1 < R ? lerpD(grid[ri + 1][ci], grid[ri + 1][ci + 1], vt)
            : lerpD(grid[ri][ci], grid[ri][ci + 1], vt);
        double[] colA0 = ri > 0 ? lerpD(grid[ri - 1][ci], grid[ri - 1][ci + 1], vt) : colA;
        double[] colB1 = ri + 2 < R ? lerpD(grid[ri + 2][ci], grid[ri + 2][ci + 1], vt) : colB;

        return catmullRomInterp(colA0, colA, colB, colB1, ut);
    }

    private static double[] sampleGridBezier(double[][][] grid, int R, int C, double u, double v) {
        // Tensor-product Bezier: evaluate along rows, then columns
        // First interpolate across row dimension at parameter u
        double[] rowWts = bezierBasis(R, u);
        double[] colWts = bezierBasis(C, v);
        double px = 0, py = 0, pz = 0;
        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                double w = rowWts[r] * colWts[c];
                px += w * grid[r][c][0];
                py += w * grid[r][c][1];
                pz += w * grid[r][c][2];
            }
        }
        return new double[] { px, py, pz };
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

    private static double[] catmullRomInterp(double[] p0, double[] p1, double[] p2, double[] p3, double t) {
        double t2 = t * t, t3 = t2 * t;
        double[] out = new double[3];
        for (int i = 0; i < 3; i++) {
            out[i] = 0.5 * ((2 * p1[i]) + (-p0[i] + p2[i]) * t
                + (2 * p0[i] - 5 * p1[i] + 4 * p2[i] - p3[i]) * t2
                + (-p0[i] + 3 * p1[i] - 3 * p2[i] + p3[i]) * t3);
        }
        return out;
    }

    private static double[] lerpD(double[] a, double[] b, double t) {
        return new double[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t };
    }

    /** Resample row to exactly n evenly-spaced points using linear interpolation. */
    static void resampleRow(List<ModelPoint> row, int n, double[][] out) {
        int m = row.size();
        if (m == 0) {
            for (int i = 0; i < n; i++) Arrays.fill(out[i], 0);
            return;
        }
        if (m == 1) {
            ModelPoint p = row.get(0);
            for (int i = 0; i < n; i++) {
                out[i][0] = p.x;
                out[i][1] = p.y;
                out[i][2] = p.z;
            }
            return;
        }
        // Build cumulative arc lengths
        double[] arc = new double[m];
        arc[0] = 0;
        for (int i = 1; i < m; i++) {
            double dx = row.get(i).x - row.get(i - 1).x;
            double dy = row.get(i).y - row.get(i - 1).y;
            double dz = row.get(i).z - row.get(i - 1).z;
            arc[i] = arc[i - 1] + Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        double totalLen = arc[m - 1];
        for (int k = 0; k < n; k++) {
            double t = (n <= 1) ? 0 : (double) k / (n - 1) * totalLen;
            // Binary search for segment
            int seg = m - 2;
            for (int i = 0; i < m - 1; i++) {
                if (arc[i + 1] >= t) {
                    seg = i;
                    break;
                }
            }
            double segLen = arc[seg + 1] - arc[seg];
            double st = segLen > 0 ? (t - arc[seg]) / segLen : 0;
            ModelPoint a = row.get(seg), b = row.get(seg + 1);
            out[k][0] = a.x + (b.x - a.x) * st;
            out[k][1] = a.y + (b.y - a.y) * st;
            out[k][2] = a.z + (b.z - a.z) * st;
        }
    }

    // ── 3D Convex Hull (incremental) ──────────────────────────────────────────

    /** Public entry point for external callers (e.g. SelectionTransforms). */
    public static List<int[]> convexHull3DPublic(List<ModelPoint> pts) {
        return convexHull3D(pts);
    }

    static List<int[]> convexHull3D(List<ModelPoint> pts) {
        int n = pts.size();
        double[][] P = new double[n][3];
        for (int i = 0; i < n; i++) {
            P[i][0] = pts.get(i).x;
            P[i][1] = pts.get(i).y;
            P[i][2] = pts.get(i).z;
        }

        int[] tet = findInitialTetrahedron(P, n);
        if (tet == null) return coplanarHull(P, n);

        int a = tet[0], b = tet[1], c = tet[2], d = tet[3];
        double[] centroid = { (P[a][0] + P[b][0] + P[c][0] + P[d][0]) / 4, (P[a][1] + P[b][1] + P[c][1] + P[d][1]) / 4,
            (P[a][2] + P[b][2] + P[c][2] + P[d][2]) / 4 };

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
                    if (invisEdges.contains(edgeKey(v1, v0))) horizon.add(new int[] { v0, v1 });
                }
            }

            double[] newCentroid = computeCentroid(P, invisible);
            faces = invisible;
            for (int[] edge : horizon) {
                addFaceOutward(faces, P, edge[0], edge[1], i, newCentroid);
            }
            inHull.add(i);
        }

        return faces;
    }

    private static List<int[]> coplanarHull(double[][] P, int n) {
        // Project to best 2D plane and return a fan
        List<int[]> result = new ArrayList<>();
        if (n < 3) return result;
        for (int i = 1; i + 1 < n; i++) result.add(new int[] { 0, i, i + 1 });
        return result;
    }

    private static int[] findInitialTetrahedron(double[][] P, int n) {
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

        return new int[] { p0, p1, p2, p3 };
    }

    private static void addFaceOutward(List<int[]> faces, double[][] P, int a, int b, int c, double[] inside) {
        double[] A = P[a], B = P[b], C = P[c];
        double nx = (B[1] - A[1]) * (C[2] - A[2]) - (B[2] - A[2]) * (C[1] - A[1]);
        double ny = (B[2] - A[2]) * (C[0] - A[0]) - (B[0] - A[0]) * (C[2] - A[2]);
        double nz = (B[0] - A[0]) * (C[1] - A[1]) - (B[1] - A[1]) * (C[0] - A[0]);
        if (nx * (A[0] - inside[0]) + ny * (A[1] - inside[1]) + nz * (A[2] - inside[2]) >= 0) {
            faces.add(new int[] { a, b, c });
        } else {
            faces.add(new int[] { a, c, b });
        }
    }

    private static boolean faceVisible(double[][] P, int[] face, double[] p) {
        double[] A = P[face[0]], B = P[face[1]], C = P[face[2]];
        double nx = (B[1] - A[1]) * (C[2] - A[2]) - (B[2] - A[2]) * (C[1] - A[1]);
        double ny = (B[2] - A[2]) * (C[0] - A[0]) - (B[0] - A[0]) * (C[2] - A[2]);
        double nz = (B[0] - A[0]) * (C[1] - A[1]) - (B[1] - A[1]) * (C[0] - A[0]);
        return nx * (p[0] - A[0]) + ny * (p[1] - A[1]) + nz * (p[2] - A[2]) > 1e-9;
    }

    private static double[] computeCentroid(double[][] P, List<int[]> faces) {
        if (faces.isEmpty()) return new double[] { 0, 0, 0 };
        double sx = 0, sy = 0, sz = 0;
        int cnt = 0;
        for (int[] f : faces) {
            for (int i = 0; i < 3; i++) {
                sx += P[f[i]][0];
                sy += P[f[i]][1];
                sz += P[f[i]][2];
                cnt++;
            }
        }
        return new double[] { sx / cnt, sy / cnt, sz / cnt };
    }

    private static long edgeKey(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }

    // ── Triangle voxelizer ────────────────────────────────────────────────────

    /** Public entry point for external callers (e.g. SelectionTransforms). */
    public static void voxelizeTriangleDPublic(Map<Long, int[]> out, double[] A, double[] B, double[] C, int[] bm) {
        voxelizeTriangleD(out, A, B, C, bm);
    }

    static void voxelizeTriangle(Map<Long, int[]> out, ModelPoint A, ModelPoint B, ModelPoint C, int[] bm) {
        double[] a = { A.x, A.y, A.z };
        double[] b = { B.x, B.y, B.z };
        double[] c = { C.x, C.y, C.z };
        voxelizeTriangleD(out, a, b, c, bm);
    }

    static void voxelizeTriangleD(Map<Long, int[]> out, double[] A, double[] B, double[] C, int[] bm) {
        double ab = Math.sqrt(dist2(A, B));
        double bc = Math.sqrt(dist2(B, C));
        double ca = Math.sqrt(dist2(C, A));
        double maxEdge = Math.max(ab, Math.max(bc, ca));
        int steps = Math.max(2, (int) Math.ceil(maxEdge * 2));

        for (int ui = 0; ui <= steps; ui++) {
            double u = (double) ui / steps;
            int viMax = steps - ui;
            for (int vi = 0; vi <= viMax; vi++) {
                double v = (double) vi / steps;
                double w = 1.0 - u - v;
                int x = (int) Math.round(u * A[0] + v * B[0] + w * C[0]);
                int y = (int) Math.round(u * A[1] + v * B[1] + w * C[1]);
                int z = (int) Math.round(u * A[2] + v * B[2] + w * C[2]);
                addPoint(out, x, y, z, bm);
            }
        }
    }

    // ── Line rasterizer ───────────────────────────────────────────────────────

    static void bresenhamLine(Map<Long, int[]> out, ModelPoint a, ModelPoint b, int[] bm) {
        int x = a.x, y = a.y, z = a.z;
        int x1 = b.x, y1 = b.y, z1 = b.z;
        int dx = Math.abs(x1 - x), dy = Math.abs(y1 - y), dz = Math.abs(z1 - z);
        int sx = x < x1 ? 1 : -1, sy = y < y1 ? 1 : -1, sz = z < z1 ? 1 : -1;
        addPoint(out, x, y, z, bm);
        if (dx >= dy && dx >= dz) {
            int err1 = 2 * dy - dx, err2 = 2 * dz - dx;
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
                addPoint(out, x, y, z, bm);
            }
        } else if (dy >= dx && dy >= dz) {
            int err1 = 2 * dx - dy, err2 = 2 * dz - dy;
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
                addPoint(out, x, y, z, bm);
            }
        } else {
            int err1 = 2 * dx - dz, err2 = 2 * dy - dz;
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
                addPoint(out, x, y, z, bm);
            }
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static void addPoint(Map<Long, int[]> out, int x, int y, int z, int[] bm) {
        out.put(ChangeProposal.packKey(x, y, z), bm);
    }

    private static double dist2(double[] A, double[] B) {
        double dx = A[0] - B[0], dy = A[1] - B[1], dz = A[2] - B[2];
        return dx * dx + dy * dy + dz * dz;
    }

    private static double distToLine2(double[] P, double[] A, double[] B) {
        double dx = B[0] - A[0], dy = B[1] - A[1], dz = B[2] - A[2];
        double len2 = dx * dx + dy * dy + dz * dz;
        if (len2 < 1e-12) return dist2(P, A);
        double t = ((P[0] - A[0]) * dx + (P[1] - A[1]) * dy + (P[2] - A[2]) * dz) / len2;
        double cx = A[0] + t * dx, cy = A[1] + t * dy, cz = A[2] + t * dz;
        double ex = P[0] - cx, ey = P[1] - cy, ez = P[2] - cz;
        return ex * ex + ey * ey + ez * ez;
    }

    private static double distToPlane(double[] P, double[] A, double[] B, double[] C) {
        double nx = (B[1] - A[1]) * (C[2] - A[2]) - (B[2] - A[2]) * (C[1] - A[1]);
        double ny = (B[2] - A[2]) * (C[0] - A[0]) - (B[0] - A[0]) * (C[2] - A[2]);
        double nz = (B[0] - A[0]) * (C[1] - A[1]) - (B[1] - A[1]) * (C[0] - A[0]);
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-12) return 0;
        return (nx * (P[0] - A[0]) + ny * (P[1] - A[1]) + nz * (P[2] - A[2])) / len;
    }

    static int[] blockToIdMeta(ItemStack stack) {
        if (stack == null) return null;
        Block blk = Block.getBlockFromItem(stack.getItem());
        if (blk == null || blk == net.minecraft.init.Blocks.air) return null;
        return new int[] { Block.getIdFromBlock(blk), stack.getItemDamage() };
    }

    private ModellingMath() {}
}
