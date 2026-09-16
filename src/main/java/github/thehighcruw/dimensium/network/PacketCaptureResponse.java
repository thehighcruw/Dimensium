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
    private Vec3DInt origin;
    private Vec3DInt dims;
    private List<int[]> blocks; // {worldX, worldY, worldZ, blockId, meta}

    public PacketCaptureResponse() {}

    private PacketCaptureResponse(int txId, boolean isFinalChunk, Vec3DInt origin, Vec3DInt dims, List<int[]> blocks) {
        this.txId = txId;
        this.isFinalChunk = isFinalChunk;
        this.origin = origin;
        this.dims = dims;
        this.blocks = blocks;
    }

    public static void sendChunked(
            EntityPlayerMP player, int txId, Vec3DInt origin, Vec3DInt dims, List<int[]> allBlocks) {
        if (allBlocks.isEmpty()) {
            PacketHandler.CHANNEL.sendTo(
                    new PacketCaptureResponse(txId, true, origin, dims, new ArrayList<>()), player);
            return;
        }
        for (int start = 0; start < allBlocks.size(); start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, allBlocks.size());
            PacketHandler.CHANNEL.sendTo(
                    new PacketCaptureResponse(
                            txId,
                            end == allBlocks.size(),
                            origin,
                            dims,
                            new ArrayList<>(allBlocks.subList(start, end))),
                    player);
        }
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(txId);
        buf.writeBoolean(isFinalChunk);
        buf.writeInt(origin.x());
        buf.writeInt(origin.y());
        buf.writeInt(origin.z());
        buf.writeInt(dims.x());
        buf.writeInt(dims.y());
        buf.writeInt(dims.z());
        PacketBlockList.encodeBlocks(buf, blocks);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        txId = buf.readInt();
        isFinalChunk = buf.readBoolean();
        origin = Vec3DInt.from(buf.readInt(), buf.readInt(), buf.readInt());
        dims = Vec3DInt.from(buf.readInt(), buf.readInt(), buf.readInt());
        blocks = PacketBlockList.decodeBlocks(buf);
    }

    // ── Client-side reassembly ────────────────────────────────────────────────

    private static final Map<Integer, List<int[]>> pendingBlocks = new HashMap<>();
    private static final Map<Integer, Vec3DInt> pendingOrigin = new HashMap<>();
    private static final Map<Integer, Vec3DInt> pendingDims = new HashMap<>();

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        pendingBlocks.computeIfAbsent(txId, k -> new ArrayList<>()).addAll(blocks);
        pendingOrigin.putIfAbsent(txId, origin);
        pendingDims.putIfAbsent(txId, dims);

        if (!isFinalChunk) return null;

        List<int[]> allBlocks = pendingBlocks.remove(txId);
        Vec3DInt originCoord = pendingOrigin.remove(txId);
        Vec3DInt clipDims = pendingDims.remove(txId);
        if (allBlocks == null || originCoord == null || clipDims == null) return null;

        BuilderToolState bts = BuilderToolState.INSTANCE;
        if (bts.phase != Phase.CAPTURING) return null; // user cancelled

        long _t0 = System.nanoTime();
        Map<Long, SelectionState.BlockData> clipboard = new HashMap<>();
        for (int[] b : allBlocks) {
            Block blk = Block.getBlockById(b[3]);
            if (blk == null || blk == Blocks.air) continue;
            Vec3DInt local = Vec3DInt.from(b[0], b[1], b[2]).minus(originCoord);
            clipboard.put(SelectionState.clipboardKey(local), new SelectionState.BlockData(blk, b[4]));
        }
        System.err.println(
                "[DIMTIMER] PacketCaptureResponse.executeClient clipboard=" + (System.nanoTime() - _t0) / 1_000_000
                        + "ms blocks="
                        + allBlocks.size());

        SelectionState sel = SelectionState.INSTANCE;
        sel.clipboard = clipboard;
        sel.clipDim = clipDims;
        sel.clipboardVersion++;

        bts.phase = Phase.MANIPULATING;
        bts.offset = Vec3DInt.ZERO;

        return null;
    }
}
