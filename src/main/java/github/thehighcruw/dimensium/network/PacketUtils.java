/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.io.IOException;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

final class PacketUtils {

    private PacketUtils() {}

    static void writeCoords(PacketBuffer buf, int x, int y, int z) throws IOException {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    static Vec3DInt readCoords(PacketBuffer buf) throws IOException {
        return Vec3DInt.from(buf.readInt(), buf.readInt(), buf.readInt());
    }

    static boolean requireCreative(NetHandlerPlayServer handler, String packetName) {
        if (!handler.playerEntity.capabilities.isCreativeMode) {
            Dimensium.logger.warn(
                    "[Dimensium] Rejected {} from non-creative player {}",
                    packetName,
                    handler.playerEntity.getCommandSenderName());
            return false;
        }
        return true;
    }

    static boolean checkVolume(
            NetHandlerPlayServer handler,
            String packetName,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ) {
        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > 1_000_000L) {
            Dimensium.logger.warn(
                    "[Dimensium] Rejected {}: volume {} exceeds limit for player {}",
                    packetName,
                    volume,
                    handler.playerEntity.getCommandSenderName());
            return false;
        }
        return true;
    }
}
