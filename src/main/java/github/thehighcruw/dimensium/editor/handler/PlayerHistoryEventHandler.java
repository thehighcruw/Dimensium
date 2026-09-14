/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import github.thehighcruw.dimensium.editor.history.ServerEditQueue;
import github.thehighcruw.dimensium.network.PacketBlockList;
import net.minecraft.entity.player.EntityPlayerMP;

public class PlayerHistoryEventHandler {

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        PacketBlockList.clearPending(event.player.getUniqueID());
        ServerEditQueue.clearPlayer(event.player.getUniqueID());
    }
}
