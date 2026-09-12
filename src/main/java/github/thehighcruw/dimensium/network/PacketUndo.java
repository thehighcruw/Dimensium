/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import java.io.IOException;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;

/** Unused — undo is now handled client-side via ClientEditHistory + sendChunkedSkipHistory. */
public class PacketUndo implements IPacket {

    @Override
    public void encode(PacketBuffer buf) throws IOException {}

    @Override
    public void decode(PacketBuffer buf) throws IOException {}

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        return null;
    }
}
