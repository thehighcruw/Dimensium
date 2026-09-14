/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.editor.history.ServerCaptureQueue;
import java.io.IOException;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

/** Client → server: request a world-state capture for the given AABB. */
public class PacketCaptureRequest implements IPacket {

    public int txId;
    public int minX, minY, minZ, maxX, maxY, maxZ;

    public PacketCaptureRequest() {}

    public PacketCaptureRequest(int txId, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.txId = txId;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(txId);
        buf.writeInt(minX);
        buf.writeInt(minY);
        buf.writeInt(minZ);
        buf.writeInt(maxX);
        buf.writeInt(maxY);
        buf.writeInt(maxZ);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        txId = buf.readInt();
        minX = buf.readInt();
        minY = buf.readInt();
        minZ = buf.readInt();
        maxX = buf.readInt();
        maxY = buf.readInt();
        maxZ = buf.readInt();
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        if (!handler.playerEntity.capabilities.isCreativeMode) {
            Dimensium.logger.warn(
                    "[Dimensium] Rejected PacketCaptureRequest from non-creative player {}",
                    handler.playerEntity.getCommandSenderName());
            return null;
        }
        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > 1_000_000L) {
            Dimensium.logger.warn(
                    "[Dimensium] Rejected PacketCaptureRequest: volume {} exceeds limit for player {}",
                    volume,
                    handler.playerEntity.getCommandSenderName());
            return null;
        }
        ServerCaptureQueue.enqueue(handler.playerEntity, txId, minX, minY, minZ, maxX, maxY, maxZ);
        return null;
    }
}
