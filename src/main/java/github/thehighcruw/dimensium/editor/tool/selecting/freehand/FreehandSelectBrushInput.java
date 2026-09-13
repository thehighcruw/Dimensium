/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.freehand;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.input.Mouse;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushState;
import github.thehighcruw.dimensium.editor.tool.brushes.BrushUtil;
import github.thehighcruw.dimensium.editor.tool.creating.freehand.FreehandToolState;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.util.RenderUtils;

@SideOnly(Side.CLIENT)
public class FreehandSelectBrushInput implements BrushInput {

    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;

    @Override
    public boolean onDragTick(Minecraft mc, int sw, int sh) {
        FreecamState fs = FreecamState.INSTANCE;
        int heldButton = -1;
        if (Mouse.isButtonDown(KeyConstants.RMB) && !fs.isMoving()) {
            heldButton = KeyConstants.RMB;
        }
        if (heldButton >= 0) {
            MovingObjectPosition mop = RenderUtils.raycastAtCursor();
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                if (mop.blockX != lastX || mop.blockY != lastY || mop.blockZ != lastZ) {
                    lastX = mop.blockX;
                    lastY = mop.blockY;
                    lastZ = mop.blockZ;
                    onMouseHeld(heldButton, mop);
                }
            }
        } else {
            lastX = Integer.MIN_VALUE;
        }
        return true;
    }

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        if (button != KeyConstants.RMB) return false;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        applyBrush(mop);
        return true;
    }

    public void onMouseHeld(int button, MovingObjectPosition mop) {
        if (button != KeyConstants.RMB) return;
        applyBrush(mop);
    }

    private static void applyBrush(MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        boolean includeAir = FreehandToolState.INSTANCE.includeAir;
        int cx = mop.blockX, cy = mop.blockY, cz = mop.blockZ;
        net.minecraft.world.World world = Minecraft.getMinecraft().theWorld;
        Set<Long> blocks = new HashSet<>();
        BrushUtil.forBrush(bs, (dx, dy, dz) -> {
            int wx = cx + dx, wy = cy + dy, wz = cz + dz;
            if (includeAir || world.getBlock(wx, wy, wz) != Blocks.air) {
                blocks.add(SelectionState.pack(wx, wy, wz));
            }
        });
        SelectionState.INSTANCE.applyOp(ToolMaskRegistry.INSTANCE.filterSelection(blocks), BooleanOp.ADD);
    }
}
