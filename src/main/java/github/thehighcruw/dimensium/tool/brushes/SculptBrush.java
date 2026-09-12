package github.thehighcruw.dimensium.tool.brushes;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.state.BrushState;
import github.thehighcruw.dimensium.tool.state.SculptToolState;

public class SculptBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        SculptToolState s = SculptToolState.INSTANCE;
        int cx = mop.blockX, centerY = mop.blockY, cz = mop.blockZ;

        float[] normal;
        if (s.sculptMaskY) {
            normal = new float[] { 0f, 1f, 0f };
        } else {
            normal = computeSobelNormal(world, cx, centerY, cz, Math.max(1, bs.brushRadius));
            if (normal == null) {
                int[] rawN = ExtrudeHelper.sideToOutwardDir(mop.sideHit);
                normal = new float[] { rawN[0], rawN[1], rawN[2] };
            }
        }
        float fnx = normal[0], fny = normal[1], fnz = normal[2];

        float[] pa1 = perp(normal, new float[] { 0f, 1f, 0f });
        if (dot(pa1, pa1) < 0.001f) pa1 = perp(normal, new float[] { 1f, 0f, 0f });
        pa1 = normalize(pa1);
        float[] pa2 = normalize(cross(normal, pa1));

        int radius = Math.max(1, bs.brushRadius);
        int dim = 2 * radius + 1;
        int[] disp = new int[dim * dim];
        int[][] basePos = new int[dim * dim][3];

        for (int d1 = -radius; d1 <= radius; d1++) {
            for (int d2 = -radius; d2 <= radius; d2++) {
                int idx = (d1 + radius) * dim + (d2 + radius);
                float dist = (float) Math.sqrt(d1 * d1 + d2 * d2) / radius;
                if (dist > 1f) {
                    disp[idx] = -1;
                    continue;
                }
                float falloff = (float) Math.sqrt(Math.max(0f, 1f - dist * dist));
                disp[idx] = Math.max(0, Math.round(s.sculptStrength * falloff));
                basePos[idx][0] = cx + Math.round(d1 * pa1[0] + d2 * pa2[0]);
                basePos[idx][1] = centerY + Math.round(d1 * pa1[1] + d2 * pa2[1]);
                basePos[idx][2] = cz + Math.round(d1 * pa1[2] + d2 * pa2[2]);
            }
        }

        if (s.sculptDenoise) {
            int[] smoothed = new int[dim * dim];
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    int idx = i * dim + j;
                    if (disp[idx] < 0) {
                        smoothed[idx] = -1;
                        continue;
                    }
                    int sum = 0, cnt = 0;
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            int ni = i + di, nj = j + dj;
                            if (ni < 0 || ni >= dim || nj < 0 || nj >= dim) continue;
                            int nidx = ni * dim + nj;
                            if (disp[nidx] < 0) continue;
                            sum += disp[nidx];
                            cnt++;
                        }
                    }
                    smoothed[idx] = cnt > 0 ? Math.round((float) sum / cnt) : 0;
                }
            }
            disp = smoothed;
        }

        int searchRange = radius + (int) Math.ceil(s.sculptStrength) + 2;
        for (int idx = 0; idx < dim * dim; idx++) {
            if (disp[idx] <= 0) continue;
            int bx = basePos[idx][0], by = basePos[idx][1], bz = basePos[idx][2];
            int depth = disp[idx];

            int[] surf = findSculptSurface(world, bx, by, bz, fnx, fny, fnz, searchRange);
            if (surf == null) continue;

            if (!s.sculptInvert) {
                Block surfBlock = world.getBlock(surf[0], surf[1], surf[2]);
                int surfMeta = world.getBlockMetadata(surf[0], surf[1], surf[2]);
                if (surfBlock == null || surfBlock == Blocks.air) surfBlock = Blocks.dirt;
                int prevTx = surf[0], prevTy = surf[1], prevTz = surf[2];
                for (int d = 1; d <= depth; d++) {
                    int tx = surf[0] + Math.round(d * fnx);
                    int ty = surf[1] + Math.round(d * fny);
                    int tz = surf[2] + Math.round(d * fnz);
                    if (ty < 0 || ty > 255) break;
                    if (tx == prevTx && ty == prevTy && tz == prevTz) continue;
                    prevTx = tx;
                    prevTy = ty;
                    prevTz = tz;
                    if (world.getBlock(tx, ty, tz) != Blocks.air) break;
                    ChangeProposal.write(world, tx, ty, tz, surfBlock, surfMeta);
                }
            } else {
                int prevTx = surf[0], prevTy = surf[1], prevTz = surf[2];
                for (int d = 0; d < depth; d++) {
                    int tx = surf[0] - Math.round(d * fnx);
                    int ty = surf[1] - Math.round(d * fny);
                    int tz = surf[2] - Math.round(d * fnz);
                    if (ty < 0 || ty > 255) break;
                    if (tx == prevTx && ty == prevTy && tz == prevTz) continue;
                    prevTx = tx;
                    prevTy = ty;
                    prevTz = tz;
                    if (world.getBlock(tx, ty, tz) == Blocks.air) break;
                    ChangeProposal.write(world, tx, ty, tz, Blocks.air, 0);
                }
            }
        }
    }

    /**
     * Sobel gradient on topmost-solid-block height map, falling back to weighted
     * face-average normal when the gradient is too flat to be informative.
     * Returns null if no surface found at all.
     */
    private static float[] computeSobelNormal(World world, int cx, int cy, int cz, int radius) {
        int search = radius + 8;
        float[] h = new float[9];
        boolean anyFound = false;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int idx = (dz + 1) * 3 + (dx + 1);
                int top = findTopY(world, cx + dx, cz + dz, cy, search);
                if (top == Integer.MIN_VALUE) {
                    h[idx] = cy;
                } else {
                    h[idx] = top;
                    anyFound = true;
                }
            }
        }
        if (!anyFound) return null;

        // Sobel kernels (divide by 8 for normalization)
        float dX = (-h[0] + h[2] - 2 * h[3] + 2 * h[5] - h[6] + h[8]) / 8f;
        float dZ = (-h[0] - 2 * h[1] - h[2] + h[6] + 2 * h[7] + h[8]) / 8f;

        float gradMag = (float) Math.sqrt(dX * dX + dZ * dZ);
        if (gradMag < 0.15f) {
            // Nearly flat — Sobel reliable, pure +Y
            return new float[] { 0f, 1f, 0f };
        }

        // Normal from height gradient: surface z = h(x,z), tangents are (1,dX,0) and (0,dZ,1)
        // normal = cross(tangents) = (-dX, 1, -dZ) normalized
        return normalize(new float[] { -dX, 1f, -dZ });
    }

    private static int findTopY(World world, int x, int z, int cy, int search) {
        for (int y = Math.min(255, cy + search); y >= Math.max(0, cy - search); y--) {
            if (world.getBlock(x, y, z) != Blocks.air) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static int[] findSculptSurface(World world, int bx, int by, int bz, float fnx, float fny, float fnz,
        int range) {
        int lastTx = Integer.MIN_VALUE, lastTy = Integer.MIN_VALUE, lastTz = Integer.MIN_VALUE;
        for (float step = range; step >= -range; step -= 0.5f) {
            int tx = bx + Math.round(step * fnx);
            int ty = by + Math.round(step * fny);
            int tz = bz + Math.round(step * fnz);
            if (ty < 0 || ty > 255) continue;
            if (tx == lastTx && ty == lastTy && tz == lastTz) continue;
            lastTx = tx;
            lastTy = ty;
            lastTz = tz;
            if (world.getBlock(tx, ty, tz) != Blocks.air) return new int[] { tx, ty, tz };
        }
        return null;
    }

    private static float[] perp(float[] a, float[] b) {
        return cross(a, b);
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static float[] normalize(float[] v) {
        float len = (float) Math.sqrt(dot(v, v));
        if (len < 0.001f) return v;
        return new float[] { v[0] / len, v[1] / len, v[2] / len };
    }
}
