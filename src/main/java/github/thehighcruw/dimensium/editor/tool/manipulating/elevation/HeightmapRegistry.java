/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.elevation;

import com.github.bsideup.jabel.Desugar;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.Dimensium;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;

@SideOnly(Side.CLIENT)
public class HeightmapRegistry {

    public static final HeightmapRegistry INSTANCE = new HeightmapRegistry();

    private static final String[] BUILTIN_PATHS = {
        "flat",
        "cone",
        "crater",
        "ridge",
        "plateau",
        "terrace",
        "jeracraft/Double",
        "jeracraft/Great",
        "jeracraft/Large",
        "jeracraft/Long",
        "jeracraft/Peak",
        "jeracraft/Wierd",
    };
    private static final String RESOURCE_PATH = "/assets/dimensium/heightmaps/";

    @Desugar
    public record HeightmapEntry(String name, HeightmapData data, boolean builtin) {}

    private final List<HeightmapEntry> builtins = new ArrayList<>();
    private final List<HeightmapEntry> userEntries = new ArrayList<>();

    private HeightmapRegistry() {}

    public void init() {
        builtins.clear();
        for (String resourcePath : BUILTIN_PATHS) {
            String fullPath = RESOURCE_PATH + resourcePath + ".png";
            String displayName = resourcePath.contains("/")
                    ? resourcePath.substring(resourcePath.lastIndexOf('/') + 1)
                    : resourcePath;
            try (InputStream stream = HeightmapRegistry.class.getResourceAsStream(fullPath)) {
                if (stream == null) {
                    Dimensium.logger.warn("Built-in heightmap not found: {}", fullPath);
                    continue;
                }
                builtins.add(new HeightmapEntry(displayName, HeightmapData.fromStream(displayName, stream), true));
            } catch (IOException e) {
                Dimensium.logger.warn("Failed to load built-in heightmap: {}", fullPath, e);
            }
        }
        refresh();
    }

    public void refresh() {
        userEntries.clear();
        File folder = getUserFolder();
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }
        File[] files = folder.listFiles((dir, n) -> n.toLowerCase().endsWith(".png"));
        if (files == null) return;
        for (File file : files) {
            try {
                userEntries.add(new HeightmapEntry(
                        HeightmapData.stripExtension(file.getName()), HeightmapData.fromFile(file), false));
            } catch (IOException e) {
                Dimensium.logger.warn("Failed to load user heightmap: {}", file.getName(), e);
            }
        }
    }

    public List<HeightmapEntry> getAll() {
        List<HeightmapEntry> all = new ArrayList<>(builtins.size() + userEntries.size());
        all.addAll(builtins);
        all.addAll(userEntries);
        return Collections.unmodifiableList(all);
    }

    public static File getUserFolder() {
        return new File(Minecraft.getMinecraft().mcDataDir, "dimensium/heightmaps");
    }
}
