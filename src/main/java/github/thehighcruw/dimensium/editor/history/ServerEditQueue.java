/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.history;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import github.thehighcruw.dimensium.network.PacketHistoryEntry;
import github.thehighcruw.dimensium.shared.util.PerfTrace;

/**
 * Drains large block edits across multiple server ticks to avoid stalling the
 * main thread (which is also the render thread in single-player).
 *
 * Flow per edit:
 * Tick 0 (capture): before-state is read from the world, PacketHistoryEntry sent to client.
 * Tick 1..N (drain): BLOCKS_PER_TICK blocks written via chunk internals (no per-block lighting).
 * Tick N+1 (finalize): sky light recalculated per affected chunk; clients notified.
 *
 * drainPlayer() forces a full synchronous flush before undo/redo.
 */
public class ServerEditQueue {

    public static final ServerEditQueue INSTANCE = new ServerEditQueue();

    /**
     * Blocks applied per server tick. Lighting is deferred to finalize, so this
     * can be much higher than the old 300. At 20 TPS = 40,000 blocks/sec.
     */
    private static final int BLOCKS_PER_TICK = 2000;
    // Keep markBlockForUpdate calls per chunk per tick below PlayerManager's S21 threshold (64).
    // Above 64 the manager sends S21PacketChunkData (full chunk resend) instead of S22
    // (multi-block-change), which forces the client to rebuild all 16 section renderers.
    private static final int NOTIFY_PER_CHUNK_PER_TICK = 60;

    private static final Map<UUID, Queue<PendingEdit>> queues = new HashMap<>();

    private static final class PendingEdit {

        final UUID playerId;
        final World world;
        final EntityPlayerMP player;
        final int txId;
        final String action;
        // null for client-originated ops (client holds after-state via pendingAfterOps)
        final int[][] after;
        final List<int[]> ops;

        boolean capturedBefore = false;
        int cursor = 0;
        // Chunk columns touched during drain; populated lazily. Packed as ((long)cx << 32) | (cz & 0xFFFFFFFFL).
        final Set<Long> affectedChunks = new HashSet<>();
        // Ops grouped by chunk key for throttled notification phase (populated when drain completes).
        // null = notify phase not started yet.
        Map<Long, Deque<int[]>> pendingNotify = null;

        PendingEdit(UUID playerId, World world, EntityPlayerMP player, int txId, String action, int[][] after,
            List<int[]> ops) {
            this.playerId = playerId;
            this.world = world;
            this.player = player;
            this.txId = txId;
            this.action = action;
            this.after = after;
            this.ops = ops;
        }
    }

    /**
     * Enqueue a client-originated edit for deferred processing.
     * No world access happens here — executeServer() returns immediately.
     */
    public static void enqueue(UUID playerId, World world, EntityPlayerMP player, int txId, String action,
        List<int[]> ops) {
        if (ops.isEmpty()) return;
        queues.computeIfAbsent(playerId, k -> new LinkedList<>())
            .add(new PendingEdit(playerId, world, player, txId, action, null, ops));
    }

    /**
     * Synchronously flush all pending ops for a player.
     * Must be called before undo/redo to prevent in-flight edits from
     * overwriting the restored before-state.
     */
    public static void drainPlayer(UUID playerId) {
        Queue<PendingEdit> q = queues.remove(playerId);
        if (q == null) return;
        for (PendingEdit edit : q) {
            if (!edit.capturedBefore) captureAndSend(edit);
            while (edit.cursor < edit.ops.size()) {
                applyOpFast(edit.world, edit.ops.get(edit.cursor), edit.affectedChunks);
                edit.cursor++;
            }
            // Synchronous finalize: sky light + notifications all at once (undo/redo path, player-triggered).
            EditHistory.finalizeChunks(edit.world, edit.affectedChunks, edit.ops);
        }
    }

    /** Remove pending ops for a player without applying (on logout). */
    public static void clearPlayer(UUID playerId) {
        queues.remove(playerId);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Queue<PendingEdit> q : queues.values()) {
            PendingEdit edit = q.peek();
            if (edit == null) continue;

            // Phase 0: capture before-state and send history packet.
            if (!edit.capturedBefore) {
                PerfTrace.begin("[SERVER] captureAndSend ops=" + edit.ops.size());
                PerfTrace.push("captureBeforeState");
                captureAndSend(edit);
                PerfTrace.pop();
                PerfTrace.end(0);
                continue;
            }

            // Phase 1: apply blocks at BLOCKS_PER_TICK rate.
            if (edit.cursor < edit.ops.size()) {
                PerfTrace.begin("[SERVER] drainEdit cursor=" + edit.cursor + "/" + edit.ops.size());
                int applied = 0;
                PerfTrace.push("applyBlocks");
                while (edit.cursor < edit.ops.size() && applied < BLOCKS_PER_TICK) {
                    applyOpFast(edit.world, edit.ops.get(edit.cursor), edit.affectedChunks);
                    edit.cursor++;
                    applied++;
                }
                PerfTrace.pop();
                // When drain completes, do skylight + light recalc in the same tick
                // (cheap), then build pendingNotify for the throttled notify phase.
                if (edit.cursor >= edit.ops.size()) {
                    PerfTrace.push("skylightAndLightRecalc chunks=" + edit.affectedChunks.size());
                    EditHistory.skylightAndLightRecalc(edit.world, edit.affectedChunks, edit.ops);
                    PerfTrace.pop();
                    PerfTrace.push("buildPendingNotify ops=" + edit.ops.size());
                    edit.pendingNotify = new HashMap<>();
                    for (int[] op : edit.ops) {
                        long ck = ((long) (op[0] >> 4) << 32) | ((op[2] >> 4) & 0xFFFFFFFFL);
                        edit.pendingNotify.computeIfAbsent(ck, k -> new ArrayDeque<>())
                            .add(op);
                    }
                    PerfTrace.pop();
                }
                PerfTrace.end(10);
                continue;
            }

            // Phase 2: drip-feed markBlockForUpdate at NOTIFY_PER_CHUNK_PER_TICK per chunk.
            // Staying below PlayerManager's threshold of 64 keeps it sending S22PacketMultiBlockChange
            // (lightweight) instead of S21PacketChunkData (full chunk resend + client rebuild of
            // all 16 section renderers).
            if (edit.pendingNotify != null && !edit.pendingNotify.isEmpty()) {
                PerfTrace.begin("[SERVER] notifyBlocks chunks=" + edit.pendingNotify.size());
                PerfTrace.push("markBlockForUpdate");
                Iterator<Map.Entry<Long, Deque<int[]>>> it = edit.pendingNotify.entrySet()
                    .iterator();
                while (it.hasNext()) {
                    Map.Entry<Long, Deque<int[]>> entry = it.next();
                    Deque<int[]> ops = entry.getValue();
                    int sent = 0;
                    while (!ops.isEmpty() && sent < NOTIFY_PER_CHUNK_PER_TICK) {
                        int[] op = ops.poll();
                        edit.world.markBlockForUpdate(op[0], op[1], op[2]);
                        sent++;
                    }
                    if (ops.isEmpty()) it.remove();
                }
                PerfTrace.pop();
                PerfTrace.end(10);
                if (edit.pendingNotify.isEmpty()) {
                    q.poll();
                }
            }
        }
    }

    private static void captureAndSend(PendingEdit edit) {
        int[][] before = new int[edit.ops.size()][5];
        for (int i = 0; i < edit.ops.size(); i++) {
            int[] op = edit.ops.get(i);
            int x = op[0], y = op[1], z = op[2];
            before[i] = new int[] { x, y, z, Block.getIdFromBlock(edit.world.getBlock(x, y, z)),
                EditHistory.getEffectiveMeta(edit.world, x, y, z) };
        }
        PacketHistoryEntry.sendChunked(edit.player, edit.txId, edit.action, before, edit.after);
        edit.capturedBefore = true;
    }

    private static void applyOpFast(World world, int[] op, Set<Long> affectedChunks) {
        int x = op[0], y = op[1], z = op[2];
        affectedChunks.add(((long) (x >> 4) << 32) | ((z >> 4) & 0xFFFFFFFFL));
        EditHistory.applyBlockFast(world, x, y, z, Block.getBlockById(op[3]), op[4]);
    }
}
