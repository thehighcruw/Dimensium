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
import github.thehighcruw.dimensium.shared.SelectionState;

public class PacketPaste implements IPacket {

    // Paste origin (min corner of clipboard placed here)
    public int ox, oy, oz;
    public int w, h, d;
    // Flat block data: [x][y][z] → index x*h*d + y*d + z
    public int[] blockIds;
    public short[] blockMetas;

    public PacketPaste() {}

    public PacketPaste(int ox, int oy, int oz, java.util.Map<Long, SelectionState.BlockData> clip, int w, int h,
        int d) {
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.w = w;
        this.h = h;
        this.d = d;
        int count = w * h * d;
        blockIds = new int[count];
        blockMetas = new short[count];
        for (java.util.Map.Entry<Long, SelectionState.BlockData> entry : clip.entrySet()) {
            long key = entry.getKey();
            int x = (int) (key >> 20) & 0xFFFFF;
            int y = (int) (key >> 10) & 0x3FF;
            int z = (int) key & 0x3FF;
            int i = x * h * d + y * d + z;
            if (i >= 0 && i < count) {
                SelectionState.BlockData bd = entry.getValue();
                blockIds[i] = Block.getIdFromBlock(bd.block);
                blockMetas[i] = (short) bd.meta;
            }
        }
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(ox);
        buf.writeInt(oy);
        buf.writeInt(oz);
        buf.writeInt(w);
        buf.writeInt(h);
        buf.writeInt(d);
        for (int id : blockIds) buf.writeInt(id);
        for (short meta : blockMetas) buf.writeShort(meta);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        ox = buf.readInt();
        oy = buf.readInt();
        oz = buf.readInt();
        w = buf.readInt();
        h = buf.readInt();
        d = buf.readInt();
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
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) for (int z = 0; z < d; z++) {
            int i = x * h * d + y * d + z;
            Block blk = Block.getBlockById(blockIds[i]);
            if (blk == null) blk = Blocks.air;
            world.setBlock(ox + x, oy + y, oz + z, blk, blockMetas[i] & 0xFFFF, 3);
        }
        return null;
    }
}
