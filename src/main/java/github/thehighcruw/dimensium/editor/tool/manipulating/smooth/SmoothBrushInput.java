/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.smooth;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.BrushApplicator;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public class SmoothBrushInput implements BrushInput {

    public static final SmoothBrushInput INSTANCE = new SmoothBrushInput();

    private final LinkedHashSet<Long> dragPositions = new LinkedHashSet<>();

    private SmoothBrushInput() {}

    @Override
    public boolean usesDragLoop() {
        return true;
    }

    public Set<Long> getDragPositions() {
        return Collections.unmodifiableSet(dragPositions);
    }

    @Override
    public boolean onBrushHeld(Minecraft mc, MovingObjectPosition mop) {
        long pk = ((long) (mop.blockX + 1048576) << 42)
                | ((long) (mop.blockY + 1048576) << 21)
                | (long) (mop.blockZ + 1048576);
        dragPositions.add(pk);
        return true;
    }

    @Override
    public void onBrushRelease(Minecraft mc) {
        if (dragPositions.isEmpty()) return;
        ChangeProposal.startDrag(ToolMaskRegistry.INSTANCE.getActiveMask());
        long t0 = System.nanoTime();
        for (long pk : dragPositions) {
            int bx = (int) ((pk >> 42) & 0x1FFFFF) - 1048576;
            int by = (int) ((pk >> 21) & 0x1FFFFF) - 1048576;
            int bz = (int) (pk & 0x1FFFFF) - 1048576;
            BrushApplicator.applyTool(mc.theWorld, bx, by, bz);
        }
        long t1 = System.nanoTime();
        List<int[]> ops = ChangeProposal.flush();
        long t2 = System.nanoTime();
        if (!ops.isEmpty()) BlockSender.sendChunked(ops, I18n.format("dimensium.action.smooth"));
        long t3 = System.nanoTime();
        long computeMs = (t1 - t0) / 1_000_000;
        long flushMs = (t2 - t1) / 1_000_000;
        long sendMs = (t3 - t2) / 1_000_000;
        if (computeMs > 5 || flushMs > 5 || sendMs > 5)
            github.thehighcruw.dimensium.Dimensium.logger.info(
                    "[DIMTIMER] smooth release compute={}ms flush={}ms sendChunked={}ms positions={} ops={}",
                    computeMs,
                    flushMs,
                    sendMs,
                    dragPositions.size(),
                    ops.size());
        dragPositions.clear();
    }
}
