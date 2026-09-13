/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.history;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.client.Minecraft;

import com.github.bsideup.jabel.Desugar;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;

@SideOnly(Side.CLIENT)
public class ClientEditHistory {

    public static final ClientEditHistory INSTANCE = new ClientEditHistory();

    /**
     * @param before {x,y,z,id,meta}
     * @param after  {x,y,z,id,meta}
     */
    @Desugar
    public record Entry(String action, int[][] before, int[][] after) {

    }

    private final List<Entry> entries = new ArrayList<>();
    public int pointer = -1;
    private boolean loaded = false;

    // ── Read API (used by RightPanelSections) ─────────────────────────────────

    public List<String> actionNames() {
        ensureLoaded();
        List<String> names = new ArrayList<>(entries.size());
        for (Entry e : entries) names.add(e.action);
        return names;
    }

    // ── Called by PacketHistoryEntry.executeClient ────────────────────────────

    public void addEntry(String action, int[][] before, int[][] after) {
        ensureLoaded();
        while (entries.size() > pointer + 1) entries.remove(entries.size() - 1);
        entries.add(new Entry(action, before, after));
        pointer = entries.size() - 1;

        long maxBytes = (long) DimensiumConfig.editHistoryMaxMb * 1024L * 1024L;
        while (totalBytes() > maxBytes && !entries.isEmpty()) {
            entries.remove(0);
            pointer = Math.max(-1, pointer - 1);
        }
        save();
    }

    // ── Undo / Redo ───────────────────────────────────────────────────────────

    /** Returns the action name of the entry that would be undone, or null if nothing to undo. */
    public String peekUndoName() {
        ensureLoaded();
        if (pointer < 0 || pointer >= entries.size()) return null;
        return entries.get(pointer).action;
    }

    /** Returns the action name of the entry that would be redone, or null if nothing to redo. */
    public String peekRedoName() {
        ensureLoaded();
        int next = pointer + 1;
        if (next >= entries.size()) return null;
        return entries.get(next).action;
    }

    /** Returns the before-state to apply on undo, or null if nothing to undo. Does NOT move pointer. */
    public List<int[]> peekUndo() {
        ensureLoaded();
        if (pointer < 0) return null;
        return Arrays.asList(entries.get(pointer).before);
    }

    /** Returns the after-state expected in the world when undoing (used for mismatch check). */
    public int[][] peekUndoExpected() {
        ensureLoaded();
        if (pointer < 0) return null;
        return entries.get(pointer).after;
    }

    /** Commits the undo: moves pointer back and saves. Call after applying (or skipping) the blocks. */
    public void commitUndo() {
        if (pointer < 0) return;
        pointer--;
        save();
    }

    /** Returns the after-state to apply on redo, or null if nothing to redo. Does NOT move pointer. */
    public List<int[]> peekRedo() {
        ensureLoaded();
        int next = pointer + 1;
        if (next >= entries.size()) return null;
        return Arrays.asList(entries.get(next).after);
    }

    /** Returns the before-state expected in the world when redoing (used for mismatch check). */
    public int[][] peekRedoExpected() {
        ensureLoaded();
        int next = pointer + 1;
        if (next >= entries.size()) return null;
        return entries.get(next).before;
    }

    /** Commits the redo: advances pointer and saves. Call after applying (or skipping) the blocks. */
    public void commitRedo() {
        int next = pointer + 1;
        if (next >= entries.size()) return;
        pointer = next;
        save();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void ensureLoaded() {
        if (!loaded) {
            load();
            loaded = true;
        }
    }

    public long totalBytes() {
        long sum = 0;
        for (Entry e : entries) sum += (long) (e.before.length + e.after.length) * 20 + e.action.length() * 2L;
        return sum;
    }

    public long entryBytes(int i) {
        Entry e = entries.get(i);
        return (long) (e.before.length + e.after.length) * 20 + e.action.length() * 2L;
    }

    public void clear() {
        ensureLoaded();
        entries.clear();
        pointer = -1;
        save();
    }

    private File saveFile() {
        String uuid = Minecraft.getMinecraft().thePlayer.getUniqueID()
            .toString();
        return new File(Minecraft.getMinecraft().mcDataDir, "dimensium_history/" + uuid + ".dat");
    }

    public void save() {
        File file = saveFile();
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            return;
        }
        try (DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))))) {
            out.writeInt(pointer);
            out.writeInt(entries.size());
            for (Entry e : entries) {
                byte[] nameBytes = e.action.getBytes(StandardCharsets.UTF_8);
                out.writeInt(nameBytes.length);
                out.write(nameBytes);
                writeBlockArray(out, e.before);
                writeBlockArray(out, e.after);
            }
        } catch (IOException ignored) {}
    }

    private void load() {
        entries.clear();
        pointer = -1;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        File file = saveFile();
        if (!file.exists()) return;
        try (DataInputStream in = new DataInputStream(
            new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))))) {
            pointer = in.readInt();
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int nameLen = in.readInt();
                byte[] nameBytes = new byte[nameLen];
                in.readFully(nameBytes);
                String action = new String(nameBytes, StandardCharsets.UTF_8);
                int[][] before = readBlockArray(in);
                int[][] after = readBlockArray(in);
                entries.add(new Entry(action, before, after));
            }
        } catch (IOException ignored) {
            entries.clear();
            pointer = -1;
        }
    }

    private void writeBlockArray(DataOutputStream out, int[][] arr) throws IOException {
        out.writeInt(arr.length);
        for (int[] b : arr) {
            out.writeInt(b[0]);
            out.writeInt(b[1]);
            out.writeInt(b[2]);
            out.writeInt(b[3]);
            out.writeInt(b[4]);
        }
    }

    private int[][] readBlockArray(DataInputStream in) throws IOException {
        int len = in.readInt();
        int[][] arr = new int[len][5];
        for (int i = 0; i < len; i++) {
            arr[i][0] = in.readInt();
            arr[i][1] = in.readInt();
            arr[i][2] = in.readInt();
            arr[i][3] = in.readInt();
            arr[i][4] = in.readInt();
        }
        return arr;
    }
}
