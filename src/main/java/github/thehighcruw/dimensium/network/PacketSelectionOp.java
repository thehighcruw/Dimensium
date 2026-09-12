/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import github.thehighcruw.dimensium.Dimensium;

public class PacketSelectionOp implements IPacket {

    public enum Op {
        FILL,
        DELETE
    }

    public Op op;
    public int x1, y1, z1, x2, y2, z2;
    public int blockId, blockMeta;

    public PacketSelectionOp() {}

    public PacketSelectionOp(Op op, int x1, int y1, int z1, int x2, int y2, int z2, int blockId, int blockMeta) {
        this.op = op;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.blockId = blockId;
        this.blockMeta = blockMeta;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeByte(op.ordinal());
        buf.writeInt(x1);
        buf.writeInt(y1);
        buf.writeInt(z1);
        buf.writeInt(x2);
        buf.writeInt(y2);
        buf.writeInt(z2);
        buf.writeInt(blockId);
        buf.writeInt(blockMeta);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        op = Op.values()[buf.readByte() & 0xFF];
        x1 = buf.readInt();
        y1 = buf.readInt();
        z1 = buf.readInt();
        x2 = buf.readInt();
        y2 = buf.readInt();
        z2 = buf.readInt();
        blockId = buf.readInt();
        blockMeta = buf.readInt();
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        if (!handler.playerEntity.capabilities.isCreativeMode) {
            Dimensium.logger.warn(
                "[Dimensium] Rejected PacketSelectionOp from non-creative player {}",
                handler.playerEntity.getCommandSenderName());
            return null;
        }
        World world = handler.playerEntity.worldObj;
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > 1_000_000L) {
            Dimensium.logger.warn(
                "[Dimensium] Rejected PacketSelectionOp: volume {} exceeds limit for player {}",
                volume,
                handler.playerEntity.getCommandSenderName());
            return null;
        }

        Block block = op == Op.DELETE ? Blocks.air : Block.getBlockById(blockId);
        if (block == null) block = Blocks.air;
        int meta = op == Op.DELETE ? 0 : blockMeta;

        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++) world.setBlock(x, y, z, block, meta, 3);

        return null;
    }
}
