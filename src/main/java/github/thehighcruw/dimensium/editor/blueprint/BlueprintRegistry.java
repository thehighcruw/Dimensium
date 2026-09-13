/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.blueprint;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Keeps blueprint headers (name/tags/dims) for every file in the blueprints directory
 * loaded in memory at all times. Headers are loaded on a background thread at startup
 * and re-loaded whenever the directory modification time changes.
 */
@SideOnly(Side.CLIENT)
public class BlueprintRegistry {

    public static final BlueprintRegistry INSTANCE = new BlueprintRegistry();

    private final Map<File, Blueprint> headers = Collections.synchronizedMap(new LinkedHashMap<>());
    private long lastDirMtime = -1;
    private volatile boolean loading = false;

    private BlueprintRegistry() {}

    /** Called once from ClientProxy.postInit — kicks off background header load. */
    public void init() {
        scheduleReload();
    }

    /**
     * Re-scans if the blueprints directory has changed since the last scan.
     * Fast (stat only) when nothing changed.
     */
    public void refresh() {
        File dir = BlueprintIO.getBlueprintsDir();
        long mtime = dirMtime(dir);
        if (mtime != lastDirMtime) {
            lastDirMtime = mtime;
            scheduleReload();
        }
    }

    /** Snapshot of all loaded headers, in scan order. */
    public List<Map.Entry<File, Blueprint>> getAll() {
        synchronized (headers) {
            return new ArrayList<>(headers.entrySet());
        }
    }

    public boolean isLoading() {
        return loading;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void scheduleReload() {
        if (loading) return;
        loading = true;
        Thread t = new Thread(this::reload, "bp-registry-reload");
        t.setDaemon(true);
        t.start();
    }

    private void reload() {
        try {
            File dir = BlueprintIO.getBlueprintsDir();
            List<File> files = BlueprintIO.scanBlueprints(dir);
            Map<File, Blueprint> next = new LinkedHashMap<>();
            for (File f : files) {
                try {
                    next.put(f, BlueprintIO.loadHeader(f));
                } catch (Exception e) {
                    github.thehighcruw.dimensium.Dimensium.logger.warn("Failed to load blueprint header: {}", f, e);
                }
            }
            synchronized (headers) {
                headers.clear();
                headers.putAll(next);
            }
            lastDirMtime = dirMtime(dir);
        } finally {
            loading = false;
        }
    }

    private static long dirMtime(File dir) {
        File[] children = dir.listFiles();
        if (children == null) return dir.lastModified();
        long max = dir.lastModified();
        for (File f : children) if (f.lastModified() > max) max = f.lastModified();
        return max;
    }
}
