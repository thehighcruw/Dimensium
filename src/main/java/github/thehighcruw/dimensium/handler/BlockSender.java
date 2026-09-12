package github.thehighcruw.dimensium.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.network.PacketBlockList;
import github.thehighcruw.dimensium.network.PacketHandler;
import github.thehighcruw.dimensium.tool.mask.ToolMaskRegistry;

@SideOnly(Side.CLIENT)
public class BlockSender {

    private static final int CHUNK_SIZE = 1500;
    private static final AtomicInteger TRANSACTION_ID_GEN = new AtomicInteger(1);

    // Stores after-state per txId so PacketHistoryEntry can retrieve it client-side.
    // ConcurrentHashMap: put happens on the send thread, remove on the main thread.
    public static final Map<Integer, List<int[]>> pendingAfterOps = new ConcurrentHashMap<>();

    // Background thread builds and stages PacketBlockList objects.
    // Main thread drains and sends them each client tick (see flushSendQueue()).
    // This keeps sendToServer on the game thread (safe for MC's loopback connection)
    // while moving all allocation and encoding off the render thread.
    private static final ExecutorService SEND_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "dimensium-build");
        t.setDaemon(true);
        return t;
    });
    private static final ConcurrentLinkedQueue<PacketBlockList> SEND_QUEUE = new ConcurrentLinkedQueue<>();

    /** Called from TickHandler.onClientTick — drains staged packets on the game thread. */
    public static void flushSendQueue() {
        PacketBlockList packet;
        while ((packet = SEND_QUEUE.poll()) != null) {
            PacketHandler.CHANNEL.sendToServer(packet);
        }
    }

    public static void sendChunked(List<int[]> ops) {
        sendChunked(ops, "Edit");
    }

    public static void sendChunked(final List<int[]> ops, final String action) {
        long t0 = System.nanoTime();
        List<int[]> filtered = ToolMaskRegistry.INSTANCE.filter(ops);
        long filterMs = (System.nanoTime() - t0) / 1_000_000;
        if (filterMs > 5) github.thehighcruw.dimensium.Dimensium.logger
            .info("[DIMTIMER] BlockSender filter={}ms in={} out={}", filterMs, ops.size(), filtered.size());
        if (filtered.isEmpty()) return;
        sendChunkedFiltered(filtered, action);
    }

    private static void sendChunkedFiltered(final List<int[]> ops, final String action) {
        if (ops.isEmpty()) return;
        final int txId = TRANSACTION_ID_GEN.incrementAndGet();
        pendingAfterOps.put(txId, ops);
        SEND_EXECUTOR.submit(() -> stageChunks(ops, action, txId, false));
    }

    /**
     * Lazy variant: supplier builds the ops list on the background thread so large
     * allocations (clipboardToPlacements, selectionToAirOps) don't block the render thread.
     * Callers must snapshot any mutable state as finals before calling.
     */
    public static void sendChunkedLazy(final Supplier<List<int[]>> opsBuilder, final String action) {
        final int txId = TRANSACTION_ID_GEN.incrementAndGet();
        SEND_EXECUTOR.submit(() -> {
            List<int[]> ops = opsBuilder.get();
            if (ops.isEmpty()) return;
            pendingAfterOps.put(txId, ops);
            stageChunks(ops, action, txId, false);
        });
    }

    /** Sends blocks to server for undo/redo replay — no history entry is created. */
    public static void sendChunkedSkipHistory(final List<int[]> ops) {
        if (ops.isEmpty()) return;
        final int txId = TRANSACTION_ID_GEN.incrementAndGet();
        SEND_EXECUTOR.submit(() -> stageChunks(ops, "", txId, true));
    }

    private static void stageChunks(List<int[]> ops, String action, int txId, boolean skipHistory) {
        for (int start = 0; start < ops.size(); start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, ops.size());
            boolean isFinal = (end == ops.size());
            SEND_QUEUE
                .add(new PacketBlockList(new ArrayList<>(ops.subList(start, end)), action, txId, isFinal, skipHistory));
        }
    }
}
