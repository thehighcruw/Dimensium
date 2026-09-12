/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import com.gtnewhorizon.gtnhlib.network.base.NetworkChannel;

public class PacketHandler {

    public static final NetworkChannel CHANNEL = new NetworkChannel("dimensium");

    public static void init() {
        CHANNEL.toServer(new PacketSelectionOp());
        CHANNEL.toServer(new PacketPaste());
        CHANNEL.toServer(new PacketShapePlacement());
        CHANNEL.toServer(new PacketBlockList());
        CHANNEL.toServer(new PacketBrushUse());
        CHANNEL.toServer(new PacketCaptureRequest());
        CHANNEL.toServer(new PacketOpenGradientGui());
        CHANNEL.toClient(new PacketHistoryEntry());
        CHANNEL.toClient(new PacketCaptureResponse());
    }
}
