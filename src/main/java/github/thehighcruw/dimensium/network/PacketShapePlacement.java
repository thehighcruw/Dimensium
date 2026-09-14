/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.history.EditHistory;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapePlacementState;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.shared.math.Mat3DFloat;

public class PacketShapePlacement implements IPacket {

    private int anchorX, anchorY, anchorZ;
    /** Pre-rotation base dimensions (shape-type adjusted, before rotX/Y/Z). */
    private int w, h, d;
    private float rotX, rotY, rotZ;
    private int shapeTypeOrd;
    private boolean hollow, keepExisting;
    private float exponent;
    private int torusRingR, torusRingRZ, torusTubeR;
    private int tubeWallThickness;
    private float supersphereExp;
    private int polygonSides;
    private float spiralSpacing, spiralTurns;
    private int paletteCount;
    private int[] blockIds;
    private int[] metas;
    private int[] weights;

    public PacketShapePlacement() {}

    public PacketShapePlacement(ShapePlacementState ps, ShapeToolState s, SelectedBlockState sbs) {
        anchorX = (int) Math.floor(ps.anchorF.x());
        anchorY = (int) Math.floor(ps.anchorF.y());
        anchorZ = (int) Math.floor(ps.anchorF.z());
        w = ps.baseW;
        h = ps.baseH;
        d = ps.baseD;
        rotX = ps.rot.x();
        rotY = ps.rot.y();
        rotZ = ps.rot.z();
        shapeTypeOrd = s.shapeType.ordinal();
        hollow = s.shapeHollow;
        keepExisting = s.shapeKeepExisting;
        exponent = s.shapeExponent;
        torusRingR = s.torusRingRadius;
        torusRingRZ = s.torusRingRadiusZ;
        torusTubeR = s.torusTubeRadius;
        tubeWallThickness = s.tubeWallThickness;
        supersphereExp = s.shapeSupersphereExp;
        polygonSides = s.shapePolygonSides;
        spiralSpacing = s.shapeSpiralSpacing;
        spiralTurns = s.shapeSpiralTurns;
        if (sbs.selectedBlock != null) {
            paletteCount = 1;
            blockIds = new int[] { Block.getIdFromBlock(Block.getBlockFromItem(sbs.selectedBlock.getItem())) };
            metas = new int[] { sbs.selectedBlock.getItemDamage() };
            weights = new int[] { 1 };
        } else {
            paletteCount = 0;
            blockIds = new int[0];
            metas = new int[0];
            weights = new int[0];
        }
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(anchorX);
        buf.writeInt(anchorY);
        buf.writeInt(anchorZ);
        buf.writeInt(w);
        buf.writeInt(h);
        buf.writeInt(d);
        buf.writeFloat(rotX);
        buf.writeFloat(rotY);
        buf.writeFloat(rotZ);
        buf.writeByte(shapeTypeOrd);
        buf.writeBoolean(hollow);
        buf.writeBoolean(keepExisting);
        buf.writeFloat(exponent);
        buf.writeInt(torusRingR);
        buf.writeInt(torusRingRZ);
        buf.writeInt(torusTubeR);
        buf.writeInt(tubeWallThickness);
        buf.writeFloat(supersphereExp);
        buf.writeInt(polygonSides);
        buf.writeFloat(spiralSpacing);
        buf.writeFloat(spiralTurns);
        buf.writeByte(paletteCount);
        for (int i = 0; i < paletteCount; i++) {
            buf.writeInt(blockIds[i]);
            buf.writeShort(metas[i]);
            buf.writeByte(weights[i]);
        }
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        anchorX = buf.readInt();
        anchorY = buf.readInt();
        anchorZ = buf.readInt();
        w = buf.readInt();
        h = buf.readInt();
        d = buf.readInt();
        rotX = buf.readFloat();
        rotY = buf.readFloat();
        rotZ = buf.readFloat();
        shapeTypeOrd = buf.readByte() & 0xFF;
        hollow = buf.readBoolean();
        keepExisting = buf.readBoolean();
        exponent = buf.readFloat();
        torusRingR = buf.readInt();
        torusRingRZ = buf.readInt();
        torusTubeR = buf.readInt();
        tubeWallThickness = buf.readInt();
        supersphereExp = buf.readFloat();
        polygonSides = buf.readInt();
        spiralSpacing = buf.readFloat();
        spiralTurns = buf.readFloat();
        paletteCount = buf.readByte() & 0xFF;
        blockIds = new int[paletteCount];
        metas = new int[paletteCount];
        weights = new int[paletteCount];
        for (int i = 0; i < paletteCount; i++) {
            blockIds[i] = buf.readInt();
            metas[i] = buf.readShort() & 0xFFFF;
            weights[i] = buf.readByte() & 0xFF;
        }
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        if (!handler.playerEntity.capabilities.isCreativeMode) {
            Dimensium.logger.warn(
                "[Dimensium] Rejected PacketShapePlacement from non-creative player {}",
                handler.playerEntity.getCommandSenderName());
            return null;
        }
        EntityPlayerMP player = handler.playerEntity;
        World world = player.worldObj;

        int tw = 0;
        for (int wt : weights) tw += wt;
        final int totalWeight = tw;
        if (totalWeight == 0 || paletteCount == 0) return null;

        ShapeToolState.ShapeType type = ShapeToolState.ShapeType.values()[shapeTypeOrd];

        Mat3DFloat R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);

        int[] bounds = ShapeMath.computeRotatedBounds(R, w, h, d);
        int ix0 = bounds[0], iy0 = bounds[1], iz0 = bounds[2];
        int ix1 = bounds[3], iy1 = bounds[4], iz1 = bounds[5];

        long bboxVolume = (long) (ix1 - ix0 + 1) * (iy1 - iy0 + 1) * (iz1 - iz0 + 1);
        if (bboxVolume > 1_000_000L) {
            Dimensium.logger.warn(
                "[Dimensium] Rejected PacketShapePlacement: bounding box volume {} exceeds limit for player {}",
                bboxVolume,
                player.getCommandSenderName());
            return null;
        }

        Random rand = new Random();
        List<int[]> ops = new ArrayList<>();
        ShapeMath.iterateRotatedShape(
            type,
            w,
            h,
            d,
            hollow,
            exponent,
            torusRingR,
            torusRingRZ,
            torusTubeR,
            tubeWallThickness,
            supersphereExp,
            polygonSides,
            spiralSpacing,
            spiralTurns,
            DimensiumConfig.shapeThreshold,
            R,
            ix0,
            iy0,
            iz0,
            ix1,
            iy1,
            iz1,
            (ox, oy, oz) -> {
                int bx = anchorX + ox, by = anchorY + oy, bz = anchorZ + oz;
                if (by < 0 || by >= world.getHeight()) return true;
                if (keepExisting && world.getBlock(bx, by, bz) != Blocks.air) return true;
                int roll = rand.nextInt(totalWeight), cum = 0, chosen = 0;
                for (int i = 0; i < weights.length; i++) {
                    cum += weights[i];
                    if (roll < cum) {
                        chosen = i;
                        break;
                    }
                }
                Block blk = Block.getBlockById(blockIds[chosen]);
                if (blk != null && blk != Blocks.air)
                    ops.add(new int[] { bx, by, bz, blockIds[chosen], metas[chosen] });
                return true;
            });

        if (!ops.isEmpty()) {
            String action = (hollow ? "Hollow " : "") + type.label;
            int txId = java.util.concurrent.ThreadLocalRandom.current()
                .nextInt(Integer.MIN_VALUE, 0);
            int[][] after = ops.toArray(new int[0][]);
            EditHistory.record(world, action, ops, player, txId, after);
        }
        return null;
    }
}
