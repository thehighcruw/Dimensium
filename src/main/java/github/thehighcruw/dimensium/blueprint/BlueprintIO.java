/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.blueprint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/**
 * Serialises blueprints in a two-section format:
 *
 * [4 bytes magic 0x444D4250]
 * [4 bytes: header section byte length]
 * [N bytes: uncompressed NBT — name, tags, clipW/H/D]
 * [remaining: gzip-compressed NBT — offsets int[]]
 *
 * Old files (gzip magic) are detected and migrated transparently on read.
 * Thumbnail PNGs are stored as sidecar files (<file>.dblueprint.png).
 */
public class BlueprintIO {

    private static final int MAGIC = 0x444D4250; // "DMBP"

    public static File getBlueprintsDir() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "dimensium/blueprints");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Failed to create blueprints directory: " + dir);
        }
        return dir;
    }

    public static void save(Blueprint bp, File dir) throws IOException {
        String filename = sanitize(bp.name.isEmpty() ? "blueprint" : bp.name) + ".dblueprint";
        File file = new File(dir, filename);

        NBTTagCompound headerTag = new NBTTagCompound();
        headerTag.setString("name", bp.name);
        NBTTagList tagList = new NBTTagList();
        for (String tg : bp.tags) tagList.appendTag(new NBTTagString(tg));
        headerTag.setTag("tags", tagList);
        headerTag.setInteger("clipW", bp.clipW);
        headerTag.setInteger("clipH", bp.clipH);
        headerTag.setInteger("clipD", bp.clipD);

        NBTTagCompound bodyTag = new NBTTagCompound();
        int n = bp.offsets.size();
        int[] flat = new int[n * 5];
        for (int i = 0; i < n; i++) {
            int[] o = bp.offsets.get(i);
            flat[i * 5] = o[0];
            flat[i * 5 + 1] = o[1];
            flat[i * 5 + 2] = o[2];
            flat[i * 5 + 3] = o[3];
            flat[i * 5 + 4] = o[4];
        }
        bodyTag.setIntArray("offsets", flat);

        ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
        CompressedStreamTools.write(headerTag, new DataOutputStream(headerBuf));
        byte[] headerBytes = headerBuf.toByteArray();

        DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
        out.writeInt(MAGIC);
        out.writeInt(headerBytes.length);
        out.write(headerBytes);
        CompressedStreamTools.writeCompressed(bodyTag, out);
        out.close();

        if (bp.thumbnailPng != null) Files.write(sidecarFor(file).toPath(), bp.thumbnailPng);
    }

    /** Reads only name/tags/dims. O(header size), not O(block count). Migrates old-format files on first read. */
    public static Blueprint loadHeader(File file) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        try {
            int magic = in.readInt();
            if (magic != MAGIC) {
                in.close();
                return migrateLegacy(file);
            }
            int headerLen = in.readInt();
            byte[] headerBytes = new byte[headerLen];
            readFully(in, headerBytes);
            return parseHeader(new DataInputStream(new ByteArrayInputStream(headerBytes)));
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {}
        }
    }

    /** Reads full blueprint including block offsets. */
    public static Blueprint load(File file) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        try {
            int magic = in.readInt();
            if (magic != MAGIC) {
                in.close();
                return migrateLegacy(file);
            }
            int headerLen = in.readInt();
            byte[] headerBytes = new byte[headerLen];
            readFully(in, headerBytes);
            Blueprint bp = parseHeader(new DataInputStream(new ByteArrayInputStream(headerBytes)));
            NBTTagCompound bodyTag = CompressedStreamTools.readCompressed(in);
            int[] flat = bodyTag.getIntArray("offsets");
            bp.offsets = new ArrayList<>(flat.length / 5);
            for (int i = 0; i + 4 < flat.length; i += 5) {
                bp.offsets.add(new int[] { flat[i], flat[i + 1], flat[i + 2], flat[i + 3], flat[i + 4] });
            }
            return bp;
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {}
        }
    }

    public static File sidecarFor(File blueprintFile) {
        return new File(blueprintFile.getParentFile(), blueprintFile.getName() + ".png");
    }

    public static List<File> scanBlueprints(File dir) {
        List<File> result = new ArrayList<>();
        scanRecursive(dir, result);
        return result;
    }

    private static void scanRecursive(File dir, List<File> result) {
        File[] children = dir.listFiles();
        if (children == null) return;
        Arrays.sort(children);
        for (File f : children) {
            if (f.isDirectory()) scanRecursive(f, result);
            else if (f.getName()
                .endsWith(".dblueprint")) result.add(f);
        }
    }

    public static List<String> collectAllTags(File dir) {
        List<String> all = new ArrayList<>();
        for (File f : scanBlueprints(dir)) {
            try {
                Blueprint bp = loadHeader(f);
                for (String t : bp.tags) if (!all.contains(t)) all.add(t);
            } catch (Exception ignored) {}
        }
        Collections.sort(all);
        return all;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static Blueprint parseHeader(DataInputStream in) throws IOException {
        NBTTagCompound tag = CompressedStreamTools.read(in);
        Blueprint bp = new Blueprint();
        bp.name = tag.getString("name");
        NBTTagList tagList = tag.getTagList("tags", 8);
        for (int i = 0; i < tagList.tagCount(); i++) bp.tags.add(tagList.getStringTagAt(i));
        bp.clipW = tag.getInteger("clipW");
        bp.clipH = tag.getInteger("clipH");
        bp.clipD = tag.getInteger("clipD");
        return bp;
    }

    /** Reads an old gzip-NBT file, rewrites it in the new format, returns the header. */
    private static Blueprint migrateLegacy(File file) throws IOException {
        NBTTagCompound tag = CompressedStreamTools.read(file);
        Blueprint bp = new Blueprint();
        bp.name = tag.getString("name");
        NBTTagList tagList = tag.getTagList("tags", 8);
        for (int i = 0; i < tagList.tagCount(); i++) bp.tags.add(tagList.getStringTagAt(i));
        bp.clipW = tag.getInteger("clipW");
        bp.clipH = tag.getInteger("clipH");
        bp.clipD = tag.getInteger("clipD");
        int[] flat = tag.getIntArray("offsets");
        bp.offsets = new ArrayList<>(flat.length / 5);
        for (int i = 0; i + 4 < flat.length; i += 5) {
            bp.offsets.add(new int[] { flat[i], flat[i + 1], flat[i + 2], flat[i + 3], flat[i + 4] });
        }
        // Extract legacy embedded thumbnail to sidecar before rewriting
        File sidecar = sidecarFor(file);
        if (!sidecar.exists() && tag.hasKey("thumbnail")) {
            try {
                Files.write(sidecar.toPath(), tag.getByteArray("thumbnail"));
            } catch (IOException ignored) {}
        }
        // Rewrite in new format so next open is fast
        save(bp, file.getParentFile());
        return bp;
    }

    private static void readFully(DataInputStream in, byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) throw new IOException("Unexpected end of file");
            off += n;
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_.\\- ]", "_")
            .trim()
            .replace(' ', '_');
    }
}
