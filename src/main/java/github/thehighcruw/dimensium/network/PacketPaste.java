/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.io.IOException;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

public class PacketPaste implements IPacket {

    // Paste origin (min corner of clipboard placed here)
    public Vec3DInt origin = Vec3DInt.ZERO;
    public Vec3DInt dim = Vec3DInt.ZERO;
    // Flat block data: [x][y][z] → index x*dim.y()*dim.z() + y*dim.z() + z
    public int[] blockIds;
    public short[] blockMetas;

    public PacketPaste() {}

    public PacketPaste(Vec3DInt origin, Map<Long, SelectionState.BlockData> clip, Vec3DInt dim) {
        this.origin = origin;
        this.dim = dim;
        int h = dim.y(), d = dim.z();
        int count = dim.product();
        blockIds = new int[count];
        blockMetas = new short[count];
        for (Map.Entry<Long, SelectionState.BlockData> entry : clip.entrySet()) {
            long key = entry.getKey();
            int x = (int) (key >> 20) & 0xFFFFF;
            int y = (int) (key >> 10) & 0x3FF;
            int z = (int) key & 0x3FF;
            int i = x * h * d + y * d + z;
            if (i >= 0 && i < count) {
                SelectionState.BlockData bd = entry.getValue();
                blockIds[i] = Block.getIdFromBlock(bd.block());
                blockMetas[i] = (short) bd.meta();
            }
        }
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(origin.x());
        buf.writeInt(origin.y());
        buf.writeInt(origin.z());
        buf.writeInt(dim.x());
        buf.writeInt(dim.y());
        buf.writeInt(dim.z());
        for (int id : blockIds) buf.writeInt(id);
        for (short meta : blockMetas) buf.writeShort(meta);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        origin = Vec3DInt.from(buf.readInt(), buf.readInt(), buf.readInt());
        int w = buf.readInt(), h = buf.readInt(), d = buf.readInt();
        dim = Vec3DInt.from(w, h, d);
        long countL = (long) w * h * d;
        if (countL > 1_000_000L || countL < 0) throw new IOException("PacketPaste volume " + countL + " exceeds limit");
        int count = (int) countL;
        blockIds = new int[count];
        blockMetas = new short[count];
        for (int i = 0; i < count; i++) blockIds[i] = buf.readInt();
        for (int i = 0; i < count; i++) blockMetas[i] = buf.readShort();
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        if (!handler.playerEntity.capabilities.isCreativeMode) {
            Dimensium.logger.warn(
                    "[Dimensium] Rejected PacketPaste from non-creative player {}",
                    handler.playerEntity.getCommandSenderName());
            return null;
        }
        World world = handler.playerEntity.worldObj;
        int h = dim.y(), d = dim.z();
        dim.forEach((x, y, z) -> {
            int i = x * h * d + y * d + z;
            Block blk = Block.getBlockById(blockIds[i]);
            world.setBlock(
                    origin.x() + x,
                    origin.y() + y,
                    origin.z() + z,
                    blk != null ? blk : Blocks.air,
                    blockMetas[i] & 0xFFFF,
                    3);
        });
        return null;
    }
}
