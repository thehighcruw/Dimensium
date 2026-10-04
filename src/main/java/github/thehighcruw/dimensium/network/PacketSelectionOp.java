/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import github.thehighcruw.dimensium.editor.history.EditHistory;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        PacketUtils.writeCoords(buf, p1);
        PacketUtils.writeCoords(buf, p2);
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
        if (!PacketUtils.checkVolume(handler, "PacketSelectionOp", mn, mx)) return null;

        Block block = op == Op.DELETE ? Blocks.air : Block.getBlockById(blockId);
        if (block == null) block = Blocks.air;
        int meta = op == Op.DELETE ? 0 : blockMeta;

        final Block blk = block;
        final int m = meta;
        Set<Long> affectedChunks = new HashSet<>();
        List<int[]> ops = new ArrayList<>();
        Vec3DInt.forEachInclusive(mn, mx, coord -> {
            EditHistory.applyBlockFast(world, coord, blk, m);
            affectedChunks.add(WorldUtils.chunkKeyForBlock(coord));
            ops.add(new int[] {coord.x(), coord.y(), coord.z()});
        });
        EditHistory.finalizeChunks(world, affectedChunks, ops);

        return null;
    }
}
