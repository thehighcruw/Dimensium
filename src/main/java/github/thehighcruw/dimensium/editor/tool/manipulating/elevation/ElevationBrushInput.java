/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.elevation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.overlay.MenuBar;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.tool.BrushApplicator;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class ElevationBrushInput implements BrushInput {

    public static final ElevationBrushInput INSTANCE = new ElevationBrushInput();

    private long lastNano = 0;
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;
    private boolean dragActive = false;
    private int lastFreehandX = Integer.MIN_VALUE;

    private ElevationBrushInput() {}

    @Override
    public boolean usesDragLoop() {
        return true;
    }

    @Override
    public boolean onDragTick(Minecraft mc, int sw, int sh) {
        FreecamState fs = FreecamState.INSTANCE;
        if (fs.isMoving()) return true;

        if (!Mouse.isButtonDown(KeyConstants.RMB)) {
            if (dragActive) {
                List<int[]> ops = ChangeProposal.flush();
                if (!ops.isEmpty())
                    BlockSender.sendChunked(
                            ops,
                            I18n.format(
                                    "dimensium.action.elevation",
                                    I18n.format(ElevationToolState.INSTANCE.elevationMode.label)));
                dragActive = false;
                lastNano = 0;
                lastX = Integer.MIN_VALUE;
                BrushApplicator.clearElevAccum();
            }
            lastFreehandX = Integer.MIN_VALUE;
            return true;
        }

        int sf = RenderUtils.scaleFactor();
        int mx = (int) fs.cursorX, my = (int) fs.cursorY;
        if (mx * sf < OverlayRenderer.TOOL_WINDOW.getWidth() || my * sf < (int) MenuBar.INSTANCE.height()) return true;

        MovingObjectPosition mop = GuiDimensiumOverlay.raycastFromMouse(mx, my, sw, sh);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return true;

        ElevationToolState es = ElevationToolState.INSTANCE;
        if (!dragActive) {
            ChangeProposal.startDrag(ToolMaskRegistry.INSTANCE.getActiveMask());
            BrushApplicator.clearElevAccum();
            dragActive = true;
        }
        boolean isOnce = es.elevationApply == ElevationToolState.ElevationApply.ONCE;
        if (isOnce) {
            if (mop.blockX == lastX && mop.blockY == lastY && mop.blockZ == lastZ) return true;
            lastX = mop.blockX;
            lastY = mop.blockY;
            lastZ = mop.blockZ;
        } else {
            long intervalNano = (long) (1e9 / Math.max(0.1, es.elevationRate));
            long now = System.nanoTime();
            if (now - lastNano < intervalNano) return true;
            lastNano = now;
        }
        BrushApplicator.applyTool(mc.theWorld, mop.blockX, mop.blockY, mop.blockZ);
        lastFreehandX = mop.blockX;
        return true;
    }

    public boolean isPaintDragging() {
        return lastFreehandX != Integer.MIN_VALUE;
    }
}
