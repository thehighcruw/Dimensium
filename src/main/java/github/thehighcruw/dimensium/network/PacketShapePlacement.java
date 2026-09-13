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
        anchorX = (int) Math.floor(ps.anchorFX);
        anchorY = (int) Math.floor(ps.anchorFY);
        anchorZ = (int) Math.floor(ps.anchorFZ);
        w = ps.baseW;
        h = ps.baseH;
        d = ps.baseD;
        rotX = ps.rotX;
        rotY = ps.rotY;
        rotZ = ps.rotZ;
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

        int totalWeight = 0;
        for (int wt : weights) totalWeight += wt;
        if (totalWeight == 0 || paletteCount == 0) return null;

        ShapeToolState.ShapeType type = ShapeToolState.ShapeType.values()[shapeTypeOrd];

        float[] R = ShapeMath.buildRotationMatrix(rotX, rotY, rotZ);
        float ccx = w / 2f, ccy = h / 2f, ccz = d / 2f;

        // AABB of rotated bounding box
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int mask = 0; mask < 8; mask++) {
            float hx = ((mask & 1) != 0 ? w : 0) - ccx;
            float hy = ((mask & 2) != 0 ? h : 0) - ccy;
            float hz = ((mask & 4) != 0 ? d : 0) - ccz;
            float wx2 = R[0] * hx + R[1] * hy + R[2] * hz + ccx;
            float wy2 = R[3] * hx + R[4] * hy + R[5] * hz + ccy;
            float wz2 = R[6] * hx + R[7] * hy + R[8] * hz + ccz;
            if (wx2 < minX) minX = wx2;
            if (wx2 > maxX) maxX = wx2;
            if (wy2 < minY) minY = wy2;
            if (wy2 > maxY) maxY = wy2;
            if (wz2 < minZ) minZ = wz2;
            if (wz2 > maxZ) maxZ = wz2;
        }
        int ix0 = (int) Math.floor(minX), iy0 = (int) Math.floor(minY), iz0 = (int) Math.floor(minZ);
        int ix1 = (int) Math.ceil(maxX), iy1 = (int) Math.ceil(maxY), iz1 = (int) Math.ceil(maxZ);

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
        for (int ox = ix0; ox <= ix1; ox++) {
            for (int oy = iy0; oy <= iy1; oy++) {
                for (int oz = iz0; oz <= iz1; oz++) {
                    float dx0 = (ox + 0.5f) - ccx;
                    float dy0 = (oy + 0.5f) - ccy;
                    float dz0 = (oz + 0.5f) - ccz;
                    float ldx = R[0] * dx0 + R[3] * dy0 + R[6] * dz0 + ccx;
                    float ldy = R[1] * dx0 + R[4] * dy0 + R[7] * dz0 + ccy;
                    float ldz = R[2] * dx0 + R[5] * dy0 + R[8] * dz0 + ccz;

                    if (!ShapeMath.inShapeGeomF(
                        type,
                        ldx,
                        ldy,
                        ldz,
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
                        DimensiumConfig.shapeThreshold)) continue;

                    int bx = anchorX + ox, by = anchorY + oy, bz = anchorZ + oz;
                    if (by < 0 || by >= world.getHeight()) continue;
                    if (keepExisting && world.getBlock(bx, by, bz) != Blocks.air) continue;

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
                }
            }
        }

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
