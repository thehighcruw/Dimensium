/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.selecting.freehand;

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
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class FreehandSelectBrushInput implements BrushInput {

    private Vec3DInt lastPos = null;

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
                Vec3DInt mopPos = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
                if (!mopPos.equals(lastPos)) {
                    lastPos = mopPos;
                    onMouseHeld(heldButton, mop);
                }
            }
        } else {
            lastPos = null;
        }
        return true;
    }

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        if (button != KeyConstants.RMB) return;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        applyBrush(mop);
    }

    public void onMouseHeld(int button, MovingObjectPosition mop) {
        if (button != KeyConstants.RMB) return;
        applyBrush(mop);
    }

    private static void applyBrush(MovingObjectPosition mop) {
        BrushState bs = BrushState.INSTANCE;
        boolean includeAir = FreehandToolState.INSTANCE.includeAir;
        Vec3DInt center = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        World world = Minecraft.getMinecraft().theWorld;
        Set<Long> blocks = new HashSet<>();
        BrushUtil.forBrush(bs, offset -> {
            Vec3DInt wc = center.plus(offset);
            if (includeAir || WorldUtils.getBlock(world, wc) != Blocks.air) {
                blocks.add(SelectionState.pack(wc));
            }
        });
        SelectionState.INSTANCE.applyOp(ToolMaskRegistry.INSTANCE.filterSelection(blocks), BooleanOp.ADD);
    }
}
