/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;

@SideOnly(Side.CLIENT)
public final class PipelineLibrary {

    public static final PipelineLibrary INSTANCE = new PipelineLibrary();

    private final List<PipelineGraph> graphs = new ArrayList<>();
    private final Set<String> builtinNames = new HashSet<>();
    private final Set<String> builtinFolders = new HashSet<>();
    private File saveDir;
    private boolean initialized = false;

    private PipelineLibrary() {}

    private void ensureInit() {
        if (initialized) return;
        initialized = true;
        File gameDir = Minecraft.getMinecraft().mcDataDir;
        saveDir = new File(gameDir, "dimensium/pipelines");
        saveDir.mkdirs();
    }

    public void init(File gameDir) {
        initialized = true;
        saveDir = new File(gameDir, "dimensium/pipelines");
        saveDir.mkdirs();
    }

    public void loadDefaults(List<PipelineGraph> defaults) {
        ensureInit();
        for (PipelineGraph g : defaults) {
            graphs.add(g);
            builtinNames.add(g.name);
            if (g.folder != null) builtinFolders.add(g.folder);
        }
        loadFromDisk();
    }

    private void loadFromDisk() {
        if (saveDir == null || !saveDir.exists()) return;
        loadDir(saveDir, null);
    }

    private void loadDir(File dir, String folderPath) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                String childPath = folderPath == null ? file.getName() : folderPath + "/" + file.getName();
                loadDir(file, childPath);
            } else if (file.getName().endsWith(".json")) {
                try (Reader reader = new FileReader(file)) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[4096];
                    int read;
                    while ((read = reader.read(buf)) != -1) sb.append(buf, 0, read);
                    PipelineGraph g = PipelineGraph.fromJson(sb.toString());
                    g.folder = folderPath;
                    if (!builtinNames.contains(g.name)) graphs.add(g);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void save(PipelineGraph graph) {
        ensureInit();
        if (!graphs.contains(graph)) graphs.add(graph);
        if (saveDir == null) return;
        writeFile(graph);
    }

    private void writeFile(PipelineGraph graph) {
        if (saveDir == null) return;
        File file = fileFor(graph);
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            writer.write(graph.toJson());
        } catch (Exception ignored) {
        }
    }

    private File fileFor(PipelineGraph graph) {
        return new File(folderDir(graph.folder), toFilename(graph.name));
    }

    private File folderDir(String folder) {
        if (folder == null) return saveDir;
        return new File(saveDir, folder.replace('/', File.separatorChar));
    }

    public void delete(PipelineGraph graph) {
        if (builtinNames.contains(graph.name)) return;
        graphs.remove(graph);
        if (saveDir != null) {
            fileFor(graph).delete();
        }
    }

    /**
     * Renames a pipeline. Deletes the old file and writes a new one.
     * Returns false if the new name is already taken.
     */
    public boolean rename(PipelineGraph graph, String newName) {
        String trimmed = newName.trim();
        if (trimmed.isEmpty()) return false;
        if (trimmed.equals(graph.name)) return true;
        if (hasName(trimmed)) return false;
        if (saveDir != null) fileFor(graph).delete();
        graph.name = trimmed;
        save(graph);
        return true;
    }

    /** Moves a pipeline to the given folder (null = root). Saves the change immediately. */
    public void moveToFolder(PipelineGraph graph, String folder) {
        if (builtinNames.contains(graph.name)) return;
        if (saveDir != null) fileFor(graph).delete();
        graph.folder = folder;
        save(graph);
    }

    /** Creates an empty named folder on disk. No-op if already exists. */
    public void createFolder(String name) {
        ensureInit();
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return;
        folderDir(trimmed).mkdirs();
    }

    /** Deletes an empty folder from disk. Does not affect pipelines. */
    public void removeExplicitFolder(String name) {
        if (saveDir == null) return;
        folderDir(name).delete();
    }

    public boolean isBuiltinFolder(String name) {
        return builtinFolders.contains(name);
    }

    public boolean hasName(String name) {
        for (PipelineGraph g : graphs) {
            if (g.name.equals(name)) return true;
        }
        return false;
    }

    /** Returns a name derived from {@code base} that is not already in use. */
    public String uniqueName(String base) {
        if (!hasName(base)) return base;
        int counter = 2;
        while (hasName(base + " (" + counter + ")")) counter++;
        return base + " (" + counter + ")";
    }

    private static String toFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".json";
    }

    public boolean isBuiltin(PipelineGraph graph) {
        return builtinNames.contains(graph.name);
    }

    public boolean hasDefaults() {
        return !builtinNames.isEmpty();
    }

    public List<PipelineGraph> all() {
        ensureInit();
        return Collections.unmodifiableList(graphs);
    }

    public PipelineGraph createNew(String name) {
        ensureInit();
        PipelineGraph g = new PipelineGraph(uniqueName(name));
        graphs.add(g);
        return g;
    }

    /** Returns all distinct non-null folder paths, derived from loaded graphs and subdirectories on disk. */
    public List<String> allFolders() {
        List<String> folders = new ArrayList<>();
        for (PipelineGraph g : graphs) {
            if (g.folder != null && !folders.contains(g.folder)) folders.add(g.folder);
        }
        if (saveDir != null && saveDir.exists()) collectDirs(saveDir, null, folders);
        return folders;
    }

    private void collectDirs(File dir, String path, List<String> result) {
        File[] children = dir.listFiles(File::isDirectory);
        if (children == null) return;
        for (File child : children) {
            String childPath = path == null ? child.getName() : path + "/" + child.getName();
            if (!result.contains(childPath)) result.add(childPath);
            collectDirs(child, childPath, result);
        }
    }
}
