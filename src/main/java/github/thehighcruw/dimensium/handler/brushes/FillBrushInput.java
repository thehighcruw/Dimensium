/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler.brushes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.handler.BlockSender;
import github.thehighcruw.dimensium.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.state.FloodfillToolState;
import github.thehighcruw.dimensium.tool.state.SelectedBlockState;
import github.thehighcruw.dimensium.tool.state.SelectionState;

@SideOnly(Side.CLIENT)
public class FillBrushInput implements BrushInput {

    public static final FillBrushInput INSTANCE = new FillBrushInput();

    private FillBrushInput() {}

    @Override
    public boolean usesDragLoop() {
        return true;
    }

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        if (button == KeyConstants.LMB) {
            GuiDimensiumOverlay.cancelFillPreview();
            return true;
        }
        if (button != KeyConstants.RMB) return false;

        if (BuilderToolState.INSTANCE.fillPreview != null) {
            sendFillPackets();
            BuilderToolState.INSTANCE.fillPreview = null;
            return true;
        }

        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;

        int[] faceOffsets = ExtrudeHelper.sideToOutwardDir(mop.sideHit);
        int airX = mop.blockX + faceOffsets[0];
        int airY = mop.blockY + faceOffsets[1];
        int airZ = mop.blockZ + faceOffsets[2];
        if (airY < 0 || airY > 255) return false;
        if (mc.theWorld.getBlock(airX, airY, airZ) != Blocks.air) return false;

        FloodfillToolState ts = FloodfillToolState.INSTANCE;
        boolean goDown = ts.floodfillDir == FloodfillToolState.FloodfillDir.DOWN;
        Set<Long> airBlocks = SelectionState
            .floodFillAir(mc.theWorld, airX, airY, airZ, ts.floodfillLimit, goDown, ts.floodfillCorners);
        if (airBlocks.isEmpty()) return false;

        ItemStack picked = SelectedBlockState.INSTANCE.selectedBlock;
        if (picked == null) return false;
        Block paintBlock = Block.getBlockFromItem(picked.getItem());
        if (paintBlock == null || paintBlock == Blocks.air) return false;
        int paintMeta = picked.getItemDamage();
        int paintId = Block.getIdFromBlock(paintBlock);

        ChangeProposal p = ChangeProposal.forPreview();
        for (long key : airBlocks) {
            int bx = SelectionState.unpackX(key);
            int by = SelectionState.unpackY(key);
            int bz = SelectionState.unpackZ(key);
            p.proposed.put(ChangeProposal.packKey(bx, by, bz), new int[] { paintId, paintMeta });
        }
        BuilderToolState.INSTANCE.fillPreview = p;
        return true;
    }

    private static void sendFillPackets() {
        ChangeProposal p = BuilderToolState.INSTANCE.fillPreview;
        if (p == null) return;
        List<int[]> positions = new ArrayList<>(p.proposed.size());
        for (Map.Entry<Long, int[]> e : p.proposed.entrySet()) {
            long key = e.getKey();
            int[] bm = e.getValue();
            positions.add(
                new int[] { ChangeProposal.unpackX(key), ChangeProposal.unpackY(key), ChangeProposal.unpackZ(key),
                    bm[0], bm[1] });
        }
        BlockSender.sendChunked(positions, I18n.format("dimensium.action.fill"));
    }
}
