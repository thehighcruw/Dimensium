/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.smooth;

import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.brushes.GaussianKernel;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.ChangeProposal;

public class SmoothBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        SmoothToolState s = SmoothToolState.INSTANCE;
        int ox = mop.blockX, oy = mop.blockY, oz = mop.blockZ;
        int sx = Math.min(bs.brushRadius, 12);
        int sy = Math.min(bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius, 12);

        GaussianKernel kernel = GaussianKernel.build(s.smoothStrength * 0.5f + 0.5f);
        int margin = kernel.kR;
        int dimX = 2 * (sx + margin) + 1;
        int dimY = 2 * (sy + margin) + 1;
        int dimZ = 2 * (sx + margin) + 1;
        int snStX = dimY * dimZ;
        int N = dimX * dimY * dimZ;

        int[] snapId = new int[N];
        int[] snapMeta = new int[N];
        int worldMinY = 0, worldMaxY = world.getHeight() - 1;
        for (int dx = -(sx + margin); dx <= sx + margin; dx++) {
            int ix = dx + sx + margin;
            for (int dy = -(sy + margin); dy <= sy + margin; dy++) {
                int iy = dy + sy + margin;
                int wy = oy + dy;
                int idx0 = ix * snStX + iy * dimZ;
                for (int dz = -(sx + margin); dz <= sx + margin; dz++) {
                    int idx = idx0 + (dz + sx + margin);
                    if (wy < worldMinY) {
                        snapId[idx] = -1;
                        snapMeta[idx] = 0;
                    } else if (wy > worldMaxY) {
                        snapId[idx] = 0;
                        snapMeta[idx] = 0;
                    } else {
                        Block b = world.getBlock(ox + dx, wy, oz + dz);
                        snapId[idx] = Block.getIdFromBlock(b);
                        snapMeta[idx] = world.getBlockMetadata(ox + dx, wy, oz + dz);
                    }
                }
            }
        }

        int maxPos = (2 * sx + 1) * (2 * sy + 1) * (2 * sx + 1);
        Vec3DInt[] positions = new Vec3DInt[maxPos];
        int[] pCentre = new int[maxPos];
        int posCount = 0, originalSolid = 0;
        for (int dx = -sx; dx <= sx; dx++) for (int dy = -sy; dy <= sy; dy++) for (int dz = -sx; dz <= sx; dz++) {
            if (!BrushUtil.inShape(bs.brushShape, dx, dy, dz, sx, sy, sx)) continue;
            int ci = (dx + sx + margin) * snStX + (dy + sy + margin) * dimZ + (dz + sx + margin);
            if (snapId[ci] != 0) originalSolid++;
            positions[posCount] = Vec3DInt.from(dx, dy, dz);
            pCentre[posCount] = ci;
            posCount++;
        }

        if (posCount == 0) return;

        int targetSolid = Math.round(originalSolid * s.smoothBlockRatio / 100f);
        float[] density = new float[posCount];
        int[] bestId = new int[posCount];
        int[] bestMeta = new int[posCount];

        final int BT_CAP = 16;
        int[] btId = new int[BT_CAP], btCnt = new int[BT_CAP], btMeta = new int[BT_CAP];

        boolean melt = s.smoothModifier == SmoothToolState.SmoothModifier.MELT;
        boolean grow = s.smoothModifier == SmoothToolState.SmoothModifier.GROW;
        float invSx = sx > 0 ? 1f / sx : 0f;
        float invSy = sy > 0 ? 1f / sy : 0f;
        float invSz = sx > 0 ? 1f / sx : 0f;

        for (int i = 0; i < posCount; i++) {
            Vec3DInt delta = positions[i];
            int dx = delta.x(), dy = delta.y(), dz = delta.z();
            int ci = pCentre[i];
            int ix = dx + sx + margin, iy = dy + sy + margin, iz = dz + sx + margin;

            float d = kernel.solidWeight(snapId, Vec3DInt.from(ix, iy, iz), snStX, dimZ) / kernel.totalWeight;

            if (s.smoothFixEdges) {
                float r = Math.max(Math.abs(dx) * invSx, Math.max(Math.abs(dy) * invSy, Math.abs(dz) * invSz));
                if (r > 0.75f) {
                    float ef = (r - 0.75f) * 4f;
                    float origSol = snapId[ci] != 0 ? 1f : 0f;
                    d = d * (1f - ef) + origSol * ef;
                }
            }

            if (melt) d -= 0.3f * d * (1f - d) * 4f;
            else if (grow) d += 0.3f * d * (1f - d) * 4f;
            density[i] = d;

            int btCount = 0;
            for (int kx = -1; kx <= 1; kx++) {
                int nx = ix + kx;
                if (nx < 0 || nx >= dimX) continue;
                int nxB = nx * snStX;
                for (int ky = -1; ky <= 1; ky++) {
                    int ny = iy + ky;
                    if (ny < 0 || ny >= dimY) continue;
                    int nyB = nxB + ny * dimZ;
                    for (int kz = -1; kz <= 1; kz++) {
                        int nz = iz + kz;
                        if (nz < 0 || nz >= dimZ) continue;
                        int bid = snapId[nyB + nz];
                        if (bid <= 0) continue;
                        boolean found = false;
                        for (int t = 0; t < btCount; t++) {
                            if (btId[t] == bid) {
                                btCnt[t]++;
                                found = true;
                                break;
                            }
                        }
                        if (!found && btCount < BT_CAP) {
                            btId[btCount] = bid;
                            btCnt[btCount] = 1;
                            btMeta[btCount] = snapMeta[nyB + nz];
                            btCount++;
                        }
                    }
                }
            }
            int bId = 0, bC = -1, bM = 0;
            for (int t = 0; t < btCount; t++) {
                if (btCnt[t] > bC) {
                    bC = btCnt[t];
                    bId = btId[t];
                    bM = btMeta[t];
                }
                btCnt[t] = 0;
            }
            bestId[i] = bId;
            bestMeta[i] = bM;
        }

        float[] sorted = Arrays.copyOf(density, posCount);
        Arrays.sort(sorted);
        float cutoff = (targetSolid > 0 && targetSolid < posCount) ? sorted[posCount - targetSolid]
            : (targetSolid <= 0 ? Float.MAX_VALUE : Float.NEGATIVE_INFINITY);

        int above = 0;
        for (int i = posCount - targetSolid; i < posCount && targetSolid > 0 && targetSolid < posCount; i++)
            if (sorted[i] > cutoff) above++;
        int atCutoffBudget = targetSolid - above;

        int solidAssigned = 0, atCutoffUsed = 0;
        for (int i = 0; i < posCount; i++) {
            boolean makeSolid;
            if (density[i] > cutoff) makeSolid = true;
            else if (density[i] == cutoff && atCutoffUsed < atCutoffBudget) {
                makeSolid = true;
                atCutoffUsed++;
            } else makeSolid = false;

            int ci = pCentre[i];
            Vec3DInt wp = Vec3DInt.from(ox, oy, oz)
                .plus(positions[i]);
            if (makeSolid && bestId[i] != 0 && solidAssigned < targetSolid) {
                if (bestId[i] == snapId[ci]) {
                    solidAssigned++;
                    continue;
                }
                Block blk = Block.getBlockById(bestId[i]);
                if (blk != null) {
                    ChangeProposal.write(world, wp.x(), wp.y(), wp.z(), blk, bestMeta[i]);
                    solidAssigned++;
                }
            } else {
                if (snapId[ci] == 0) continue;
                ChangeProposal.write(world, wp.x(), wp.y(), wp.z(), Blocks.air, 0);
            }
        }
    }
}
