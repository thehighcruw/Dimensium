/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.smooth;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.editor.tool.BrushApplicator;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
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

    private final LinkedHashSet<Vec3DInt> dragPositions = new LinkedHashSet<>();

    private SmoothBrushInput() {}

    @Override
    public boolean usesDragLoop() {
        return true;
    }

    public Set<Vec3DInt> getDragPositions() {
        return Collections.unmodifiableSet(dragPositions);
    }

    @Override
    public boolean onBrushHeld(Minecraft mc, MovingObjectPosition mop) {
        dragPositions.add(Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ));
        return true;
    }

    @Override
    public void onBrushRelease(Minecraft mc) {
        if (dragPositions.isEmpty()) return;
        ChangeProposal.startDrag(ToolMaskRegistry.INSTANCE.getActiveMask());
        long t0 = System.nanoTime();
        for (Vec3DInt pos : dragPositions) {
            BrushApplicator.applyTool(mc.theWorld, pos);
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
            Dimensium.logger.info(
                    "[DIMTIMER] smooth release compute={}ms flush={}ms sendChunked={}ms positions={} ops={}",
                    computeMs,
                    flushMs,
                    sendMs,
                    dragPositions.size(),
                    ops.size());
        dragPositions.clear();
    }
}
