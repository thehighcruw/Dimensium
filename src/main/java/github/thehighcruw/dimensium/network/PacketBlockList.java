/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.network;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.editor.history.EditHistory;
import github.thehighcruw.dimensium.editor.history.ServerEditQueue;
import github.thehighcruw.dimensium.shared.util.PerfTrace;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

/** Arbitrary list of (x, y, z, blockId, meta) placements executed server-side. */
public class PacketBlockList implements IPacket {

    public List<int[]> blocks; // each int[5]: {x, y, z, blockId, meta}
    public String action = "Edit";
    public int transactionId = 0;
    public boolean isFinalChunk = true;
    public boolean skipHistory = false;

    // ── Server-side transaction accumulator ───────────────────────────────────
    // Keyed by player UUID → (transactionId → accumulated ops/action/skipHistory).
    private static final Map<UUID, Map<Integer, List<int[]>>> pendingOps = new HashMap<>();
    private static final Map<UUID, Map<Integer, String>> pendingActions = new HashMap<>();
    private static final Map<UUID, Map<Integer, Boolean>> pendingSkip = new HashMap<>();

    /** Called on player logout to prevent memory leaks from incomplete transactions. */
    public static void clearPending(UUID playerId) {
        pendingOps.remove(playerId);
        pendingActions.remove(playerId);
        pendingSkip.remove(playerId);
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public PacketBlockList() {
        blocks = new ArrayList<>();
    }

    public PacketBlockList(List<int[]> blocks, String action, int transactionId, boolean isFinalChunk) {
        this.blocks = blocks;
        this.action = action;
        this.transactionId = transactionId;
        this.isFinalChunk = isFinalChunk;
    }

    public PacketBlockList(
            List<int[]> blocks, String action, int transactionId, boolean isFinalChunk, boolean skipHistory) {
        this(blocks, action, transactionId, isFinalChunk);
        this.skipHistory = skipHistory;
    }

    // ── Wire format ───────────────────────────────────────────────────────────

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        PacketUtils.writeString(buf, action);
        buf.writeInt(transactionId);
        buf.writeBoolean(isFinalChunk);
        buf.writeBoolean(skipHistory);
        encodeBlocks(buf, blocks);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        action = PacketUtils.readString(buf);
        transactionId = buf.readInt();
        isFinalChunk = buf.readBoolean();
        skipHistory = buf.readBoolean();
        blocks = decodeBlocks(buf);
    }

    static void encodeBlocks(PacketBuffer buf, List<int[]> blocks) throws IOException {
        buf.writeInt(blocks.size());
        for (int[] b : blocks) {
            buf.writeInt(b[0]);
            buf.writeInt(b[1]);
            buf.writeInt(b[2]);
            buf.writeInt(b[3]);
            buf.writeShort(b[4]);
        }
    }

    static List<int[]> decodeBlocks(PacketBuffer buf) throws IOException {
        int count = buf.readInt();
        List<int[]> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new int[] {buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readShort() & 0xFFFF});
        }
        return list;
    }

    // ── Server execution ──────────────────────────────────────────────────────

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        if (!handler.playerEntity.capabilities.isCreativeMode) {
            Dimensium.logger.warn(
                    "[Dimensium] Rejected PacketBlockList from non-creative player {}",
                    handler.playerEntity.getCommandSenderName());
            return null;
        }

        UUID pid = handler.playerEntity.getUniqueID();

        if (!isFinalChunk) {
            pendingOps
                    .computeIfAbsent(pid, k -> new HashMap<>())
                    .computeIfAbsent(transactionId, k -> new ArrayList<>())
                    .addAll(blocks);
            pendingActions.computeIfAbsent(pid, k -> new HashMap<>()).putIfAbsent(transactionId, action);
            pendingSkip.computeIfAbsent(pid, k -> new HashMap<>()).putIfAbsent(transactionId, skipHistory);
            return null;
        }

        // Final chunk: merge all accumulated chunks, then apply/record as one operation.
        Map<Integer, List<int[]>> playerOps = pendingOps.getOrDefault(pid, Collections.emptyMap());
        Map<Integer, String> playerActions = pendingActions.getOrDefault(pid, Collections.emptyMap());
        Map<Integer, Boolean> playerSkip = pendingSkip.getOrDefault(pid, Collections.emptyMap());

        List<int[]> accumulated = playerOps.remove(transactionId);
        String fullAction = playerActions.remove(transactionId);
        Boolean shouldSkip = playerSkip.remove(transactionId);

        if (accumulated == null) accumulated = new ArrayList<>(blocks.size());
        if (fullAction == null) fullAction = action;
        if (shouldSkip == null) shouldSkip = skipHistory;
        accumulated.addAll(blocks);

        if (shouldSkip) {
            // Flush any still-draining edit before applying undo/redo so the
            // before-state isn't overwritten by ops that haven't landed yet.
            PerfTrace.begin("[SERVER] PacketBlockList skipHistory ops=" + accumulated.size());
            PerfTrace.push("drainPlayer");
            ServerEditQueue.drainPlayer(pid);
            PerfTrace.pop();
            PerfTrace.push("applyBlocks");
            EditHistory.applyBlocks(handler.playerEntity.worldObj, accumulated);
        } else {
            // Enqueue for deferred processing — no world access here so executeServer()
            // returns immediately without stalling the main thread.
            PerfTrace.begin("[SERVER] PacketBlockList enqueue ops=" + accumulated.size());
            PerfTrace.push("enqueue");
            ServerEditQueue.enqueue(
                    pid, handler.playerEntity.worldObj, handler.playerEntity, transactionId, fullAction, accumulated);
        }
        PerfTrace.pop();
        PerfTrace.end(0);
        return null;
    }
}
