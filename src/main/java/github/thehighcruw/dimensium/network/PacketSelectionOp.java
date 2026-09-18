/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.io.IOException;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

public class PacketSelectionOp implements IPacket {

    public enum Op {
        FILL,
        DELETE
    }

    public Op op;
    public Vec3DInt p1 = Vec3DInt.ZERO;
    public Vec3DInt p2 = Vec3DInt.ZERO;
    public int blockId, blockMeta;

    public PacketSelectionOp() {}

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeByte(op.ordinal());
        PacketUtils.writeCoords(buf, p1.x(), p1.y(), p1.z());
        PacketUtils.writeCoords(buf, p2.x(), p2.y(), p2.z());
        buf.writeInt(blockId);
        buf.writeInt(blockMeta);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        op = Op.values()[buf.readByte() & 0xFF];
        p1 = PacketUtils.readCoords(buf);
        p2 = PacketUtils.readCoords(buf);
        blockId = buf.readInt();
        blockMeta = buf.readInt();
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        if (!PacketUtils.requireCreative(handler, "PacketSelectionOp")) return null;
        World world = handler.playerEntity.worldObj;
        Vec3DInt mn = p1.min(p2), mx = p1.max(p2);
        if (!PacketUtils.checkVolume(handler, "PacketSelectionOp", mn.x(), mn.y(), mn.z(), mx.x(), mx.y(), mx.z()))
            return null;

        Block block = op == Op.DELETE ? Blocks.air : Block.getBlockById(blockId);
        if (block == null) block = Blocks.air;
        int meta = op == Op.DELETE ? 0 : blockMeta;

        final Block blk = block;
        final int m = meta;
        Vec3DInt.forEachInclusive(mn, mx, (x, y, z) -> world.setBlock(x, y, z, blk, m, 3));

        return null;
    }
}
