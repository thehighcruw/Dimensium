/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Lightweight per-thread span recorder. Not thread-safe across threads by design —
 * each thread gets its own trace. Typical usage:
 *
 * <pre>
 * PerfTrace.begin("myOp");
 * PerfTrace.push("step1");
 * doStep1();
 * PerfTrace.pop();
 * PerfTrace.push("step2");
 * doStep2();
 * PerfTrace.pop();
 * PerfTrace.end(5); // dumps if total > 5ms
 * </pre>
 */
public final class PerfTrace {

    private static final ThreadLocal<PerfTrace> CURRENT = new ThreadLocal<>();

    private final String rootName;
    private final long rootStart;
    private final List<Span> spans = new ArrayList<>();
    private final Deque<long[]> stack = new ArrayDeque<>(); // {startNanos, depth}
    private int depth = 0;

    private PerfTrace(String name) {
        this.rootName = name;
        this.rootStart = System.nanoTime();
    }

    /** Start a new trace on this thread, replacing any existing one. */
    public static void begin(String name) {
        CURRENT.set(new PerfTrace(name));
    }

    /** Open a named span. Must be paired with {@link #pop()}. */
    public static void push(String name) {
        PerfTrace t = CURRENT.get();
        if (t == null) return;
        t.stack.push(new long[] {System.nanoTime(), t.depth, spans_index(t, name)});
        t.depth++;
    }

    private static int spans_index(PerfTrace t, String name) {
        int idx = t.spans.size();
        t.spans.add(new Span(name, t.depth));
        return idx;
    }

    /** Close the most recently opened span. */
    public static void pop() {
        PerfTrace t = CURRENT.get();
        if (t == null || t.stack.isEmpty()) return;
        long[] frame = t.stack.pop();
        long elapsed = System.nanoTime() - frame[0];
        t.depth = (int) frame[1];
        t.spans.get((int) frame[2]).ms = elapsed / 1_000_000L;
    }

    /**
     * End the trace and dump results to STDERR if total elapsed exceeds {@code thresholdMs}.
     * Pass 0 to always dump.
     */
    public static void end(long thresholdMs) {
        PerfTrace t = CURRENT.get();
        CURRENT.remove();
        if (t == null) return;
        // close any unclosed spans
        while (!t.stack.isEmpty()) pop();
        long totalMs = (System.nanoTime() - t.rootStart) / 1_000_000L;
        if (totalMs < thresholdMs) return;
        StringBuilder sb = new StringBuilder();
        sb.append("[DIMTRACE] ")
                .append(t.rootName)
                .append(" total=")
                .append(totalMs)
                .append("ms\n");
        for (Span s : t.spans) {
            for (int i = 0; i < s.depth; i++) sb.append("  ");
            sb.append(s.name).append(' ').append(s.ms).append("ms\n");
        }
        System.err.print(sb);
    }

    private static final class Span {

        final String name;
        final int depth;
        long ms;

        Span(String name, int depth) {
            this.name = name;
            this.ms = 0;
            this.depth = depth;
        }
    }
}
