/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.BuilderToolState.Phase;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.PacketBuffer;

/** Server → client: block data for a captured AABB. Chunked for large selections. */
public class PacketCaptureResponse implements IPacket {

    private static final int CHUNK_SIZE = 1000;

    private int txId;
    private boolean isFinalChunk;
    private int originX, originY, originZ;
    private int width, height, depth;
    private List<int[]> blocks; // {worldX, worldY, worldZ, blockId, meta}

    public PacketCaptureResponse() {}

    private PacketCaptureResponse(
            int txId,
            boolean isFinalChunk,
            int originX,
            int originY,
            int originZ,
            int width,
            int height,
            int depth,
            List<int[]> blocks) {
        this.txId = txId;
        this.isFinalChunk = isFinalChunk;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.blocks = blocks;
    }

    public static void sendChunked(
            EntityPlayerMP player,
            int txId,
            int originX,
            int originY,
            int originZ,
            int width,
            int height,
            int depth,
            List<int[]> allBlocks) {
        if (allBlocks.isEmpty()) {
            PacketHandler.CHANNEL.sendTo(
                    new PacketCaptureResponse(
                            txId, true, originX, originY, originZ, width, height, depth, new ArrayList<>()),
                    player);
            return;
        }
        for (int start = 0; start < allBlocks.size(); start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, allBlocks.size());
            PacketHandler.CHANNEL.sendTo(
                    new PacketCaptureResponse(
                            txId,
                            end == allBlocks.size(),
                            originX,
                            originY,
                            originZ,
                            width,
                            height,
                            depth,
                            new ArrayList<>(allBlocks.subList(start, end))),
                    player);
        }
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(txId);
        buf.writeBoolean(isFinalChunk);
        buf.writeInt(originX);
        buf.writeInt(originY);
        buf.writeInt(originZ);
        buf.writeInt(width);
        buf.writeInt(height);
        buf.writeInt(depth);
        PacketBlockList.encodeBlocks(buf, blocks);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        txId = buf.readInt();
        isFinalChunk = buf.readBoolean();
        originX = buf.readInt();
        originY = buf.readInt();
        originZ = buf.readInt();
        width = buf.readInt();
        height = buf.readInt();
        depth = buf.readInt();
        blocks = PacketBlockList.decodeBlocks(buf);
    }

    // ── Client-side reassembly ────────────────────────────────────────────────

    private static final Map<Integer, List<int[]>> pendingBlocks = new HashMap<>();
    private static final Map<Integer, int[]> pendingOrigin = new HashMap<>(); // {ox, oy, oz, w, h, d}

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        pendingBlocks.computeIfAbsent(txId, k -> new ArrayList<>()).addAll(blocks);
        pendingOrigin.putIfAbsent(txId, new int[] {originX, originY, originZ, width, height, depth});

        if (!isFinalChunk) return null;

        List<int[]> allBlocks = pendingBlocks.remove(txId);
        int[] origin = pendingOrigin.remove(txId);
        if (allBlocks == null || origin == null) return null;

        BuilderToolState bts = BuilderToolState.INSTANCE;
        if (bts.phase != Phase.CAPTURING) return null; // user cancelled

        Vec3DInt originCoord = Vec3DInt.from(origin[0], origin[1], origin[2]);
        int w = origin[3], h = origin[4], d = origin[5];

        long _t0 = System.nanoTime();
        Map<Long, SelectionState.BlockData> clipboard = new HashMap<>();
        for (int[] b : allBlocks) {
            Block blk = Block.getBlockById(b[3]);
            if (blk == null || blk == Blocks.air) continue;
            Vec3DInt local = Vec3DInt.from(b[0], b[1], b[2]).minus(originCoord);
            int lx = local.x(), ly = local.y(), lz = local.z();
            clipboard.put(SelectionState.clipboardKey(lx, ly, lz), new SelectionState.BlockData(blk, b[4]));
        }
        System.err.println(
                "[DIMTIMER] PacketCaptureResponse.executeClient clipboard=" + (System.nanoTime() - _t0) / 1_000_000
                        + "ms blocks="
                        + allBlocks.size());

        SelectionState sel = SelectionState.INSTANCE;
        sel.clipboard = clipboard;
        sel.clipDim = Vec3DInt.from(w, h, d);
        sel.clipboardVersion++;

        bts.phase = Phase.MANIPULATING;
        bts.offset = Vec3DInt.ZERO;

        return null;
    }
}
