package github.thehighcruw.dimensium.network;

import java.io.IOException;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.tool.BrushApplicator;

/** Applies the currently selected brush/paint tool at the given block position. */
public class PacketBrushUse implements IPacket {

    private int blockX, blockY, blockZ;

    public PacketBrushUse() {}

    public PacketBrushUse(int blockX, int blockY, int blockZ) {
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(blockX);
        buf.writeInt(blockY);
        buf.writeInt(blockZ);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        blockX = buf.readInt();
        blockY = buf.readInt();
        blockZ = buf.readInt();
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
        BrushApplicator.applyTool(world, blockX, blockY, blockZ);
        return null;
    }
}
