/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.history.ClientEditHistory;
import github.thehighcruw.dimensium.shared.BlockSender;

/**
 * Server → client. Carries one history entry (before + after block states).
 * Large entries are chunked; the client reassembles via txId.
 *
 * For ops that originated from the client (sendChunked), the after-state is
 * already buffered in BlockSender.pendingAfterOps and is not sent over the wire.
 * For server-originated ops (e.g. PacketShapePlacement), after-state is included.
 */
public class PacketHistoryEntry implements IPacket {

    private static final int CHUNK_SIZE = 750; // block-slots per packet (~25 KB with both arrays)

    // ── Fields (per packet) ───────────────────────────────────────────────────

    private int txId;
    private boolean isFinalChunk;
    private boolean includesAfter; // true when server sends after-state explicitly
    private String action = "";
    private List<int[]> beforeChunk = new ArrayList<>(); // {x,y,z,id,meta}
    private List<int[]> afterChunk = new ArrayList<>(); // {x,y,z,id,meta} — only when includesAfter

    public PacketHistoryEntry() {}

    private PacketHistoryEntry(int txId, boolean isFinalChunk, boolean includesAfter, String action, List<int[]> before,
        List<int[]> after) {
        this.txId = txId;
        this.isFinalChunk = isFinalChunk;
        this.includesAfter = includesAfter;
        this.action = action;
        this.beforeChunk = before;
        this.afterChunk = after;
    }

    /**
     * Used by server-originated ops (PacketShapePlacement): after-state is unknown
     * to the client, so it must be included.
     */
    public static void sendChunked(EntityPlayerMP player, int txId, String action, int[][] before, int[][] after) {
        boolean hasAfter = (after != null && after.length > 0);
        int total = before.length;
        for (int start = 0; start < total; start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, total);
            boolean fin = (end == total);

            List<int[]> bChunk = new ArrayList<>(end - start);
            for (int i = start; i < end; i++) bChunk.add(before[i]);

            List<int[]> aChunk = new ArrayList<>();
            if (hasAfter) {
                int aEnd = Math.min(start + CHUNK_SIZE, after.length);
                for (int i = start; i < aEnd; i++) aChunk.add(after[i]);
            }

            PacketHandler.CHANNEL.sendTo(new PacketHistoryEntry(txId, fin, hasAfter, action, bChunk, aChunk), player);
        }
    }

    // ── Wire format ───────────────────────────────────────────────────────────

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(txId);
        buf.writeBoolean(isFinalChunk);
        buf.writeBoolean(includesAfter);
        byte[] nameBytes = action.getBytes("UTF-8");
        buf.writeShort(nameBytes.length);
        buf.writeBytes(nameBytes);
        PacketBlockList.encodeBlocks(buf, beforeChunk);
        if (includesAfter) PacketBlockList.encodeBlocks(buf, afterChunk);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        txId = buf.readInt();
        isFinalChunk = buf.readBoolean();
        includesAfter = buf.readBoolean();
        int nameLen = buf.readShort() & 0xFFFF;
        byte[] nameBytes = new byte[nameLen];
        buf.readBytes(nameBytes);
        action = new String(nameBytes, "UTF-8");
        beforeChunk = PacketBlockList.decodeBlocks(buf);
        afterChunk = includesAfter ? PacketBlockList.decodeBlocks(buf) : new ArrayList<>();
    }

    // ── Client execution ──────────────────────────────────────────────────────

    private static final Map<Integer, List<int[]>> pendingBefore = new HashMap<>();
    private static final Map<Integer, List<int[]>> pendingAfter = new HashMap<>();
    private static final Map<Integer, String> pendingAction = new HashMap<>();

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(net.minecraft.client.network.NetHandlerPlayClient handler) {
        pendingBefore.computeIfAbsent(txId, k -> new ArrayList<>())
            .addAll(beforeChunk);
        pendingAction.putIfAbsent(txId, action);
        if (includesAfter) {
            pendingAfter.computeIfAbsent(txId, k -> new ArrayList<>())
                .addAll(afterChunk);
        }

        if (!isFinalChunk) return null;

        List<int[]> allBefore = pendingBefore.remove(txId);
        String act = pendingAction.remove(txId);
        if (allBefore == null) return null;

        // After-state: prefer explicitly sent (server-originated); fall back to client buffer.
        List<int[]> allAfter = pendingAfter.remove(txId);
        if (allAfter == null) allAfter = BlockSender.pendingAfterOps.remove(txId);
        if (allAfter == null) allAfter = new ArrayList<>();

        ClientEditHistory.INSTANCE.addEntry(act, allBefore.toArray(new int[0][]), allAfter.toArray(new int[0][]));
        return null;
    }
}
