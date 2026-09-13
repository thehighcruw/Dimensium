/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.stamp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.editor.tool.creating.stamp.StampScatter.StampInstance;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.tool.ChangeProposal;

@SideOnly(Side.CLIENT)
public final class StampBrushInput implements BrushInput {

    public static final StampBrushInput INSTANCE = new StampBrushInput();

    /** Live preview, updated during drag. Read by SelectionRenderer. */
    public ChangeProposal dragPreview = null;

    private final Set<Long> strokeSet = new HashSet<>();
    private final List<int[]> strokePositions = new ArrayList<>();
    /** Seed fixed at drag-start so preview stays stable as the stroke grows. */
    private long dragSeed;
    private final Random rng = new Random();
    private final Random previewRng = new Random();

    private StampBrushInput() {}

    @Override
    public boolean usesDragLoop() {
        return true;
    }

    @Override
    public void onBrushDragStart(Minecraft mc, MovingObjectPosition mop) {
        dragSeed = rng.nextLong();
        strokeSet.clear();
        strokePositions.clear();
        collectBrushPositions(mop.blockX, mop.blockY, mop.blockZ);
        rebuildPreview(mc);
    }

    @Override
    public boolean onBrushHeld(Minecraft mc, MovingObjectPosition mop) {
        collectBrushPositions(mop.blockX, mop.blockY, mop.blockZ);
        rebuildPreview(mc);
        return true;
    }

    @Override
    public void onBrushRelease(Minecraft mc) {
        dragPreview = null;
        StampToolState state = StampToolState.INSTANCE;
        if (state.blueprints.isEmpty() || strokePositions.isEmpty()) {
            strokeSet.clear();
            strokePositions.clear();
            return;
        }
        rng.setSeed(dragSeed);
        List<StampInstance> instances = StampScatter.scatter(strokePositions, state, rng);
        strokeSet.clear();
        strokePositions.clear();
        if (instances.isEmpty()) return;
        List<int[]> ops = buildOps(instances, state, mc);
        if (!ops.isEmpty()) BlockSender.sendChunked(ops, "Stamp");
    }

    private void collectBrushPositions(int cx, int cy, int cz) {
        BrushState bs = BrushState.INSTANCE;
        BrushUtil.forBrush(bs, (dx, dy, dz) -> {
            if (dy != 0) return;
            long key = ChangeProposal.packKey(cx + dx, 0, cz + dz);
            if (strokeSet.add(key)) strokePositions.add(new int[] { cx + dx, cy, cz + dz });
        });
    }

    private void rebuildPreview(Minecraft mc) {
        StampToolState state = StampToolState.INSTANCE;
        if (state.blueprints.isEmpty() || strokePositions.isEmpty()) {
            dragPreview = null;
            return;
        }
        previewRng.setSeed(dragSeed);
        List<StampInstance> instances = StampScatter.scatter(strokePositions, state, previewRng);
        if (instances.isEmpty()) {
            dragPreview = null;
            return;
        }
        ChangeProposal p = ChangeProposal.forPreview();
        List<int[]> ops = buildOps(instances, state, mc);
        for (int[] op : ops) {
            long key = ChangeProposal.packKey(op[0], op[1], op[2]);
            p.proposed.put(key, new int[] { op[3], op[4] });
        }
        dragPreview = p;
    }

    private List<int[]> buildOps(List<StampInstance> instances, StampToolState state, Minecraft mc) {
        List<int[]> ops = new ArrayList<>();
        for (StampInstance inst : instances) {
            StampEntry entry = state.blueprints.get(inst.entryIdx);
            List<int[]> offsets = entry.blueprint.offsets;
            int clipW = entry.blueprint.clipW;
            int clipH = entry.blueprint.clipH;
            int clipD = entry.blueprint.clipD;

            boolean rotated = inst.yaw != 0f;
            float[] R = rotated ? ShapeMath.buildRotationMatrix(0f, inst.yaw, 0f) : null;
            float cx = clipW / 2f, cy = clipH / 2f, cz = clipD / 2f;

            for (int[] o : offsets) {
                int lx = o[0], ly = o[1], lz = o[2];

                if (inst.flipX) lx = (clipW - 1) - lx;
                if (inst.flipZ) lz = (clipD - 1) - lz;

                int wx, wy, wz;
                if (rotated) {
                    float dx = lx + 0.5f - cx, dy = ly + 0.5f - cy, dz = lz + 0.5f - cz;
                    wx = inst.anchorX + (int) Math.floor(R[0] * dx + R[1] * dy + R[2] * dz + cx);
                    wy = inst.anchorY + (int) Math.floor(R[3] * dx + R[4] * dy + R[5] * dz + cy);
                    wz = inst.anchorZ + (int) Math.floor(R[6] * dx + R[7] * dy + R[8] * dz + cz);
                } else {
                    wx = inst.anchorX + lx;
                    wy = inst.anchorY + ly;
                    wz = inst.anchorZ + lz;
                }

                if (state.keepExisting) {
                    Block existing = mc.theWorld.getBlock(wx, wy, wz);
                    if (existing != null && existing != Blocks.air) continue;
                }

                ops.add(new int[] { wx, wy, wz, o[3], o[4] });
            }
        }
        return ops;
    }
}
