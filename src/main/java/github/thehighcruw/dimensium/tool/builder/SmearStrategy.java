/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.builder;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.freecam.FreecamUtils;
import github.thehighcruw.dimensium.handler.BlockSender;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.state.SelectionState;
import github.thehighcruw.dimensium.tool.state.SelectionState.BlockData;

public class SmearStrategy implements BuilderToolStrategy {

    @Override
    public boolean needsCapture() {
        return true;
    }

    @Override
    public void confirm(BuilderToolState bts, SelectionState sel) {
        if (sel.clipboard == null) return;
        int ox = sel.minX(), oy = sel.minY(), oz = sel.minZ();
        int dx = bts.offsetX, dy = bts.offsetY, dz = bts.offsetZ;
        if (dx == 0 && dy == 0 && dz == 0) return;

        int stepX = 0, stepY = 0, stepZ = 0, steps;
        if (Math.abs(dx) >= Math.abs(dy) && Math.abs(dx) >= Math.abs(dz)) {
            steps = Math.abs(dx);
            stepX = dx > 0 ? 1 : -1;
        } else if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) >= Math.abs(dz)) {
            steps = Math.abs(dy);
            stepY = dy > 0 ? 1 : -1;
        } else {
            steps = Math.abs(dz);
            stepZ = dz > 0 ? 1 : -1;
        }

        World world = Minecraft.getMinecraft().theWorld;
        List<int[]> ops = new ArrayList<>();
        for (int i = 1; i <= steps; i++) {
            int baseX = ox + stepX * i, baseY = oy + stepY * i, baseZ = oz + stepZ * i;
            for (int x = 0; x < sel.clipW; x++) {
                for (int y = 0; y < sel.clipH; y++) {
                    for (int z = 0; z < sel.clipD; z++) {
                        BlockData bd = sel.clipboardGet(x, y, z);
                        if (bd.block == Blocks.air) continue;
                        int px = baseX + x, py = baseY + y, pz = baseZ + z;
                        boolean inOrigSel = px >= sel.minX() && px <= sel.maxX()
                            && py >= sel.minY()
                            && py <= sel.maxY()
                            && pz >= sel.minZ()
                            && pz <= sel.maxZ();
                        if (!inOrigSel && world.getBlock(px, py, pz) != Blocks.air) continue;
                        ops.add(new int[] { px, py, pz, Block.getIdFromBlock(bd.block), bd.meta });
                    }
                }
            }
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

        int ox = sel.minX(), oy = sel.minY(), oz = sel.minZ();
        int dx = mop.blockX - ox, dy = mop.blockY - oy, dz = mop.blockZ - oz;
        bts.offsetX = dx;
        bts.offsetY = dy;
        bts.offsetZ = dz;
        if (dx == 0 && dy == 0 && dz == 0) {
            bts.smearPreview = null;
            return;
        }

        int stepX = 0, stepY = 0, stepZ = 0, steps;
        if (Math.abs(dx) >= Math.abs(dy) && Math.abs(dx) >= Math.abs(dz)) {
            steps = Math.abs(dx);
            stepX = dx > 0 ? 1 : -1;
        } else if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) >= Math.abs(dz)) {
            steps = Math.abs(dy);
            stepY = dy > 0 ? 1 : -1;
        } else {
            steps = Math.abs(dz);
            stepZ = dz > 0 ? 1 : -1;
        }

        ChangeProposal p = ChangeProposal.forPreview();
        outer: for (int i = 1; i <= steps; i++) {
            int baseX = ox + stepX * i, baseY = oy + stepY * i, baseZ = oz + stepZ * i;
            for (int x = 0; x < sel.clipW; x++) {
                for (int y = 0; y < sel.clipH; y++) {
                    for (int z = 0; z < sel.clipD; z++) {
                        BlockData bd = sel.clipboardGet(x, y, z);
                        if (bd.block == Blocks.air) continue;
                        int px = baseX + x, py = baseY + y, pz = baseZ + z;
                        boolean inOrigSel = px >= sel.minX() && px <= sel.maxX()
                            && py >= sel.minY()
                            && py <= sel.maxY()
                            && pz >= sel.minZ()
                            && pz <= sel.maxZ();
                        if (!inOrigSel) {
                            p.proposed.put(
                                ChangeProposal.packKey(px, py, pz),
                                new int[] { Block.getIdFromBlock(bd.block), bd.meta });
                        }
                        if (p.proposed.size() > DimensiumConfig.smearBlockCap) break outer;
                    }
                }
            }
        }
        bts.smearPreview = p.proposed.isEmpty() ? null : p;
    }
}
