/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.history.EditHistory;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapePlacementState;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

public class PacketShapePlacement implements IPacket {

    private Vec3DInt anchor;
    /** Pre-rotation base dimensions (shape-type adjusted, before rotation). */
    private Vec3DInt dims;

    private Vec3DFloat rot;
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
        anchor = Vec3DInt.floor(ps.anchorF);
        dims = ps.baseDims;
        rot = ps.rot;
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
            blockIds = new int[] {Block.getIdFromBlock(Block.getBlockFromItem(sbs.selectedBlock.getItem()))};
            metas = new int[] {sbs.selectedBlock.getItemDamage()};
            weights = new int[] {1};
        } else {
            paletteCount = 0;
            blockIds = new int[0];
            metas = new int[0];
            weights = new int[0];
        }
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(anchor.x());
        buf.writeInt(anchor.y());
        buf.writeInt(anchor.z());
        buf.writeInt(dims.x());
        buf.writeInt(dims.y());
        buf.writeInt(dims.z());
        buf.writeFloat(rot.x());
        buf.writeFloat(rot.y());
        buf.writeFloat(rot.z());
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
        anchor = Vec3DInt.from(buf.readInt(), buf.readInt(), buf.readInt());
        dims = Vec3DInt.from(buf.readInt(), buf.readInt(), buf.readInt());
        rot = Vec3DFloat.from(buf.readFloat(), buf.readFloat(), buf.readFloat());
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

        Mat3DFloat R = ShapeMath.buildRotationMatrix(rot.x(), rot.y(), rot.z());

        Vec3DInt[] bounds = ShapeMath.computeRotatedBounds(R, dims);
        Vec3DInt boundsMin = bounds[0], boundsMax = bounds[1];

        long bboxVolume = (long) (boundsMax.x() - boundsMin.x() + 1)
                * (boundsMax.y() - boundsMin.y() + 1)
                * (boundsMax.z() - boundsMin.z() + 1);
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
                dims,
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
                boundsMin,
                boundsMax,
                offset -> {
                    Vec3DInt pos = anchor.plus(offset);
                    if (pos.y() < 0 || pos.y() >= world.getHeight()) return true;
                    if (keepExisting && WorldUtils.getBlock(world, pos) != Blocks.air) return true;
                    int roll = rand.nextInt(totalWeight), cum = 0, chosen = 0;
                    for (int i = 0; i < weights.length; i++) {
                        cum += weights[i];
                        if (roll < cum) {
                            chosen = i;
                            break;
                        }
                    }
                    Block blk = Block.getBlockById(blockIds[chosen]);
                    if (blk != null && blk != Blocks.air) ops.add(pos.toBlockOp(blockIds[chosen], metas[chosen]));
                    return true;
                });

        if (!ops.isEmpty()) {
            String action = (hollow ? "Hollow " : "") + type.label;
            int txId = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, 0);
            int[][] after = ops.toArray(new int[0][]);
            EditHistory.record(world, action, ops, player, txId, after);
        }
        return null;
    }
}
