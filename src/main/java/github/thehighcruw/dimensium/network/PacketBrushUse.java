/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.editor.tool.BrushApplicator;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.io.IOException;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

/** Applies the currently selected brush/paint tool at the given block position. */
public class PacketBrushUse implements IPacket {

    private Vec3DInt coord;

    public PacketBrushUse() {}

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(coord.x());
        buf.writeInt(coord.y());
        buf.writeInt(coord.z());
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        coord = Vec3DInt.from(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        if (!handler.playerEntity.capabilities.isCreativeMode) {
            Dimensium.logger.warn(
                    "[Dimensium] Rejected PacketBrushUse from non-creative player {}",
                    handler.playerEntity.getCommandSenderName());
            return null;
        }
        World world = handler.playerEntity.worldObj;
        BrushApplicator.applyTool(world, coord);
        return null;
    }
}
