/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.blueprint;

import github.thehighcruw.dimensium.editor.clipboard.ClipboardBlock;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/**
 * Serialises blueprints in a two-section format (v2):
 *
 * [4 bytes magic 0x444D4250]
 * [4 bytes: header section byte length]
 * [N bytes: uncompressed NBT — name, version, tags, clipW/H/D]
 * [remaining: gzip-compressed NBT — palette (string list) + offsets int[x,y,z,paletteIdx,meta]]
 *
 * v1 files (numeric block IDs, no palette) are migrated to registry names on read using the
 * current session's block registry. Thumbnail PNGs are stored as sidecar files (&lt;file&gt;.dblueprint.png).
 */
public class BlueprintIO {

    private static final int MAGIC = 0x444D4250; // "DMBP"
    private static final int FORMAT_VERSION = 2;

    public static File getBlueprintsDir() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "dimensium/blueprints");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Failed to create blueprints directory: " + dir);
        }
        return dir;
    }

    public static void save(Blueprint bp, File dir) throws IOException {
        String filename = sanitize(bp.name().isEmpty() ? "blueprint" : bp.name()) + ".dblueprint";
        File file = new File(dir, filename);

        NBTTagCompound headerTag = new NBTTagCompound();
        headerTag.setString("name", bp.name());
        headerTag.setInteger("version", FORMAT_VERSION);
        NBTTagList tagList = new NBTTagList();
        for (String tg : bp.tags()) tagList.appendTag(new NBTTagString(tg));
        headerTag.setTag("tags", tagList);
        headerTag.setInteger("clipW", bp.clipDim().x());
        headerTag.setInteger("clipH", bp.clipDim().y());
        headerTag.setInteger("clipD", bp.clipDim().z());

        // Build palette: unique registry names in encounter order.
        List<String> palette = new ArrayList<>();
        Map<String, Integer> paletteIndex = new HashMap<>();
        for (ClipboardBlock o : bp.offsets()) {
            if (!paletteIndex.containsKey(o.registryName())) {
                paletteIndex.put(o.registryName(), palette.size());
                palette.add(o.registryName());
            }
        }
        NBTTagList paletteTag = new NBTTagList();
        for (String name : palette) paletteTag.appendTag(new NBTTagString(name));

        NBTTagCompound bodyTag = new NBTTagCompound();
        bodyTag.setTag("palette", paletteTag);
        int n = bp.offsets().size();
        int[] flat = new int[n * 5];
        for (int i = 0; i < n; i++) {
            ClipboardBlock o = bp.offsets().get(i);
            flat[i * 5] = o.offset().x();
            flat[i * 5 + 1] = o.offset().y();
            flat[i * 5 + 2] = o.offset().z();
            flat[i * 5 + 3] = paletteIndex.get(o.registryName());
            flat[i * 5 + 4] = o.meta();
        }
        bodyTag.setIntArray("offsets", flat);

        ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
        CompressedStreamTools.write(headerTag, new DataOutputStream(headerBuf));
        byte[] headerBytes = headerBuf.toByteArray();

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeInt(MAGIC);
            out.writeInt(headerBytes.length);
            out.write(headerBytes);
            CompressedStreamTools.writeCompressed(bodyTag, out);
        }

        if (bp.thumbnailPng() != null) Files.write(sidecarFor(file).toPath(), bp.thumbnailPng());
    }

    /** Reads only name/tags/dims. O(header size), not O(block count). */
    public static Blueprint loadHeader(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            return readHeader(in);
        }
    }

    /** Reads full blueprint including block offsets. */
    public static Blueprint load(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            NBTTagCompound headerTag = readHeaderTag(in);
            Blueprint header = fromHeaderTag(headerTag);
            int version = headerTag.getInteger("version");
            NBTTagCompound bodyTag = CompressedStreamTools.readCompressed(in);
            List<ClipboardBlock> offsets = version >= FORMAT_VERSION
                    ? decodeOffsets(bodyTag)
                    : decodeOffsetLegacy(bodyTag.getIntArray("offsets"));
            return new Blueprint(header.name(), header.tags(), header.clipDim(), offsets, null);
        }
    }

    private static Blueprint readHeader(DataInputStream in) throws IOException {
        return fromHeaderTag(readHeaderTag(in));
    }

    private static NBTTagCompound readHeaderTag(DataInputStream in) throws IOException {
        int magic = in.readInt();
        if (magic != MAGIC) throw new IOException("trying to open blueprint file that is not a blueprint");
        int headerLen = in.readInt();
        byte[] headerBytes = new byte[headerLen];
        readFully(in, headerBytes);
        return CompressedStreamTools.read(new DataInputStream(new ByteArrayInputStream(headerBytes)));
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
            else if (f.getName().endsWith(".dblueprint")) result.add(f);
        }
    }

    public static List<String> collectAllTags(File dir) {
        List<String> all = new ArrayList<>();
        for (File f : scanBlueprints(dir)) {
            try {
                Blueprint bp = loadHeader(f);
                for (String t : bp.tags()) if (!all.contains(t)) all.add(t);
            } catch (Exception ignored) {
            }
        }
        Collections.sort(all);
        return all;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static Blueprint fromHeaderTag(NBTTagCompound tag) {
        NBTTagList tagList = tag.getTagList("tags", 8);
        List<String> tags = new ArrayList<>(tagList.tagCount());
        for (int i = 0; i < tagList.tagCount(); i++) tags.add(tagList.getStringTagAt(i));
        return new Blueprint(
                tag.getString("name"),
                tags,
                Vec3DInt.from(tag.getInteger("clipW"), tag.getInteger("clipH"), tag.getInteger("clipD")),
                new ArrayList<>(),
                null);
    }

    private static List<ClipboardBlock> decodeOffsets(NBTTagCompound bodyTag) {
        NBTTagList paletteTag = bodyTag.getTagList("palette", 8);
        String[] palette = new String[paletteTag.tagCount()];
        for (int i = 0; i < palette.length; i++) palette[i] = paletteTag.getStringTagAt(i);
        int[] flat = bodyTag.getIntArray("offsets");
        List<ClipboardBlock> offsets = new ArrayList<>(flat.length / 5);
        for (int i = 0; i + 4 < flat.length; i += 5) {
            int paletteIdx = flat[i + 3];
            if (paletteIdx < 0 || paletteIdx >= palette.length) continue;
            offsets.add(new ClipboardBlock(
                    Vec3DInt.from(flat[i], flat[i + 1], flat[i + 2]), palette[paletteIdx], flat[i + 4]));
        }
        return offsets;
    }

    /**
     * Migrates v1 blueprints (numeric IDs) to registry names using the current session's block registry.
     * Best-effort: IDs must be valid in the current world's registry mapping.
     */
    private static List<ClipboardBlock> decodeOffsetLegacy(int[] flat) {
        List<ClipboardBlock> offsets = new ArrayList<>(flat.length / 5);
        for (int i = 0; i + 4 < flat.length; i += 5) {
            Block block = Block.getBlockById(flat[i + 3]);
            String registryName =
                    block != null ? (String) Block.blockRegistry.getNameForObject(block) : "minecraft:air";
            offsets.add(
                    new ClipboardBlock(Vec3DInt.from(flat[i], flat[i + 1], flat[i + 2]), registryName, flat[i + 4]));
        }
        return offsets;
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
        return name.replaceAll("[^a-zA-Z0-9_.\\- ]", "_").trim().replace(' ', '_');
    }
}
