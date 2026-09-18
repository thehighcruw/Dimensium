/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.stamp;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.clipboard.ClipboardBlock;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.editor.tool.creating.stamp.StampScatter.StampInstance;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public final class StampBrushInput implements BrushInput {

    public static final StampBrushInput INSTANCE = new StampBrushInput();

    /** Live preview, updated during drag. Read by SelectionRenderer. */
    public ChangeProposal dragPreview = null;

    private final Set<Long> strokeSet = new HashSet<>();
    private final List<Vec3DInt> strokePositions = new ArrayList<>();
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
        collectBrushPositions(Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ));
        rebuildPreview(mc);
    }

    @Override
    public boolean onBrushHeld(Minecraft mc, MovingObjectPosition mop) {
        collectBrushPositions(Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ));
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

    private void collectBrushPositions(Vec3DInt center) {
        BrushState bs = BrushState.INSTANCE;
        BrushUtil.forBrush(bs, offset -> {
            if (offset.y() != 0) return;
            Vec3DInt pos = center.plus(offset.x(), 0, offset.z());
            long key = ChangeProposal.packKey(pos.x(), 0, pos.z());
            if (strokeSet.add(key)) strokePositions.add(pos);
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
            long key = ChangeProposal.packKey(Vec3DInt.from(op[0], op[1], op[2]));
            p.proposed.put(key, new int[] {op[3], op[4]});
        }
        dragPreview = p;
    }

    private List<int[]> buildOps(List<StampInstance> instances, StampToolState state, Minecraft mc) {
        List<int[]> ops = new ArrayList<>();
        for (StampInstance inst : instances) {
            StampEntry entry = state.blueprints.get(inst.entryIdx);
            List<ClipboardBlock> offsets = entry.blueprint.offsets();
            Vec3DInt dim = entry.blueprint.clipDim();

            boolean rotated = inst.yaw != 0f;
            Mat3DFloat R = rotated ? ShapeMath.buildRotationMatrix(0f, inst.yaw, 0f) : null;
            Vec3DFloat center = dim.toFloat().divide(2f);

            for (ClipboardBlock o : offsets) {
                int lx = o.offset().x(), ly = o.offset().y(), lz = o.offset().z();

                if (inst.flipX) lx = (dim.x() - 1) - lx;
                if (inst.flipZ) lz = (dim.z() - 1) - lz;

                Vec3DInt worldPos;
                if (rotated) {
                    Vec3DFloat local = Vec3DFloat.from(lx, ly, lz).plus(0.5f).minus(center);
                    worldPos = inst.anchor.plus(Vec3DInt.floor(R.mul(local).plus(center)));
                } else {
                    worldPos = inst.anchor.plus(lx, ly, lz);
                }

                if (state.keepExisting) {
                    Block existing = WorldUtils.getBlock(worldPos);
                    if (existing != null && existing != Blocks.air) continue;
                }

                ops.add(worldPos.toBlockOp(o.blockId(), o.meta()));
            }
        }
        return ops;
    }
}
