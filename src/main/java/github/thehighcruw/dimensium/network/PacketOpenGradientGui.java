package github.thehighcruw.dimensium.network;

import java.io.IOException;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.handler.GradientGuiHandler;

public class PacketOpenGradientGui implements IPacket {

    @Override
    public void encode(PacketBuffer buf) throws IOException {}

    @Override
    public void decode(PacketBuffer buf) throws IOException {}

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        handler.playerEntity.openGui(
            Dimensium.instance,
            GradientGuiHandler.GUI_ID,
            handler.playerEntity.worldObj,
            (int) handler.playerEntity.posX,
            (int) handler.playerEntity.posY,
            (int) handler.playerEntity.posZ);
        return null;
    }
}
