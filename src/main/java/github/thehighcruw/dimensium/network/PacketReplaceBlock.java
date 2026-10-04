/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import java.io.IOException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

/** Replaces the block at the given position with the player's held block. */
public class PacketReplaceBlock implements IPacket {

    private static final int BLOCK_BREAK_EFFECT_ID = 2001;

    private Vec3DInt coord;
    private int sideHit;
    private float hitX;
    private float hitY;
    private float hitZ;

    public PacketReplaceBlock() {}

    public PacketReplaceBlock(Vec3DInt coord, int sideHit, float hitX, float hitY, float hitZ) {
        this.coord = coord;
        this.sideHit = sideHit;
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(coord.x());
        buf.writeInt(coord.y());
        buf.writeInt(coord.z());
        buf.writeInt(sideHit);
        buf.writeFloat(hitX);
        buf.writeFloat(hitY);
        buf.writeFloat(hitZ);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        coord = Vec3DInt.from(buf.readInt(), buf.readInt(), buf.readInt());
        sideHit = buf.readInt();
        hitX = buf.readFloat();
        hitY = buf.readFloat();
        hitZ = buf.readFloat();
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        EntityPlayerMP player = handler.playerEntity;
        if (!player.capabilities.isCreativeMode) {
            Dimensium.logger.warn(
                    "[Dimensium] Rejected PacketReplaceBlock from non-creative player {}",
                    player.getCommandSenderName());
            return null;
        }
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemBlock itemBlock)) return null;

        World world = player.worldObj;
        Block newBlock = Block.getBlockFromItem(itemBlock);
        if (newBlock == null || newBlock == Blocks.air) return null;

        int x = coord.x();
        int y = coord.y();
        int z = coord.z();

        Block oldBlock = WorldUtils.getBlock(world, coord);
        int oldMeta = WorldUtils.getBlockMetadata(world, coord);

        // Play break particles + sound (null = visible to all clients).
        world.playAuxSFXAtEntity(
                null, BLOCK_BREAK_EFFECT_ID, x, y, z, Block.getIdFromBlock(oldBlock) + (oldMeta << 12));
        world.setBlock(x, y, z, Blocks.air, 0, 3);

        if (oldBlock instanceof BlockStairs && newBlock instanceof BlockStairs) {
            // Stairs → stairs: preserve facing + upside-down (bits 0-2) from existing stair.
            world.setBlock(x, y, z, newBlock, oldMeta & 7, 3);
        } else if (oldBlock instanceof BlockSlab && newBlock instanceof BlockSlab) {
            // Slab → slab: preserve top/bottom (bit 3) from existing slab, take variant from held item.
            int itemMeta = itemBlock.getMetadata(held.getItemDamage());
            world.setBlock(x, y, z, newBlock, (itemMeta & 7) | (oldMeta & 8), 3);
        } else if (oldBlock == newBlock) {
            // Same block (non-stair/slab) — preserve meta exactly.
            world.setBlock(x, y, z, newBlock, oldMeta, 3);
        } else {
            // Different block — place with player-facing orientation.
            int itemMeta = itemBlock.getMetadata(held.getItemDamage());
            int placedMeta = newBlock.onBlockPlaced(world, x, y, z, sideHit, hitX, hitY, hitZ, itemMeta);
            world.setBlock(x, y, z, newBlock, placedMeta, 3);
            newBlock.onBlockPlacedBy(world, x, y, z, player, held);
        }

        // Play block placement sound.
        world.playSoundEffect(
                x + 0.5,
                y + 0.5,
                z + 0.5,
                newBlock.stepSound.func_150496_b(),
                (newBlock.stepSound.getVolume() + 1.0F) / 2.0F,
                newBlock.stepSound.getPitch() * 0.8F);

        return null;
    }
}
