/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import github.thehighcruw.dimensium.editor.history.ServerCaptureQueue;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.io.IOException;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

/** Client → server: request a world-state capture for the given AABB. */
public class PacketCaptureRequest implements IPacket {

    public int txId;
    public Vec3DInt min = Vec3DInt.ZERO;
    public Vec3DInt max = Vec3DInt.ZERO;

    public PacketCaptureRequest() {}

    public PacketCaptureRequest(int txId, Vec3DInt min, Vec3DInt max) {
        this.txId = txId;
        this.min = min;
        this.max = max;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(txId);
        PacketUtils.writeCoords(buf, min.x(), min.y(), min.z());
        PacketUtils.writeCoords(buf, max.x(), max.y(), max.z());
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        txId = buf.readInt();
        min = PacketUtils.readCoords(buf);
        max = PacketUtils.readCoords(buf);
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        if (!PacketUtils.requireCreative(handler, "PacketCaptureRequest")) return null;
        if (!PacketUtils.checkVolume(handler, "PacketCaptureRequest", min, max)) return null;
        ServerCaptureQueue.enqueue(handler.playerEntity, txId, min, max);
        return null;
    }
}
