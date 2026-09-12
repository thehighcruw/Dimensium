package github.thehighcruw.dimensium.handler;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import github.thehighcruw.dimensium.history.ServerEditQueue;
import github.thehighcruw.dimensium.network.PacketBlockList;

public class PlayerHistoryEventHandler {

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        PacketBlockList.clearPending(event.player.getUniqueID());
        ServerEditQueue.clearPlayer(event.player.getUniqueID());
    }
}
