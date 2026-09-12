/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.Dimensium;
import imgui.ImGui;

@SideOnly(Side.CLIENT)
public final class LayoutPresetRegistry {

    public static final LayoutPresetRegistry INSTANCE = new LayoutPresetRegistry();

    private static final String DIR = "dimensium_presets";
    private static final String EXT = ".ini";
    private static final String WINDOWS_MARKER = ";;DimensiumWindows;;";

    private String activePreset = null;

    private final LinkedHashMap<String, BooleanSupplier> getters = new LinkedHashMap<>();
    private final LinkedHashMap<String, Consumer<Boolean>> setters = new LinkedHashMap<>();
    private final LinkedHashMap<String, Boolean> defaults = new LinkedHashMap<>();

    private LayoutPresetRegistry() {}

    public void registerWindow(String key, BooleanSupplier getter, Consumer<Boolean> setter, boolean defaultOpen) {
        getters.put(key, getter);
        setters.put(key, setter);
        defaults.put(key, defaultOpen);
    }

    public void resetToDefaults() {
        for (Map.Entry<String, Boolean> e : defaults.entrySet()) {
            Consumer<Boolean> setter = setters.get(e.getKey());
            if (setter != null) setter.accept(e.getValue());
        }
    }

    public List<String> list() {
        File dir = new File(DIR);
        if (!dir.isDirectory()) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName()
                .endsWith(EXT)) {
                names.add(
                    f.getName()
                        .substring(
                            0,
                            f.getName()
                                .length() - EXT.length()));
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public String getActive() {
        return activePreset;
    }

    public void clearActive() {
        activePreset = null;
    }

    public void save(String name) {
        File dir = new File(DIR);
        if (!dir.exists()) dir.mkdirs();

        StringBuilder sb = new StringBuilder(ImGui.saveIniSettingsToMemory());
        sb.append('\n')
            .append(WINDOWS_MARKER)
            .append('\n');
        for (Map.Entry<String, BooleanSupplier> e : getters.entrySet()) {
            sb.append(e.getKey())
                .append('=')
                .append(
                    e.getValue()
                        .getAsBoolean())
                .append('\n');
        }

        try (FileWriter fw = new FileWriter(new File(dir, name + EXT))) {
            fw.write(sb.toString());
        } catch (IOException e) {
            Dimensium.logger.error("Failed to save layout preset '{}'", name, e);
            return;
        }
        activePreset = name;
    }

    public boolean load(String name) {
        File f = new File(DIR, name + EXT);
        if (!f.isFile()) return false;
        String content = readFile(f);
        if (content == null) return false;
        ImGui.loadIniSettingsFromMemory(content);
        applyWindowStates(content);
        activePreset = name;
        return true;
    }

    public boolean rename(String oldName, String newName) {
        File from = new File(DIR, oldName + EXT);
        File to = new File(DIR, newName + EXT);
        if (!from.isFile() || to.exists()) return false;
        boolean ok = from.renameTo(to);
        if (ok && oldName.equals(activePreset)) activePreset = newName;
        return ok;
    }

    public boolean delete(String name) {
        boolean ok = new File(DIR, name + EXT).delete();
        if (ok && name.equals(activePreset)) activePreset = null;
        return ok;
    }

    private void applyWindowStates(String content) {
        int idx = content.indexOf(WINDOWS_MARKER);
        if (idx < 0) return;
        String block = content.substring(idx + WINDOWS_MARKER.length());
        Properties props = new Properties();
        try {
            props.load(new StringReader(block));
        } catch (IOException e) {
            Dimensium.logger.error("Failed to parse window states from preset", e);
            return;
        }
        for (Map.Entry<String, Consumer<Boolean>> e : setters.entrySet()) {
            String val = props.getProperty(e.getKey());
            if (val != null) e.getValue()
                .accept(Boolean.parseBoolean(val));
        }
    }

    private static String readFile(File f) {
        try (FileReader fr = new FileReader(f)) {
            char[] buf = new char[(int) f.length()];
            int n = fr.read(buf);
            return new String(buf, 0, n);
        } catch (IOException e) {
            Dimensium.logger.error("Failed to read layout preset '{}'", f.getName(), e);
            return null;
        }
    }
}
