/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.tool.strategy;

import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.freecam.FreecamUtils;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.SelectionState.BlockData;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class SmearStrategy implements BuilderToolStrategy {

    @Override
    public boolean needsCapture() {
        return true;
    }

    @Override
    public void confirm(BuilderToolState bts, SelectionState sel) {
        if (sel.clipboard == null) return;
        Vec3DInt origin = Vec3DInt.from(sel.minX(), sel.minY(), sel.minZ());
        Vec3DInt offset = bts.offset;
        if (offset.equals(Vec3DInt.ZERO)) return;

        Vec3DInt abs = offset.abs();
        Vec3DInt step;
        int steps;
        if (abs.x() >= abs.y() && abs.x() >= abs.z()) {
            steps = abs.x();
            step = Vec3DInt.from(offset.x() > 0 ? 1 : -1, 0, 0);
        } else if (abs.y() >= abs.x() && abs.y() >= abs.z()) {
            steps = abs.y();
            step = Vec3DInt.from(0, offset.y() > 0 ? 1 : -1, 0);
        } else {
            steps = abs.z();
            step = Vec3DInt.from(0, 0, offset.z() > 0 ? 1 : -1);
        }

        World world = Minecraft.getMinecraft().theWorld;
        List<int[]> ops = new ArrayList<>();
        for (int i = 1; i <= steps; i++) {
            Vec3DInt base = origin.plus(step.times(i));
            sel.clipDim.forEach((x, y, z) -> {
                BlockData bd = sel.clipboardGet(x, y, z);
                if (bd.block() == Blocks.air) return;
                Vec3DInt dest = base.plus(Vec3DInt.from(x, y, z));
                boolean inOrigSel = dest.x() >= sel.minX()
                        && dest.x() <= sel.maxX()
                        && dest.y() >= sel.minY()
                        && dest.y() <= sel.maxY()
                        && dest.z() >= sel.minZ()
                        && dest.z() <= sel.maxZ();
                if (!inOrigSel && world.getBlock(dest.x(), dest.y(), dest.z()) != Blocks.air) return;
                ops.add(new int[] {dest.x(), dest.y(), dest.z(), Block.getIdFromBlock(bd.block()), bd.meta()});
            });
            if (ops.size() > DimensiumConfig.smearBlockCap) break;
        }
        if (!ops.isEmpty()) BlockSender.sendChunked(ops, "Smear");
    }

    /** Preview proposal for the current cursor position. Called each render frame. */
    public static void buildPreview(Minecraft mc, SelectionState sel, BuilderToolState bts) {
        MovingObjectPosition mop = FreecamUtils.rayTrace(mc, FreecamUtils.REACH);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            bts.smearPreview = null;
            return;
        }

        Vec3DInt origin = Vec3DInt.from(sel.minX(), sel.minY(), sel.minZ());
        Vec3DInt offset = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ).minus(origin);
        bts.offset = offset;
        if (offset.equals(Vec3DInt.ZERO)) {
            bts.smearPreview = null;
            return;
        }

        Vec3DInt abs = offset.abs();
        Vec3DInt step;
        int steps;
        if (abs.x() >= abs.y() && abs.x() >= abs.z()) {
            steps = abs.x();
            step = Vec3DInt.from(offset.x() > 0 ? 1 : -1, 0, 0);
        } else if (abs.y() >= abs.x() && abs.y() >= abs.z()) {
            steps = abs.y();
            step = Vec3DInt.from(0, offset.y() > 0 ? 1 : -1, 0);
        } else {
            steps = abs.z();
            step = Vec3DInt.from(0, 0, offset.z() > 0 ? 1 : -1);
        }

        ChangeProposal p = ChangeProposal.forPreview();
        boolean[] done = {false};
        for (int i = 1; i <= steps && !done[0]; i++) {
            Vec3DInt base = origin.plus(step.times(i));
            sel.clipDim.forEach((x, y, z) -> {
                if (done[0]) return;
                BlockData bd = sel.clipboardGet(x, y, z);
                if (bd.block() == Blocks.air) return;
                Vec3DInt dest = base.plus(Vec3DInt.from(x, y, z));
                boolean inOrigSel = dest.x() >= sel.minX()
                        && dest.x() <= sel.maxX()
                        && dest.y() >= sel.minY()
                        && dest.y() <= sel.maxY()
                        && dest.z() >= sel.minZ()
                        && dest.z() <= sel.maxZ();
                if (!inOrigSel) {
                    p.proposed.put(
                            ChangeProposal.packKey(dest.x(), dest.y(), dest.z()),
                            new int[] {Block.getIdFromBlock(bd.block()), bd.meta()});
                }
                if (p.proposed.size() > DimensiumConfig.smearBlockCap) done[0] = true;
            });
        }
        bts.smearPreview = p.proposed.isEmpty() ? null : p;
    }
}
