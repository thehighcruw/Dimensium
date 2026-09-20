/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.smooth;

import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushStrategy;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.brushes.GaussianKernel;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.Arrays;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class SmoothBrush implements BrushStrategy {

    @Override
    public void apply(World world, MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        SmoothToolState s = SmoothToolState.INSTANCE;
        Vec3DInt origin = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        int sx = Math.min(bs.brushRadius, 12);
        int sy = Math.min(bs.brushShape.hasHeight ? bs.brushHeight : bs.brushRadius, 12);

        GaussianKernel kernel = GaussianKernel.build(s.smoothStrength * 0.5f + 0.5f);
        int margin = kernel.kR;
        Vec3DInt dims = Vec3DInt.from(sx, sy, sx).plus(margin).times(2).plus(1);
        int snStX = dims.y() * dims.z();
        int N = dims.product();

        int[] snapId = new int[N];
        int[] snapMeta = new int[N];
        int worldMinY = 0, worldMaxY = world.getHeight() - 1;
        Vec3DInt snapHalf = Vec3DInt.from(sx + margin, sy + margin, sx + margin);
        Vec3DInt.forEachInclusive(snapHalf.negate(), snapHalf, offset -> {
            int idx = offset.plus(snapHalf).toIndex(dims);
            Vec3DInt wc = origin.plus(offset);
            if (wc.y() < worldMinY) {
                snapId[idx] = -1;
                snapMeta[idx] = 0;
            } else if (wc.y() > worldMaxY) {
                snapId[idx] = 0;
                snapMeta[idx] = 0;
            } else if (!ChangeProposal.testSource(world, wc)) {
                snapId[idx] = 0;
                snapMeta[idx] = 0;
            } else {
                snapId[idx] = Block.getIdFromBlock(WorldUtils.getBlock(world, wc));
                snapMeta[idx] = WorldUtils.getBlockMetadata(world, wc);
            }
        });

        Vec3DInt brushBounds = Vec3DInt.from(sx, sy, sx);
        int maxPos = brushBounds.times(2).plus(1).product();
        Vec3DInt[] positions = new Vec3DInt[maxPos];
        int[] pCentre = new int[maxPos];
        int[] pc = {0}, os = {0};
        Vec3DInt.forEachInclusive(brushBounds.negate(), brushBounds, offset -> {
            if (!BrushUtil.inShape(bs.brushShape, offset, brushBounds)) return;
            int ci = offset.plus(sx + margin, sy + margin, sx + margin).toIndex(dims);
            if (snapId[ci] != 0) os[0]++;
            positions[pc[0]] = offset;
            pCentre[pc[0]] = ci;
            pc[0]++;
        });
        int posCount = pc[0], originalSolid = os[0];

        if (posCount == 0) return;

        int targetSolid = Math.round(originalSolid * s.smoothBlockRatio / 100f);
        float[] density = new float[posCount];
        int[] bestId = new int[posCount];
        int[] bestMeta = new int[posCount];

        final int BT_CAP = 16;
        int[] btId = new int[BT_CAP], btCnt = new int[BT_CAP], btMeta = new int[BT_CAP];

        boolean melt = s.smoothModifier == SmoothToolState.SmoothModifier.MELT;
        boolean grow = s.smoothModifier == SmoothToolState.SmoothModifier.GROW;
        Vec3DFloat brushSizeF = brushBounds.toFloat();
        Vec3DFloat invBrushSize = Vec3DFloat.from(
                brushSizeF.x() > 0 ? 1f / brushSizeF.x() : 0f,
                brushSizeF.y() > 0 ? 1f / brushSizeF.y() : 0f,
                brushSizeF.z() > 0 ? 1f / brushSizeF.z() : 0f);
        Vec3DInt dimsMax = dims.minus(1);

        for (int i = 0; i < posCount; i++) {
            Vec3DInt delta = positions[i];
            int ci = pCentre[i];
            Vec3DInt snap = delta.plus(snapHalf);

            float d = kernel.solidWeight(snapId, snap, snStX, dims.z()) / kernel.totalWeight;

            if (s.smoothFixEdges) {
                float r = delta.toFloat().abs().times(invBrushSize).max();
                if (r > 0.75f) {
                    float ef = (r - 0.75f) * 4f;
                    float origSol = snapId[ci] != 0 ? 1f : 0f;
                    d = d * (1f - ef) + origSol * ef;
                }
            }

            if (melt) d -= 0.3f * d * (1f - d) * 4f;
            else if (grow) d += 0.3f * d * (1f - d) * 4f;
            density[i] = d;

            int[] btC = {0};
            Vec3DInt.forEachInclusive(Vec3DInt.from(-1, -1, -1), Vec3DInt.ONE, (kx, ky, kz) -> {
                Vec3DInt nb = snap.plus(kx, ky, kz);
                if (!nb.inBounds(Vec3DInt.ZERO, dimsMax)) return;
                int idx = nb.toIndex(dims);
                int bid = snapId[idx];
                if (bid <= 0) return;
                boolean found = false;
                for (int t = 0; t < btC[0]; t++) {
                    if (btId[t] == bid) {
                        btCnt[t]++;
                        found = true;
                        break;
                    }
                }
                if (!found && btC[0] < BT_CAP) {
                    btId[btC[0]] = bid;
                    btCnt[btC[0]] = 1;
                    btMeta[btC[0]] = snapMeta[idx];
                    btC[0]++;
                }
            });
            int btCount = btC[0];
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
        float cutoff = (targetSolid > 0 && targetSolid < posCount)
                ? sorted[posCount - targetSolid]
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
            Vec3DInt wp = origin.plus(positions[i]);
            if (makeSolid && bestId[i] != 0 && solidAssigned < targetSolid) {
                if (bestId[i] == snapId[ci]) {
                    solidAssigned++;
                    continue;
                }
                Block blk = Block.getBlockById(bestId[i]);
                if (blk != null) {
                    ChangeProposal.write(world, wp, blk, bestMeta[i]);
                    solidAssigned++;
                }
            } else {
                if (snapId[ci] == 0) continue;
                ChangeProposal.write(world, wp, Blocks.air, 0);
            }
        }
    }
}
