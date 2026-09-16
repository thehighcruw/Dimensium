/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.fill;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public class FillBrushInput implements BrushInput {

    public static final FillBrushInput INSTANCE = new FillBrushInput();

    private FillBrushInput() {}

    @Override
    public boolean usesDragLoop() {
        return true;
    }

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        if (button == KeyConstants.LMB) {
            GuiDimensiumOverlay.cancelFillPreview();
            return;
        }
        if (button != KeyConstants.RMB) return;

        if (BuilderToolState.INSTANCE.fillPreview != null) {
            sendFillPackets();
            BuilderToolState.INSTANCE.fillPreview = null;
            return;
        }

        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        int[] faceOffsets = ExtrudeHelper.sideToOutwardDir(mop.sideHit);
        int airX = mop.blockX + faceOffsets[0];
        int airY = mop.blockY + faceOffsets[1];
        int airZ = mop.blockZ + faceOffsets[2];
        if (airY < 0 || airY > 255) return;
        if (mc.theWorld.getBlock(airX, airY, airZ) != Blocks.air) return;

        FloodfillToolState ts = FloodfillToolState.INSTANCE;
        boolean goDown = ts.floodfillDir == FloodfillToolState.FloodfillDir.DOWN;
        Set<Long> airBlocks = SelectionState.floodFillAir(
                mc.theWorld, Vec3DInt.from(airX, airY, airZ), ts.floodfillLimit, goDown, ts.floodfillCorners);
        if (airBlocks.isEmpty()) return;

        ItemStack picked = SelectedBlockState.INSTANCE.selectedBlock;
        if (picked == null) return;
        Block paintBlock = Block.getBlockFromItem(picked.getItem());
        if (paintBlock == null || paintBlock == Blocks.air) return;
        int paintMeta = picked.getItemDamage();
        int paintId = Block.getIdFromBlock(paintBlock);

        ChangeProposal p = ChangeProposal.forPreview();
        for (long key : airBlocks) {
            Vec3DInt bv = SelectionState.unpack(key);
            int bx = bv.x(), by = bv.y(), bz = bv.z();
            p.proposed.put(ChangeProposal.packKey(bx, by, bz), new int[] {paintId, paintMeta});
        }
        BuilderToolState.INSTANCE.fillPreview = p;
    }

    private static void sendFillPackets() {
        ChangeProposal p = BuilderToolState.INSTANCE.fillPreview;
        if (p == null) return;
        BlockSender.sendChunked(p.toOps(), I18n.format("dimensium.action.fill"));
    }
}
